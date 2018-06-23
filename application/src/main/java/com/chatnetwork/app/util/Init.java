package com.chatnetwork.app.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collection;
import java.util.Iterator;
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
	public static final int End = 50;

	public static void main(String[] args) throws EnrollmentException, InvalidArgumentException, CryptoException, org.hyperledger.fabric.sdk.exception.InvalidArgumentException, TransactionException, ProposalException, NoSuchAlgorithmException, InvalidKeySpecException, IOException {
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
				if (e.getMessage().contains("Cannot create ledger from genesis block, due to LedgerID already exists"))
					Logger.getLogger(Init.class.getName()).log(Level.INFO, String.format("org [%s] is alreadgy in the channel [%s]", orgName, "officialchannel"));
				else
					throw e;
			}
		}
		Logger.getLogger(Init.class.getName()).log(Level.INFO,String.format("org %d~%d is inited done in the officialchannel", Start, End));
        
	}

}
