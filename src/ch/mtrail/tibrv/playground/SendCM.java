package ch.mtrail.tibrv.playground;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.tibco.tibrv.Tibrv;
import com.tibco.tibrv.TibrvCmMsg;
import com.tibco.tibrv.TibrvCmTransport;
import com.tibco.tibrv.TibrvDispatcher;
import com.tibco.tibrv.TibrvException;
import com.tibco.tibrv.TibrvListener;
import com.tibco.tibrv.TibrvMsg;
import com.tibco.tibrv.TibrvMsgCallback;
import com.tibco.tibrv.TibrvRvdTransport;

public class SendCM implements TibrvMsgCallback {

	private TibrvRvdTransport transport = null;
	private TibrvCmTransport cmTransport;
	private final String subject;
	private final String FIELD_NAME = "DATA";
	private final String FIELD_INDEX = "INDEX";
	private String cmname = "MyProgrammAndTheTaskItDoesIdentification__SendCM";
	// Confirmation advisory subject
	private static final String confirmAdvisorySubject = "_RV.INFO.RVCM.DELIVERY.CONFIRM.>";
	private int msgSend = 0;

	public SendCM(final String service, final String network, final String daemon, final String subject) {
		
		try {
			cmname = "MyProgrammAndTheTaskItDoesIdentification__SendCM_" + InetAddress.getLocalHost().getHostName();
		} catch (final UnknownHostException e1) {
			e1.printStackTrace();
			System.exit(1);
		}

		this.subject = subject;

		// open Tibrv in native implementation
		try {
			Tibrv.open(Tibrv.IMPL_NATIVE);
		} catch (

		final TibrvException e) {
			System.err.println("Failed to open Tibrv in native implementation:");
			e.printStackTrace();
			System.exit(1);
		}

		// Create RVD transport
		try {
			transport = new TibrvRvdTransport(service, network, daemon);
			cmTransport = new TibrvCmTransport(transport, cmname, true);

			// Create listener for delivery confirmation advisory messages
			new TibrvListener(Tibrv.defaultQueue(), this, transport, confirmAdvisorySubject, null);
			new TibrvDispatcher(Tibrv.defaultQueue());

		} catch (final TibrvException e) {
			System.err.println("Failed to create TibrvRvdTransport:");
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void onMsg(final TibrvListener listener, final TibrvMsg msg) {
		try {
			final long seqno = msg.getAsLong("seqno", 0);
			System.out.println((new Date()).toString() + " RECEIVED: seqno=" + seqno + " message=  " + msg.toString());
			System.out.flush();
		} catch (final TibrvException e) {
			System.out.println(
					"Exception occurred while getting \"seqno\" field from DELIVERY.CONFIRM advisory message:");
			e.printStackTrace();
			System.exit(1);
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
			System.exit(1);
		}

		msg.add(FIELD_NAME, msgString);
		msg.add(FIELD_INDEX, msgSend);
		
		// Msg must be delivered within 5sec
		TibrvCmMsg.setTimeLimit(msg, 5.0);

		// Send message into the queue
		cmTransport.send(msg);

		System.out.println(
				(new Date()).toString() + " SEND: subject=" + msg.getSendSubject() + ", message=" + msg.toString());

		msg.dispose();
		msgSend++;
	}
	
	public void stopDispatcher() {
		try {
			Tibrv.close();
		} catch (final TibrvException e) {
			e.printStackTrace();
		}
	}

	public static void main(final String args[]) {
		// Debug.diplayEnvInfo();

		final ArgParser argParser = new ArgParser("SendCM");
		// Interval milli seconds to repeat message
		argParser.setOptionalParameter("service", "network", "daemon", "interval");
		argParser.setRequiredArg("msg", "subject");
		argParser.parse(args);

		final SendCM send = new SendCM(//
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
		
		send.stopDispatcher();
	}

}
