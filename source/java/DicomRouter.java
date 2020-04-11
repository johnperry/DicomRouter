package org.rsna.router;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import org.apache.log4j.*;
import org.rsna.ui.ApplicationProperties;
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

	Configuration config;
	
	/**
	 * The main method to start the program.
	 * @param args the list of arguments from the command line.
	 */
    public static void main(String args[]) {
		new DicomRouter();
    }

	/**
	 * Class constructor; creates the program main class.
	 */
    public DicomRouter() {
		super();
		setLayout(new BorderLayout());
		setTitle(windowTitle);
		
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
