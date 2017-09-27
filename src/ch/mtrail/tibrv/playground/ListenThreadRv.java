package ch.mtrail.tibrv.playground;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import com.tibco.tibrv.TibrvDispatcher;
import com.tibco.tibrv.TibrvException;
import com.tibco.tibrv.TibrvListener;
import com.tibco.tibrv.TibrvMsg;
import com.tibco.tibrv.TibrvMsgCallback;
import com.tibco.tibrv.TibrvQueue;
import com.tibco.tibrv.TibrvQueueGroup;

public class ListenThreadRv extends Abstract implements TibrvMsgCallback {

	public ListenThreadRv(final String service, final String network, final String daemon, final String subject,
			final int threads) {
		super(service, network, daemon);

		try {
			final TibrvQueueGroup group = new TibrvQueueGroup();
			
			// Create main listener.
			final TibrvQueue queue = new TibrvQueue();
			new TibrvListener(queue, this, transport, subject, null);
			group.add(queue);

			// Create error listener
			group.add(createErrorHandler());

			for (int i = 0; i <= threads; i++) {
				new TibrvDispatcher("Dispatcher-" + i, group);
			}
			System.out.println("Created " + threads + " threads on " + subject);
		} catch (final TibrvException e) {
			System.err.println("Failed to create listener:");
			handleFatalError(e);
		}
	}

	@Override
	public void onMsg(final TibrvListener listener, final TibrvMsg msg) {
		System.out.println((new Date()).toString() + ": subject=" + msg.getSendSubject() + ", reply="
				+ msg.getReplySubject() + ", message=" + msg.toString() + " THREAD: " + Thread.currentThread().getName());
		System.out.flush();

		msg.dispose();

		LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(500));
	}

	public static void main(final String args[]) {
		// Debug.diplayEnvInfo();

		final ArgParser argParser = new ArgParser("ListenThreadRv");
		argParser.setRequiredParameter("threads");
		argParser.setOptionalParameter("service", "network", "daemon");
		argParser.setRequiredArg("subject");
		argParser.parse(args);

		new ListenThreadRv(//
				argParser.getParameter("service"), //
				argParser.getParameter("network"), //
				argParser.getParameter("daemon"), //
				argParser.getArgument("subject"), //
				Integer.parseInt(argParser.getParameter("threads")));
	}
}
