/*
 * Copyright (c) 2006 TYCO Traffic & Transportation (TT&T) Australasia.
 * All rights reserved.
 * 
 * File: MessageNotifierData.java
 * Created by: jdalecki, 02/06/2012
 * Last Modification by: $Author$, $Date$
 * SVN Revision: $Revision$
 * SVN Repository: $URL$
 */

package rejkid.ev.com;

import rejkid.ev.com.ClubItem;

/**
 * @author jdalecki
 *
 */
public class MessageNotifierData {
  @Override
  public String toString() {
    return "MessageNotifierData [status=" + status + ", item=" + item + ", errorStr=" + errorStr
        + "]";
  }
  public STATUS getStatus() {
    return status;
  }
  public ClubItem getItem() {
    return item;
  }
  public String getErrorStr() {
    return errorStr;
  }
  /**
   * @param status
   * @param item
   * @param errorStr
   */
  public MessageNotifierData(STATUS status, ClubItem item, String errorStr) {
    super();
    this.status = status;
    this.item = item;
    this.errorStr = errorStr;
  }
  public enum STATUS {CONNECTED, ERROR};
  private STATUS status;
  private ClubItem item;
  private String errorStr;
}
