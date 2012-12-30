package com.casmack;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import com.iris.R;

public class XmppConnectionService extends Service implements IXmppConnectionService { 
	 
	private static final String TAG = "XmppConnectionService";
	
	private XmppConnectionServiceBinder binder ;  
	
	private XMPPConnection m_connection;
	
	public static final String SERVER_HOST = "serverHost";
	public static final String LOGIN = "login";
	public static final String PASSWORD = "password";
	public static final String RESOURCE = "resource";
	 
	@Override 
	public void onCreate() { 
	    super.onCreate();
	    binder = new XmppConnectionServiceBinder(this); 
	    Log.d(this.getClass().getName(), "onCreate"); 
	} 
	
	@Override 
	public int onStartCommand(Intent intent, int flags, int startId) { 
	    Log.d(this.getClass().getName(), "onStart");
	    
	    String serverHost = intent.getStringExtra(SERVER_HOST);
	    String login = intent.getStringExtra(LOGIN);
	    String password = intent.getStringExtra(PASSWORD);
	    String resource = intent.getStringExtra(RESOURCE);
	    try {
	    	connection(serverHost, login, password, resource);
		} catch (XMPPException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
			Log.d(TAG, "Unable to connect to xmpp :", e);
		}
	    
		Intent openIntent = new Intent(getBaseContext(), ContactActivity.class);
		openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		getApplication().startActivity(openIntent);
	    return START_NOT_STICKY; 
	} 
	 
	@Override 
	public void onDestroy() { 
	    Log.d(this.getClass().getName(), "onDestroy"); 
	    disconnect();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
	/**
     * Initialisation de la connexion
     * 
     */
    public void connection(String serverHost, String login, String password, String resource) throws XMPPException {
    	ConnectionConfiguration config = new ConnectionConfiguration(serverHost);
		m_connection = new XMPPConnection(config);
        m_connection.connect();
        m_connection.login(login, password, resource);
        Presence presence = new Presence(Presence.Type.available);
        m_connection.sendPacket(presence);
	}
    
    public void disconnect() {
    	if ( m_connection != null && !m_connection.isConnected() ){
	    	m_connection.disconnect();
	    }
    }
    
    public void sendMessage(String text, String to) {
    	Message msg = new Message(to, Message.Type.chat);
		msg.setBody(text);
		if ( m_connection != null && m_connection.isConnected() ) {
			m_connection.sendPacket(msg);
		}
    }
    
    public XMPPConnection getConnection () {
    	return m_connection;
    }
    
    public Roster getRoster () {
    	Roster roster = null;
    	if ( m_connection != null && m_connection.isConnected() ) {
    		roster = m_connection.getRoster();	
    	}
    	
    	return roster;
    }
    
}