package com.chatnetwrok.app.simulation;

import static org.junit.Assert.fail;

import org.junit.Test;

import com.chatnetwork.app.script.ClientRegisterUser;

public class MultiClientsRegisterUser {
	public int start = 4;
	public int end = 5;
	@Test
	public void multiClientsRegisterUser() {
		Thread[] threads = new Thread[100];
		for (int i=start;i<=end;i++) {
			threads[i] = new Thread(new ClientRegisterUser("org"+i), "org"+i);
			threads[i].start();
		}
		try {
			for (int i=start;i<=end;i++) {
				threads[i].join();
			}
        } catch(InterruptedException e){
        	e.printStackTrace();
        	fail("GG");
        }
	}

}
