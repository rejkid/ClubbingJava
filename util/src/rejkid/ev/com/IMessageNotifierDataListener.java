/*
 * Copyright (c) 2006 TYCO Traffic & Transportation (TT&T) Australasia.
 * All rights reserved.
 * 
 * File: IMessageReceiver.java
 * Created by: jdalecki, 03/06/2012
 * Last Modification by: $Author$, $Date$
 * SVN Revision: $Revision$
 * SVN Repository: $URL$
 */

package rejkid.ev.com;

/**
 * @author jdalecki
 *
 */
public interface IMessageNotifierDataListener {
  void receivedMessageNotifierData(final MessageNotifierData data);
}
