package com.casmack;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.casmack.model.ChatMessage;
import com.iris.R;


public class ChatMessageList  implements Parcelable{

		private ArrayList<ChatMessage> messages = new ArrayList<ChatMessage>();

		public ChatMessageList(){
	          
	     }
	     
	     public ChatMessageList(Parcel in){
	          readFromParcel(in);
	     }
	     
	     
		 public  static final Parcelable.Creator<ChatMessageList> CREATOR = new Parcelable.Creator<ChatMessageList>() {
		
	          public ChatMessageList createFromParcel(Parcel in) {
	               return new ChatMessageList(in);
	          }

	          public ChatMessageList[] newArray(int arg0) {
	               return null;
	          }

	     };
	     
	     private void readFromParcel(Parcel in) {
	    	 messages.clear();

	          //First we have to read the list size
	          int size = in.readInt();

	          //Reading remember that we wrote first the Name and later the Phone Number.
	          //Order is fundamental
	          
	          for (int i = 0; i < size; i++) {
	               ChatMessage c = new ChatMessage();
	               c.setBody(in.readString());
	               c.setFrom(in.readString());
	               messages.add(c);
	          }
	          
	     }

	     public int describeContents() {
	          return 0;
	     }

	     public void writeToParcel(Parcel dest, int flags) {
	          int size = messages.size();
	          //We have to write the list size, we need him recreating the list
	          dest.writeInt(size);
	          //We decided arbitrarily to write first the Name and later the Phone Number.
	          for (int i = 0; i < size; i++) {
	               ChatMessage c = messages.get(i);
	               dest.writeString(c.getBody());
	               dest.writeString(c.getFrom());
	          }
	     }
	     
		public ArrayList<ChatMessage> getMessages() {
			return messages;
		}

	} 