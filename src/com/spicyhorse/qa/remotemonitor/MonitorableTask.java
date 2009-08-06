package com.spicyhorse.qa.remotemonitor;
import java.util.Observable;


public abstract class MonitorableTask extends Observable implements Runnable{
	protected long sleep_timeout;  // in milli-second
	protected double present_status;
	protected boolean is_verbose ;
	
	protected volatile Thread thisThread ;
	protected boolean isThreadSuspended = false ;

	/** Constructor */
	public MonitorableTask(){
		is_verbose = true ;
		sleep_timeout = 5000 ;
		System.out.println("setting frequency in base class to 5000");
	}
	
	public boolean isThreadSuspended() {
		return isThreadSuspended;
	}
	
	protected Thread getThisThread() {
		return thisThread;
	}
	
	protected void setThisThread(Thread pingThread) {
		this.thisThread = pingThread;
	}
	
	public void start(){
		thisThread.start();
	}

	public synchronized void stop(){
		thisThread = null ;
	}
	
	public void suspend(){
		this.isThreadSuspended = true ;
	}
	
	public void resume(){
		this.isThreadSuspended = false ;
	}
	
	
	// enforce the subclass to construct the status entity(string, Data)
	public StatusInfo makeStatusObject(String status, Object data){
		return new StatusInfo(status, data);
	}

	// how to enforce subclass of MonitorableTask to use setChange() and notify() 
	// in its run() method?
//	protected void observable_run(){
//		// if initialized
//		run();
//	}
//	
//	protected void addObserver(GuiStatusMonitor mon){
//		super.addObserver(mon);
//	}
	
}

