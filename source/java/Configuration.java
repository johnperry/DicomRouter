package org.rsna.router;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.log4j.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.rsna.ui.ApplicationProperties;
import org.rsna.util.IPUtil;
import org.rsna.util.StringUtil;
import org.rsna.util.XmlUtil;

/**
 * The DicomRouter configuration.
 */
public class Configuration {
	
	static final Logger logger = Logger.getLogger(Configuration.class);
    public static Color background = Color.getHSBColor(0.5833f, 0.17f, 0.95f);
	
	private static Configuration instance = null;
	
    static final File configFile = new File("config.xml");
    static final File propsFile = new File("program.properties");
    static final File helpfile = new File("help.html");
    
    ApplicationProperties props;

	boolean autodetect;
	String storeIPAddress;
	String storePort;
	String storeAETitle;
	Queue[] queues;
	int logDepth = 100;
	int httpPort = 0;

	/**
	 * Get the singleton instance of the Configuration, instantiating it if necessary.
	 */
	public static Configuration getInstance() {
		if (instance == null) {
			try { instance = new Configuration(); }
			catch (Exception ex) { ex.printStackTrace(); }
		}
		return instance;
	}

	/**
	 * Class constructor; loads the configuration from the XML config file.
	 * @param file the configuration file.
	 */
	protected Configuration() throws Exception {
		props = new ApplicationProperties(propsFile);
		Document doc = XmlUtil.getDocument(configFile);
		String configString = "Configuration:\n" + XmlUtil.toPrettyString(doc);
		logger.info(configString);
		
		Element root = doc.getDocumentElement();

		//Set the log depth and HTTP server port
		logDepth = StringUtil.getInt(root.getAttribute("logDepth"), 100);
		httpPort = StringUtil.getInt(root.getAttribute("httpPort"), 0);

		//Set the SCP parameters
		NodeList nl = root.getElementsByTagName("scp");
		Element scp = (Element)nl.item(0);
		storeIPAddress = scp.getAttribute("ipaddress").trim();
		storePort = scp.getAttribute("port").trim();
		storeAETitle = scp.getAttribute("aetitle");
		if (storeIPAddress.equals("autodetect"))
			storeIPAddress = IPUtil.getIPAddress();

		//Create the queues
		ArrayList<Queue> queueList = new ArrayList<Queue>();
		nl = root.getElementsByTagName("queue");
		for (int i=0; i<nl.getLength(); i++) {
			Element q = (Element)nl.item(i);
			String name = q.getAttribute("name").trim();
			String destination = q.getAttribute("destination").trim();
			int priority = StringUtil.getInt(q.getAttribute("priority").trim(), 0);
			NodeList qnl = q.getElementsByTagName("script");
			Element s = (Element)qnl.item(0);
			String script = (s != null) ? s.getTextContent() : "";
			queueList.add(new Queue(name, priority, destination, script));
		}
		queues = new Queue[queueList.size()];
		queues = queueList.toArray(queues);
		Arrays.sort(queues);
	}

	/**
	 * Get the log depth.
	 * @return the depth of the log buffer.
	 */
	public int getLogDepth() {
		return logDepth;
	}

	/**
	 * Determine whether theprogram can run as a service.
	 * @return true if the program can as a service (requires a servicePort);
	 * false otherwise.
	 */
	public boolean canRunAsService() {
		return (httpPort != 0);
	}

	/**
	 * Get the port of the server if running as a service.
	 * @return the server port.
	 */
	public int getHttpPort() {
		return httpPort;
	}

	/**
	 * Get the IP address of the DICOM storage SCP.
	 * @return the IP address of the DICOM storage SCP.
	 */
	public String getStoreIPAddress() {
		return storeIPAddress;
	}

	/**
	 * Get the port of the DICOM storage SCP.
	 * @return the port of the DICOM storage SCP.
	 */
	public String getStorePort() {
		return storePort;
	}

	/**
	 * Get the port of the DICOM storage SCP.
	 * @return the port of the DICOM storage SCP.
	 */
	public int getStorePortInt() {
		try { return Integer.parseInt(storePort); }
		catch (Exception useDefault) { return 104; }
	}

	/**
	 * Get the AE title of the DICOM storage SCP.
	 * @return the AE title of the DICOM storage SCP.
	 */
	public String getStoreAETitle() {
		return storeAETitle;
	}

	/**
	 * Get the array of queues.
	 */
	public Queue[] getQueues() {
		return queues;
	}

	//Manage the program pedrsistent properties
	public ApplicationProperties getProps() {
		return props;
	}

	public void put(String key, String value) {
		props.setProperty(key, value);
	}

	public String get(String key) {
		return props.getProperty(key);
	}

	public void store() {
		props.store();
	}

}