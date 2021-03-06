/*
 * Copyright (c) 2009, Marcus Lindblom
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class BuildbotStatusMonitor extends MonitorableTask {

	private String ip = null;
	private String port = null;
	private String builder = null;
	// value in the quotation <a href="builders/periodic_builder/builds/116">
	private String partial_build_link = ""; 


	static Logger logger = Logger.getLogger(BuildbotStatusMonitor.class);

	private Matcher s_matcher = Pattern.compile(
			"(success|warn|fail|exception)",
			Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher("");

	public BuildbotStatusMonitor(long sleep, String ping_addr, String port,
			String builder) {
		this.thisThread = new Thread(this);
		this.sleep_timeout = sleep;
		this.ip = ping_addr;
		this.port = port;
		this.builder = builder;
	}

	public BuildbotStatusMonitor(String ping_addr) {
		super();
		this.ip = ping_addr;
		this.port = "9911";
	}

	@Override
	public void run() {
		Thread holder = Thread.currentThread();
		try {
			while (holder == thisThread) {
				System.out.println("Buildbot builder detecting " + this.ip
						+ ":" + this.port + " from Thread at interval "
						+ this.sleep_timeout + " : " + Thread.currentThread());
				boolean isOK = buildbotstatus(this.ip, this.port, this.builder);
				Thread.sleep(this.sleep_timeout);

				synchronized (this) {
					while (isThreadSuspended())
						Thread.sleep(this.sleep_timeout); // Notes: do not use
					// wait() as we have
					// no shared monitor
				}

				String demo_status = "";
				if (isOK) {
					demo_status = "Builder.OK of " + this.ip + ":" + this.port
							+ " " + this.builder;
					status_msg = new StatusMessage(this.thisThread.getId(),"Builder.UP", demo_status);
					status_msg.setCategory(BuildbotStatusMonitor.class.getName());
					logger
							.debug("goes to ok, builder's status: "
									+ this.status + "link:" + this.partial_build_link);

				} else {
					demo_status = "Builder.DOWN of " + this.ip + ":"
							+ this.port + " " + this.builder;
					
					logger.debug("goes to error, builder's status: "
							+ this.status);
					status_msg = new StatusMessage(this.thisThread.getId(),"Builder.DOWN", demo_status);
					  status_msg.setCategory(BuildbotStatusMonitor.class.getName());
					  
				}
				status_msg.setProperties("build_link", this.partial_build_link);  //ESP: if note set in computeBuildStatus(), use this line
				logger.debug("Builder status: " + this.ip + ", got "
						+ demo_status + "link:" + this.partial_build_link);
				setChanged();
				notifyObservers(status_msg);
			}
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}
	}

	/**
	 * To return T/F for thread's run method, SUG: to implemented as an interface.
	 * */
	private boolean buildbotstatus(String pingIp, String port2, String builder2) {
		try {
			this.computeBuildStatus();
		} catch (Exception e) {
			logger.error("BuildbotStatusMonitor caught an exception. Error: \n"
					+ e.getMessage());
			return false ;
		}
		if (this.status == null || this.status.toString() != "SUCCESS") {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Compute build status by searching the HTML page for keywords
	 * */
	private void computeBuildStatus() throws Exception {
		URL url = new URL("http://" + this.ip + ":" + this.port
				+ "/one_box_per_builder?" + this.builder);
		logger.debug("url: " + url.toString());
		BufferedReader in = new BufferedReader(new InputStreamReader(url
				.openStream()));
		String l;
		Status new_status = null;

		while ((l = in.readLine()) != null) {
			s_matcher.reset(l);
			while (s_matcher.find()) {
				//logger.debug("line and builder found " + this.builder );
				if (l.toLowerCase().contains(this.builder.toLowerCase())) {
					//logger.debug("line info:" +l + " \n match info:" + s_matcher.group(1));
					this.partial_build_link = getPartialLink(l);
					logger.debug("try to extract partial linke: from : " + l + " got: " + this.partial_build_link);
					String s = s_matcher.group(1);
					for (Status st : Status.values()) {
						if (s.equalsIgnoreCase(st.name())) {
							new_status = st;
						}
					}
				}
			}
		}

		in.close();

		this.status = new_status;
		logger.debug("computerBuildStatus for judging ...." + this.status.toString());
	}
	
	/**
	 * To extract partial link of build from a line of HTML
	 * */ 
	private String getPartialLink(String str){

		String partial_link = null ;
		Pattern p = Pattern.compile("<a href=\"(.*)\">");
		Matcher m = p.matcher(str);
		if (m.find()){
			//logger.debug("str to match: " + str);
			//logger.debug("match info:" + m.group(0));
			partial_link  = m.group(0).substring(9, m.group(0).length()-2);
			
			logger.debug("partial link of the build: "+partial_link);
		}
		return partial_link;
	}

}

enum Status {
	SUCCESS, WARN, FAIL, EXCEPTION, CONNECT_ERROR
}