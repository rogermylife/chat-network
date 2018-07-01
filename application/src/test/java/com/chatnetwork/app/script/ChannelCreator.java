package com.chatnetwork.app.script;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Peer;

import com.chatnetwork.app.client.Client;
import com.chatnetwork.app.config.Config;
import com.chatnetwork.app.util.Util;

public class ChannelCreator implements Callable<String>{
	private String orgName;
	private String channelName;
	private ArrayList<String> orgList;
	
	public ChannelCreator (String orgName, String channelName, ArrayList<String> orgList) {
		this.orgName = orgName;
		this.channelName = channelName;
		this.orgList = orgList;
	}

	@Override
	public String call() throws Exception {
		Client client = new Client(new Config(this.orgName));
		client.createChatRoom(this.channelName, this.orgList);
		Channel channel = Util.newChannel(this.channelName, client.getHFClient(), client.getConfig());
		Collection<Peer> peers;
		peers = channel.getPeers();
		client.installChaincode("chatroom", "1.0", client.getConfig().getChaincodeBase(), client.getConfig().getChaincodeChatroom(), peers);
		client.instantiateChainCode("chatroom", "1.0", client.getConfig().getChaincodeChatroom(), this.channelName, new String[] {});
		Thread.sleep(10000);
		client.initChatRoom(channelName);
		return String.format("[%s] create chatroom [%s] done", this.orgName, this.channelName);
	}

}
