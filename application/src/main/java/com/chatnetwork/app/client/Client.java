package com.chatnetwork.app.client;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.BlockEvent.TransactionEvent;
import org.hyperledger.fabric.sdk.ChaincodeResponse.Status;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;
import org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException;

import com.chatnetwork.app.chaincode.Invoke;
import com.chatnetwork.app.chaincode.Query;
import com.chatnetwork.app.config.Config;
import com.chatnetwork.app.user.AppUser;
import com.chatnetwork.app.util.Util;

public class Client {
	
	private Config config;
	private HFCAClient caClient;
	private AppUser admin;
	private HFClient hfClient;
	
	
	public Client () throws MalformedURLException, EnrollmentException, InvalidArgumentException, CryptoException, org.hyperledger.fabric.sdk.exception.InvalidArgumentException {
		this("Config.json");
	}
	
	public Client (String configFile) throws MalformedURLException, EnrollmentException, InvalidArgumentException, CryptoException, org.hyperledger.fabric.sdk.exception.InvalidArgumentException {
		this(Config.newConfig(configFile));
	}
	
	public Client (Config config) throws MalformedURLException, EnrollmentException, InvalidArgumentException, CryptoException, org.hyperledger.fabric.sdk.exception.InvalidArgumentException {
		this.config = config;
		this.caClient = Util.newCAClient(config.getCAUrl());
		this.admin = new AppUser(config, caClient.enroll(config.getAdminAccount(), config.getAdminPassword()));
		this.hfClient = Util.newHFClient(admin);
		
		
	}
	
	public boolean createChatRoom() {
		return true;
	}
	
	public Config getConfig() {
		return this.config;
	}
	
	public boolean inviteUser() {
		return true;
	}
	
	public boolean invoke(String channelName, String chaincodeName, String functionName, String[] args) {
		try {
			Channel channel = Util.newChannel(config.getDefaultChannelName(), hfClient, config);
			TransactionProposalRequest tpr = hfClient.newTransactionProposalRequest();
			tpr.setChaincodeID(ChaincodeID.newBuilder().setName("status").build());
			tpr.setFcn(functionName);
			tpr.setArgs(args);
			tpr.setProposalWaitTime(1000);
			Collection<ProposalResponse> response;
			response = channel.sendTransactionProposal(tpr, channel.getPeers());
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
				Logger.getLogger(Invoke.class.getName()).log(Level.INFO,"Invoked registerUser on "+chaincodeName + ". Status - " + status);
			}
		} catch (org.hyperledger.fabric.sdk.exception.InvalidArgumentException | TransactionException e) {
			Logger.getLogger(Client.class.getName()).log(Level.SEVERE,
					"Can not init a channel while call the function " + functionName +" of " +chaincodeName);
			e.printStackTrace();
			return false;
		} catch (ProposalException e) {
			Logger.getLogger(Client.class.getName()).log(Level.SEVERE,
					"can not send transaction proposal while call the function " + functionName +" of " +chaincodeName);
			e.printStackTrace();
		}
		
		return true;
	}
	
	public String query(String channelName, String chaincodeName, String functionName, String[] args) {
		String result = new String();
		try {
			Channel channel = Util.newChannel(channelName, hfClient, config);
			QueryByChaincodeRequest request = hfClient.newQueryProposalRequest();
			ChaincodeID.newBuilder().setName(chaincodeName).build();
			request.setChaincodeID(ChaincodeID.newBuilder().setName(chaincodeName).build());
			request.setFcn(functionName);
			request.setArgs(args);
			Collection<ProposalResponse> res = channel.queryByChaincode(request);
			for (ProposalResponse pres : res) {
			    String stringResponse = new String(pres.getChaincodeActionResponsePayload());
			    result = result.concat(stringResponse);
			}
		    Logger.getLogger(Query.class.getName()).log(Level.INFO, "query result:\n" + result);
									
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
		return result;
	}
	
	public boolean queryChatHistory() {
		return true;
	}
	
	public String qeryUserStatus() {
		return query(config.getDefaultChannelName(), "status", "queryUser", new String[] {config.getOrgName()});
	}
	
	public boolean registerUser() {
		return invoke(config.getDefaultChannelName(), "status", "registerUser", new String[] {config.getOrgName()});
	}
	
	public boolean sendMsg() {
		return true;
	}
}
