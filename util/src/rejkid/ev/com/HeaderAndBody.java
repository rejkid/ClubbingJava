/**
 * 
 */
package rejkid.ev.com;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

/**
 * @author jdalecki
 * 
 */
public class HeaderAndBody {
	static Logger logger = Logger.getLogger("HeaderAndBody.class");
	
	public static final int HEADER_SIZE = 10;
	private ByteBuffer header = ByteBuffer.allocateDirect(HEADER_SIZE);
	private ByteBuffer body = null;

	// private SelectionKey key;
	private boolean headerRead = false;
	private boolean bodyRead = false;
	int iteration = 0;
	byte soh = 0;
	byte magic = 0;
	int bodySize = -1;
	String command = "";

	public HeaderAndBody() {
		super();
	}

	/**
	 * @return the body
	 */
	public ByteBuffer getBody() {
		return body;
	}
	
	/**
	 * @return the soh
	 */
	public byte getSoh() {
		return soh;
	}

	/**
	 * @return the magic
	 */
	public byte getMagic() {
		return magic;
	}

	/**
	 * @return the command
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * @return the bodySize
	 */
	public int getBodySize() {
		return bodySize;
	}

	public int read(SocketChannel channel) throws IOException {
		int numRead = 0;
		if (!headerRead) {
			numRead = channel.read(header);
			if (numRead == -1) {
				return numRead;
			}
			if (!header.hasRemaining()) {
				logger.info("Got message from="
						+ channel.socket().getRemoteSocketAddress());
				int bodySize = parseHeader();
				if (bodySize != 0) {
					body = ByteBuffer.allocateDirect(bodySize);
				} else {
					// No body required
					bodyRead = true;
				}
				headerRead = true;
			}
		} else {
			if (body != null) {
				numRead = channel.read(body);
				if (numRead == -1) {
					return numRead;
				} else if (!body.hasRemaining()) {
					body.clear();
					bodyRead = true;
				}
			} else {
				bodyRead = true;
			}
		}
		return numRead;
	}

	public boolean hasHeader() {
		return headerRead;
	}

	public boolean hasBody() {
		return bodyRead;
	}

	public ByteBuffer getHeader() {
		return header;
	}

	private int parseHeader() {
		// return the body size
		header.clear();
		soh = header.get();
		magic = header.get();

		byte[] buffer = new byte[4];
		header.get(buffer, 0, 4);
		command = new String(buffer);

		bodySize = header.getInt();
		return bodySize;
	}

	public void reset() {
		header.clear();
		body = null;
		headerRead = false;
		bodyRead = false;
		soh = 0;
		magic = 0;
		command = "";
		bodySize = -1;
	}

}
