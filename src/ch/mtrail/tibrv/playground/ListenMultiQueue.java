package ch.mtrail.tibrv.playground;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import com.tibco.tibrv.TibrvException;
import com.tibco.tibrv.TibrvListener;
import com.tibco.tibrv.TibrvMsg;
import com.tibco.tibrv.TibrvMsgCallback;
import com.tibco.tibrv.TibrvQueue;
import com.tibco.tibrv.TibrvQueueGroup;

public class ListenMultiQueue extends Abstract implements TibrvMsgCallback {

	private TibrvQueueGroup group;

	public ListenMultiQueue(final String service, final String network, final String daemon,
			final String subjectPrefix) {
		super(service, network, daemon);

		try {
			// create two queues
			final TibrvQueue queue1 = new TibrvQueue();
			final TibrvQueue queue2 = new TibrvQueue();

			// set priorities
			queue1.setPriority(10);
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
			System.out.println("Listening on: " + subjectPrefix + ".COMMAND.>  with prio: " + queue1.getPriority());
			System.out.println("Listening on: " + subjectPrefix + ".VIDEO_STREAM.> with prio: " + queue2.getPriority());

			// Create error listener
			group.add(createErrorHandler());
		} catch (final TibrvException e) {
			handleFatalError(e);
		}
	}

	public void dispatch() {
		dispatch(group);
	}

	@Override
	public void onMsg(final TibrvListener listener, final TibrvMsg msg) {
		System.out.println((new Date()).toString() + ": subject=" + msg.getSendSubject() + ", reply="
				+ msg.getReplySubject() + ", message=" + msg.toString());
		System.out.flush();

		LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(500));
		
		if (msg.getReplySubject() != null) {
			// Send reply msg if a request subject is set.
			try {

				final TibrvMsg replyMsg = new TibrvMsg();
				replyMsg.add("TYPE", "ANSWER");
				replyMsg.add("ORG_MSG", msg.toString());

				listener.getTransport().sendReply(replyMsg, msg);
			} catch (final TibrvException e) {
				System.err.println("Failed to reply to msg:");
				e.printStackTrace();
			}
		}
		if (performDispose) {
			msg.dispose();
		}
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
}
