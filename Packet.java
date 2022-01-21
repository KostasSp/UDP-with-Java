import java.io.Serializable;
import java.util.Arrays;

/* creating a custom class for the packet object (for both client and server), 
which contains the SN and byte payload and implements methods to return them, 
and checks whether the packet received is the final packet, in order to close the connection.*/
public class Packet implements java.io.Serializable {

	public int seqNum;

	public boolean finalPacket;

	public byte[] data;

	// Packet class constructor
	public Packet(int seqNum, boolean finalPacket, byte[] data) {
		super();
		this.seqNum = seqNum;
		this.finalPacket = finalPacket;
		this.data = data;

	}

	// Getting the current SN
	public int getSeqNum() {
		return seqNum;
	}

	// get the byte data
	public byte[] getData() {
		return data;
	}

	// checking if current packet SN is the final
	public boolean isFinalPacket() {
		return finalPacket;
	}

}
