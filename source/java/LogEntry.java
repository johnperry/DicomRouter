package org.rsna.router;

import java.awt.Color;

public class LogEntry {
	Color color;
	String time;
	String text;
	static String margin = "          ";
	
	public LogEntry(Color color, String time, String text) {
		this.color = color;
		this.time = time;
		this.text = text;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		String[] lines = text.split("\n");
		sb.append(time + "  " + lines[0] + "\n");
		for (int i=1; i<lines.length; i++) {
			sb.append(LogEntry.margin + lines[i] + "\n");
		}
		return sb.toString();
	}
}
