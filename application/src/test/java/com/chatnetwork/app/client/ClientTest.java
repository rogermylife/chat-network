package com.chatnetwork.app.client;

import static org.junit.Assert.*;

import java.net.MalformedURLException;

import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;
import org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException;
import org.junit.Test;

public class ClientTest {

	@Test
	public void testQueryChatHistory() {
		fail("Not yet implemented");
	}

	@Test
	public void testQeryUserStatus() {
		fail("Not yet implemented");
	}

	@Test
	public void testRegisterUser() throws MalformedURLException, EnrollmentException, InvalidArgumentException, CryptoException, org.hyperledger.fabric.sdk.exception.InvalidArgumentException {
		Client client = new Client();
		boolean res = client.registerUser();
		assertTrue("register user failed", res);
		res = client.qeryUserStatus();
		assertTrue("query userStatus failed", res);
	}

}
