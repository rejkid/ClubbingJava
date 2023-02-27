/**
 * 
 */
package rejkid.ev.com;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;


class ChannelData implements IProcessClubItem {
  static Logger logger = Logger.getLogger("ChannelData.class");
  protected ISender server;
  protected SocketChannel socket;
  protected HeaderAndBody request;
  protected ClubItem clubItem;
  protected ByteBuffer[] reply;
  private IMessageNotifierDataListener notifier;

  public ChannelData(ISender server, SocketChannel socket, IMessageNotifierDataListener notifier) {
    this.server = server;
    this.socket = socket;
    this.notifier = notifier;
    this.request = new HeaderAndBody();
    this.clubItem = new ClubItem();
  }

  public void close() {
    this.server = null;
    this.socket = null;
    this.notifier = null;
    this.request = null;
    this.clubItem = null;
  }
  
  public int read(SocketChannel channel) throws IOException {
    int retVal = request.read(channel);
    String body = (request.getBody() != null) ? new String(request.getBody().toString()) : "Empty";
    logger.info("Read message Header="
        + new String(request.getHeader().toString() + " Obj=" + this.toString())
        + " Read message Body=" + body);
    try {
      if (request.hasBody()) {
        synchronized (clubItem) {
          clubItem.parseClubMessage(request);
          receivedClubItem(clubItem);
          logger.info(clubItem.toString());
        }
      }
    } catch (ParsingClubDataException ex) {
      logger.info("", ex);
    }
    return retVal;
  }

  protected ByteBuffer[] composeMessage() {
    synchronized (clubItem) {
      if (!clubItem.getId().isEmpty()) {
        ByteBuffer[] data = MessageParser.composeUpdateMessage(clubItem.getId(),
            clubItem.getName(), clubItem.getBoys(), clubItem.getGirls());
        return data;
      } else {
        return null;
      }
    }
  }

  @Override
  public void receivedClubItem(ClubItem item) {
    synchronized (clubItem) {
      if (notifier != null) {
        notifier.receivedMessageNotifierData(new MessageNotifierData(MessageNotifierData.STATUS.CONNECTED, clubItem, null));
      }
    }
  }
}