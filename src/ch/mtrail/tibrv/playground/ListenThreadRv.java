package ch.mtrail.tibrv.playground;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import com.tibco.tibrv.Tibrv;
import com.tibco.tibrv.TibrvDispatcher;
import com.tibco.tibrv.TibrvException;
import com.tibco.tibrv.TibrvListener;
import com.tibco.tibrv.TibrvMsg;
import com.tibco.tibrv.TibrvMsgCallback;
import com.tibco.tibrv.TibrvQueue;
import com.tibco.tibrv.TibrvQueueGroup;
import com.tibco.tibrv.TibrvRvdTransport;
import com.tibco.tibrv.TibrvTransport;

public class ListenThreadRv implements TibrvMsgCallback {

	public ListenThreadRv(final String service, final String network, final String daemon, final String subject,
			final int threads) {

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
			final TibrvQueueGroup group = new TibrvQueueGroup();
			
			// Create main listener.
			final TibrvQueue queue = new TibrvQueue();
			new TibrvListener(queue, this, transport, subject, null);
			group.add(queue);

			// Create error listener
			final TibrvQueue errorQueue = new TibrvQueue();
			final ErrorLogger errorLogger = new ErrorLogger();
			new TibrvListener(errorQueue, errorLogger, transport, "_RV.ERROR.>", null);
			new TibrvListener(errorQueue, errorLogger, transport, "_RV.WARN.>", null);
			group.add(errorQueue);

			for (int i = 0; i <= threads; i++) {
				new TibrvDispatcher("Dispatcher-" + i, group);
			}
			System.out.println("Created " + threads + " threads on " + subject);
		} catch (final TibrvException e) {
			System.err.println("Failed to create listener:");
			e.printStackTrace();
			System.exit(1);
		}
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
