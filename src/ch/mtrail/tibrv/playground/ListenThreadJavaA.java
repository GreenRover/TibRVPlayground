package ch.mtrail.tibrv.playground;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

public class ListenThreadJavaA implements TibrvMsgCallback {

	private TibrvQueueGroup group;

	public ListenThreadJavaA(final String service, final String network, final String daemon, final String subject) {

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
			group = new TibrvQueueGroup();
			final TibrvQueue queue = new TibrvQueue();
			new TibrvListener(queue, this, transport, subject, null);
			group.add(queue);

			// Create error listener
			final TibrvQueue errorQueue = new TibrvQueue();
			final ErrorLogger errorLogger = new ErrorLogger();
			new TibrvListener(errorQueue, errorLogger, transport, "_RV.ERROR.>", null);
			new TibrvListener(errorQueue, errorLogger, transport, "_RV.WARN.>", null);
			group.add(errorQueue);
		} catch (final TibrvException e) {
			System.err.println("Failed to create listener:");
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void dispatch(final int threads) {
		final ExecutorService threadPool = Executors.newFixedThreadPool(threads);
		for (int i = 0; i <= threads; i++) {
			threadPool.execute(() -> {
				while (true) {
					try {
						group.dispatch();
					} catch (final TibrvException | InterruptedException e) {
						System.err.println("Exception dispatching default queue:");
						e.printStackTrace();
					}
				}
			});
		}
		System.out.println("Created " + threads + " threads");
		
	}

	@Override
	public void onMsg(final TibrvListener listener, final TibrvMsg msg) {
		System.out.println((new Date()).toString() + ": subject=" + msg.getSendSubject() + ", reply="
				+ msg.getReplySubject() + ", message=" + msg.toString());
		System.out.flush();
		
		msg.dispose();
		
		LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(500));
	}

	public static void main(final String args[]) {
		final ArgParser argParser = new ArgParser("ListenThreadJavaA");
		argParser.setRequiredParameter("threads");
		argParser.setOptionalParameter("service", "network", "daemon");
		argParser.setRequiredArg("subject");
		argParser.parse(args);

		final ListenThreadJavaA listen = new ListenThreadJavaA(//
				argParser.getParameter("service"), //
				argParser.getParameter("network"), //
				argParser.getParameter("daemon"), //
				argParser.getArgument("subject")
				);

		listen.dispatch(Integer.parseInt(argParser.getParameter("threads")));
	}	
}
