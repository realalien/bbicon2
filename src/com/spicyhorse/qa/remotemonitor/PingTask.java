package com.spicyhorse.qa.remotemonitor;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

// REF: thread processing, see: http://java.sun.com/j2se/1.4.2/docs/guide/misc/threadPrimitiveDeprecation.html

//TODO: use enum instead of string
public class PingTask extends MonitorableTask {

	private String ip = null ;
	
	static Logger logger = Logger.getLogger(PingTask.class);
	public PingTask(long sleep, String ping_addr){
		this.thisThread = new Thread(this);
		this.sleep_timeout = sleep ;
		this.ip = ping_addr ;
	}
	
	public PingTask(String ping_addr){
		super(); 
		this.ip = ping_addr ;
	}
	@Override
	public void run() {
		Thread holder = Thread.currentThread();
		try {
			while(holder == this.thisThread){
				logger.debug("Pinging " + this.ip +" from Thread at interval " + this.sleep_timeout +" : " + Thread.currentThread().getId());
				boolean isOK = ping(this.ip);
				Thread.sleep(this.sleep_timeout) ;
				
				synchronized(this) {
	                while (isThreadSuspended())
	                    Thread.sleep(this.sleep_timeout);// Notes: do not use wait() as we have no shared monitor
	            }
				String demo_status = "";
				if(isOK){
					demo_status = "PingTask.OK of " + this.ip ;
				}else{
					demo_status = "PingTask.DOWN of " + this.ip ;
				}
				logger.debug("Ping " + this.ip + ", got " + demo_status);
				setChanged();
				notifyObservers(demo_status);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static boolean ping(String name) throws UnknownHostException, IOException{
		InetAddress machine = InetAddress.getByName(name); 
		return machine.isReachable(10000);
	}
}
