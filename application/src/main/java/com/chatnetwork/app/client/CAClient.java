
package com.chatnetwork.app.client;

import java.net.MalformedURLException;

import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;
import org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException;

public class CAClient {
	private HFCAClient client;
	
	public CAClient(String url) throws MalformedURLException {
		CryptoSuite cryptoSuite = CryptoSuite.Factory.getCryptoSuite();
		client = HFCAClient.createNewInstance(url, null);
		client.setCryptoSuite(cryptoSuite);
	}
	
	public Enrollment enrollAdmin(String account, String password) throws EnrollmentException, InvalidArgumentException {
		return client.enroll(account, password);
	}
}
