package com.chatnetwork.app.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;



public class Config {
	private String caUrl;
	private String orgName;
	private String orgMSP;
	private String adminAccount;
	private String adminPassword;
	private String peerName;
	private String peerUrl;
	private String eventhubUrl;
	private String ordererName;
	private String ordererUrl;
	public static void main(String[] args) {
		Config config = newConfig();
		Gson gson = new Gson();
		String temp = gson.toJson(config);
		Logger.getLogger(Config.class.getName()).log(Level.INFO, "config's content: "+temp);
	}
	
	static public Config newConfig()
	{
		Config config = new Config();
		Gson gson = new Gson();
		try {
		  InputStream stream = new FileInputStream("Config.json");
		  JsonReader reader = new JsonReader(new InputStreamReader(stream, "UTF-8"));
		  // Read file in stream mode
		  config = gson.fromJson(reader, Config.class);
		  reader.close();
		} catch (UnsupportedEncodingException ex) {
		  ex.printStackTrace();
		} catch (IOException ex) {
		  ex.printStackTrace();
		}
		
		return config;
	}

	public String getCAUrl() {
		return caUrl;
	}

	public void setCAUrl(String caUrl) {
		this.caUrl = caUrl;
	}

	public String getOrgName() {
		return orgName;
	}

	public void setOrgName(String orgName) {
		this.orgName = orgName;
	}

	public String getOrgMSP() {
		return orgMSP;
	}

	public void setOrgMSP(String orgMSP) {
		this.orgMSP = orgMSP;
	}

	public String getAdminAccount() {
		return adminAccount;
	}

	public void setAdminAccount(String adminAccount) {
		this.adminAccount = adminAccount;
	}

	public String getAdminPassword() {
		return adminPassword;
	}

	public void setAdminPassword(String adminPassword) {
		this.adminPassword = adminPassword;
	}

	public String getPeerName() {
		return peerName;
	}

	public void setPeerName(String peerName) {
		this.peerName = peerName;
	}

	public String getPeerUrl() {
		return peerUrl;
	}

	public void setPeerUrl(String peerUrl) {
		this.peerUrl = peerUrl;
	}

	public String getEventhubUrl() {
		return eventhubUrl;
	}

	public void setEventhubUrl(String eventhubUrl) {
		this.eventhubUrl = eventhubUrl;
	}

	public String getOrdererName() {
		return ordererName;
	}

	public void setOrdererName(String ordererName) {
		this.ordererName = ordererName;
	}

	public String getOrdererUrl() {
		return ordererUrl;
	}

	public void setOrdererUrl(String ordererUrl) {
		this.ordererUrl = ordererUrl;
	}

}
