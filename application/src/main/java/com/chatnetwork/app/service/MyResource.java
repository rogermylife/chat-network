package com.chatnetwork.app.service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;

import com.chatnetwork.app.client.Client;
import com.chatnetwork.app.config.Config;
import com.chatnetwork.app.util.Util;

@Path("myresource")
public class MyResource {

	@GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getIt() {
		try {
			Client client = new Client(new Config("official"));
			Util.newChannel("officialchannel", client.getHFClient(), client.getConfig());
			String chatHistory = client.queryChatHistory("officialchannel");
			return chatHistory;
		} catch (EnrollmentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CryptoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			// TODO Auto-generated catch block
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
		} catch (TransactionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return "failed";
    }
}
