package org.rsna.router;

import java.io.*;
import org.rsna.util.FileUtil;
import org.rsna.util.HtmlUtil;
import org.rsna.util.StringUtil;
import org.rsna.util.XmlUtil;
import org.rsna.servlets.Servlet;
import org.rsna.server.HttpRequest;
import org.rsna.server.HttpResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The RouterServlet.
 * This servlet provides a browser-accessible user interface for
 * viewing the status of the router.
 */
public class RouterServlet extends Servlet {

	/**
	 * Construct a RouterServlet.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public RouterServlet(File root, String context) {
		super(root, context);
	}

	/**
	 * The servlet method that responds to an HTTP GET.
	 * If called with no file path, this method returns an
	 * HTML page listing the files in the logs directory in reverse
	 * chronological order. Each filename is a link to display the file's
	 * contents. If called with a file path, this method returns
	 * the contents of the file in an HTML page.
	 * @param req The HttpServletRequest provided by the servlet container.
	 * @param res The HttpServletResponse provided by the servlet container.
	 */
	public void doGet( HttpRequest req, HttpResponse res ) {
		res.setContentEncoding(req);
		res.disableCaching();
		try {
			Configuration config = Configuration.getInstance();
			Document doc = XmlUtil.getDocument();
			Element root = doc.createElement("Status");
			doc.appendChild(root);
			root.setAttribute("ip", config.getStoreIPAddress());
			root.setAttribute("port", config.getStorePort());
			root.setAttribute("ae", config.getStoreAETitle());
			
			Queue[] queues = config.getQueues();
			for (Queue q : queues) {
				Element e = doc.createElement("Queue");
				e.setAttribute("name", q.name);
				e.setAttribute("priority", ""+q.priority);
				e.setAttribute("destination", q.destination);
				e.setAttribute("size", ""+q.getSize());
				e.setAttribute("count", ""+q.getTransmittedFileCount());
				root.appendChild(e);
			}
			Document xsl = getDocument( "RouterServlet.xsl" );
			String page = XmlUtil.getTransformedText(doc, xsl, null);
			res.write( page );
			res.disableCaching();
			res.setContentType("html");
		}
		catch (Exception unable) {
			unable.printStackTrace();
			res.setResponseCode(res.servererror);
		}
		res.send();
	}
	
	private Document getDocument(String name) throws Exception {
		File file = new File(root, name);
		InputStream in = FileUtil.getStream(file, "/"+name);
		return XmlUtil.getDocument(in);
	}
}
