/**
 * 
 */
package rejkid.ev.com;

import org.apache.log4j.Logger;

public class RspHandler {
	static Logger logger = Logger.getLogger("RspHandler.class");
	private byte[] rsp = null;
	
	public synchronized boolean handleResponse(byte[] rsp) {
		this.rsp = rsp;
		this.notify();
		return true;
	}
	
	public synchronized void waitForResponse() {
		while(this.rsp == null) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		logger.info(new String(this.rsp));
	}
}
