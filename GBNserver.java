import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.Serializable;

public class GBNserver {

	// setting a probability of loss during packet sending
	public static double packetLossRate = 0.05;

	// method for serializing the object to send to the client
	public static byte[] serialize(Object object) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(object);
		return baos.toByteArray();
	}

	// method for de-serializing the object received from the server
	public static Object deSerialize(byte[] bytes) throws IOException, ClassNotFoundException {
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		ObjectInputStream ois = new ObjectInputStream(bais);
		return ois.readObject();
	}

	public static void main(String[] args) throws Exception {

		// Instantiating a Client object and passing hostname and port as arguments
		GBNclient client = new GBNclient("localhost", 9876);

		// opening socket to act as the receiving point for client data
		DatagramSocket fromClient = new DatagramSocket(client.port);

		System.out.println("Server socket is now open");

		// allocating enough bytes to receive the Packet object
		byte[] receivedData = new byte[100];

		/*
		 * checking if the latest SN is the one that is expected.
		 * "The receiver process keeps track of the sequence number of the next frame it expects to receive"
		 * (wiki)
		 */
		int nextSeqNumExpected = 0;

		// Array to receive the de-serialized data
		ArrayList<Packet> dataArray = new ArrayList<Packet>();

		// this sets/stops the while loop of the server listening for packets
		boolean stopLoop = false;

		while (!stopLoop) {

			System.out.println("Server still open");

			// Creating datagram packet to receive client's packet
			DatagramPacket receivedPacket = new DatagramPacket(receivedData, receivedData.length);
			fromClient.receive(receivedPacket);

			/*
			 * De-serializing the Packet object(custom Packet class, see Packet.java)
			 * received from the Client
			 */
			Packet packet = (Packet) GBNserver.deSerialize(receivedPacket.getData());

			// examinining if the SN is the one expected and whether it is last
			if (packet.getSeqNum() == nextSeqNumExpected && packet.isFinalPacket()) {

				nextSeqNumExpected++;
				// adding data received to an array for printing
				dataArray.add(packet);
				System.out.println(
						"Packet with sequence number " + nextSeqNumExpected + " has been received");

				System.out.println("Last packet received");

				stopLoop = true;

				// examinining if the SN is matching and whether it is NOT last
			} else if (packet.getSeqNum() == nextSeqNumExpected && !packet.isFinalPacket()) {

				nextSeqNumExpected++;
				dataArray.add(packet);
				System.out.println(
						"Packet with sequence number " + nextSeqNumExpected + " has been received");
				System.out.println("Packet placed in the buffer");

			} else {
				System.out.println("Packet not in order; Discarded");
			}

			// Creating an ACK object (custom Ack class, see Ack.java)
			Ack ackObject = new Ack(nextSeqNumExpected);

			// Serializing the ACK object
			byte[] ackBytes = GBNserver.serialize(ackObject);

			// ACK datagram packet for the client
			DatagramPacket ackDatagram = new DatagramPacket(ackBytes, ackBytes.length,
					receivedPacket.getAddress(), receivedPacket.getPort());

			// Send acknowldgement packet with some probability of loss
			if (packetLossRate < Math.random()) {
				fromClient.send(ackDatagram);
			} else {
				System.out.println("ACK Packet with sequence number " + ackObject.getAckNum()
						+ " has been lost");
			}

			System.out.println("Re-transmitting ACK for Sequence Number " + (nextSeqNumExpected));

		}

		System.out.println(" ----------------------------------- ");
		// Print the data received
		System.out.print("Message: ");
		for (Packet p : dataArray) {
			for (byte b : p.getData()) {
				System.out.print((char) b);
			}
		}
		fromClient.close();
		System.out.println("");
		System.out.println("All data have been received - Server socket closed");
	}
}
