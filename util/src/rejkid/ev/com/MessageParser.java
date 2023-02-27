package rejkid.ev.com;

import java.nio.ByteBuffer;

public class MessageParser {
	private static final int SHORT_LENGTH = 2;

	public static enum SEX  {MALE, FEMALE};
	
	public static byte SOH = 0x01;
	public static byte MAGIC = 0x03;
	
	public static String ADDB = "ADDB";
	public static String DELB = "DELB";
	public static String ADDG = "ADDG";
	public static String DELG = "DELG";
	public static String REST = "REST";
	public static String UPDT = "UPDT";
	public static String SUCC = "SUCC";
	public static String ERRO = "ERRO";

	static public ByteBuffer[] composeAddDeleteMessage(String id, String name, String command) {
		int bodyLength = SHORT_LENGTH+id.length()+SHORT_LENGTH+name.length();
		ByteBuffer header = buildHeader(command, bodyLength);
		ByteBuffer body = ByteBuffer.allocate(bodyLength);
		body.putShort((short)id.length());
		body.put(id.getBytes());
		body.putShort((short)name.length());
		body.put(name.getBytes());
		header.flip();
		body.flip();
		return new ByteBuffer[]{header, body};
	}
	
	static public ByteBuffer[] composeUpdateMessage(String id, String name, int boys, int girls) {
		int bodyLength = SHORT_LENGTH+SHORT_LENGTH+SHORT_LENGTH+id.length()+SHORT_LENGTH+name.length();
		ByteBuffer header = buildHeader(UPDT, bodyLength);
		ByteBuffer body = ByteBuffer.allocate(bodyLength);
		body.putShort((short)boys);
		body.putShort((short)girls);
		MessageUtil.writeString(id, body);
		MessageUtil.writeString(name, body);
		header.flip();
		body.flip();
		return new ByteBuffer[]{header, body};
	}

	static public ByteBuffer buildHeader (String command, int bodyLength) {
		ByteBuffer bb = ByteBuffer.allocate(HeaderAndBody.HEADER_SIZE);
		bb.put(SOH);
		bb.put(MAGIC);
		bb.put(command.getBytes());
		bb.putInt(bodyLength);
		return bb;
	}
	
	static public ByteBuffer[] buildReply (String error) {
		boolean succ = error == null;
		ByteBuffer header = buildHeader(succ ? SUCC : ERRO, succ ? 0 : SHORT_LENGTH+error.length());
		ByteBuffer body = ByteBuffer.allocate(SHORT_LENGTH+error.length());
		body.putInt(error.length());
		body.put(error.getBytes());
		header.flip();
		body.flip();
		return new ByteBuffer[]{header, body};
	}
}
