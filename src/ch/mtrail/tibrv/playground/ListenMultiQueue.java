package ch.mtrail.tibrv.playground;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import com.tibco.tibrv.Tibrv;
import com.tibco.tibrv.TibrvException;
import com.tibco.tibrv.TibrvListener;
import com.tibco.tibrv.TibrvMsg;
import com.tibco.tibrv.TibrvMsgCallback;
import com.tibco.tibrv.TibrvQueue;
import com.tibco.tibrv.TibrvQueueGroup;
import com.tibco.tibrv.TibrvRvdTransport;
import com.tibco.tibrv.TibrvTransport;

public class ListenMultiQueue implements TibrvMsgCallback {

	private boolean performDispose = false;
	private boolean performDispatch = true;
	private TibrvQueueGroup group;

	public ListenMultiQueue(final String service, final String network, final String daemon,
			final String subjectPrefix) {

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
		TibrvTransport transport = null;
		try {
			transport = new TibrvRvdTransport(service, network, daemon);
		} catch (final TibrvException e) {
			System.err.println("Failed to create TibrvRvdTransport:");
			e.printStackTrace();
			System.exit(1);
		}

		try {
			// create two queues
			TibrvQueue queue1 = new TibrvQueue();
			TibrvQueue queue2 = new TibrvQueue();

			// set priorities
			queue1.setPriority(1);
			queue2.setPriority(2);

			final int limitPolicy = TibrvQueue.DISCARD_FIRST;
			final int maxEvents = 1000;
			final int discardAmount = 25;
			queue2.setLimitPolicy(limitPolicy, maxEvents, discardAmount);

			group = new TibrvQueueGroup();
			group.add(queue1);
			group.add(queue2);

			// Create listeners
			new TibrvListener(queue1, this, transport, subjectPrefix + ".COMMAND.>", null);
			new TibrvListener(queue2, this, transport, subjectPrefix + ".VIDEO_STREAM.>", null);

			// Create error listener
			final ErrorLogger errorLogger = new ErrorLogger();
			new TibrvListener(queue2, errorLogger, transport, "_RV.ERROR.>", null);
			new TibrvListener(queue2, errorLogger, transport, "_RV.WARN.>", null);
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
					group.timedDispatch(1);
				} catch (final TibrvException e) {
					System.err.println("Exception dispatching default queue:");
					e.printStackTrace();
					System.exit(1);
				} catch (final InterruptedException ie) {
					System.exit(1);
				}

			} else {
				// Dispatch is disabled, just idle
				LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(500));
			}
		}
	}

	@Override
	public void onMsg(final TibrvListener listener, final TibrvMsg msg) {
		System.out.println((new Date()).toString() + ": subject=" + msg.getSendSubject() + ", reply="
				+ msg.getReplySubject() + ", message=" + msg.toString());
		System.out.flush();

		if (msg.getReplySubject() != null) {
			// Send reply msg if a request subject is set.
			try {
				LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(500));

				final TibrvMsg replyMsg = new TibrvMsg();
				replyMsg.add("TYPE", "ANSWER");
				replyMsg.add("ORG_MSG", msg.toString());

				listener.getTransport().sendReply(replyMsg, msg);
			} catch (TibrvException e) {
				System.err.println("Failed to reply to msg:");
				e.printStackTrace();
			}
		}
		if (performDispose) {
			msg.dispose();
		}
	}

	public void setPerformDispose() {
		setPerformDispose(true);
	}

	public void setPerformDispose(final boolean performDispose) {
		this.performDispose = performDispose;
	}

	public boolean isPerformDispatch() {
		return performDispatch;
	}

	public void setPerformDispatch(boolean performDispatch) {
		this.performDispatch = performDispatch;
	}

	public static void main(final String args[]) {
		// Debug.diplayEnvInfo();

		final ArgParser argParser = new ArgParser("ListenMultiQueue ");
		argParser.setOptionalParameter("service", "network", "daemon");
		argParser.setRequiredArg("subject-prefix");
		argParser.setFlags("perform-dispose");
		argParser.parse(args);

		final String subjectPrefix = argParser.getArgument("subject-prefix");

		final ListenMultiQueue listen = new ListenMultiQueue(//
				argParser.getParameter("service"), //
				argParser.getParameter("network"), //
				argParser.getParameter("daemon"), //
				subjectPrefix);

		if (argParser.isFlagSet("perform-dispose")) {
			listen.setPerformDispose();
		}

		listen.startKeyListener();

		listen.dispatch();
	}

	private void startKeyListener() {
		printKeyUsage();

		new Thread(() -> {
			try (BufferedReader input = new BufferedReader(new InputStreamReader(System.in, "UTF-8"))) {
				while (true) {
					char c = (char) input.read();

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
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}

	private void printKeyUsage() {
		System.out.println("Press\n\t\"D\" to disable Dispatcher\n\t\"E\" to enable Dispatcher ");
	}

}
