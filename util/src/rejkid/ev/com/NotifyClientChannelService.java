/**
 * 
 */
package rejkid.ev.com;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;

public class NotifyClientChannelService implements Runnable {
	static Logger logger = Logger.getLogger("NotifyClientChannelService.class");
	private Collection<SocketChannel> queue = new ArrayList<SocketChannel>();
	private NioServer server;

	public void processData(NioServer server, SocketChannel recipient) {
		this.server = server;

		synchronized (queue) {
			queue.add(recipient);
			// Notify only if we have something to write
			if (!queue.isEmpty()) {
				queue.notify();
			}
		}
	}

	public void run() {
		SocketChannel[] channels;
		while (true) {
			// Wait for data to become available
			synchronized (queue) {
				while (queue.isEmpty()) {
					try {
						queue.wait();
					} catch (InterruptedException e) {
						logger.error("", e);
					}
				}
				channels = queue.toArray(new SocketChannel[queue.size()]);
				queue.clear();
				for (SocketChannel recipient : channels) {
					// Reply to every client we know of...
					for (ChannelData producer : ((NioServer) server)
							.getExistingChannelData()) {
						ByteBuffer[] data = producer.composeMessage();
						if (data != null) {
							server.send(recipient, data);
						}
					}
				}
			}
		}
	}
}
