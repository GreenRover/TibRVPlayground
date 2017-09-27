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

public class SendCM extends Abstract implements TibrvMsgCallback {

	private TibrvCmTransport cmTransport;
	private final String subject;
	private String cmname = "MyProgrammAndTheTaskItDoesIdentification__SendCM";
	// Confirmation advisory subject
	private static final String confirmAdvisorySubject = "_RV.INFO.RVCM.DELIVERY.CONFIRM.>";
	private int msgCount = 0;

	public SendCM(final String service, final String network, final String daemon, final String subject) {
		super(service, network, daemon);
		this.subject = subject;
		
		try {
			cmname = "MyProgrammAndTheTaskItDoesIdentification__SendCM_" + InetAddress.getLocalHost().getHostName();

			cmTransport = new TibrvCmTransport(transport, cmname, true);

			// Create listener for delivery confirmation advisory messages
			new TibrvListener(Tibrv.defaultQueue(), this, transport, confirmAdvisorySubject, null);
			new TibrvDispatcher(Tibrv.defaultQueue());

		} catch (final TibrvException | UnknownHostException e) {
			handleFatalError(e);
		}
	}

	@Override
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
		try {
			// Create the message
			final TibrvMsg msg = new TibrvMsg();
			
			msg.setSendSubject(subject);
			
			msg.add("DATA", msgString);
			msg.add("INDEX", msgCount);
			
			// Msg must be delivered within 5sec
			TibrvCmMsg.setTimeLimit(msg, 5.0);
			
			// Send message into the queue
			cmTransport.send(msg);
			
			System.out.println(
					(new Date()).toString() + " SEND: subject=" + msg.getSendSubject() + ", message=" + msg.toString());
			
			msg.dispose();
			msgCount++;
			
		} catch (final TibrvException e) {
			handleFatalError(e);
		}
	}
	
	public static void main(final String args[]) {
		// Debug.diplayEnvInfo();

		final ArgParser argParser = new ArgParser("SendCM");
		// Interval milli seconds to repeat message
		argParser.setOptionalParameter("service", "network", "daemon", "interval");
		argParser.setRequiredArg("msg", "subject");
		argParser.parse(args);

		final SendCM sender = new SendCM(//
				argParser.getParameter("service"), //
				argParser.getParameter("network"), //
				argParser.getParameter("daemon"), //
				argParser.getArgument("subject"));

		
		
		try {
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
				}
			}
			
			Tibrv.close();
		} catch (TibrvException | InterruptedException e) {
			e.printStackTrace();
		}
	}

}
