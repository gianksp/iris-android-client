package com.casmack;

import android.os.Binder;
import com.iris.R;

public class XmppConnectionServiceBinder extends Binder { 
  
    private IXmppConnectionService service = null; 
  
    public XmppConnectionServiceBinder(IXmppConnectionService service) { 
        super(); 
        this.service = service; 
    } 
 
    public IXmppConnectionService getService(){ 
        return service; 
    } 
};