package com.chatnetwork.app.user;

import java.io.Serializable;
import java.util.Set;

import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;

import com.chatnetwork.app.config.Config;

public class AppUser implements User, Serializable{
	
	private String account;
	private String affiliation;
	private String mspId;
	private String name;
	private Enrollment enrollment;
	protected Set<String> roles;
	
	public AppUser (Config config, Enrollment enrollment) {
		account = config.getAdminAccount();
		affiliation = config.getOrgName();
		mspId = config.getOrgMSP();
		name = config.getAdminAccount();
		this.enrollment = enrollment;
	}
	
	@Override
	public String getAccount() {
		return this.account;
	}

	@Override
	public String getAffiliation() {
		return this.affiliation;
	}

	@Override
	public Enrollment getEnrollment() {
		return this.enrollment;
	}

	@Override
	public String getMspId() {
		return this.mspId;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Set<String> getRoles() {
		return this.roles;
	}
}
