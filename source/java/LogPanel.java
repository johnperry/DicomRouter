package org.rsna.router;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.LinkedList;
import javax.swing.*;
import org.rsna.ui.ColorPane;
import org.rsna.util.FileUtil;
import org.rsna.util.StringUtil;

public class LogPanel extends BasePanel {

	static LogPanel logPanel = null;

	JScrollPane jsp;
	JButton delete;
	JButton refresh;
	ColorPane cp;
	String margin = "          ";
	LinkedList<LogEntry> entries;
	int logDepth = 100;

	public static synchronized LogPanel getInstance() {
		if (logPanel == null) logPanel = new LogPanel();
		return logPanel;
	}

	protected LogPanel() {
		super();
		entries = new LinkedList<LogEntry>();
		Configuration.getInstance().getLogDepth();
		cp = new ColorPane();
		cp.setScrollableTracksViewportWidth(false);
		BasePanel bp = new BasePanel();
		bp.add(cp, BorderLayout.CENTER);
		jsp = new JScrollPane();
		jsp.getVerticalScrollBar().setUnitIncrement(10);
		jsp.setViewportView(bp);
		jsp.getViewport().setBackground(Color.white);
		add(jsp, BorderLayout.CENTER);
	}
	
	public synchronized void log(Color color, String text) {
		String time = StringUtil.getTime(":") + " ";
		time = time.substring(0, time.lastIndexOf("."));
		LogEntry entry = new LogEntry(color, time, text);
		entries.add(entry);
		purge();
		print(entry);
	}
	
	private void purge() {
		while (entries.size() > logDepth) {
			entries.removeFirst();
		}
	}
	
	public synchronized void refresh() {
		cp.clear();
		for (LogEntry entry : entries) {
			print(entry);
		}
	}			
	
	private synchronized void print(LogEntry e) {
		String[] lines = e.text.split("\n");
		cp.println(e.color, e.time + "  " + lines[0]);
		for (int i=1; i<lines.length; i++) {
			cp.println(e.color, margin + lines[i]);
		}
	}
	
	class LogEntry {
		Color color;
		String time;
		String text;
		public LogEntry(Color color, String time, String text) {
			this.color = color;
			this.time = time;
			this.text = text;
		}
	}
	
}
