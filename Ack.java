import java.io.Serializable;
import java.util.Arrays;

// creating a class for the ACK object (for both client and server) for instantiation
public class Ack implements java.io.Serializable {

	private int ackNum = 0;

	// Ack class constructor
	public Ack(int ackNum) {
		super();
		this.ackNum = ackNum;
	}

	// getting the current ack num
	public int getAckNum() {
		return ackNum;
	}

}
