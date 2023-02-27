/*
 * Copyright (c) 2006 TYCO Traffic & Transportation (TT&T) Australasia.
 * All rights reserved.
 * 
 * File: MessageUtil.java
 * Created by: lgardner, 23/10/2006
 * Last Modification by: $Author: lgardner $, $Date: 2010-05-10 16:28:39 +1000 (Mon, 10 May 2010) $
 * SVN Revision: $Revision: 9279 $
 * SVN Repository: $URL: http://subserver/repos/trunk/Node-Controller/src/tycoint/nodeplugins/status/util/MessageUtil.java $
 */

package rejkid.ev.com;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author jdalecki
 * 
 */
public abstract class MessageUtil {

  private static final int STR_LENGTH = 2;
public static final String ERROR_MAGIC = "ERRO";
  public static final String SUCCESS_MAGIC = "SUCC";

  public static final int MAX_STRING_LENGTH = 512 * 1024; // 1/2 Megabyte;

  private MessageUtil() {
    // Never called
  }

  public static ByteBuffer ensureCapacity(ByteBuffer bb, int capacity) {
    if (bb == null) {
      return ByteBuffer.allocate(capacity);
    } else if (bb.remaining() < capacity) {
      if (bb.position() == 0) {
        return ByteBuffer.allocate(capacity);
      }

      byte[] oldData = bb.array();
      byte[] newData = new byte[bb.position() + capacity];
      System.arraycopy(oldData, 0, newData, 0, bb.position());

      return ByteBuffer.wrap(newData);
    }

    return bb;
  }

  public static String readErrorString(ByteBuffer bb) {
    try {
      return readString(bb);
    } catch (IOException ex) {
      return (ex.getMessage() != null && ex.getMessage().length() != 0) ? ex.getMessage() : ex
          .toString();
    }
  }

  public static String readString(ByteBuffer bb) throws IOException {
    if (bb.remaining() < STR_LENGTH) {
      throw new IOException("Not enough data bytes (" + bb.remaining()
          + "), expecting at least 2 bytes");
    }
    short strlength = bb.getShort();
    if (strlength > bb.remaining()) {
      throw new IOException("Not enough data bytes (" + bb.remaining() + "), expecting "
          + strlength);
    }

    if (strlength > MAX_STRING_LENGTH) {
      throw new IOException("String message with length " + strlength
          + " exceeds maximum string length!");
    }

    byte[] buffer = new byte[strlength];
    bb.get(buffer, 0, strlength);

    return new String(buffer);
  }

  public static byte[] writeString(String message) {
    return writeString(message, null).array();
  }

  public static ByteBuffer writeString(String message, ByteBuffer buffer) {
    int length = message != null ? message.length() : 0;
    ByteBuffer bb = ensureCapacity(buffer, length + STR_LENGTH);

    bb.putShort((short)length);
    if (length != 0) {
      bb.put(message.getBytes());
    }

    return bb;
  }

  public static boolean compareMagic(byte[] magic1, byte[] magic2, int magic2offset) {
    return (magic1[0] == magic2[magic2offset] && magic1[1] == magic2[magic2offset + 1]
        && magic1[2] == magic2[magic2offset + 2] && magic1[3] == magic2[magic2offset + 3]);
  }
}
