package org.rsna.installer;

import org.rsna.installer.SimpleInstaller;

/**
 * The Anonymizer program installer, consisting of just a
 * main method that instantiates a SimpleInstaller.
 */
public class Installer {

	static String windowTitle = "DicomRouter Installer";
	static String programName = "DicomRouter";
	static String introString = "<p><b>DicomRouter</b> is a stand-alone tool for distributing "
								+ "DICOM objects to multiple destinations.</p>";

	public static void main(String args[]) {
		new SimpleInstaller(windowTitle,programName,introString);
	}
}
