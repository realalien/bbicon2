package com.spicyhorse.qa.remotemonitor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.log4j.Logger;

// REF: thread processing, see: http://java.sun.com/j2se/1.4.2/docs/guide/misc/threadPrimitiveDeprecation.html

//TODO: use enum instead of string
public class BuildbotTask extends MonitorableTask {

	private String ping_ip = null;

	static Logger logger = Logger.getLogger(BuildbotTask.class);

	public BuildbotTask(long sleep, String ping_addr) {
		this.thisThread = new Thread(this);
		this.sleep_timeout = sleep;
		this.ping_ip = ping_addr;
	}

	public BuildbotTask(String ping_addr) {
		super();
		this.ping_ip = ping_addr;
	}

	@Override
	public void run() {
		Thread holder = Thread.currentThread();
		try {
			while (holder == this.thisThread) {
				logger.debug("Pinging " + this.ping_ip
						+ " from Thread at interval " + this.sleep_timeout
						+ " : " + Thread.currentThread().getId());
				boolean isOK;
				isOK = isOK(this.ping_ip);

				Thread.sleep(this.sleep_timeout);

				synchronized (this) {
					while (isThreadSuspended())
						Thread.sleep(this.sleep_timeout);// Notes: do not use
															// wait() as we have
															// no shared monitor
				}
				String demo_status = "";
				if (isOK) {
					demo_status = "BuildbotTask.DOWN of " + this.ping_ip;
				} else {
					demo_status = "BuildbotTask.OK of " + this.ping_ip;
				}
				logger.debug("Ping " + this.ping_ip + ", got " + demo_status);
				
				//Q: how to move the following the code to super class to make it call always. 
				setChanged();
				notifyObservers(makeStatusObject(demo_status,demo_status));
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
			// } catch (UnknownHostException e) {
			// e.printStackTrace();
			// } catch (IOException e) {
			// e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	// Problem is:
	// * we also need more information passed to the GUI, not just a string of
	// status.

	public static boolean isOK(String name) throws ClassNotFoundException,
			SQLException { // throws UnknownHostException, IOException
		// periodically read the database for last status record.
		Class.forName("com.mysql.jdbc.Driver");
		String url = "jdbc:mysql://localhost:3306/mysql";
		Connection con = DriverManager.getConnection(url, "root", "mes0Spicy");

		return false;
	}

}
