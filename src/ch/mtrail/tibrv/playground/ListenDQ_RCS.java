package ch.mtrail.tibrv.playground;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import com.tibco.tibrv.TibrvCmListener;
import com.tibco.tibrv.TibrvCmMsg;
import com.tibco.tibrv.TibrvCmQueueTransport;
import com.tibco.tibrv.TibrvDispatcher;
import com.tibco.tibrv.TibrvException;
import com.tibco.tibrv.TibrvListener;
import com.tibco.tibrv.TibrvMsg;
import com.tibco.tibrv.TibrvMsgCallback;
import com.tibco.tibrv.TibrvQueue;

public class ListenDQ_RCS extends Abstract {

	private final String dqGroupName = "DQgroupName";
	private TibrvQueue queue;
	private final Map<Thread, RcsDispatcher> dispatchers = new HashMap<>();
	private final SynchronousQueue<TibrvMsg> syncQueue = new SynchronousQueue<>();

	private final static int rv_threads = 1;
	private final static int threads = 5;
	private TibrvCmQueueTransport dq;

	public ListenDQ_RCS(final String service, final String network, final String daemon, final String subject)
			throws TibrvException {
		super(service, network, daemon);

		queue = new TibrvQueue();

		dq = new TibrvCmQueueTransport(transport, dqGroupName);
		dq.setWorkerTasks(rv_threads);

		new TibrvCmListener(queue, new RvDispatcher(syncQueue), dq, subject, null);
		System.out.println("Listening on: " + subject);

		// Init Thread to take msg from TbiRv and put them into syncQueue
		// Will block if no one is polling the queue
		for (int i = 1; i <= rv_threads; i++) {
			new TibrvDispatcher("RvDisp-" + i, queue);
		}

		// Process msgs form syncQueue
		for (int i = 1; i <= threads; i++) {
			final RcsDispatcher dispatcher = new RcsDispatcher(syncQueue);
			final Thread thread = new Thread(dispatcher, "Worker-" + i);
			thread.start();
			dispatchers.put(thread, dispatcher);
		}
	}

	public void printDebugInfo() throws TibrvException {
		while (true) {
				System.out.println(
						"QueueLength: " + queue.getCount() + " DQueueLength: " + dq.getUnassignedMessageCount());

				LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(10));
		}
	}

	@Override
	public void setPerformDispatch(final boolean performDispatch) {
		if (performDispatch) {
			dispatchers.keySet().forEach(thread -> {
				System.out.println("Starting: " + thread.getName());
				final RcsDispatcher dispatcher = dispatchers.get(thread);
				dispatcher.setRun(true);
			});
		} else {
			dispatchers.keySet().forEach(thread -> {
				System.out.println("Stopping: " + thread.getName());
				final RcsDispatcher dispatcher = dispatchers.get(thread);
				dispatcher.setRun(false);
			});
		}
	}

	public static void main(final String args[]) throws Exception {
		// Debug.diplayEnvInfo();

		final ArgParser argParser = new ArgParser("TibRvListenFT");
		argParser.setOptionalParameter("service", "network", "daemon");
		argParser.setRequiredArg("subject");
		argParser.parse(args);

		final ListenDQ_RCS listen = new ListenDQ_RCS(//
				argParser.getParameter("service"), //
				argParser.getParameter("network"), //
				argParser.getParameter("daemon"), //
				argParser.getArgument("subject"));

		listen.startKeyListener();

		listen.printDebugInfo();
	}

	class RvDispatcher implements TibrvMsgCallback {
		private final SynchronousQueue<TibrvMsg> syncQueue;

		public RvDispatcher(final SynchronousQueue<TibrvMsg> syncQueue) {
			this.syncQueue = syncQueue;
		}

		@Override
		public void onMsg(final TibrvListener listener, final TibrvMsg msg) {
			try {
				System.out.println((new Date()).toString() + " " + Thread.currentThread().getName() + " TAKE  " //
						+ "subject=" + msg.getSendSubject() + ", message=" + msg.toString() + ", seqno="
						+ TibrvCmMsg.getSequence(msg));

				syncQueue.put(msg);

			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}

	class RcsDispatcher implements Runnable {

		private final SynchronousQueue<TibrvMsg> syncQueue;
		private boolean run = true;

		public RcsDispatcher(final SynchronousQueue<TibrvMsg> syncQueue) {
			this.syncQueue = syncQueue;
		}

		@Override
		public void run() {
			while (true) {
				if (!run) {
					LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(500));
					continue;
				}

				try {
					final TibrvMsg msg = syncQueue.poll(500, TimeUnit.MILLISECONDS);
					if (msg != null) {
						onMsg(msg);
					}
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		}

		public void onMsg(final TibrvMsg msg) throws InterruptedException, TibrvException {
			System.out.println((new Date()).toString() + " " + Thread.currentThread().getName() + " START " //
					+ "subject=" + msg.getSendSubject() + ", message=" + msg.toString() + ", seqno="
					+ TibrvCmMsg.getSequence(msg));

			LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));

			msg.dispose();

			System.out.println((new Date()).toString() + " " + Thread.currentThread().getName() + " FINISHED");
		}

		public void setRun(final boolean run) {
			this.run = run;
		}
	}
}
