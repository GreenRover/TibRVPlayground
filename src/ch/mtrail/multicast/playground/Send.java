package ch.mtrail.multicast.playground;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.NumberFormat;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import ch.mtrail.tibrv.playground.ArgParser;

public class Send {
	private int msgCount = 0;
	private final InetAddress group;
	private final int port;
	private final DatagramSocket socket;

	public Send(final String multicastGroup, final int port) throws UnknownHostException, SocketException {
		group = InetAddress.getByName(multicastGroup);
		this.port = port;

		socket = new DatagramSocket();
		socket.setBroadcast(true);
		
		System.out.println("Send to " + group + ":" + port);
	}

	public void send(final String msgString) throws IOException {
		final byte[] buffer = msgString.getBytes();

		final DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, port);
		socket.send(packet);

		msgCount++;
	}

	public void printStatus() {
		final NumberFormat nf = NumberFormat.getInstance();
		System.out.print("Msg send: " + nf.format(msgCount) + "\n");
	}

	public static void main(final String args[]) throws Exception {
		// Debug.diplayEnvInfo();

		final ArgParser argParser = new ArgParser("SendMulticast");
		// Interval milli seconds to repeat message
		argParser.setOptionalParameter("interval");
		argParser.setRequiredArg("multicastGroup", "port", "msg");
		argParser.parse(args);

		final Send sender = new Send(//
				argParser.getArgument("multicastGroup"), //
				Integer.parseInt(argParser.getArgument("port")));

		sender.send(argParser.getArgument("msg"));
		System.out.println("Submitted: " + argParser.getArgument("msg"));

		final String intervalStr = argParser.getParameter("interval");
		if (Objects.nonNull(intervalStr) && !intervalStr.isEmpty()) {
			final int intervalMs = Integer.parseInt(intervalStr);
			while (true) {
				if (intervalMs > 0) {
					// -interval 0 == hard core stress test
					TimeUnit.MILLISECONDS.sleep(intervalMs);
				}
				sender.send(argParser.getArgument("msg"));
				sender.printStatus();
			}
		}

	}

}
