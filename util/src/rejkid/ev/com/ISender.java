/**
 * 
 */
package rejkid.ev.com;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author Janusz
 *
 */
public interface ISender {
	void send(SocketChannel socket, ByteBuffer[] data);

}
