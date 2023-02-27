/**
 * 
 */
package rejkid.ev.com;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

public class ReplyService implements Runnable {
	static Logger logger = Logger.getLogger("ReplyService.class");
	private List<ByteBuffer> queue = new ArrayList<ByteBuffer>();
	NioServer server;
	int clientNumber = 0;

	public void processData(NioServer server, ByteBuffer[] data) {
		this.server = server;
		synchronized (queue) {

			for (ByteBuffer b : data) {
				if (b != null) {
					queue.add(b);
				}
			}
			// Notify only if we have something to write
			if (!queue.isEmpty()) {
				queue.notify();
			}
		}
	}

	public void run() {
		ByteBuffer[] data;

		while (true) {
			// Wait for data to become available
			synchronized (queue) {
				while (queue.isEmpty()) {
					try {
						queue.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				data = queue.toArray(new ByteBuffer[queue.size()]);
				queue.clear();
				// Reply to every client we know of...
				Collection<SocketChannel> channels = server
						.getExistingChannels();
				for (SocketChannel c : channels) {
					logger.info("Sending data to channel="+c.toString());
					ByteBuffer[] localData = new ByteBuffer[data.length];
					for (int i = 0; i < data.length; i++) {
						localData[i] = data[i].duplicate();
					}
					server.send(c, localData);
				}

			}

		}
	}
}
