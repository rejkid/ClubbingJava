/**
 * 
 */
package rejkid.ev.com;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

/**
 * @author jdalecki
 * 
 */
public class NioUtil {
	static public String getSelectionString(final int selectionKeys) {
		StringBuffer selectionKeysStr = new StringBuffer();
		if ((selectionKeys & SelectionKey.OP_ACCEPT) != 0) {
			addOptStr(selectionKeysStr, "ACCEPT");
		}
		if ((selectionKeys & SelectionKey.OP_CONNECT) != 0) {
			addOptStr(selectionKeysStr, "CONNECT");
		}
		if ((selectionKeys & SelectionKey.OP_READ) != 0) {
			addOptStr(selectionKeysStr, "READ");
		}
		if ((selectionKeys & SelectionKey.OP_WRITE) != 0) {
			addOptStr(selectionKeysStr, "WRITE");
		}
		if(selectionKeysStr.length() == 0) {
			addOptStr(selectionKeysStr, "NO_INTEREST");
		}
		
		return selectionKeysStr.toString();
	}
	
	/*
	 * This methos assumes the ByteBuffers buffers are flipped
	 */
	static public ByteBuffer appendByteBuffers(final ByteBuffer[] buffers) {
	  int size = 0;
	  for(ByteBuffer b : buffers) {
	    size += b.limit();
	  }
	  ByteBuffer output = ByteBuffer.allocate(size);
    for(ByteBuffer b : buffers) {
      output.put(b);
    }
    output.flip();
    return output;
  }

	private static void addOptStr(StringBuffer selectionKeysStr, String opt) {
		if(selectionKeysStr.length() != 0) {
			selectionKeysStr.append("|");
		}
		selectionKeysStr.append(opt);
	}
}
