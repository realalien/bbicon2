/*
 * Copyright (c) 2009, Zhu Jia Cheng
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY <copyright holder> ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <copyright holder> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
