import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Scanner;

public class GBNclient {

	private String IPAddress;

	int port;

	// setting a probability of loss during packet sending
	public static double packetLossRate = 0.05;

	/*
	 * Sliding window size for Go-Back-N - No. of packets that can be sent without
	 * acknowledgement
	 */
	public static int windowSizeGBN = 5;

	// milliseconds to pass before re-transmitting all the non-acked packets
	public static int timer = 100;

	// Data to be sent
	public static String file = new String("umbrella");
	public static byte[] fileBytes = file.getBytes();

	// client constructor
	public GBNclient(String IPAdr, int port) {
		this.IPAddress = IPAdr;
		this.port = port;
	}

	/*
	 * method for serializing the object to send to the server Serialization allows
	 * us to convert objects to bytes in order to send our data over a channel, and
	 * then de-serialize it back to an object on the receiving side
	 */
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

		// Initial sequence number
		int SN = 0;

		/*
		 * waiting for packet acknowledgement from server, before moving the sliding
		 * window
		 */
		int waitingForAck = 0;

		// Host name
		InetAddress IPAddress = InetAddress.getByName(client.IPAddress);

		// Port number
		int port = client.port;

		// Final packet's sequence number; utilised when closing the connection
		int finalPacketSN = fileBytes.length;

		System.out.println("Number of packets to be sent: " + finalPacketSN);

		// opening socket to send packets to the server
		DatagramSocket toServer = new DatagramSocket();

		/*
		 * List of all the packets sent to the server. This will be used to re-transmit
		 * sent packets that have not been acknowledged
		 */
		ArrayList<Packet> sentPackets = new ArrayList<Packet>();

		// sending until the whole file has been iterated
		while (SN < file.length() + 1) {

			/*
			 * Sending while the there is up to 5 un-acknowleded packets, and current SN is
			 * not the final SN
			 */
			while (SN - waitingForAck < windowSizeGBN && SN < finalPacketSN) {

				/*
				 * initialising byte array for the bytes to be sent. The message is sent
				 * character by character, so the byte size is only 1
				 */
				byte[] fileByteArray = new byte[1];

				/*
				 * Copying part of data bytes to a byte array, which will be the size of bytes
				 * being sent off with a single packet.
				 */
				fileByteArray = Arrays.copyOfRange(fileBytes, SN, SN + 1);

				/*
				 * Creates a Packet class object (custom class, see Packet.java) containing the
				 * byte data and the SN, and is also checking whether this is the last packet
				 * received. If it is, it turns the Server variable "stopLoop" to true, then the
				 * Server closes its socket and prints the received data
				 */
				Packet PacketObject = new Packet(SN, (SN == finalPacketSN - 1) ? true : false,
						fileByteArray);

				// Serializing the Packet object
				byte[] byteDataToServer = GBNclient.serialize(PacketObject);

				/*
				 * Creating the datagram packet to send all the above, with all the required UDP
				 * headers
				 */
				DatagramPacket packet = new DatagramPacket(byteDataToServer, byteDataToServer.length,
						IPAddress, port);

				// Add packet to the sent packets list
				sentPackets.add(PacketObject);

				// Send packet with some probability of loss
				if (packetLossRate < Math.random()) {
					toServer.send(packet);
				} else {
					System.out.println("LOST packet with sequence number -> " + SN);
				}

				// Increment the sequence number
				SN++;

				System.out.println("Sending packet with sequence number " + SN);

			}

			// Byte array for the ACK sent by the receiver
			byte[] ackByteArray = new byte[40];

			// Creating datagram packet to send the ack to the server
			DatagramPacket ackPacket = new DatagramPacket(ackByteArray, ackByteArray.length);

			try {
				/*
				 * If an ACK is not received during the time allocated, all packets in window
				 * are re-sent (see CATCH below)
				 */
				toServer.setSoTimeout(timer);

				// Receive the ack
				toServer.receive(ackPacket);

				// Unserialize the Ack object (custom Ack class, see Ack.java)
				Ack ackObject = (Ack) GBNclient.deSerialize(ackPacket.getData());

				System.out.println("Received ACK for packet " + ackObject.getAckNum());

				/*
				 * If the ACK in the received packet is equal to the final seq. number, the
				 * client stops transmission
				 */
				if (ackObject.getAckNum() == finalPacketSN) {
					break;
				}

				if (waitingForAck > ackObject.getAckNum()) {
					continue;
				} else {
					waitingForAck = ackObject.getAckNum();
				}

			} catch (SocketTimeoutException e) {

				// re-send all the non-acked packets after timeout
				for (int i = waitingForAck; i < SN; i++) {

					// Serializing the Packet object
					byte[] byteDataToServer = GBNclient.serialize(sentPackets.get(i));

					// Creating the datagram packet (of the non-acked packets) to send to server
					DatagramPacket packet = new DatagramPacket(byteDataToServer, byteDataToServer.length,
							IPAddress, port);

					// Sending acknowldgement packet with some probability of loss
					if (Math.random() > packetLossRate) {
						toServer.send(packet);
					} else {
						System.out.println("Packet with sequence number "
								+ (sentPackets.get(i).getSeqNum()) + " has been lost");

					}

					System.out.println("Re-transmitting the packet with sequence number "
							+ sentPackets.get(i).getSeqNum());
				}

			}

		}
		// close socket
		toServer.close();
		System.out.println(" ----------------------------------- ");
		System.out.println("All data have been sent - Client socket closed");

	}

}
