/**
 * 
 */
package rejkid.ev.com;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;

import org.apache.log4j.Logger;

public class NioServer implements Runnable, ISender {
	static Logger logger = Logger.getLogger("NioServer.class");

	// The host:port combination to listen on
	private InetAddress hostAddress;
	private int port;

	// The channel on which we'll accept connections
	private ServerSocketChannel serverChannel;

	// The selector we'll be monitoring
	private Selector selector;

	private ReplyService replyService;

	// A list of PendingChange instances
	private List<ChangeRequest> pendingChanges = new LinkedList<ChangeRequest>();

	// Maps a SocketChannel to a list of ByteBuffer instances
	private Map<SocketChannel, List<ByteBuffer>> pendingData = new HashMap<SocketChannel, List<ByteBuffer>>();

	// Maps a SocketChannel to a list of ByteBuffer instances
	private Map<SocketChannel, ChannelData> channel2DataMap = Collections
			.synchronizedMap(new HashMap<SocketChannel, ChannelData>());
	private NotifyClientChannelService notifyClientChannelService;

	public NioServer(InetAddress hostAddress, int port,
			ReplyService replySerice,
			NotifyClientChannelService notifyClientChannelService)
			throws IOException {
		this.hostAddress = hostAddress;
		this.port = port;
		this.selector = this.initSelector();
		this.replyService = replySerice;
		this.notifyClientChannelService = notifyClientChannelService;
	}

	public Collection<SocketChannel> getExistingChannels() {
		synchronized (channel2DataMap) {
			return new ArrayList<SocketChannel>(channel2DataMap.keySet());
		}
	}

	public Collection<ChannelData> getExistingChannelData() {
		synchronized (channel2DataMap) {
			return new ArrayList<ChannelData>(channel2DataMap.values());
		}
	}

	public void send(SocketChannel socket, ByteBuffer[] data) {

		// Here we should send to every client number of boys and girls for the
		// restaurant reported...
		// not just the socket

		synchronized (this.pendingChanges) {
			// Indicate we want the interest ops set changed
			this.pendingChanges.add(new ChangeRequest(socket,
					ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));

			// And queue the data we want to be written
			synchronized (this.pendingData) {
				for (ByteBuffer d : data) {
					List<ByteBuffer> queue = this.pendingData.get(socket);
					if (queue == null) {
						queue = new ArrayList<ByteBuffer>();
						this.pendingData.put(socket, queue);
					}
					queue.add(d);
						logger.info("Data queued=" + new String(d.toString())
								+ " for remote=" + socket.socket().getRemoteSocketAddress());
				}
			}
		}

		// Finally, wake up our selecting thread so it can make the required
		// changes
		logger.info("Waking up selector after mode change");
		this.selector.wakeup();
	}

	String getSelectionKeyStr(int val) {
		switch (val) {
		case SelectionKey.OP_ACCEPT:
			return "OP_ACCEPT";
		case SelectionKey.OP_CONNECT:
			return "OP_CONNECT";
		case SelectionKey.OP_READ:
			return "OP_READ";
		case SelectionKey.OP_WRITE:
			return "OP_WRITE";
		default:
			return "UNKNOWN";
		}
	}

	public void run() {
		while (true) {
			try {
				// Process any pending changes
				synchronized (this.pendingChanges) {
					Iterator changes = this.pendingChanges.iterator();
					while (changes.hasNext()) {
						ChangeRequest change = (ChangeRequest) changes.next();
						switch (change.type) {
						case ChangeRequest.CHANGEOPS:
							SelectionKey key = change.socket
									.keyFor(this.selector);
//							switchSelectionKeyMode(key, change.ops);
							key.interestOps(change.ops);
							logger.info("Switching to="+getSelectionKeyStr(change.ops) +" from run()");
						}
					}
					this.pendingChanges.clear();
				}

				// Wait for an event one of the registered channels
				this.selector.select();

				// Iterate over the set of keys for which events are available
				Iterator selectedKeys = this.selector.selectedKeys().iterator();
				while (selectedKeys.hasNext()) {
					SelectionKey key = (SelectionKey) selectedKeys.next();
					selectedKeys.remove();

					if (!key.isValid()) {
						continue;
					}

					// Check what event is available and deal with it
					if (key.isAcceptable()) {
						this.accept(key);
					} else if (key.isReadable()) {
						this.read(key);
					} else if (key.isWritable()) {
						this.write(key);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("Unexpected exception", e);
			}
		}
	}

	private void accept(SelectionKey key) throws IOException {
		// For an accept to be pending the channel must be a server socket
		// channel.
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key
				.channel();

		// Accept the connection and make it non-blocking
		SocketChannel socketChannel = serverSocketChannel.accept();
		Socket socket = socketChannel.socket();
		socketChannel.configureBlocking(false);

		// Register the new SocketChannel with our Selector, indicating
		// we'd like to be notified when there's data waiting to be read
		socketChannel.register(this.selector, SelectionKey.OP_READ);
		logger.info("Switching to="+getSelectionKeyStr(SelectionKey.OP_READ)+" from accept");
		logger.info("Listening on for read channel="
				+ socketChannel.socket().getRemoteSocketAddress());
		ServerChannelData scd = new ServerChannelData(this, socketChannel,
				replyService, notifyClientChannelService, null);
		if (channel2DataMap.get(socketChannel) == null) {
			channel2DataMap.put(socketChannel, scd);
		} else {
			System.out.println();
		}
		// Ask new ServerChannelData to accept the connection...
		scd.accept();
	}

	private void read(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();

		// There is no need to synchronize on channel2DataMap object as
		// channel2DataMap and sde contained by it
		// are accessed only by selecting thread.
		ChannelData sde = channel2DataMap.get(key.channel());

		synchronized (channel2DataMap) {
			// Attempt to read off the channel
			int numRead;
			try {
				numRead = sde.read(socketChannel);
			} catch (IOException e) {
				// The remote forcibly closed the connection, cancel
				// the selection key and close the channel.
				channel2DataMap.remove(key.channel());
				key.cancel();
				socketChannel.close();
				return;
			}

			if (numRead == -1) {
				// Remote entity shut the socket down cleanly. Do the
				// same from our end and cancel the channel.
				channel2DataMap.remove(key.channel());
				key.channel().close();
				key.cancel();
				return;
			}
		}
	}

	private void write(SelectionKey key) throws IOException {

		SocketChannel socketChannel = (SocketChannel) key.channel();

		synchronized (this.pendingData) {
			List<ByteBuffer> queue = this.pendingData.get(socketChannel);

			// Write until there's not more data ...
			while (!queue.isEmpty()) {
				ByteBuffer buf = (ByteBuffer) queue.get(0);
				logger.info("Writing data="
						+ new String(buf.toString()) + " for remote="
						+ socketChannel.socket().getRemoteSocketAddress());
				socketChannel.write(buf);
				if (buf.remaining() > 0) {
					// ... or the socket's buffer fills up
					break;
				}
				queue.remove(0);
			}

			if (queue.isEmpty()) {
				// We wrote away all data, so we're no longer interested
				// in writing on this socket. Switch back to waiting for
				// data.
				key.interestOps(SelectionKey.OP_READ);
				logger.info("Switching to="+getSelectionKeyStr(SelectionKey.OP_READ)+" from write()");
//				switchSelectionKeyMode(key, SelectionKey.OP_READ);
			}
		}

	}

	private void switchSelectionKeyMode(SelectionKey key, int mode) {
		key.interestOps(mode);
		logger.info("Switching to="+getSelectionKeyStr(mode));
		
	}

	private Selector initSelector() throws IOException {
		// Create a new selector
		Selector socketSelector = SelectorProvider.provider().openSelector();

		// Create a new non-blocking server socket channel
		this.serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);

		// Bind the server socket to the specified address and port
		InetSocketAddress isa = new InetSocketAddress(this.hostAddress,
				this.port);
		serverChannel.socket().bind(isa);

		// Register the server socket channel, indicating an interest in
		// accepting new connections
		logger.info("Switching to="+getSelectionKeyStr(SelectionKey.OP_ACCEPT));
		serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);

		return socketSelector;
	}

}
