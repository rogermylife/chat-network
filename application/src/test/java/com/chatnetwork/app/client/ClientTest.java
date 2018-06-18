package com.chatnetwork.app.client;

import static org.junit.Assert.*;

import java.net.MalformedURLException;

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
	public void testRegisterUser() throws MalformedURLException, EnrollmentException, InvalidArgumentException, CryptoException, org.hyperledger.fabric.sdk.exception.InvalidArgumentException {
		Client client = new Client(new Config("org4"));
		boolean result;
		String response;
		result = client.registerUser();
		assertTrue("register user failed", result);
		response = client.qeryUserStatus();
		result = response.contains(client.getConfig().getOrgName());
		assertTrue("query userStatus failed", result);
	}

}
