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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class BuildbotStatusMonitor extends MonitorableTask {

	private String ip = null;
	private String port = null;
	private String builder = null ;
	private Status status;

	static Logger logger = Logger.getLogger(BuildbotStatusMonitor.class);

    private static Matcher s_matcher = Pattern.compile(
            "(success|warn|fail|exception)",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher("");
    
	public BuildbotStatusMonitor(long sleep, String ping_addr, String port, String builder) {
		this.thisThread = new Thread(this);
		this.sleep_timeout = sleep;
		this.ip = ping_addr;
		this.port = port;
		this.builder = builder;
	}

	public BuildbotStatusMonitor(String ping_addr) {
		super();
		this.ip = ping_addr;
		this.port = "80";
	}


	@Override
	public void run() {
		Thread holder = Thread.currentThread();
		try {
			while (holder == thisThread) {
				System.out.println("Web status detecting " + this.ip + ":"
						+ this.port + " from Thread at interval "
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
					demo_status = "WebStatus.DOWN of " + this.ip + ":"
							+ this.port;
				} else {
					demo_status = "WebStatus.OK of " + this.ip + ":"
							+ this.port;
				}

//				demo_status = "WebStatus.DOWN of " + this.ping_ip + ":"
//						+ this.port;
				logger.debug("WebStatus " + this.ip + ", got " + demo_status);
				setChanged();
				notifyObservers(demo_status);
			}
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}

	}

	private boolean buildbotstatus(String pingIp, String port2, String builder2) {
		// TODO Auto-generated method stub
		try{ this.computeBuildStatus();}catch (Exception e){
			logger.error("BuildbotStatusMonitor caught an exception.\n" + e.getMessage());
		}
		if (this.status == null || this.status.toString() != "SUCCESS"){
			return false ;
		}else{
			return true;
		}
	}
	
	
    /// Compute build status by searching the HTML page for keywords
	// we only use 
    private void computeBuildStatus() throws Exception  {
           URL url = new URL("http://"+this.ip+":"+this.port);
    	
            BufferedReader in = new BufferedReader(new InputStreamReader(url
                            .openStream()));
            String l;
            Status new_status = null;

            while ((l = in.readLine()) != null) {
                    s_matcher.reset(l);

                    while (s_matcher.find()) {
                            String s = s_matcher.group(1);

                            for (Status st : Status.values()) {
                                    if ((new_status == null || st.ordinal() > new_status
                                                    .ordinal())
                                                    && s.equalsIgnoreCase(st.name())) {
                                            new_status = st;
                                    }
                            }
                    }
            }

            in.close();

            this.status = new_status;
    }

}
enum Status {
    SUCCESS, WARN, FAIL, EXCEPTION, CONNECT_ERROR
}