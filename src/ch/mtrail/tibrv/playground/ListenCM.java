package ch.mtrail.tibrv.playground;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import com.tibco.tibrv.Tibrv;
import com.tibco.tibrv.TibrvCmListener;
import com.tibco.tibrv.TibrvCmMsg;
import com.tibco.tibrv.TibrvCmTransport;
import com.tibco.tibrv.TibrvException;
import com.tibco.tibrv.TibrvListener;
import com.tibco.tibrv.TibrvMsg;
import com.tibco.tibrv.TibrvMsgCallback;

public class ListenCM extends Abstract implements TibrvMsgCallback {

	private String cmname = "MyProgrammAndTheTaskItDoesIdentification__ListenCM";
	private TibrvCmListener cmListener = null;
	private TibrvCmTransport cmTransport = null;

	public ListenCM(final String service, final String network, final String daemon, final String subject)
			throws TibrvException, IOException {
		super(service, network, daemon);

		cmname = "MyProgrammAndTheTaskItDoesIdentification__ListenCM_" + InetAddress.getLocalHost().getHostName();

		cmTransport = new TibrvCmTransport(transport, cmname, true);

		cmListener = new TibrvCmListener(Tibrv.defaultQueue(), this, cmTransport, subject, null);
		System.out.println("Listening on: " + subject);

		// Set explicit confirmation
		cmListener.setExplicitConfirm();
	}

	public void dispatch() throws TibrvException, InterruptedException {
		dispatch(Tibrv.defaultQueue());
	}

	@Override
	public void onMsg(final TibrvListener listener, final TibrvMsg msg) {
		try {
			System.out.println((new Date()).toString() + ": subject=" + msg.getSendSubject() + ", reply="
					+ msg.getReplySubject() + ", message=" + msg.toString());

			// Report we are confirming message
			final long seqno = TibrvCmMsg.getSequence(msg);

			// do some work.
			LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(500));

			// If it was not CM message or very first message
			// we'll get seqno=0. Only confirm if seqno > 0.
			if (seqno > 0) {
				System.out.println("\t\t\tConfirming message with seqno=" + seqno);

				// Confirm the message after we didt the work, so we can fetch
				// it again as after program crash.
				cmListener.confirmMsg(msg);
			}

			msg.dispose();
		} catch (final TibrvException e) {
			e.printStackTrace();
		}
	}

	public static void main(final String args[]) throws Exception {
		// Debug.diplayEnvInfo();

		final ArgParser argParser = new ArgParser("TibRvListenCM");
		argParser.setOptionalParameter("service", "network", "daemon");
		argParser.setRequiredArg("subject");
		argParser.parse(args);

		final ListenCM listen = new ListenCM(//
				argParser.getParameter("service"), //
				argParser.getParameter("network"), //
				argParser.getParameter("daemon"), //
				argParser.getArgument("subject"));

		listen.startKeyListener();

		listen.dispatch();
	}
}
