/**
 * 
 */
package rejkid.ev.com.server;


import handlers.IPacketChannel;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.sun.org.apache.bcel.internal.generic.ISUB;

import rejkid.ev.com.MessageParser;
import rejkid.ev.com.MessageUtil;
import rejkid.ev.com.ParsingClubDataException;

/**
 * @author jdalecki
 * 
 */
public class ClubItem {
  private static int userId = 0;
	private String id = "";
	private String name = "";
	int boys = 0;
  int girls = 0;
  private boolean isClub = true;
  private IPacketChannel channel = null;


  public IPacketChannel getChannel() {
    return channel;
  }

  public boolean isClub() {
    return isClub;
  }

  public void setBoys(int boys) {
    this.boys = boys;
  }

	public void setGirls(int girls) {
    this.girls = girls;
  }

  public ClubItem() {
	}

  public ClubItem copy() {
    ClubItem copy = new ClubItem();
    copy.id = this.id;
    copy.name = this.name;
    copy.boys = this.boys;
    copy.girls = this.girls;
    copy.isClub = this.isClub;
    copy.channel = this.channel;
    return copy;
  }

  /**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the boys
	 */
	public int getBoys() {
		return boys;
	}

	/**
	 * @return the girls
	 */
	public int getGirls() {
		return girls;
	}

	void addBoy() {
		boys++;
	}

	void deleteBoy() {
		boys--;
	}

	void addGirl() {
		girls++;
	}

	void deleteGirl() {
		girls--;
	}

	public void parseClubMessage(IPacketChannel pc, ByteBuffer body)
			throws ParsingClubDataException, IOException {
	  this.channel = pc;
		if (pc.getCommand().equalsIgnoreCase(MessageParser.ADDB)) {
			addBoy();
		} else if (pc.getCommand().equalsIgnoreCase(MessageParser.DELB)) {
			deleteBoy();
		} else if (pc.getCommand().equalsIgnoreCase(MessageParser.ADDG)) {
			addGirl();
		} else if (pc.getCommand().equalsIgnoreCase(MessageParser.DELG)) {
			deleteGirl();
		} else if (pc.getCommand().equalsIgnoreCase(MessageParser.UPDT)) {
			boys = body.getShort();
			girls = body.getShort();
		}

		id = MessageUtil.readString(body);
		if(id.isEmpty()) {
		  isClub = false;
		  id = "USER_" + Integer.toString(userId++);
		} 
		name = MessageUtil.readString(body);
	}

	@Override
	public String toString() {
		return "ClubItem [id=" + id + ", name=" + name + ", boys=" + boys
				+ ", girls=" + girls + "isClub= "+isClub+" channel="+channel+"]";
	}

	public void append(ClubItem item) {
		boys += item.boys;
		girls += item.girls;
	}
}
