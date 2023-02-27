/**
 * 
 */
package rejkid.ev.com;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Date;

import org.apache.log4j.Logger;


/**
 * @author Janusz
 * 
 */
public class ServerChannelData extends ChannelData {
	static Logger logger = Logger.getLogger("ServerChannelData.class");

	private ReplyService replyService;
	private NotifyClientChannelService notifyClientChannelService;

	public ServerChannelData(NioServer server, SocketChannel socket,
			ReplyService replyService,
			NotifyClientChannelService notifyClientChannelService, IMessageNotifierDataListener notifier) {
		super(server, socket, notifier);
		this.replyService = replyService;
		this.notifyClientChannelService = notifyClientChannelService;
	}

  public void receivedClubItem(ClubItem item) {
    super.receivedClubItem(item);
    logger.info("Got body. Processing data");
    ByteBuffer[]  replyMsg = composeMessage();
    // if replyMsg == null it is just a registration from the user, not a club update
    if(replyMsg != null) {
    	// Club is updating its data...
    	replyService.processData((NioServer) server, composeMessage());
    }
  }

	protected void accept() {
		// We have just accepted the connection from new client channel, so send
		// to the new client channel all the other clients data we have...
		notifyClientChannelService.processData((NioServer) server, socket);
	}
}
