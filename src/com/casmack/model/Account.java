package com.casmack.model;

import java.io.Serializable;

import org.jivesoftware.smack.packet.Presence.Mode;


public class Account implements Serializable {
    	
		private static final long serialVersionUID = 1L;
		
		private String name;
		private String user;
		private String status = "";
    	private Mode mode;
    	
    	public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		
    	public String getUser() {
			return user;
		}
		public void setUser(String user) {
			this.user = user;
		}
		public String getStatus() {
			return status;
		}
		public void setStatus(String status) {
			this.status = status;
		}
		public Mode getMode() {
			return mode;
		}
		public void setMode(Mode mode) {
			this.mode = mode;
		}
		
		@Override
		public String toString() {
			return "Account [mode=" + mode + ", name=" + name + ", status="
					+ status + ", user=" + user + "]";
		}
    }