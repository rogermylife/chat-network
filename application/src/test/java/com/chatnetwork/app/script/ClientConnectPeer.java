package com.chatnetwork.app.script;

import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;
import org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException;

import com.chatnetwork.app.client.Client;
import com.chatnetwork.app.config.Config;

public class ClientConnectPeer extends Script implements Runnable{

	public ClientConnectPeer(String orgName) {
		super(orgName);
	}

	@Override
	public void run() {
		Logger.getLogger(Thread.currentThread().getName()).log(Level.INFO,"Time");
		try {
			Client client = new Client(new Config(getOrgName()));
			return;
		} catch (MalformedURLException | EnrollmentException | InvalidArgumentException | CryptoException
				| org.hyperledger.fabric.sdk.exception.InvalidArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		fail(getOrgName() + " connect failed");
		
	}

}
