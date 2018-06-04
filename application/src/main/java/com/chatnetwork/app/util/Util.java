package com.chatnetwork.app.util;

import java.net.MalformedURLException;
import java.util.Properties;

import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;

import com.chatnetwork.app.config.Config;
import com.chatnetwork.app.user.AppUser;

public class Util {
	static public HFCAClient newCAClient(String url) throws MalformedURLException {
		CryptoSuite cryptoSuite = CryptoSuite.Factory.getCryptoSuite();
		Properties p =new Properties();
		p.setProperty("negotiationType", "TLS");
		HFCAClient client = HFCAClient.createNewInstance(url, p);
		
		
		client.setCryptoSuite(cryptoSuite);
		return client;
	}
	
	static public HFClient newHFClient(AppUser user) throws CryptoException, InvalidArgumentException {
		CryptoSuite cryptoSuite = CryptoSuite.Factory.getCryptoSuite();
		HFClient client = HFClient.createNewInstance();
		client.setCryptoSuite(cryptoSuite);
		client.setUserContext(user);
		return client;
	}
	
	static public Channel newChannel(String channelName, HFClient client, Config config )
					throws InvalidArgumentException, TransactionException {
		Channel channel = client.newChannel(channelName);
//		System.out.println("GGGGGGGGGG:    "+peerUrl);
		channel.addPeer(client.newPeer("peer", config.getPeerUrl()));
		channel.addEventHub(client.newEventHub("eventhub", config.getEventhubUrl()));
		channel.addOrderer(client.newOrderer("orderer", config.getOrdererUrl()));
		channel.initialize();
		
		return channel;
	}
}
