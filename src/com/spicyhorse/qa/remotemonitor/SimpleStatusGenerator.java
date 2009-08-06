package com.spicyhorse.qa.remotemonitor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Random;

public class SimpleStatusGenerator extends MonitorableTask implements Runnable {

	final String arr[] = { "DOWN", "RUNNING", "SUSPENDED", "PAUSED" };

	public void run() {
		String old_status = null;
		while (true) {
			// simulate a status change
			Random ran = new Random();

			String new_status = arr[ran.nextInt(arr.length)];
			// new_status = get_status();

			//System.out.println("from SimpleStatusGenerator source: new status :" + new_status);
			if (new_status != old_status) {
				System.out.printf("from SimpleStatusGenerator source: status changed! old:["
						+ old_status + "] => new:[" + new_status + "]\n");
				setChanged();
				notifyObservers(new_status);
				old_status = new_status;
			}
			try {
				Thread.sleep(this.sleep_timeout);
			} catch (InterruptedException e) {
				System.out.println("from SimpleStatusGenerator source: RemoteStatusSource was interrupted");
			}
		}
	}

	public static void main(String args[]) {
		(new Thread(new SimpleStatusGenerator())).start();
	}

	// public static Enum<Status> get_status() throws UnknownHostException,
	// IOException {
	//
	// if (ping("192.168.6.111") == false) {
	// return Status.NETWORK_DOWN;
	// } else {
	// System.out.println("network ok, ping back ok!");
	// }
	//
	// if (!webappIsRunning("192.168.6.111:80")) {
	// return Status.BUGZILLA_DOWN;
	// } else {
	// System.out.println("Bugzilla ok, response 200 ok!");
	// }
	//
	// return Status.OK;
	//
	// }

	// public static String get_status() throws UnknownHostException,
	// IOException {
	//
	// if (ping("192.168.6.111") == false) {
	// return "ping DOWN";
	// } else {
	// System.out.println("network ok, ping back ok!");
	// }
	//
	// // if (!webappIsRunning("192.168.6.111:80")) {
	// // return Status.BUGZILLA_DOWN;
	// // } else {
	// // System.out.println("Bugzilla ok, response 200 ok!");
	// // }
	//
	// return Status.OK;
	//
	// }

	/**
	 * @throws UnknownHostException, IOException
	 * */
	public static boolean ping(String name) throws UnknownHostException,
			IOException {
		InetAddress machine = InetAddress.getByName(name);
		return machine.isReachable(5000);
	}

	public static boolean webappIsRunning(String ipAddr)
			throws MalformedURLException, IOException {
		boolean webappIsRunning = false;

		// Create a URLConnection object for a URL
		URL url = new URL("http://" + ipAddr);
		URLConnection conn = url.openConnection();

		// List all the response headers from the server.
		// Note: The first call to getHeaderFieldKey() will implicit send
		// the HTTP request to the server.
		for (int i = 0;; i++) {
			String headerName = conn.getHeaderFieldKey(i);
			String headerValue = conn.getHeaderField(i);
			System.out.println(headerName + " = " + headerValue);

			if (headerValue.contains("200 OK")) {
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
		return false;
	}
}
