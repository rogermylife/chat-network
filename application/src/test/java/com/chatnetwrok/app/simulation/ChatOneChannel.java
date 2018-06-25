package com.chatnetwrok.app.simulation;

import static org.junit.Assert.fail;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;

import com.chatnetwork.app.script.Sender;

public class ChatOneChannel {

	@Test
	public void chatOneChannel() throws InterruptedException, ExecutionException {
		ExecutorService service = Executors.newFixedThreadPool(1);
		Future<String> sender = service.submit(new Sender("official", "officialchannel", 1, 15));
		
		System.out.println(sender.get());


	}
}
