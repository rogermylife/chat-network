package com.chatnetwrok.app.simulation;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

import com.chatnetwork.app.script.ChannelCreator;
import com.chatnetwork.app.script.Receiver;
import com.chatnetwork.app.script.Sender;

public class ChatMultiChannel {
	
	@Test
	public void chatMultiChannel() throws InterruptedException, ExecutionException {
		Logger logger = Logger.getLogger(ChatMultiChannel.class.getName());
		ExecutorService service = Executors.newFixedThreadPool(25);
		int[] nums = new int[] {2};
		ArrayList<Future<String>> creators = new ArrayList<Future<String>>();
		ArrayList<Future<String>> senders = new ArrayList<Future<String>>();
		ArrayList<Future<String>> receivers = new ArrayList<Future<String>>();
		for (int num : nums) {
			for (int i=1 ;i<=num; i++) {
				int temp = (1+10*(i-1));
				String orgName = String.format("org%d", temp);
				System.out.println("QQQQQQQQQQQ "+orgName);
				String channelName = orgName+"-"+num;
				ArrayList<String> orgList = new ArrayList<String>();
				for (int j=2;j<=10;j++) {
					orgList.add("org" + (j+10*(i-1)));
				}
				creators.add(service.submit(new ChannelCreator(orgName, channelName, orgList)));
				logger.log(Level.INFO, creators.get(i-1).get());
			}
			Thread.sleep(2000);
			for (int i=1 ;i<=num; i++) {
				String senderName = "org"+(1+10*(i-1));
				String receiverName = "org"+(2+10*(i-1));
				String channelName = senderName+"_"+num;
				senders.add(service.submit(new Sender(senderName, channelName, 1, 100)));
				Thread.sleep(2000);
				receivers.add(service.submit(new Receiver(receiverName, channelName, new String[] {senderName}, 1, 100)));
			}
			for (int i=1 ;i<=num; i++) {
				logger.log(Level.INFO, senders.get(i-1).get());
				logger.log(Level.INFO, receivers.get(i-1).get());
			}
		}
//		ArrayList<String> orgList = new ArrayList<String>();
//		for (int i=2;i<=10;i++) {
//			orgList.add("org"+i);
//		}
//		Future<String> creator = service.submit(new ChannelCreator("org1", "org12", orgList));
//		System.out.println(creator.get());
//		
//		Future<String> sender = service.submit(new Sender("official", "officialchannel", 1, 100));
//		Thread.sleep(2000);
//		Future<String> receiver = service.submit(new Receiver("official", "officialchannel", 
//															  new String[] {"official"}, 1, 100));
		
		System.out.println("HAHA");
	}


}
