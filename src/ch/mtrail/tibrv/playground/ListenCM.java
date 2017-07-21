package ch.mtrail.tibrv.playground;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

import com.tibco.tibrv.Tibrv;
import com.tibco.tibrv.TibrvCmListener;
import com.tibco.tibrv.TibrvCmMsg;
import com.tibco.tibrv.TibrvCmTransport;
import com.tibco.tibrv.TibrvException;
import com.tibco.tibrv.TibrvListener;
import com.tibco.tibrv.TibrvMsg;
import com.tibco.tibrv.TibrvMsgCallback;
import com.tibco.tibrv.TibrvRvdTransport;

public class ListenCM implements TibrvMsgCallback {

	private boolean performDispatch = true;

	private String cmname = "MyProgrammAndTheTaskItDoesIdentification__ListenCM";
	private TibrvCmListener cmListener = null;

	public ListenCM(final String service, final String network, final String daemon, final String subject) {
		try {
			cmname = "MyProgrammAndTheTaskItDoesIdentification__ListenCM_" + InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
			System.exit(1);
		}

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
		TibrvRvdTransport transport = null;
		TibrvCmTransport cmTransport = null;
		try {
			transport = new TibrvRvdTransport(service, network, daemon);
			cmTransport = new TibrvCmTransport(transport, cmname, true);
		} catch (final TibrvException e) {
			System.err.println("Failed to create TibrvRvdTransport:");
			e.printStackTrace();
			System.exit(1);
		}

		try {
			cmListener = new TibrvCmListener(Tibrv.defaultQueue(), this, cmTransport, subject, null);
			System.err.println("Listening on: " + subject);

			// Set explicit confirmation
			cmListener.setExplicitConfirm();
		} catch (final TibrvException e) {
			System.err.println("Failed to create listener:");
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void dispatch() {
		while (true) {
			if (performDispatch) {
				// dispatch Tibrv events
				try {
					// Wait max 1 sec, to listen on keyboard.
					Tibrv.defaultQueue().timedDispatch(1);
				} catch (final TibrvException e) {
					System.err.println("Exception dispatching default queue:");
					e.printStackTrace();
					System.exit(1);
				} catch (final InterruptedException ie) {
					System.exit(1);
				}

			} else {
				// Dispatch is disabled, just idle
				try {
					Thread.sleep(500);
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void onMsg(final TibrvListener listener, final TibrvMsg msg) {
		try {
			System.out.println((new Date()).toString() + ": subject=" + msg.getSendSubject() + ", reply="
					+ msg.getReplySubject() + ", message=" + msg.toString());
			System.out.flush();

			// Report we are confirming message
			final long seqno = TibrvCmMsg.getSequence(msg);

			// do some work.
			Thread.sleep(500);

			// If it was not CM message or very first message
			// we'll get seqno=0. Only confirm if seqno > 0.
			if (seqno > 0) {
				System.out.println("\t\t\tConfirming message with seqno=" + seqno);

				// Confirm the message after we didt the work, so we can fetch it again as after program crash.
				cmListener.confirmMsg(msg);
			}

			msg.dispose();
		} catch (TibrvException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public boolean isPerformDispatch() {
		return performDispatch;
	}

	public void setPerformDispatch(final boolean performDispatch) {
		this.performDispatch = performDispatch;
	}

	public static void main(final String args[]) {
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

	private void startKeyListener() {
		printKeyUsage();

		new Thread(() -> {
			try (BufferedReader input = new BufferedReader(new InputStreamReader(System.in, "UTF-8"))) {
				while (true) {
					final char c = (char) input.read();

					switch (c) {
					case 'd':
					case 'D':
						System.out.println("Dispatcher is DISABLED");
						setPerformDispatch(false);
						break;

					case 'e':
					case 'E':
						System.out.println("Dispatcher is ENABLED");
						setPerformDispatch(true);
						break;

					case '\r':
					case '\n':
						break;

					default:
						printKeyUsage();
						break;
					}

				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}).start();
	}

	private void printKeyUsage() {
		System.out.println("Press\n\t\"D\" to disable Dispatcher\n\t\"E\" to enable Dispatcher ");
	}

}
