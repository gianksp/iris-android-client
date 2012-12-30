package com.casmack;


import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.casmack.model.ChatMessage;
import com.speech.*;

import com.iris.R;
import com.nuance.nmdp.speechkit.Recognition;
import com.nuance.nmdp.speechkit.Recognizer;
import com.nuance.nmdp.speechkit.SpeechError;
import com.nuance.nmdp.speechkit.Vocalizer;



public class ChatActivity extends Activity implements ServiceConnection {
	
	private static final String TAG = "ChatActivity";

	private ChatMessageList m_discussionThread;
	private ArrayAdapter<ChatMessage> m_discussionThreadAdapter;
	private Handler m_handler;
	
	
    private Vocalizer _vocalizer;
    private Object _lastTtsContext = null;
    
    
    
    
	private static final String MESSAGES = "messages";
	
	
	
	
	
	
	
	
    private static final int LISTENING_DIALOG = 0;
    private Handler _handler = null;
    private final Recognizer.Listener _listener;
    private Recognizer _currentRecognizer;
    private ListeningDialog _listeningDialog;
    private ArrayAdapter<String> _arrayAdapter;
    private boolean _destroyed;
    
    private class SavedState
    {
        String DialogText;
        String DialogLevel;
        boolean DialogRecording;
        Recognizer Recognizer;
        Handler Handler;
    }

    public ChatActivity()
    {
        super();
        _listener = createListener();
        _currentRecognizer = null;
        _listeningDialog = null;
        _destroyed = true;
    }
    private Recognizer.Listener createListener()
    {
        return new Recognizer.Listener()
        {            
            @Override
            public void onRecordingBegin(Recognizer recognizer) 
            {
                _listeningDialog.setText("Recording...");
            	_listeningDialog.setStoppable(true);
                _listeningDialog.setRecording(true);
                
                // Create a repeating task to update the audio level
                Runnable r = new Runnable()
                {
                    public void run()
                    {
                        if (_listeningDialog != null && _listeningDialog.isRecording() && _currentRecognizer != null)
                        {
                            _listeningDialog.setLevel(Float.toString(_currentRecognizer.getAudioLevel()));
                            _handler.postDelayed(this, 500);
                        }
                    }
                };
                r.run();
            }

            @Override
            public void onRecordingDone(Recognizer recognizer) 
            {
                _listeningDialog.setText("Processing...");
                _listeningDialog.setLevel("");
                _listeningDialog.setRecording(false);
            	_listeningDialog.setStoppable(false);
            }

            @Override
            public void onError(Recognizer recognizer, SpeechError error) 
            {
            	if (recognizer != _currentRecognizer) return;
            	if (_listeningDialog.isShowing()) dismissDialog(LISTENING_DIALOG);
                _currentRecognizer = null;
                _listeningDialog.setRecording(false);

                // Display the error + suggestion in the edit box
                String detail = error.getErrorDetail();
                String suggestion = error.getSuggestion();
                
                if (suggestion == null) suggestion = "";
                setResult(detail + "\n" + suggestion);
                // for debugging purpose: printing out the speechkit session id
                android.util.Log.d("Nuance SampleVoiceApp", "Recognizer.Listener.onError: session id ["
                        + ConnectionActivity.getSpeechKit().getSessionId() + "]");
            }

            @Override
            public void onResults(Recognizer recognizer, Recognition results) {
                if (_listeningDialog.isShowing()) dismissDialog(LISTENING_DIALOG);
                _currentRecognizer = null;
                _listeningDialog.setRecording(false);
                int count = results.getResultCount();
                Recognition.Result [] rs = new Recognition.Result[count];
                for (int i = 0; i < count; i++)
                {
                    rs[i] = results.getResult(i);
                }
                setResults(rs);
                // for debugging purpose: printing out the speechkit session id
                android.util.Log.d("Nuance SampleVoiceApp", "Recognizer.Listener.onResults: session id ["
                        + ConnectionActivity.getSpeechKit().getSessionId() + "]");
            }
        };
    }
    
    private void setResult(String result)
    {
    	final TextView recipient = (TextView) this.findViewById(R.id.recipient);
		ChatMessage chatMessage = new ChatMessage("me", result);
		xmppService.sendMessage(result, recipient.getText().toString());
		m_discussionThread.getMessages().add(0,chatMessage);
		m_discussionThreadAdapter.notifyDataSetChanged();
    }
    
    private void setResults(Recognition.Result[] results)
    {

        if (results.length > 0)
        {
            setResult(results[0].getText());

        }
    }
    
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setVolumeControlStream(AudioManager.STREAM_MUSIC); // So that the 'Media Volume' applies to this activity

        
        _destroyed = false;
        // Create Vocalizer listener
        Vocalizer.Listener vocalizerListener = new Vocalizer.Listener()
        {
            @Override
            public void onSpeakingBegin(Vocalizer vocalizer, String text, Object context) {

            }

            @Override
            public void onSpeakingDone(Vocalizer vocalizer,
                    String text, SpeechError error, Object context) 
            {

            }
        };
        
        _vocalizer = ConnectionActivity.getSpeechKit().createVocalizerWithLanguage("es_MX", vocalizerListener, new Handler());   
        Intent connectionService = new Intent(this,XmppConnectionService.class); 
        bindService(connectionService,this, Context.BIND_AUTO_CREATE);
        
        setContentView(R.layout.chat);
        
        Intent intent = getIntent();
        CharSequence bundleUser = intent.getCharSequenceExtra("user");
        
        m_handler = new Handler();
        
        final TextView recipient = (TextView) this.findViewById(R.id.recipient);
		recipient.setText(bundleUser);
		final EditText message = (EditText) this.findViewById(R.id.embedded_text_editor);		
		ListView list = (ListView) this.findViewById(R.id.thread);
		
		
		m_discussionThread = (ChatMessageList) (savedInstanceState != null ? savedInstanceState.getParcelable(MESSAGES) : null);
		if ( m_discussionThread == null ) {
			m_discussionThread = new ChatMessageList();	
		}
		
		
		m_discussionThreadAdapter = new MessageAdapter(this, R.layout.multi_line_list_item, m_discussionThread);
		list.setAdapter(m_discussionThreadAdapter);
		
		
		Button send = (Button) this.findViewById(R.id.send_button);
		send.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				
            	_listeningDialog.setText("Initializing...");   
                showDialog(LISTENING_DIALOG);
            	_listeningDialog.setStoppable(false);
                setResults(new Recognition.Result[0]);

               _currentRecognizer = ConnectionActivity.getSpeechKit().createRecognizer(Recognizer.RecognizerType.Dictation, Recognizer.EndOfSpeechDetection.Long, "es_MX", _listener, _handler);
               _currentRecognizer.start();
                
				/*String to = recipient.getText().toString();
				String text = message.getText().toString();
				xmppService.sendMessage(text, to);
				ChatMessage chatMessage = new ChatMessage(to, text);
				m_discussionThread.getMessages().add(chatMessage);
				m_discussionThreadAdapter.notifyDataSetChanged();*/
			}
		});
		
		
        // Initialize the listening dialog
        createListeningDialog();
        
        SavedState savedState = (SavedState)getLastNonConfigurationInstance();
        if (savedState == null)
        {
            // Initialize the handler, for access to this application's message queue
            _handler = new Handler();
        } else
        {
            // There was a recognition in progress when the OS destroyed/
            // recreated this activity, so restore the existing recognition
            _currentRecognizer = savedState.Recognizer;
            _listeningDialog.setText(savedState.DialogText);
            _listeningDialog.setLevel(savedState.DialogLevel);
            _listeningDialog.setRecording(savedState.DialogRecording);
            _handler = savedState.Handler;
            
            if (savedState.DialogRecording)
            {
                // Simulate onRecordingBegin() to start animation
                _listener.onRecordingBegin(_currentRecognizer);
            }
            
            _currentRecognizer.setListener(_listener);
        }
		
		

		
		
	}
    private void createListeningDialog()
    {
        _listeningDialog = new ListeningDialog(this);
        _listeningDialog.setOnDismissListener(new OnDismissListener()
        {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (_currentRecognizer != null) // Cancel the current recognizer
                {
                    _currentRecognizer.cancel();
                    _currentRecognizer = null;
                }
                
                if (!_destroyed)
                {
                    // Remove the dialog so that it will be recreated next time.
                    // This is necessary to avoid a bug in Android >= 1.6 where the 
                    // animation stops working.
                    ChatActivity.this.removeDialog(LISTENING_DIALOG);
                    createListeningDialog();
                }
            }
        });
    }
    
    
    @Override
    protected void onPrepareDialog(int id, final Dialog dialog) {
        switch(id)
        {
        case LISTENING_DIALOG:
            _listeningDialog.prepare(new Button.OnClickListener()
            {
                @Override
                public void onClick(View v) {
                    if (_currentRecognizer != null)
                    {
                        _currentRecognizer.stopRecording();
                    }
                }
            });
            break;
        }
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id)
        {
        case LISTENING_DIALOG:
            return _listeningDialog;
        }
        return null;
    }
    
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(MESSAGES, m_discussionThread);
        super.onSaveInstanceState(outState);
    }
    
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
      m_discussionThread = savedInstanceState.getParcelable(MESSAGES);
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
    	super.onNewIntent(intent);
    	Log.i(TAG, intent.toString());
    }

	
	class MessageAdapter extends ArrayAdapter<ChatMessage> {
		
		Context context;
		
		MessageAdapter(Context context, int resource, ChatMessageList objects) {
			super(context, resource, objects.getMessages());
			this.context=context;
		}


		
		
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = View.inflate(context, R.layout.multi_line_list_item, null);
			if ( position < m_discussionThread.getMessages().size() ) {
				ChatMessage mess = m_discussionThread.getMessages().get(position);
				TextView message=(TextView)row.findViewById(R.id.message);
				message.setText(mess.getBody());
				
				TextView from=(TextView)row.findViewById(R.id.from);
				if ( mess.getFrom() != null ) {
					String fromName = StringUtils.parseBareAddress(mess.getFrom());
					from.setText(fromName +" :");
				} else {
					from.setText("me :");
				}
				
				ImageView icon=(ImageView)row.findViewById(R.id.icon);
				if ( position%2 == 0) {
					icon.setImageResource(R.drawable.bebe_gnu_small);
				} else {
					icon.setImageResource(R.drawable.bebe_tux_small);
				}
			}

			return(row);
		}
	}


	private IXmppConnectionService xmppService;
	
	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		Log.i(TAG, "Connected!"); 
        xmppService = ((XmppConnectionServiceBinder)service).getService(); 
		
		PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
		xmppService.getConnection().addPacketListener(new PacketListener() {
				public void processPacket(Packet packet) {
					Message message = (Message) packet;
					if (message.getBody() != null) {
						ChatMessage chatMessage = new ChatMessage(message.getFrom(), message.getBody());
						m_discussionThread.getMessages().add(0,chatMessage);

		                _lastTtsContext = new Object();
		                _vocalizer.speakString(chatMessage.getBody(), _lastTtsContext);
		                
						m_handler.post(new Runnable() {
							public void run() {
								m_discussionThreadAdapter.notifyDataSetChanged();
							}
						});
					}
				}
			}, filter);
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		Log.i(TAG, "DisConnected!"); 
	}

	
	
	
	


}