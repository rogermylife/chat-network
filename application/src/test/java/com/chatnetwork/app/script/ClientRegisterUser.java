package com.chatnetwork.app.script;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;
import org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException;

import com.chatnetwork.app.client.Client;
import com.chatnetwork.app.config.Config;

public class ClientRegisterUser extends Script implements Runnable{

	public ClientRegisterUser(String orgName) {
		super(orgName);
	}

	@Override
	public void run() {
		Logger.getLogger(Thread.currentThread().getName()).log(Level.INFO,"Time");
		try {
			Client client = new Client(new Config(getOrgName()));
			Logger.getLogger(Thread.currentThread().getName()).log(Level.INFO,"Time");
			boolean result;
			String response;
			result = client.registerUser();
			assertTrue("register user failed", result);
			for (int i=0;i<20;i++){
				Thread.sleep(100);
				response = client.qeryUserStatus();
				result = response.contains(client.getConfig().getOrgName());
				if(result)
					break;
			}
			assertTrue("query userStatus failed", result);
		} catch (MalformedURLException | EnrollmentException | CryptoException
				| org.hyperledger.fabric.sdk.exception.InvalidArgumentException e) {
			e.printStackTrace();
			return;
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
