package com.chatnetwork.app.client;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hyperledger.fabric.sdk.BlockEvent.TransactionEvent;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.ChaincodeResponse.Status;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.ChannelConfiguration;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.InstallProposalRequest;
import org.hyperledger.fabric.sdk.InstantiateProposalRequest;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.TransactionRequest.Type;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;

import com.chatnetwork.app.chaincode.Invoke;
import com.chatnetwork.app.chaincode.Query;
import com.chatnetwork.app.config.Config;
import com.chatnetwork.app.user.AppUser;
import com.chatnetwork.app.util.Init;
import com.chatnetwork.app.util.Util;

public class Client {
	
	private Config config;
	private HFCAClient caClient;
	private AppUser admin;
	private HFClient hfClient;
	
	
//	public Client () throws MalformedURLException, EnrollmentException, InvalidArgumentException, CryptoException, org.hyperledger.fabric.sdk.exception.InvalidArgumentException {
//		this("Config.json");
//	}
//	
//	public Client (String configFile) throws MalformedURLException, EnrollmentException, InvalidArgumentException, CryptoException, org.hyperledger.fabric.sdk.exception.InvalidArgumentException {
//		this(Config.newConfig(configFile));
//	}
	
	public Client (Config config) throws EnrollmentException, CryptoException, org.hyperledger.fabric.sdk.exception.InvalidArgumentException, NoSuchAlgorithmException, InvalidKeySpecException, IOException {
		this.config = config;
		this.caClient = Util.newCAClient(config.getCAUrl());
		Enrollment enrollment = Util.newEnrollment(config.getPk(), config.getCert());
		this.admin = new AppUser(config, enrollment);
		this.hfClient = Util.newHFClient(admin);
	}
	
	public boolean createChatRoom(String channelName) {
		ArrayList<String> orgList = new ArrayList<String>();
		return createChatRoom(channelName, orgList);
	}
	
	public boolean createChatRoom(String channelName, ArrayList<String> orgList) {
		orgList.add(this.config.getOrgName());
		String txPath = Util.newChannelTxPath(channelName, orgList);
		try {
			ChannelConfiguration channelConfig = new ChannelConfiguration(new File(txPath));
			byte[] channelConfigurationSignatures;
			channelConfigurationSignatures = this.hfClient.getChannelConfigurationSignature(channelConfig, this.admin);
			Orderer orderer = this.hfClient.newOrderer(this.config.getOrdererName(), this.config.getOrdererUrl());
			Channel channel = this.hfClient.newChannel(channelName, orderer, channelConfig, channelConfigurationSignatures);
			channel.joinPeer(this.hfClient.newPeer(this.config.getOrgName(), this.config.getPeerUrl()));
			channel.addOrderer(orderer);
			channel.initialize();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		} catch (TransactionException e) {
			e.printStackTrace();
		} catch (ProposalException e) {
			e.printStackTrace();
		}

		return false;
	}
	
	public AppUser getAdmin() {
		return this.admin;
	}
	
	public Config getConfig() {
		return this.config;
	}
	
	public HFClient getHFClient() {
		return this.hfClient;
	}
	
	public boolean joinChannel(String channelName) throws org.hyperledger.fabric.sdk.exception.InvalidArgumentException {
		Channel channel = hfClient.getChannel(channelName);
		if(channel == null) {
			try {
				channel = Util.newChannel(channelName, this.hfClient, this.config);
			} catch (TransactionException e) {
				e.printStackTrace();
				return false;
			}
			if (channel == null) {
				Logger.getLogger(channelName).log(Level.SEVERE, String.format("[%s]-> get channel [%s] failed", this.config.getPeerName(), channelName));
				return false;
			}
		}
		return joinChannel(channel);
	}
	
	public boolean joinChannel(Channel channel) {
		if(channel ==null) {
			Logger.getLogger(Client.class.getName()).log(Level.SEVERE, String.format("[%s]-> channel is null", this.config.getPeerName()));
			return false;
		}
		try {
			return joinChannel(channel, this.hfClient.newPeer(this.config.getPeerName(), this.config.getPeerUrl()));
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean joinChannel(Channel channel, Peer peer) {
		try {
			channel.joinPeer(peer);
			return true;
		} catch (Exception e) {
			if (e.getMessage().contains("Cannot create ledger from genesis block, due to LedgerID already exists")) {
				Logger.getLogger(Init.class.getName()).log(Level.WARNING, String.format("[%s]-> is alreadgy in the channel [%s]", this.config.getOrgName(), channel.getName()));
				try {
					channel.addPeer(peer);
				} catch (InvalidArgumentException e1) {
					e1.printStackTrace();
					return false;
				}
				return true;
			}
			else
				e.printStackTrace();
		}
		return false;
	}
	
	public boolean initChatRoom(String channelName) {
		return invoke(channelName, "chatroom", "init", new String[] {channelName});
	}
	
	public boolean installChaincode(String name, String version, String base, String path, Collection<Peer> peers) throws InvalidArgumentException, IOException, ProposalException {
		InstallProposalRequest request = hfClient.newInstallProposalRequest();
		ChaincodeID.Builder chaincodeIDBuilder = ChaincodeID.newBuilder()
												 .setName(name).setVersion(version).setPath(path);
		ChaincodeID chaincodeID = chaincodeIDBuilder.build();
//		Logger.getLogger(Client.class.getName()).log(Level.INFO,
//				"Deploying chaincode " + name + " using Fabric client " + hfClient.getUserContext().getMspId()
//						+ " " + hfClient.getUserContext().getName());
		request.setChaincodeID(chaincodeID);
		request.setUserContext(hfClient.getUserContext());
		request.setChaincodeSourceLocation(new File(base));
		request.setChaincodeVersion(version);
		
		Collection<ProposalResponse> responses = hfClient.sendInstallProposal(request, peers);
		boolean res = false;
		for (ProposalResponse response : responses) {
            if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                String temp = String.format("Successful install chaincode [%s] proposal response Txid: %s from peer %s", name, response.getTransactionID(), response.getPeer().getName());
                Logger.getLogger(Client.class.getName()).log(Level.FINE, temp);
                res = true;
            } else {
            	if (response.getMessage().contains("Error installing chaincode code") && response.getMessage().contains("exists")) {
            		Logger.getLogger(Client.class.getName()).log(Level.WARNING, String.format("[%s]-> chaincode [%s]:%s is installed", response.getPeer().getName(), name, version));
            		return true;
            	}
            	System.out.println(response.getMessage());
            	res = false;
            }
        }
		return res;
	}
	
	public boolean instantiateChainCode(String name, String version, String path, String channelName, String[] functionArgs) {
		InstantiateProposalRequest instantiateProposalRequest = this.hfClient.newInstantiationProposalRequest();
		ChaincodeID.Builder chaincodeIDBuilder = ChaincodeID.newBuilder().setName(name).setVersion(version).setPath(path);
		ChaincodeID ccid = chaincodeIDBuilder.build();
        instantiateProposalRequest.setProposalWaitTime(180000);
        instantiateProposalRequest.setChaincodeID(ccid);
        instantiateProposalRequest.setChaincodeLanguage(Type.GO_LANG);
        instantiateProposalRequest.setFcn("init");
        instantiateProposalRequest.setArgs(functionArgs);
        Map<String, byte[]> tm = new HashMap<>();
        tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
        tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
        try {
			instantiateProposalRequest.setTransientMap(tm);
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
			return false;
		}

        Collection<ProposalResponse> responses;
		try {
			responses = this.hfClient.getChannel(channelName).sendInstantiationProposal(instantiateProposalRequest);
			for (ProposalResponse response : responses) {
	            if (response.isVerified() && response.getStatus() == ProposalResponse.Status.SUCCESS) {
	                String temp = String.format("Succesful instantiate chaincode [%s] proposal Txid: %s from peer %s", name, response.getTransactionID(), response.getPeer().getName());
	                Logger.getLogger(Client.class.getName()).log(Level.FINE, temp);
	            } else {
	            	if (response.getMessage().contains("chaincode exists")) {
	            		Logger.getLogger(Client.class.getName()).log(Level.WARNING, String.format("[%s]-> chaincode [%s] is instantiated", response.getPeer().getName(), name));
	            		return true;
	            	}
	            	String temp = String.format("Failed instantiate chaincode [%s] proposal Txid: %s from peer %s", name, response.getTransactionID(), response.getPeer().getName());
	                System.out.println(temp);
	                System.out.println(response.getMessage());
	                return false;
	            }
	        }
	        
	        hfClient.getChannel(channelName).sendTransaction(responses);
//	        CompletableFuture<TransactionEvent> cf = hfClient.getChannel(channelName).sendTransaction(responses);
//			Logger.getLogger(Client.class.getName()).log(Level.INFO,
//					"Chaincode [" + name + "] on channel " + channelName + " instantiation " + cf);
		} catch (InvalidArgumentException | ProposalException e) {
			e.printStackTrace();
			return false;
		}
        return true;
	}
	
	public boolean inviteUser(String channelName, Peer peer) {
		Channel channel = hfClient.getChannel(channelName);
		try {
			channel.joinPeer(peer);
		} catch (ProposalException e) {
			e.printStackTrace();
			return false;
		}
		return invoke("officialchannel", "status", "inviteUser", new String[] {peer.getName(), channelName});
	}
	
	public boolean invoke(String channelName, String chaincodeName, String functionName, String[] args) {
		try {
			Channel channel = this.hfClient.getChannel(channelName);
			TransactionProposalRequest tpr = hfClient.newTransactionProposalRequest();
			tpr.setChaincodeID(ChaincodeID.newBuilder().setName(chaincodeName).build());
			tpr.setFcn(functionName);
			tpr.setArgs(args);
			tpr.setProposalWaitTime(30000);
			Collection<ProposalResponse> response;
			response = channel.sendTransactionProposal(tpr, channel.getPeers());
			for (ProposalResponse pres : response) {
				if (pres.getStatus() == ProposalResponse.Status.SUCCESS) {
					String stringResponse = new String(pres.getChaincodeActionResponsePayload());
					Logger.getLogger(Client.class.getName()).log(Level.FINE,
							"Transaction proposal on channel " + channelName + " " + pres.getMessage() + " "
									+ pres.getStatus() + " with transaction id:" + pres.getTransactionID());
					Logger.getLogger(Client.class.getName()).log(Level.FINE,stringResponse);
				}
				else {
					Logger.getLogger(Client.class.getName()).log(Level.SEVERE,"response failed \n"+pres.getMessage() + " " 
										+ pres.getStatus() + " with transaction id:" + pres.getTransactionID());
					return false;
				}
			}
			CompletableFuture<TransactionEvent> cf = channel.sendTransaction(response);
			Logger.getLogger(Invoke.class.getName()).log(Level.FINE,cf.toString());
			
//			for (ProposalResponse res: response) {
//				Status status = res.getStatus();
//				Logger.getLogger(Invoke.class.getName()).log(Level.INFO,"Invoked registerUser on "+chaincodeName + ". Status - " + status + " " +res.getMessage());
//			}
		} catch (org.hyperledger.fabric.sdk.exception.InvalidArgumentException e) {
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
			Channel channel = this.hfClient.getChannel(channelName);
			QueryByChaincodeRequest request = hfClient.newQueryProposalRequest();
			ChaincodeID.newBuilder().setName(chaincodeName).build();
			request.setChaincodeID(ChaincodeID.newBuilder().setName(chaincodeName).build());
			request.setFcn(functionName);
			request.setArgs(args);
			Collection<ProposalResponse> res = channel.queryByChaincode(request);
			for (ProposalResponse pres : res) {
				if (pres.getStatus() == ProposalResponse.Status.SUCCESS) {
					String stringResponse = new String(pres.getChaincodeActionResponsePayload());
					Logger.getLogger(Client.class.getName()).log(Level.FINE,
							"Transaction proposal on channel " + channelName + " " + pres.getMessage() + " "
									+ pres.getStatus() + " with transaction id:" + pres.getTransactionID());
					Logger.getLogger(Client.class.getName()).log(Level.FINE,stringResponse);
					result = result.concat(stringResponse);
				}
				else {
					Logger.getLogger(Client.class.getName()).log(Level.SEVERE,"response failed \n"+pres.getMessage() + " " 
										+ pres.getStatus() + " with transaction id:" + pres.getTransactionID());
					return "";
				}
			    
			}
		    Logger.getLogger(Query.class.getName()).log(Level.FINE, "query result:\n" + result);
									
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
		return result;
	}
	
	public String queryChatHistory(String channelName) {
		return query(channelName, "chatroom", "getChatHistory", new String[] {});
	}
	
	public String qeryUserStatus() {
		return query(config.getDefaultChannelName(), "status", "queryUser", new String[] {config.getOrgName()});
	}
	
	public boolean registerUser() {
		return invoke(config.getDefaultChannelName(), "status", "registerUser", new String[] {config.getOrgName()});
	}
	
	public boolean sendMsg(String channelName, String content) throws org.hyperledger.fabric.sdk.exception.InvalidArgumentException {
		return invoke(channelName, "chatroom", "addMsg", new String[] {this.config.getOrgName(), 
																	   Long.toString(System.currentTimeMillis()), 
																	   content});
	}
}
