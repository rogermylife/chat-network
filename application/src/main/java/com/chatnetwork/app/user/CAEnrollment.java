package com.chatnetwork.app.user;

import java.io.Serializable;
import java.security.PrivateKey;

import org.hyperledger.fabric.sdk.Enrollment;

public class CAEnrollment implements Enrollment, Serializable{

	private static final long serialVersionUID = 1L;
	private PrivateKey key;
	private String cert;
	
	public CAEnrollment(PrivateKey pkey, String signedPem) {
		this.key = pkey;
		this.cert = signedPem;
	}

	@Override
	public PrivateKey getKey() {
		return key;
	}

	@Override
	public String getCert() {
		return cert;
	}

}
