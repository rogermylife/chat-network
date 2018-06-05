package com.chatnetwork.app.chaincode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.chatnetwork.app.client.CAClient;
import com.chatnetwork.app.config.Config;
import com.chatnetwork.app.user.AppUser;
import com.chatnetwork.app.util.Util;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.EventHub;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric.sdk.ChaincodeResponse.Status;


public class Query {

	public static void main(String[] args) {
		String channelName = args[0];
		String chaincodeName = args[1];
		String functionName = args[2];
		Gson gson = new Gson();
		ArrayList<String> fcnArgsArrayList = gson.fromJson(args[3], ArrayList.class);
		String[] fcnArgs = fcnArgsArrayList.toArray(new String[0]);
		System.out.println("fcn args: "+Arrays.toString(fcnArgs));
		System.out.println();
		query(channelName, chaincodeName, functionName, fcnArgs);

	}
	
	public static void query(String channelName, String chaincodeName, String functionName, String[] args) {
		try {
			Config config = Config.newConfig();
			HFCAClient caClient = Util.newCAClient(config.getCAUrl());
			AppUser admin = new AppUser(config, caClient.enroll(config.getAdminAccount(), config.getAdminPassword()));
			HFClient client = Util.newHFClient(admin);
			Channel channel = Util.newChannel(channelName, client, config);
			QueryByChaincodeRequest request = client.newQueryProposalRequest();
			ChaincodeID.newBuilder().setName(chaincodeName).build();
			request.setChaincodeID(ChaincodeID.newBuilder().setName(chaincodeName).build());
			request.setFcn(functionName);
			request.setArgs(args);
			Collection<ProposalResponse> res = channel.queryByChaincode(request);
			for (ProposalResponse pres : res) {
			    String stringResponse = new String(pres.getChaincodeActionResponsePayload());
			    Logger.getLogger(Query.class.getName()).log(Level.INFO, stringResponse);
			    
			}
									
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
