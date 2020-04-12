package org.rsna.router;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import org.apache.log4j.*;
import org.rsna.server.*;
import org.rsna.servlets.*;
import org.rsna.ui.ApplicationProperties;
import org.rsna.util.Cache;
import org.rsna.util.FileUtil;
import org.rsna.util.StringUtil;

/**
 * The DicomRouter program base class.
 */
public class DicomRouter extends JFrame implements TransferListener, ChangeListener {

	static Logger logger;
	
    String windowTitle = "DicomRouter - version 2";
    
	JTabbedPane tabbedPane;
	FooterPanel footerPanel;
	StatusPanel statusPanel;
	LogPanel logPanel;
	HtmlJPanel helpPanel;
	StorageSCP storageSCP;
	ExportService exportService = null;
	
	static Configuration config;
	static boolean runningAsService = false;
	
	/**
	 * The startup method of the DicomRouter program.
	 * This method is used when running the program as a Windows service.
	 * It does not return until the stopService method is called
	 * independently by the service manager.
	 * @param args the command line arguments
	 */
	public static void startService(String[] args) {
		System.out.println("Start [ServiceManager]");
		if (Configuration.getInstance().canRunAsService()) {
			runningAsService = true;
			main(args);
			while (runningAsService) {
				try { Thread.sleep(2000); }
				catch (Exception ignore) { }
			}
		}
		else System.out.println("Unable to start as a service (port==0)");
		System.out.println("Stop [ServiceManager]");
	}

	/**
	 * The shutdown method of the DicomRouter program.
	 * This method is used when running the program as a Windows service.
	 * @param args the command line arguments
	 */
	public static void stopService(String[] args) {
		runningAsService = false;
	}

	/**
	 * The main method to start the program.
	 * @param args the list of arguments from the command line.
	 */
    public static void main(String args[]) {
		//Initialize Log4J
		File logs = new File("logs");
		logs.mkdirs();
		for (File f : logs.listFiles()) FileUtil.deleteAll(f);
		File logProps = new File("log4j.properties");
		String propsPath = logProps.getAbsolutePath();
		if (!logProps.exists()) {
			System.out.println("Logger configuration file: "+propsPath);
			System.out.println("Logger configuration file not found.");
		}
		PropertyConfigurator.configure(propsPath);
		logger = Logger.getLogger(DicomRouter.class);
		
		//Figure out how to start
		config = Configuration.getInstance();
		if (config.canRunAsService() && runningAsService) {
			//Start the threads
			StorageSCP.getInstance().startSCP();
			ExportService.getInstance().start();
			if (!startHttpServer()) runningAsService = false;
		}
		else {
			//Run with a UI
			new DicomRouter();
		}
    }
    
    static boolean startHttpServer() {
		//Create the ServletSelector for the HttpServer
		boolean requireAuthentication = false;
		File root = new File("ROOT");
		root.mkdirs();
		ServletSelector selector = 
				new ServletSelector(root , requireAuthentication);

		//Add in the servlets
		selector.addServlet("login",		LoginServlet.class);
		selector.addServlet("users",		UserManagerServlet.class);
		selector.addServlet("router",		RouterServlet.class);
		selector.addServlet("logs",			LogServlet.class);
		selector.addServlet("ping",			PingServlet.class);
		selector.addServlet("svrsts",		ServerStatusServlet.class);
		selector.addServlet("attacklog",	AttackLogServlet.class);

		//Instantiate the singleton Users class
		Users users = Users.getInstance("org.rsna.server.UsersXmlFileImpl", null);
		
		//Set the session timeout
		Authenticator authenticator = Authenticator.getInstance();
		authenticator.setSessionTimeout( 12 * 60 * 60 * 1000 ); //12 hrs
		authenticator.setSessionCookieName("DICOMROUTERSESSION");

		//Instantiate the server.
		int port = config.getHttpPort();
		boolean ssl = false;
		int maxThreads = 4;
		HttpServer httpServer = null;
		try { 
			httpServer = new HttpServer(ssl, port, maxThreads, selector);
			httpServer.start();
			return true;
		}
		catch (Exception ex) {
			System.out.println("Unable to instantiate the HTTP Server on port "+port);
			logger.error("Unable to instantiate the HTTP Server on port "+port, ex);
			return false;
		}
	}

	/**
	 * Class constructor; creates the program main class.
	 */
    public DicomRouter() {
		super();
		Cache cache = Cache.getInstance(new File("CACHE"));
		cache.clear();

		setLayout(new BorderLayout());
		setTitle(windowTitle);
		
		config = Configuration.getInstance();
		addWindowListener(new WindowCloser(this));
		
		statusPanel = new StatusPanel();
		logPanel = LogPanel.getInstance();
		helpPanel = new HtmlJPanel(config.helpfile);
		footerPanel = new FooterPanel();

		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.addTab("Status", statusPanel);
		tabbedPane.addTab("Event Log", logPanel);
		tabbedPane.addTab("Help", helpPanel);
		tabbedPane.addChangeListener(this);
		tabbedPane.setSelectedIndex(0);
		this.add(tabbedPane, BorderLayout.CENTER);
		this.add(footerPanel, BorderLayout.SOUTH);
		
		//Create and initialize the processing elements.
		storageSCP = StorageSCP.getInstance();
		exportService = ExportService.getInstance();
		storageSCP.addTransferListener(this);
		exportService.addTransferListener(this);

		statusPanel.update();
		
		pack();
		positionFrame();
		setVisible(true);
		
		//Start the subordinate threads.
		storageSCP.startSCP();
		exportService.start();
		startHttpServer();
    }
    
	class FooterPanel extends JPanel {
		JLabel status;
		public FooterPanel() {
			super();
			this.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
			this.setLayout(new FlowLayout(FlowLayout.LEFT));
			this.setBackground(config.background);
			status = new JLabel("Ready");
			Font font = new Font(status.getFont().getFontName(),Font.BOLD,14);
			status.setFont(font);
			this.add(status);
		}
		public void setText(String text) {
			text = StringUtil.getTime(":") + ": " + text;
			if (SwingUtilities.isEventDispatchThread()) status.setText(text);
			else {
				final String t = text;
				Runnable r = new Runnable() {
					public void run() { status.setText(t); }
				};
				SwingUtilities.invokeLater(r);
			}
		}
	}

	public void stateChanged(ChangeEvent event) {
		Component c = tabbedPane.getSelectedComponent();
		if (c.equals(logPanel)) logPanel.refresh();
	}
	
    public void attention(TransferEvent event) {
		String message = event.message;
		if (message != null) footerPanel.setText(message);
		statusPanel.update();
	}

    class WindowCloser extends WindowAdapter {
		JFrame parent;
		public WindowCloser(JFrame parent) {
			this.parent = parent;
		}
		public void windowClosing(WindowEvent evt) {
			Configuration config = Configuration.getInstance();
			Point p = getLocation();
			config.put("x", Integer.toString(p.x));
			config.put("y", Integer.toString(p.y));
			Toolkit t = getToolkit();
			Dimension d = parent.getSize ();
			config.put("w", Integer.toString(d.width));
			config.put("h", Integer.toString(d.height));
			config.store();
			System.exit(0);
		}
    }

	private void positionFrame() {
		Configuration config = Configuration.getInstance();
		int x = StringUtil.getInt( config.get("x"), 0 );
		int y = StringUtil.getInt( config.get("y"), 0 );
		int w = StringUtil.getInt( config.get("w"), 0 );
		int h = StringUtil.getInt( config.get("h"), 0 );
		boolean noProps = ((w == 0) || (h == 0));
		int wmin = 800;
		int hmin = 600;
		if ((w < wmin) || (h < hmin)) {
			w = wmin;
			h = hmin;
		}
		if ( noProps || !screensCanShow(x, y) || !screensCanShow(x+w-1, y+h-1) ) {
			Toolkit t = getToolkit();
			Dimension scr = t.getScreenSize ();
			x = (scr.width - wmin)/2;
			y = (scr.height - hmin)/2;
			w = wmin;
			h = hmin;
		}
		setSize( w, h );
		setLocation( x, y );
	}

	private boolean screensCanShow(int x, int y) {
		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] screens = env.getScreenDevices();
		for (GraphicsDevice screen : screens) {
			GraphicsConfiguration[] configs = screen.getConfigurations();
			for (GraphicsConfiguration gc : configs) {
				if (gc.getBounds().contains(x, y)) return true;
			}
		}
		return false;
	}
}
