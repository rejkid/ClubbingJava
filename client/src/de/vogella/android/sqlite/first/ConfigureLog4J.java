package de.vogella.android.sqlite.first;

/**
 * Copyright (c) 2006 TYCO Traffic & Transportation (TT&T) Australasia.
 * All rights reserved.
 * 
 * File: ConfigureLog4J.java
 * Created by: jdalecki, 25/05/2012
 * Last Modification by: $Author$, $Date$
 * SVN Revision: $Revision$
 * SVN Repository: $URL$
 */

import org.apache.log4j.Level;
import android.os.Environment;
import de.mindpipe.android.logging.log4j.LogConfigurator;

/** * Call {@link #configure()} from your application's activity. */
public class ConfigureLog4J {
  public static void configure() {
    final LogConfigurator logConfigurator = new LogConfigurator();
    logConfigurator.setFileName(Environment.getExternalStorageDirectory() + "/clubbing.log");
    logConfigurator.setRootLevel(Level.DEBUG); // Set log level of a specific logger
    logConfigurator.setLevel("org.apache", Level.ERROR);
    logConfigurator.configure();
  }
}
