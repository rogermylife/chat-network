package com.chatnetwork.app.script;

import java.util.concurrent.Callable;

import com.chatnetwork.app.client.Client;
import com.chatnetwork.app.config.Config;

public class ChannelJoiner implements Callable<String>{
	private String orgName;
	private String channelName;
	
	public ChannelJoiner(String orgName, String channelName) {
		this.orgName = orgName;
		this.channelName = channelName;
	}
	
	@Override
	public String call() throws Exception {
		Client client = new Client(new Config(this.orgName));
		client.joinChannel(this.channelName);
		return String.format("[%s] join channel [%s] done", this.orgName, this.channelName);
	}

}
