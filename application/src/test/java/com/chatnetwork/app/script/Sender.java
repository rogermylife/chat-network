package com.chatnetwork.app.script;

import java.util.concurrent.Callable;

import org.hyperledger.fabric.sdk.Channel;

import com.chatnetwork.app.client.Client;
import com.chatnetwork.app.config.Config;
import com.chatnetwork.app.util.Util;

public class Sender implements Callable<String>{
	
	private String orgName;
	private String channelName;
	private double seconds;
	private int num;

	public Sender (String orgName, String channelName, double seconds, int num) {
		this.orgName = orgName;
		this.channelName = channelName;
		this.seconds = seconds;
		this.num = num;
	}
	@Override
	public String call() throws Exception {
		
		Client client = new Client(new Config(this.orgName));
		Util.newChannel(this.channelName, client.getHFClient(), client.getConfig());
		for (int i=1; i<= this.num; i++) {
			client.sendMsg(channelName, String.format("[%s] test message:%d", this.orgName, i));
			Thread.sleep((long) (seconds * 1000));
		}
		return String.format("sender [%s] done %d messages %f seconds one msg", this.orgName, this.num, this.seconds);
	}

}
