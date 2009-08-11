package com.spicyhorse.qa.remotemonitor;

import java.util.Map;
import java.util.TreeMap;

public class StatusMessage {

	private long id;
	private String category;
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	private String status;
	private String data;
	private Map<String,String> properties;
	
	public long getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getData() {
		return data;
	}
	public void setData(String data) {
		this.data = data;
	}
	public StatusMessage(long l, String status, String data) {
		this.id = l;
		this.status = status;
		this.data = data;
		this.properties = new TreeMap<String,String>();
	}
	public boolean isDown() {
		if (this.status != null && this.status.contains("DOWN")){
			return true ;
		}else{
			return false;	
		}
	}
	public boolean isUp() {
		return !isDown();
	}
	public String getProperty(String key) {
		if (properties.containsKey(key)){
			return (String) properties.get(key);
		}
		return null;
	}
	
	public void setProperties(String key, String value){
		this.properties.put(key, value);
	}
}
