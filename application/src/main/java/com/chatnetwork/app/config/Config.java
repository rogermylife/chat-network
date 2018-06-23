package com.chatnetwork.app.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.chatnetwork.app.util.Util;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;



public class Config {
	private String adminAccount;
	private String adminPassword;
	private String caUrl;
	private String cert;
	private String defaultChannelName;
	private String eventhubUrl;
	private String ordererName;
	private String ordererUrl;
	private String orgName;
	private String orgMSP;
	private String peerName;
	private String peerUrl;
	private String pk;
	
	
	public static void main(String[] args) {
		Config config = newConfig();
		Gson gson = new Gson();
		String temp = gson.toJson(config);
		Logger.getLogger(Config.class.getName()).log(Level.INFO, "config's content: "+temp);
	}
	
	public Config() {
		
	}
	public Config(String orgName) {
		String userBasePath = ".." + File.separator + "crypto-config" + File.separator + 
					   "peerOrganizations" + File.separator + 
					   String.format("%s.chat-network.com", orgName) + File.separator + 
					   "users" + File.separator + 
					   String.format("Admin@%s.chat-network.com", orgName) + File.separator + "msp";
		String prefix = Util.getPortPrefixString(orgName);
		this.adminAccount = "admin";
	    this.adminPassword = "adminpw";
		this.caUrl = String.format("http://localhost:%s4",prefix);
		this.cert = userBasePath + File.separator + "admincerts";;
		this.defaultChannelName = "officialchannel";
		this.eventhubUrl = String.format("grpc://localhost:%s3", prefix);
		this.ordererName = "orderer.chat-network.com";
		this.ordererUrl = "grpc://localhost:7050";
	    this.orgName = orgName;
	    this.orgMSP = String.format("%sMSP", orgName.substring(0, 1).toUpperCase() + orgName.substring(1));
//		this.peerName = String.format("peer0.%s.chat-network.com", orgName);
		this.peerName = orgName;
		this.peerUrl = String.format("grpc://localhost:%s1", prefix);
		this.pk = userBasePath + File.separator + "keystore";
		
		
	}
	
	static public Config newConfig()
	{
		return newConfig("Config.json");
	}
	
	static public Config newConfig(String configFile)
	{
		Config config = new Config();
		Gson gson = new Gson();
		try {
		  InputStream stream = new FileInputStream(configFile);
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

	public String getDefaultChannelName() {
		return defaultChannelName;
	}

	public void setDefaultChannelName(String defaultChannelName) {
		this.defaultChannelName = defaultChannelName;
	}

	public String getPk() {
		return pk;
	}

	public void setPk(String pk) {
		this.pk = pk;
	}

	public String getCert() {
		return cert;
	}

	public void setCert(String cert) {
		this.cert = cert;
	}

}
