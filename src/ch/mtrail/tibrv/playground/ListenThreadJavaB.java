package ch.mtrail.tibrv.playground;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import com.tibco.tibrv.TibrvException;
import com.tibco.tibrv.TibrvListener;
import com.tibco.tibrv.TibrvMsg;
import com.tibco.tibrv.TibrvMsgCallback;
import com.tibco.tibrv.TibrvQueue;
import com.tibco.tibrv.TibrvQueueGroup;

public class ListenThreadJavaB extends Abstract implements TibrvMsgCallback {

	private TibrvQueueGroup group;
	private final ExecutorService threadPool;

	public ListenThreadJavaB(final String service, final String network, final String daemon, final String subject,
			final int threads) {
		super(service, network, daemon);

		threadPool = Executors.newFixedThreadPool(threads);

		try {
			group = new TibrvQueueGroup();
			final TibrvQueue queue = new TibrvQueue();
			new TibrvListener(queue, this, transport, subject, null);
			group.add(queue);

			// Create error listener
			group.add(createErrorHandler());
		} catch (final TibrvException e) {
			System.err.println("Failed to create listener:");
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void dispatch() {
		dispatch(group);
	}

	@Override
	public void onMsg(final TibrvListener listener, final TibrvMsg msg) {
		System.out.println((new Date()).toString() + ": take THREAD: " + Thread.currentThread().getName());
		System.out.flush();
		
		// System.out.println("onMsg");
		threadPool.execute(() -> {
			System.out.println((new Date()).toString() + ": subject=" + msg.getSendSubject() + ", reply="
					+ msg.getReplySubject() + ", message=" + msg.toString() + " THREAD: " + Thread.currentThread().getName());
			System.out.flush();

			LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(500));

			msg.dispose();
		});
	}

	public static void main(final String args[]) {
		final ArgParser argParser = new ArgParser("ListenThreadJavaB ");
		argParser.setRequiredParameter("threads");
		argParser.setOptionalParameter("service", "network", "daemon");
		argParser.setRequiredArg("subject");
		argParser.parse(args);

		final ListenThreadJavaB listen = new ListenThreadJavaB(//
				argParser.getParameter("service"), //
				argParser.getParameter("network"), //
				argParser.getParameter("daemon"), //
				argParser.getArgument("subject"), //
				Integer.parseInt(argParser.getParameter("threads")));

		listen.dispatch();
	}
}
