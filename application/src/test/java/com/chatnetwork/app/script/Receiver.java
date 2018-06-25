package com.chatnetwork.app.script;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import com.chatnetwork.app.client.Client;
import com.chatnetwork.app.config.Config;
import com.chatnetwork.app.util.Util;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class Receiver implements Callable<String>{
	private String orgName;
	private String channelName;
	private String[] orgList;
	private double seconds;
	private int num;

	public Receiver (String orgName, String channelName, String[] orgList, double seconds, int num) {
		this.orgName = orgName;
		this.channelName = channelName;
		this.orgList = orgList;
		this.seconds = seconds;
		this.num = num;
	}

	@Override
	public String call() throws Exception {
		Client client = new Client(new Config(this.orgName));
		Util.newChannel(this.channelName, client.getHFClient(), client.getConfig());
		HashMap<String, Record> records= new HashMap<String, Record>();
		JsonParser parser = new JsonParser();
		for(String orgName : this.orgList) {
			records.put(orgName, new Record(this.num));
		}
		long startTime = System.currentTimeMillis();
		do {
			String chatHistory = client.queryChatHistory(this.channelName);
			long time = System.currentTimeMillis();
			JsonObject object = parser.parse(chatHistory).getAsJsonObject(); 
			JsonArray messages = object.get("Messages").getAsJsonArray();
			for (JsonElement msg : messages) {
				JsonObject temp = msg.getAsJsonObject();
				
				String tempSender	= temp.get("Sender").toString().replaceAll("^\"|\"$", "");
				long tempTime 		= Long.parseLong(temp.get("Timestamp").toString().replaceAll("^\"|\"$", ""));
				String tempContent 	= temp.get("Content").toString().replaceAll("^\"|\"$", "");
				int tempNum 		= Integer.parseInt(tempContent.split(":")[1]);
				
				if (records.get(tempSender).timeRecords[tempNum] == 0) {
					records.get(tempSender).timeRecords[tempNum] = time - tempTime;
					records.get(tempSender).recvNum++;
					records.get(tempSender).totalTime+=time - tempTime;
//					System.out.println(String.format("from [%s] num:[%d] sent %d now %d diff %d", tempSender, 
//																									 tempNum, 
//																									 tempTime, 
//																									 time, 
//																									 time - tempTime));
				}
//				System.out.println(tempSender);
//				System.out.println(tempTime);
//				System.out.println(tempNum);
			}
			
//			for(String orgName : this.orgList) {
//		        for (int i=1; i<=num ;i++) {
//		        	if (chatHistory.contains(orgName) && chatHistory.contains(i)) {
//		        		
//		        	}
//		        }
//		    }
			
		}while(System.currentTimeMillis() - startTime < this.seconds*1000*this.num + 2000);
//		System.out.println("DONE");
		String result = new String();
		for (Map.Entry<String, Record> entry : records.entrySet()) {
		    String orgName = entry.getKey();
		    Record record = entry.getValue();
		    result = result.concat(String.format("%s-> %s \nAVG:%f\n", orgName, Arrays.toString(record.timeRecords), (double)record.totalTime/record.recvNum));
		}
		return result;
	}
	
	
}
class Record {
	Record (int num) {
		timeRecords = new long[num+1];
	}
	int recvNum=0;
	long totalTime=0;
	long[] timeRecords;
}
