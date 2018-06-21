package com.chatnetwrok.app.simulation;

import static org.junit.Assert.fail;

import org.junit.Test;

import com.chatnetwork.app.script.ClientConnectPeer;

public class MultiClientsConnectPeers {
	public int num = 500;
	public int start = 1;
	@Test
	public void multiClientsConnectPeers() {
		Thread[] threads = new Thread[1000];
		for (int i=start;i<start+num;i++) {
			threads[i] = new Thread(new ClientConnectPeer("org"+i), "org"+i);
			threads[i].start();
		}
		try {
			for (int i=start;i<start+num;i++) {
				threads[i].join();
			}
        } catch(InterruptedException e){
        	e.printStackTrace();
        	fail("GG");
        }
	}
}
