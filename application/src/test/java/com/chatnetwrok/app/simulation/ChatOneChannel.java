package com.chatnetwrok.app.simulation;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;
import org.junit.Test;

import com.chatnetwork.app.client.Client;
import com.chatnetwork.app.config.Config;
import com.chatnetwork.app.script.Receiver;
import com.chatnetwork.app.script.Sender;
import com.chatnetwork.app.util.Util;

public class ChatOneChannel {

	@Test
	public void chatOneChannel() throws InterruptedException, ExecutionException, EnrollmentException, CryptoException, InvalidArgumentException, NoSuchAlgorithmException, InvalidKeySpecException, IOException, TransactionException {
		Client client = new Client(new Config("official"));
		Util.newChannel("officialchannel", client.getHFClient(), client.getConfig());
		double[] f = new double[] {0.01, 0.3, 0.5, 1, 1.5, 3};
		for (double ff : f) {
			client.initChatRoom("officialchannel");
			Thread.sleep(2000);
			ExecutorService service = Executors.newFixedThreadPool(2);
			Future<String> sender = service.submit(new Sender("official", "officialchannel", ff, 10));
			Thread.sleep(2000);
			Future<String> receiver = service.submit(new Receiver("official", "officialchannel", 
																  new String[] {"official"}, ff, 10));
			System.out.println(sender.get());
			System.out.println(receiver.get());
		}
		


	}
}
