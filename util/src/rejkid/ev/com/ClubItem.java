/**
 * 
 */
package rejkid.ev.com;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author jdalecki
 * 
 */
public class ClubItem {
	String id = "";
	String name = "";
	int boys = 0;
  int girls = 0;


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

	public void parseClubMessage(HeaderAndBody hb)
			throws ParsingClubDataException, IOException {
		if (hb.getCommand().equalsIgnoreCase(MessageParser.ADDB)) {
			addBoy();
		} else if (hb.getCommand().equalsIgnoreCase(MessageParser.DELB)) {
			deleteBoy();
		} else if (hb.getCommand().equalsIgnoreCase(MessageParser.ADDG)) {
			addGirl();
		} else if (hb.getCommand().equalsIgnoreCase(MessageParser.DELG)) {
			deleteGirl();
		} else if (hb.getCommand().equalsIgnoreCase(MessageParser.UPDT)) {
			boys = hb.getBody().getShort();
			girls = hb.getBody().getShort();
		}

		id = MessageUtil.readString(hb.getBody());
		name = MessageUtil.readString(hb.getBody());
		hb.reset();
	}

	@Override
	public String toString() {
		return "ClubItem [id=" + id + ", name=" + name + ", boys=" + boys
				+ ", girls=" + girls + "]";
	}
}
