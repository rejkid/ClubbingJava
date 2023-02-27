/**
 * 
 */
package rejkid.ev.com;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import static rejkid.ev.com.GlobalConstants.*;

import org.apache.log4j.Logger;


public class NioClient implements Runnable, ISender, IMessageNotifierDataListener {
  static Logger logger = Logger.getLogger("NioClient.class");

  // The selector we'll be monitoring
  private Selector selector;

  // A list of PendingChange instances
  private List<ChangeRequest> pendingChanges = new LinkedList<ChangeRequest>();

  // Maps a SocketChannel to a list of ByteBuffer instances
  private Map<SocketChannel, List<ByteBuffer>> pendingData = new HashMap<SocketChannel, List<ByteBuffer>>();

  private boolean keepAlive = false;
  
  AtomicBoolean abort = new AtomicBoolean(false);

  // The host:port combinations to connect to
  private InetAddress[] hostAddresses;
  private int port;
  private List<IMessageNotifierDataListener> listeners;

  // Maps a SocketChannel to a list of ByteBuffer instances
  private Map<SocketChannel, ChannelData> channel2DataMap = Collections
      .synchronizedMap(new HashMap<SocketChannel, ChannelData>());

  public String NAME = "";
  public String ID = "";

  public NioClient(InetAddress[] hostAddresses, int port) throws IOException {
    logger.info("NioClient ctor");
    listeners = new CopyOnWriteArrayList<IMessageNotifierDataListener>();
    this.selector = this.initSelector();
    this.hostAddresses = hostAddresses;
    this.port = port;
  }

  public void connectClient() throws IOException {
    for (SocketChannel channel : channel2DataMap.keySet()) {
      try {
        channel2DataMap.get(channel).close();
        channel.close();
      } catch (Exception ex) {
        logger.error("Could not close the channel to the server: "
            + channel.socket().getRemoteSocketAddress(), ex);
      }
    }
    channel2DataMap.clear();
    // Connect to all servers
    for (InetAddress hostAddress : hostAddresses) {
      SocketChannel socketChannel = null;
      // Create a non-blocking socket channel
      socketChannel = SocketChannel.open();
      socketChannel.configureBlocking(false);

      // Kick off connection establishment
      logger.info("Connecting to channel..."+socketChannel);
      boolean finished = socketChannel.connect(new InetSocketAddress(hostAddress, port));
      logger.info("Returned from connecting to channel="+socketChannel+" finished="+finished);

      connect(socketChannel);
    }
    // Finally, wake up our selecting thread so it can make the required
    // changes
    this.selector.wakeup();
  }

  /**
   * Finish the NIO thread.
   * 
   */
  public void abort() {
    abort.set(true);
    this.selector.wakeup();
  }

  /**
   * Register listener for message update event.
   * 
   * @param listener
   */
  public void addMessageListener(IMessageNotifierDataListener listener) {
    synchronized (listeners) {
      if (!listeners.contains(listener)) {
        listeners.add(listener);
      }
    }
  }

  /**
   * Register listener for message update event.
   * 
   * @param listener
   */
  public void removeMessageListener(IMessageNotifierDataListener listener) {
    synchronized (listeners) {
      listeners.remove(listener);
    }
  }

  private void fireMessageUpdate(MessageNotifierData data) {
    logger.info("fireMessageUpdate() - msg=" + data.toString());

    for (final IMessageNotifierDataListener l : listeners) {
      try {
        l.receivedMessageNotifierData(data);
      } catch (Exception ex) {
        logger.error("Unhandled error while pushing message update event.", ex);
      }
    }
  }

  private void connect(SocketChannel socketChannel) {
    // Queue a channel registration since the caller is not the
    // selecting thread. As part of the registration we'll register
    // an interest in connection events. These are raised when a channel
    // is ready to complete connection establishment.
    synchronized (this.pendingChanges) {
      this.pendingChanges.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER,
          SelectionKey.OP_CONNECT));
      // Register the response handler
      // this.rspHandlers.put(socketChannel, new RspHandler());
    }
  }

  public void send(SocketChannel socket, ByteBuffer[] data) {
    // // Start a new connection
    // SocketChannel socket = this.initiateConnection();
    //
    // // Register the response handler
    // this.rspHandlers.put(socket, handler);

    // And queue the data we want written
    synchronized (this.pendingData) {
      for (ByteBuffer d : data) {

        List<ByteBuffer> queue = this.pendingData.get(socket);
        if (queue == null) {
          queue = new ArrayList<ByteBuffer>();
          this.pendingData.put(socket, queue);
        }
        queue.add(d);
      }
    }

    this.pendingChanges.add(new ChangeRequest(socket, ChangeRequest.CHANGEOPS,
        SelectionKey.OP_WRITE));
    logger.info("Switching to write mode on channel:" + socket.socket().getLocalAddress());

    // Finally, wake up our selecting thread so it can make the required
    // changes
    this.selector.wakeup();
  }

  public void run() {
    while (!abort.get()) {
      try {
        // Process any pending changes
        synchronized (this.pendingChanges) {
          Iterator changes = this.pendingChanges.iterator();
          while (changes.hasNext()) {
            ChangeRequest change = (ChangeRequest) changes.next();
            switch (change.type) {
            case ChangeRequest.CHANGEOPS:
              SelectionKey key = change.socket.keyFor(this.selector);
              key.interestOps(change.ops);
              break;
            case ChangeRequest.REGISTER:
              change.socket.register(this.selector, change.ops);
              break;
            }
          }
          this.pendingChanges.clear();
        }

        // Wait for an event one of the registered channels
        this.selector.select();

        // Iterate over the set of keys for which events are available
        Iterator<SelectionKey> selectedKeys = this.selector.selectedKeys().iterator();
        while (selectedKeys.hasNext()) {
          SelectionKey key = (SelectionKey) selectedKeys.next();
          selectedKeys.remove();
          SocketChannel socketChannel = (SocketChannel) key.channel();
          logger
              .info("Woken up from select on channel:" + socketChannel.socket().getLocalAddress());

          if (!key.isValid()) {
            continue;
          }

          // try {
          // if (key.isConnectable()) {
          // this.finishConnection(key);
          // }
          // } catch (Exception ex) {
          //
          // }

          // Check what event is available and deal with it
          if (key.isConnectable()) {
            this.finishConnection(key);
          } else if (key.isReadable()) {
            this.read(key);
          } else if (key.isWritable()) {
            this.write(key);
          }
        }
      } catch (Exception e) {
        logger.error("Unexpected error", e);
      }
    }
    logger.error("NIOClient finished");
  }

  private void read(SelectionKey key) throws IOException {
    SocketChannel socketChannel = (SocketChannel) key.channel();

    ChannelData sde = channel2DataMap.get(key.channel());
    // Attempt to read off the channel
    int numRead;
    try {
      numRead = sde.read(socketChannel);
    } catch (IOException e) {
      // The remote forcibly closed the connection, cancel the selection key and close the channel.
      key.cancel();
      socketChannel.close();
      logger
          .warn(
              "The remote forcibly closed the connection, cancel the selection key and close the channel",
              e);
      return;
    }

    if (numRead == -1) {
      // Remote entity shut the socket down cleanly. Do the same from our end and cancel the
      // channel.
      key.channel().close();
      key.cancel();
      logger.info("Remote entity shut the socket down cleanly(numRead == -1)"
          + socketChannel.socket().getLocalAddress());
      return;
    }
  }

  // private void handleResponse(SocketChannel socketChannel, byte[] data,
  // int numRead) throws IOException {
  // // Make a correctly sized copy of the data before handing it
  // // to the client
  // byte[] rspData = new byte[numRead];
  // System.arraycopy(data, 0, rspData, 0, numRead);
  //
  // // Look up the handler for this channel
  // RspHandler handler = (RspHandler) this.rspHandlers.get(socketChannel);
  //
  // // And pass the response to it
  // if (handler.handleResponse(rspData)) {
  // // The handler has seen enough, close the connection
  // socketChannel.close();
  // socketChannel.keyFor(this.selector).cancel();
  // }
  // }

  private void write(SelectionKey key) throws IOException {
    SocketChannel socketChannel = (SocketChannel) key.channel();

    synchronized (this.pendingData) {
      List queue = (List) this.pendingData.get(socketChannel);

      // Write until there's not more data ...
      while (!queue.isEmpty()) {
        ByteBuffer buf = (ByteBuffer) queue.get(0);
        logger.info("Writing data from client =" + new String(buf.toString()) + " for remote="
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
        logger.info("All data writen. Switching to READ mode on channel:"
            + socketChannel.socket().getLocalAddress());
        key.interestOps(SelectionKey.OP_READ);
      }
    }
  }

  private void finishConnection(SelectionKey key) throws IOException {
    SocketChannel socketChannel = (SocketChannel) key.channel();
    boolean connected = false;
    // Finish the connection. If the connection operation failed
    // this will raise an IOException.
    try {
      logger.info("Begin to wait for connection to "
          + socketChannel.socket().getRemoteSocketAddress());
      connected = socketChannel.finishConnect();
    } catch (IOException e) {
      // Cancel the channel's registration with our selector
      logger.warn("Canceling the channel's registration with our selector", e);
      key.cancel();
      socketChannel.close();
      receivedMessageNotifierData(new MessageNotifierData(MessageNotifierData.STATUS.ERROR, null, e
          .getMessage()));
      return;
    } catch (Throwable e) {
      // Cancel the channel's registration with our selector
      logger.info("Cancel the channel's registration with our selector", e);
      key.cancel();
      socketChannel.close();
      logger.info("Closed the channel"+socketChannel);
      receivedMessageNotifierData(new MessageNotifierData(MessageNotifierData.STATUS.ERROR, null, e
          .getMessage()));
      return;
    }
    if(!connected) {
      // not connected yet
      return;
    }
    if (channel2DataMap.get(socketChannel) == null) {
      channel2DataMap.put(socketChannel, new ChannelData(this, socketChannel, this));
    }
    receivedMessageNotifierData(new MessageNotifierData(MessageNotifierData.STATUS.CONNECTED, null, null));

    // Register an interest in writing on this channel
    // key.interestOps(SelectionKey.OP_WRITE);

    // Register an interest in reading on this channel
    key.interestOps(SelectionKey.OP_READ);
    logger.info("Entering initial READ mode after connection on channel:"
        + socketChannel.socket().getLocalAddress());
    initialClubUpdate();
  }

  private Selector initSelector() throws IOException {
    // Create a new selector
    return SelectorProvider.provider().openSelector();
  }

  public void initialClubUpdate() {
    ByteBuffer[] msg;
    // Initial update...
    msg = MessageParser.composeUpdateMessage(ID, NAME, 0, 0);
    add(msg);
  }

  public static void main(String[] args) {
    try {
      NioClient client = new NioClient(
          new InetAddress[] { InetAddress.getByName(REMOTE_SERVER_IP) }, SERVER_PORT);
      client.addMessageListener(new IMessageNotifierDataListener() {

        @Override
        public void receivedMessageNotifierData(MessageNotifierData data) {
          if (data.getStatus() == MessageNotifierData.STATUS.CONNECTED) {
            logger.info(data.toString());
          } else {
            logger.info("Could not connect to server: " + data.getErrorStr());
          }
        }
      });

      client.ID = args[0];
      client.NAME = args[1];
      Thread t = new Thread(client);
      t.start();
      client.connectClient();

      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

      String input;
      while ((input = in.readLine()) != null) {
        if (input.equalsIgnoreCase("addBoy")) {
          ByteBuffer[] msg = MessageParser.composeAddDeleteMessage(client.ID, client.NAME,
              MessageParser.ADDB);
          client.add(msg);
        } else if (input.equalsIgnoreCase("addGirl")) {
          ByteBuffer[] msg = MessageParser.composeAddDeleteMessage(client.ID, client.NAME,
              MessageParser.ADDG);
          client.add(msg);
        } else if (input.equalsIgnoreCase("delGirl")) {
          ByteBuffer[] msg = MessageParser.composeAddDeleteMessage(client.ID, client.NAME,
              MessageParser.DELG);
          client.add(msg);
        } else if (input.equalsIgnoreCase("delBoy")) {
          ByteBuffer[] msg = MessageParser.composeAddDeleteMessage(client.ID, client.NAME,
              MessageParser.DELB);
          client.add(msg);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void add(ByteBuffer[] msg) {
    // RspHandler handler = new RspHandler();
	  if(channel2DataMap.keySet().isEmpty()) {
		  logger.warn("No clients to send the message to!!!");
	  }
    for (SocketChannel socket : channel2DataMap.keySet()) {
      send(socket, msg);
    }
  }

  @Override
  public void receivedMessageNotifierData(MessageNotifierData data) {
    fireMessageUpdate(data);
  }

}
