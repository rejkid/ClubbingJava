/**
 * 
 */
package handlers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * @author jdalecki
 * 
 */
public interface IPacketChannel {
	/**
	 * Activates reading from the socket. This method is non-blocking.
	 */
	public void resumeReading() throws IOException;

	public void close();

	/**
	 * Reads from the socket into the internal buffer. This method should be
	 * called only from the SelectorThread class.
	 */
	public void handleRead();

	/**
	 * Disable interest in reading.
	 * 
	 * @throws IOException
	 */
	public void disableReading() throws IOException;

	/**
	 * Sends a packet using non-blocking writes. One packet cannot be sent
	 * before the previous one has been dispatched. The caller must ensure this.
	 * 
	 * This class keeps a reference to buffer given as argument while sending
	 * it. So it is important not to change this buffer after calling this
	 * method.
	 * 
	 * @param packet
	 *            The packet to be sent.
	 */
	public void sendPacket(ByteBuffer packet);

	/**
	 * Writes to the underlying channel. Non-blocking. This method is called
	 * only from sendPacket() and from the SelectorThread class.
	 */
	public void handleWrite();

	public SocketChannel getSocketChannel();

	public String toString();
	
	/**
	 * @return the command
	 */
	public String getCommand();
	
  /**
   * @param id - id of the channel that serves the remote client (either user or club).
   */
  public void setChannelID(String id);

  /**
     * Returns the ID of this channel.
     * @return  id of the channel that serves the remote client (either user or club)..
   */
  public String getChannelID();
}
