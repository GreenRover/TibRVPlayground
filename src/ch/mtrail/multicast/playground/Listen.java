package ch.mtrail.multicast.playground;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Date;

import ch.mtrail.tibrv.playground.ArgParser;

public class Listen {

	private final MulticastSocket socket;
	protected byte[] buf = new byte[256];
	private final InetAddress group;

	public Listen(final String multicastGroup, final int port) throws IOException {
		socket = new MulticastSocket(port);
		group = InetAddress.getByName(multicastGroup);
		socket.joinGroup(group);
		
		System.out.println("Joining " + group + ":" + port);
	}

	public void dispatch() {
		while (true) {
			try {
				final DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				final String received = new String(packet.getData(), 0, packet.getLength());
				if ("end".equals(received)) {
					close();
				} else {
					System.out.println((new Date()).toString() + ": " + received);
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void close() {
		try {
			socket.leaveGroup(group);
		} catch (final IOException e) {
			System.out.println(e);
		}
		socket.close();
		System.exit(0);
	}

	public static void main(final String args[]) throws Exception {
		final ArgParser argParser = new ArgParser("MulticastListen");
		argParser.setRequiredArg("multicastGroup", "port");
		argParser.parse(args);

		final Listen listen = new Listen(//
				argParser.getArgument("multicastGroup"), //
				Integer.valueOf(argParser.getArgument("port")));

		listen.dispatch();
	}
}
