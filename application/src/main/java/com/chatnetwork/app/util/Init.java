package com.chatnetwork.app.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;
import org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException;

import com.chatnetwork.app.client.Client;
import com.chatnetwork.app.config.Config;

public class Init {
	
	public static final int Start = 1;
	public static final int End = 5;

	public static void main(String[] args) throws EnrollmentException, InvalidArgumentException, CryptoException, org.hyperledger.fabric.sdk.exception.InvalidArgumentException, TransactionException, ProposalException, NoSuchAlgorithmException, InvalidKeySpecException, IOException {
		Logger logger = Logger.getLogger(Init.class.getName());
		Collection<Peer> peers;
		
		
//		Client officialClient = new Client(new Config("official"));
//		Channel officialChannel = Util.newChannel("officialchannel", officialClient.getHFClient(), officialClient.getConfig());
//		peers = officialChannel.getPeers();
//		officialClient.installChaincode("status", "1.0", officialClient.getConfig().getChaincodeBase(), 
//										officialClient.getConfig().getChaincodeStatus(), peers);
//		boolean result = officialClient.instantiateChainCode("status", "1.0", officialClient.getConfig().getChaincodeStatus(), 
//											"officialchannel", new String[] {});
//		if (!result) {
//			System.out.println("official instantiateChainCode failed");
//			return;
//		}

		
		for (int i=Start; i<=End; ++i )
		{
			String orgName = "org"+i;
			Config config = new Config(orgName);
			Client client = new Client(config);
			Channel channel = Util.newChannel("officialchannel", client.getHFClient(), config);
			if (channel == null) {
				Logger.getLogger(Init.class.getName()).log(Level.SEVERE, "can not get channel officialchannel");
				return;
			}
			String prefix = Util.getPortPrefixString(orgName);
			try {
				channel.joinPeer(client.getHFClient().newPeer(orgName, String.format("grpc://localhost:%s1", prefix)));
			} catch (Exception e) {
				if (e.getMessage().contains("Cannot create ledger from genesis block, due to LedgerID already exists")) {
					Logger.getLogger(Init.class.getName()).log(Level.INFO, String.format("org [%s] is alreadgy in the channel [%s]", orgName, "officialchannel"));
					channel.addPeer(client.getHFClient().newPeer(client.getConfig().getPeerName(),
																 client.getConfig().getPeerUrl()));
				}
					
				else
					throw e;
			}
			// In normal, there is only one peer listed.
			peers = channel.getPeers();
			if (peers.isEmpty()) {
				logger.log(Level.INFO,String.format("[%s] peers of channel is empty", orgName));
			}
			client.installChaincode("status", "1.0", client.getConfig().getChaincodeBase(), 
									client.getConfig().getChaincodeStatus(), peers);
		}
		logger.log(Level.INFO,String.format("org %d~%d is inited done in the officialchannel", Start, End));
        
	}

}
