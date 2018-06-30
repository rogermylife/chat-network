package com.chatnetwork.app.client;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;
import org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException;
import org.junit.Test;

import com.chatnetwork.app.config.Config;

public class ClientTest {
	
	@Test
	public void testQueryChatHistory() {
		new Config("official");
		fail("Not yet implemented");
	}

	@Test
	public void testQeryUserStatus() {
		fail("Not yet implemented");
	}

	@Test
	public void testRegisterUser() throws EnrollmentException, InvalidArgumentException, CryptoException, org.hyperledger.fabric.sdk.exception.InvalidArgumentException, NoSuchAlgorithmException, InvalidKeySpecException, IOException {
		Client client = new Client(new Config("org1"));
		boolean result;
		String response;
		result = client.registerUser();
		assertTrue("register user failed", result);
		response = client.qeryUserStatus();
		result = response.contains(client.getConfig().getOrgName());
		assertTrue("query userStatus failed", result);
	}
	
	@Test
	public void testJoinChannel() throws EnrollmentException, InvalidArgumentException, CryptoException, org.hyperledger.fabric.sdk.exception.InvalidArgumentException, NoSuchAlgorithmException, InvalidKeySpecException, IOException {
		Client client = new Client(new Config("org3"));
//		client.qeryUserStatus();
//		client.registerUser();
//		client.qeryUserStatus();
		client.joinChannel("testchannel2");
	}
	
	@Test
	public void testCreateChannel() throws EnrollmentException, CryptoException, org.hyperledger.fabric.sdk.exception.InvalidArgumentException, NoSuchAlgorithmException, InvalidKeySpecException, IOException {
		Client client = new Client(new Config("org1"));
		ArrayList<String> orgList = new ArrayList<String>();
		orgList.add("org2");
		orgList.add("org3");
		client.createChatRoom("testchannel2", orgList);
	}

}
