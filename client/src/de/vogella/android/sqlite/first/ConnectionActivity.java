/*
 * Copyright (c) 2006 TYCO Traffic & Transportation (TT&T) Australasia.
 * All rights reserved.
 * 
 * File: ConnectionActivity.java
 * Created by: jdalecki, 30/05/2012
 * Last Modification by: $Author$, $Date$
 * SVN Revision: $Revision$
 * SVN Repository: $URL$
 */

package de.vogella.android.sqlite.first;

import rejkid.ev.com.NioClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;

/**
 * @author jdalecki
 *
 */
public class ConnectionActivity extends Activity {
  private NioClient client;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    setContentView(R.layout.connection);
  }

}
