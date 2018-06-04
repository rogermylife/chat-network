package com.chatnetwork.app.chaincode;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hyperledger.fabric.sdk.BlockEvent.TransactionEvent;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.ChaincodeResponse.Status;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric_ca.sdk.HFCAClient;

import com.chatnetwork.app.config.Config;
import com.chatnetwork.app.user.AppUser;
import com.chatnetwork.app.util.Util;
import com.google.gson.Gson;

public class Invoke {
	private static final byte[] EXPECTED_EVENT_DATA = "!".getBytes(UTF_8);
	private static final String EXPECTED_EVENT_NAME = "event";

	public static void main(String[] args) {
		String channelName = args[0];
		String chaincodeName = args[1];
		String functionName = args[2];
		Gson gson = new Gson();
		ArrayList<String> fcnArgsArrayList = gson.fromJson(args[3], ArrayList.class);
		String[] fcnArgs = fcnArgsArrayList.toArray(new String[0]);
		System.out.println("fcn args: "+Arrays.toString(fcnArgs));
		System.out.println();
		invoke(channelName, chaincodeName, functionName, fcnArgs);

	}
	
	public static void invoke(String channelName, String chaincodeName, String functionName, String[] args)
	{
		try {
			//init
			Config config = Config.newConfig();
			HFCAClient caClient = Util.newCAClient(config.getCAUrl());
			AppUser admin = new AppUser(config, caClient.enroll(config.getAdminAccount(), config.getAdminPassword()));
			HFClient client = Util.newHFClient(admin);
			Channel channel = Util.newChannel(channelName, client, config);
			
			TransactionProposalRequest tpr = client.newTransactionProposalRequest();
			tpr.setChaincodeID(ChaincodeID.newBuilder().setName(chaincodeName).build());
			tpr.setFcn(functionName);
			tpr.setArgs(args);
			tpr.setProposalWaitTime(1000);

			Map<String, byte[]> tm2 = new HashMap<>();
			tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8)); 																								
			tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8)); 
			tm2.put("result", ":)".getBytes(UTF_8));
			tm2.put(EXPECTED_EVENT_NAME, EXPECTED_EVENT_DATA); 
			tpr.setTransientMap(tm2);
			Collection<ProposalResponse> response = channel.sendTransactionProposal(tpr, channel.getPeers());
			for (ProposalResponse pres : response) {
				String stringResponse = new String(pres.getChaincodeActionResponsePayload());
				Logger.getLogger(Invoke.class.getName()).log(Level.INFO,
						"Transaction proposal on channel " + channelName + " " + pres.getMessage() + " "
								+ pres.getStatus() + " with transaction id:" + pres.getTransactionID());
				Logger.getLogger(Invoke.class.getName()).log(Level.INFO,stringResponse);
			}

			CompletableFuture<TransactionEvent> cf = channel.sendTransaction(response);
			Logger.getLogger(Invoke.class.getName()).log(Level.INFO,cf.toString());
			
			for (ProposalResponse res: response) {
				Status status = res.getStatus();
				Logger.getLogger(Invoke.class.getName()).log(Level.INFO,"Invoked createCar on "+chaincodeName + ". Status - " + status);
			}

									
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
