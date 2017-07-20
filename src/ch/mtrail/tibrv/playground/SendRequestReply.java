package ch.mtrail.tibrv.playground;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.tibco.tibrv.Tibrv;
import com.tibco.tibrv.TibrvException;
import com.tibco.tibrv.TibrvMsg;
import com.tibco.tibrv.TibrvRvdTransport;
import com.tibco.tibrv.TibrvTransport;

public class SendRequestReply {

	private TibrvTransport transport = null;
	private final String subject;
	private final String FIELD_NAME = "DATA";
	private final String FIELD_INDEX = "INDEX";
	private int msgSend = 0;

	public SendRequestReply(final String service, final String network, final String daemon, final String subject) {

		this.subject = subject;

		// open Tibrv in native implementation
		try {
			Tibrv.open(Tibrv.IMPL_NATIVE);
		} catch (

		final TibrvException e) {
			System.err.println("Failed to open Tibrv in native implementation:");
			e.printStackTrace();
			System.exit(0);
		}

		// Create RVD transport
		try {
			transport = new TibrvRvdTransport(service, network, daemon);
		} catch (final TibrvException e) {
			System.err.println("Failed to create TibrvRvdTransport:");
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void send(final String msgString) throws TibrvException {
		// Create the message
		final TibrvMsg msg = new TibrvMsg();

		// Set send subject into the message
		try {
			msg.setSendSubject(subject);
		} catch (final TibrvException e) {
			System.err.println("Failed to set send subject:");
			e.printStackTrace();
			System.exit(0);
		}

		msg.add(FIELD_NAME, msgString);
		msg.add(FIELD_INDEX, msgSend);
		System.out.println(
				(new Date()).toString() + " QUSTION: subject=" + msg.getSendSubject() + ", message=" + msg.toString());

		// Timeout 30sec
		final TibrvMsg replyMsg = transport.sendRequest(msg, 30);

		System.out.println(
				(new Date()).toString() + " REPLY: subject=" + msg.getSendSubject() + ", message=" + msg.toString());

		msg.dispose();
		replyMsg.dispose();
		msgSend++;
	}

	public static void main(final String args[]) {
		// Debug.diplayEnvInfo();

		final ArgParser argParser = new ArgParser("TibRvListen");
		// Interval milli seconds to repeat message
		argParser.setOptionalParameter("service", "network", "daemon", "interval");
		argParser.setRequiredArg("msg", "subject");
		argParser.parse(args);

		final SendRequestReply send = new SendRequestReply(//
				argParser.getParameter("service"), //
				argParser.getParameter("network"), //
				argParser.getParameter("daemon"), //
				argParser.getArgument("subject"));

		try {
			send.send(argParser.getArgument("msg"));
			System.out.println("Submitted: " + argParser.getArgument("msg"));

			final String intervalStr = argParser.getParameter("interval");
			if (Objects.nonNull(intervalStr) && !intervalStr.isEmpty()) {
				final int intervalMs = Integer.parseInt(intervalStr);
				while (true) {
					if (intervalMs > 0) {
						// -interval 0 == hard core stress test
						TimeUnit.MILLISECONDS.sleep(intervalMs);
					}
					send.send(argParser.getArgument("msg"));
				}
			}
		} catch (TibrvException | InterruptedException e) {
			e.printStackTrace();
		}

	}

}
