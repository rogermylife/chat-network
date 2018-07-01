package com.chatnetwork.app.script;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import org.hyperledger.fabric.sdk.Peer;

import com.chatnetwork.app.client.Client;
import com.chatnetwork.app.config.Config;

public class Initer implements Callable<String>{
	
	private String orgName;
	
	public Initer(String orgName) {
		this.orgName = orgName;
	}

	@Override
	public String call() throws Exception {
		Client client = new Client( new Config(this.orgName) );
		ArrayList<Peer> peers = new ArrayList<Peer> ();
		peers.add(client.getHFClient().newPeer(client.getConfig().getPeerName(), client.getConfig().getPeerUrl()));
		client.installChaincode("chatroom", "1.0", client.getConfig().getChaincodeBase(), client.getConfig().getChaincodeChatroom(), peers);
		return String.format("[%s] install [chatroom] done", this.orgName);
	}
	

}
