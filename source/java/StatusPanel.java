package org.rsna.router;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Hashtable;
import javax.swing.*;
import org.rsna.ui.RowLayout;
import org.rsna.util.FileUtil;
import org.rsna.util.StringUtil;

public class StatusPanel extends BasePanel {

	static StatusPanel instance = null;

	JScrollPane jsp;
	Color bg = Color.white;

	Font mono = new java.awt.Font( "Monospaced", java.awt.Font.BOLD, 16 );
	Font b18 = new java.awt.Font( "SansSerif", java.awt.Font.BOLD, 24 );
	Font b12 = new java.awt.Font( "SansSerif", java.awt.Font.BOLD, 16 );
	Font p12 = new java.awt.Font( "SansSerif", java.awt.Font.PLAIN, 16 );
	
	Configuration config;
	
	Hashtable<Queue, JLabel> sizes;

	public static synchronized StatusPanel getInstance() {
		if (instance == null) instance = new StatusPanel();
		return instance;
	}

	protected StatusPanel() {
		super();
		config = Configuration.getInstance();
		sizes = new Hashtable<Queue, JLabel>();
		MainPanel main = new MainPanel();
		jsp = new JScrollPane();
		jsp.getVerticalScrollBar().setUnitIncrement(10);
		jsp.setViewportView(main);
		jsp.getViewport().setBackground(bg);
		add(jsp, BorderLayout.CENTER);
	}
	
	public synchronized void update() {
		Runnable r = new Runnable() {
			public void run() {
				for (Queue q : sizes.keySet()) {
					JLabel size = sizes.get(q);
					size.setText(""+q.getSize());
				}
			}
		};
		SwingUtilities.invokeLater(r);
	}
	
	class MainPanel extends JPanel {
		public MainPanel() {
			super();
			setLayout(new FlowLayout(FlowLayout.CENTER));
			setBackground(bg);
			Box vbox = Box.createVerticalBox();
			vbox.setBackground(bg);
			
			HeadingsPanel headingsPanel = new HeadingsPanel();
			Box hbox = Box.createHorizontalBox();
			hbox.add(headingsPanel);
			vbox.add(hbox);
			
			vbox.add(Box.createVerticalStrut(15));
			
			TablePanel tablePanel = new TablePanel();
			hbox = Box.createHorizontalBox();
			hbox.add(tablePanel);
			vbox.add(hbox);

			add(vbox);
		}
	}
	
	class HeadingsPanel extends JPanel {
		public HeadingsPanel() {
			setLayout(new RowLayout());
			setBackground(bg);

			//space down a little
			add(Box.createVerticalStrut(10));
			add(RowLayout.crlf());

			//put in the title
			add(new XLabel("Queue Status", b18, 0.5f), RowLayout.span(2));
			add(RowLayout.crlf());

			//space down a little
			add(Box.createVerticalStrut(10));
			add(RowLayout.crlf());

			//insert the SCP IP Address
			add(new XLabel("SCP IP Address:", p12, 1.0f));
			add(new XLabel(config.getStoreIPAddress(), b12, 0.0f));
			add(RowLayout.crlf());

			//insert the port
			add(new XLabel("SCP Port:", p12, 1.0f));
			add(new XLabel(config.getStorePort(), b12, 0.0f));
			add(RowLayout.crlf());

			//insert the AE Title
			add(new XLabel("SCP AE Title:", p12, 1.0f));
			add(new XLabel(config.getStoreAETitle(), b12, 0.0f));
			add(RowLayout.crlf());
		}
	}
	
	class TablePanel extends JPanel {
		public TablePanel() {
			setLayout(new RowLayout());
			setBackground(bg);

			//put in the title row
			add(new XLabel("Name", b12, 0.0f));
			add(new XLabel("Priority", b12, 0.5f));
			add(new XLabel("Destination", b12, 0.0f));
			add(new XLabel("Size", b12, 0.5f));
			add(RowLayout.crlf());
			
			//now put in the data rows
			for (Queue q : config.getQueues())  {
				add(new XLabel(q.getName(), b12, 0.0f));
				add(new XLabel(""+q.getPriority(), p12, 0.5f));
				add(new XLabel(q.getDestination(), mono, 0.0f));
				
				//put the size label in the hashtable for later updating
				XLabel size = new XLabel(""+q.getSize(), p12, 0.5f);
				sizes.put(q, size);
				add(size);
				
				add(RowLayout.crlf());
			}
		}
	}
	
	class XLabel extends JLabel	{
		public XLabel(String text, Font font, float x) {
			super(text);
			setFont(font);
			setAlignmentX(x);
		}
	}
}
		
		
	
