package com.spicyhorse.qa.remotemonitor;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


import javax.swing.JOptionPane;


//TODO: return normal if status is ok! 
public class App {

	static final Logger logger = Logger.getLogger(App.class);
	static final String LOG_PROPERTIES_FILE = "log4j.properties";

	public static void main(String[] args) {

		try {
			Properties logProperties = new Properties();

			logProperties.load(new FileInputStream(LOG_PROPERTIES_FILE));
			PropertyConfigurator.configure(logProperties);
			logger.info("Logging initialized.");
		} catch (IOException e) {
			logger.error("Unable to load logging property!"+ e.getMessage());
			JOptionPane.showMessageDialog(null,
					"Unable to load logging property! \n"
							+ e.getMessage());
			
		}

		try {
			logger.info("===========   starting   ===========");

			// create an observer, let oberserver spawn child threads
			final GuiStatusMonitor handler = GuiStatusMonitor
					.getGUIStatusMonitor();
		} catch (SecurityException e) {
			logger.error("Application can not start due to security reason!"+ e.getMessage());
			JOptionPane.showMessageDialog(null,
					"Application can not start due to security reason! \n"
							+ e.getMessage());
		} catch (Exception e) {
			logger.error("Application fails!"+ e.getMessage());
			JOptionPane.showMessageDialog(null,
					"Application fails! \n"
							+ e.getMessage());
		}
	}
}
