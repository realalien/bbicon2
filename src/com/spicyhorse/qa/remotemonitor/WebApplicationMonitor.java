package com.spicyhorse.qa.remotemonitor;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

/**
 * Detect if a local or Internet web page is reachable, the first idea is to get
 * response of "200 OK!"
 * 
 * */
public class WebApplicationMonitor extends MonitorableTask implements Runnable {

	private String ping_ip = null;
	private String port = null;

	static Logger logger = Logger.getLogger(WebApplicationMonitor.class);
	
	public WebApplicationMonitor(long sleep, String ping_addr, String port) {
		this.thisThread = new Thread(this);
		this.sleep_timeout = sleep;
		this.ping_ip = ping_addr;
		this.port = port;
	}

	public WebApplicationMonitor(String ping_addr) {
		super();
		this.ping_ip = ping_addr;
		this.port = "80";
	}

	@Override
	public void run() {
		Thread holder = Thread.currentThread();
		try {
			while (holder == thisThread) {
				System.out.println("Web status detecting " + this.ping_ip + ":"
						+ this.port + " from Thread at interval "
						+ this.sleep_timeout + " : " + Thread.currentThread());
				boolean isOK = webappIsRunning(this.ping_ip, this.port);
				Thread.sleep(this.sleep_timeout);

				synchronized (this) {
					while (isThreadSuspended())
						Thread.sleep(this.sleep_timeout); // Notes: do not use
															// wait() as we have
															// no shared monitor
				}

				String demo_status = "";
				if (isOK) {
					demo_status = "WebStatus.DOWN of " + this.ping_ip + ":"
							+ this.port;
				} else {
					demo_status = "WebStatus.OK of " + this.ping_ip + ":"
							+ this.port;
				}

//				demo_status = "WebStatus.DOWN of " + this.ping_ip + ":"
//						+ this.port;
				logger.debug("WebStatus " + this.ping_ip + ", got " + demo_status);
				setChanged();
				notifyObservers(demo_status);
			}
		} catch (UnknownHostException e) {
			logger.error(e.getMessage());
		} catch (IOException e) {
			logger.error(e.getMessage());
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}
	}

	public static boolean webappIsRunning(String ipAddr, String port)
			throws MalformedURLException, IOException {
		boolean webappIsRunning = false;

		// Create a URLConnection object for a URL
		URL url = new URL("http://" + ipAddr + ":" + port);
		URLConnection conn = url.openConnection();

		// List all the response headers from the server.
		// Note: The first call to getHeaderFieldKey() will implicit send
		// the HTTP request to the server.
		if (conn  != null && conn.getHeaderFields().size() !=0) {
			for (int i = 0;; i++) {
				String headerName = conn.getHeaderFieldKey(i);
				String headerValue = conn.getHeaderField(i);
				logger.debug(headerName + " = " + headerValue);

				if (headerValue.contains("200 OK")) { // TODO: maybe redirected,
														// see more info
					webappIsRunning = true;
					return webappIsRunning;
				}
				if (headerName == null && headerValue == null) {
					// No more headers
					break;
				}
				if (headerName == null) {
					// The header value contains the server's HTTP version
				}
			}
		}
		return false;
	}
}
