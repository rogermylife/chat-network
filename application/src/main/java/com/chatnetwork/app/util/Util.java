package com.chatnetwork.app.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
	
	// In general, portList file is next to manageNetwrok.sh
	static final public String portList = new String("../portList");
	
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
	
	static public String getPortPrefixString(String orgName) {
		String prefix = new String();
		try (BufferedReader br = new BufferedReader(new FileReader(Util.portList))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		       if (line.contains(orgName)) {
		    	   String[] parts = line.split(":");
		    	   prefix = parts[0];
		    	   return prefix;
		       }
		    }
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return prefix;
	}
}
