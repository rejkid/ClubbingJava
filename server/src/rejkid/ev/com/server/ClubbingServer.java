/**
 * 
 */
package rejkid.ev.com.server;

import handlers.Acceptor;
import handlers.AcceptorListener;
import handlers.HeaderBodyPacketChannel;
import handlers.IPacketChannel;
import handlers.PacketChannelListener;
import io.SelectorThread;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.log4j.Logger;

import rejkid.ev.com.MessageParser;
import rejkid.ev.com.NioUtil;
import rejkid.ev.com.ParsingClubDataException;

/**
 * A clubbing server using the IO Multiplexing framework. After accepting a connection, it will read
 * packets as defined by the SimpleProtocolDecoder class and echo them back.
 * 
 * This server can accept and manage large numbers of incoming connections. For added fun remove the
 * System.out statements and try it with several thousand (>10.000) clients. You might have to
 * increase the maximum number of sockets allowed by the operating system.
 * 
 * @author jdalecki
 * 
 */
public class ClubbingServer implements AcceptorListener, PacketChannelListener {
  /*
   * I don't envisage the bigger queue per channel then 50
   */
  private static final int CHANNEL_BYTE_ARRAY_QUEUE = 50;

  static Logger logger = Logger.getLogger("ClubbingServer.class");

  private final SelectorThread st;
  private Map<String, ClubItem> clubId2ClubInfo = Collections
      .synchronizedMap(new HashMap<String, ClubItem>());
  private List<IPacketChannel> users = Collections
      .synchronizedList(new ArrayList<IPacketChannel>());

  /*
   * This object maintains the user channels only, not the club ones
   */
  private Map<IPacketChannel, ArrayBlockingQueue<ByteBuffer>> userChannel2ByteBufferQueueMap = Collections
      .synchronizedMap(new HashMap<IPacketChannel, ArrayBlockingQueue<ByteBuffer>>());

  /**
   * Starts the server.
   * 
   * @param listenPort The port where to listen for incoming connections.
   * @throws Exception
   */
  public ClubbingServer(int listenPort) throws Exception {
    st = new SelectorThread();
    Acceptor acceptor = new Acceptor(listenPort, st, this);
    acceptor.openServerSocket();
  }

  public static void main(String[] args) throws Exception {
    int listenPort = Integer.parseInt(args[0]);
    new ClubbingServer(listenPort);
  }

  // ////////////////////////////////////////
  // Implementation of the callbacks from the
  // Acceptor and PacketChannel classes
  // ////////////////////////////////////////
  /**
   * A new client connected. Creates a PacketChannel to handle it.
   */
  public void socketConnected(Acceptor acceptor, SocketChannel sc) {
    try {
      // We should reduce the size of the TCP buffers or else we will
      // easily run out of memory when accepting several thousands of
      // connections.
      sc.socket().setReceiveBufferSize(2 * 1024);
      sc.socket().setSendBufferSize(2 * 1024);
      logger
          .info("Processing accept from new client=" + sc.getRemoteAddress() + " finished. 'HeaderBodyPacketChannel' created to handle requests\\responses for the client.\n");
      // The constructor enables reading automatically.
      HeaderBodyPacketChannel pc = new HeaderBodyPacketChannel(sc, st, this);

      pc.resumeReading();
    } catch (IOException e) {
      logger.error("Error sending packet", e);
    }
  }

  public void socketError(Acceptor acceptor, Exception ex) {
    logger.info("[" + acceptor + "] Error: " + ex.getMessage());
  }

  public void packetArrived(IPacketChannel pc, ByteBuffer pckt) {
    ClubItem reportingItem = new ClubItem();
    try {
      reportingItem.parseClubMessage(pc, pckt);
      pc.setChannelID(reportingItem.getId());
    } catch (ParsingClubDataException | IOException e) {
      logger.error("Could not parse the message", e);
    }
    logger
        .info("Packet body received [" + reportingItem.toString() + "] from " + pc.getChannelID());
    ClubItem updatedItem = reportingItem;
    synchronized (clubId2ClubInfo) {
      if (reportingItem.isClub()) {
        // Do we have that club already ?
        ClubItem existingItem = clubId2ClubInfo.get(reportingItem.getId());
        if (existingItem == null) {
          // no we don't, create one...
          clubId2ClubInfo.put(reportingItem.getId(), reportingItem);
        } else {
          // yes we do, append the latest update to the existing one
          existingItem.append(reportingItem);
          updatedItem = existingItem;
        }
      } else {
        // Just user registration
        users.add(pc);
      }
      queueMessages4Users(pc, updatedItem);
      if (reportingItem.isClub()) {
        // Its message from club - resume reading from the club...
        try {
          pc.resumeReading();
        } catch (IOException e) {
          try {
            logger.error("Could not resume reading on club channel="
                + pc.getSocketChannel().getRemoteAddress(), e);
          } catch (IOException e1) {
            logger.warn("Could not retrieve remote address", e1);
          }
        }
      }
      // Send all club items to all subscribed users ...
      synchronized (userChannel2ByteBufferQueueMap) {
        if (userChannel2ByteBufferQueueMap.keySet().size() > 0) {
          logger.info("REPORTING msg received by CHANNEL=" + pc.getChannelID() + " TO "
              + userChannel2ByteBufferQueueMap.keySet().size() + " of CHANNELS");
          logger.info("\n");
          for (IPacketChannel c : userChannel2ByteBufferQueueMap.keySet()) {
            logger.info("channel =" + c.getChannelID());
            ByteBuffer buffer = userChannel2ByteBufferQueueMap.get(c).poll();
            if (buffer != null) {
              c.sendPacket(buffer);
            }
          }
        } else {
          logger.info("Packet arrived but no channels to report to =" + pc.getChannelID());
        }
      }
    }
  }

  private void queueMessages4Users(IPacketChannel reportingChannel, ClubItem reportingItem) {
    synchronized (userChannel2ByteBufferQueueMap) {
      if (reportingItem.isClub()) {
        // its a club, update the queue of every user with the new info from the club...
        for (IPacketChannel sc : users) {
          ByteBuffer[] messages = MessageParser.composeUpdateMessage(reportingItem.getId(),
              reportingItem.getName(), reportingItem.getBoys(), reportingItem.getGirls());
          ByteBuffer message = NioUtil.appendByteBuffers(messages);
          ArrayBlockingQueue<ByteBuffer> queue = userChannel2ByteBufferQueueMap.get(sc);
          if (queue == null) {
            queue = new ArrayBlockingQueue<ByteBuffer>(CHANNEL_BYTE_ARRAY_QUEUE);
            userChannel2ByteBufferQueueMap.put(sc, queue);
          }
          queue.add(message);
        }
      } else {
        // its a user, send all available clubs to the new user...
        ArrayBlockingQueue<ByteBuffer> queue = userChannel2ByteBufferQueueMap.get(reportingChannel);
        if (queue == null) {
          queue = new ArrayBlockingQueue<ByteBuffer>(CHANNEL_BYTE_ARRAY_QUEUE);
          userChannel2ByteBufferQueueMap.put(reportingChannel, queue);
        }
        for (ClubItem ci : clubId2ClubInfo.values()) {
          ByteBuffer[] messages = MessageParser.composeUpdateMessage(ci.getId(), ci.getName(), ci
              .getBoys(), ci.getGirls());
          ByteBuffer message = NioUtil.appendByteBuffers(messages);
          queue.add(message);
        }

      }
    }
  }

  public void socketException(IPacketChannel pc, Exception ex) {
    logger.info("[" + pc.toString() + "] Error: " + ex.getMessage());
    removeDisconnectedSubscriber(pc);
  }

  public void socketDisconnected(IPacketChannel pc) {
    logger.info("[" + pc.toString() + "] Disconnected.");
    removeDisconnectedSubscriber(pc);
  }

  /**
   * The answer to a request was sent. Prepare to read the next request.
   */
  public void packetSent(IPacketChannel pc, ByteBuffer pckt) {
    logger.info("Packet [" + pc.toString() + "] SENT.");
    // Send all club items to the pc subscribed user ...
    synchronized (userChannel2ByteBufferQueueMap) {
      userChannel2ByteBufferQueueMap.get(pc);
      ByteBuffer buffer = userChannel2ByteBufferQueueMap.get(pc).poll();
      if (buffer != null) {
        logger.info("Submitting NEXT message to send to channel=" + pc.getChannelID());
        pc.sendPacket(buffer);
      }
    }
  }

  private void removeDisconnectedSubscriber(IPacketChannel pc) {
    synchronized (clubId2ClubInfo) {
      if (clubId2ClubInfo.containsValue(pc)) {
        clubId2ClubInfo.remove(pc);
      }
      clubId2ClubInfo.remove(pc.getSocketChannel());
    }
    synchronized (users) {
      if (users.contains(pc)) {
        users.remove(pc);
      }
    }
  }

}
