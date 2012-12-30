package com.casmack;

import com.iris.R;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

public interface IXmppConnectionService { 
    
	void connection(String serverHost, String login, String password, String resource) throws XMPPException ;
	
	void sendMessage(String text, String to) ;
	
	Roster getRoster () ;
	
	XMPPConnection getConnection () ;
} 