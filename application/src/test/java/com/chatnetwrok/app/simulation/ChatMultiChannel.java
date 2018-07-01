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
import com.chatnetwork.app.script.ChannelJoiner;
import com.chatnetwork.app.script.Initer;
import com.chatnetwork.app.script.Receiver;
import com.chatnetwork.app.script.Sender;

public class ChatMultiChannel {
	
	@Test
	public void chatMultiChannel() throws InterruptedException, ExecutionException {
		Logger logger = Logger.getLogger(ChatMultiChannel.class.getName());
		ExecutorService service = Executors.newFixedThreadPool(100);
		String seperator = "-02-";
		int[] nums = new int[] {2};
		ArrayList<Future<String>> creators = new ArrayList<Future<String>>();
		ArrayList<Future<String>> initers = new ArrayList<Future<String>>();
		ArrayList<Future<String>> joiners = new ArrayList<Future<String>>();
		ArrayList<Future<String>> senders = new ArrayList<Future<String>>();
		ArrayList<Future<String>> receivers = new ArrayList<Future<String>>();
		for (int num : nums) {
			creators.clear();
			initers.clear();
			joiners.clear();
			senders.clear();
			receivers.clear();
			for (int i=1 ;i<=num; i++) {
				int temp = (1+10*(i-1));
				String orgName = String.format("org%d", temp);
				String channelName = orgName+seperator+num;
				ArrayList<String> orgList = new ArrayList<String>();
				for (int j=2;j<=10;j++) {
					orgList.add("org" + (j+10*(i-1)));
				}
				creators.add(service.submit(new ChannelCreator(orgName, channelName, orgList)));
				logger.log(Level.INFO, creators.get(i-1).get());
			}
			logger.log(Level.INFO, "chatrooms init done");
			Thread.sleep(5000);
			
			for (int i=1; i<=num; i++) {
				for (int j=2; j<=10; j++) {
					String orgName = "org"+ (j+10*(i-1));
					String channelName = "org" + (1+10*(i-1)) + seperator + num;
					initers.add(service.submit(new Initer(orgName)));
					joiners.add(service.submit(new ChannelJoiner(orgName, channelName)));
				}
			}
			for ( Future<String> joiner : joiners) {
				logger.log(Level.INFO, joiner.get());
			}
			for ( Future<String> initer : initers) {
				logger.log(Level.INFO, initer.get());
			}
			logger.log(Level.INFO, "join channel and init done");
			Thread.sleep(5000);
			
			logger.log(Level.INFO, "start Chat");
			for (int i=1 ;i<=num; i++) {
				String senderName = "org"+(1+10*(i-1));
				String receiverName = "org"+(2+10*(i-1));
				String channelName = senderName+seperator+num;
				senders.add(service.submit(new Sender(senderName, channelName, 1, 100)));
				Thread.sleep(2000);
				receivers.add(service.submit(new Receiver(receiverName, channelName, new String[] {senderName}, 1, 100)));
			}
			for (int i=1 ;i<=num; i++) {
				logger.log(Level.INFO, senders.get(i-1).get());
				logger.log(Level.INFO, receivers.get(i-1).get());
			}
		}
		
		System.out.println("HAHA");
	}


}
