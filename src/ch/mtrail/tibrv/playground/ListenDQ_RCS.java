package ch.mtrail.tibrv.playground;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import com.tibco.tibrv.Tibrv;
import com.tibco.tibrv.TibrvCmListener;
import com.tibco.tibrv.TibrvCmMsg;
import com.tibco.tibrv.TibrvCmQueueTransport;
import com.tibco.tibrv.TibrvDispatcher;
import com.tibco.tibrv.TibrvException;
import com.tibco.tibrv.TibrvListener;
import com.tibco.tibrv.TibrvMsg;
import com.tibco.tibrv.TibrvMsgCallback;
import com.tibco.tibrv.TibrvQueue;
import com.tibco.tibrv.TibrvRvdTransport;

public class ListenDQ_RCS {

	private final String dqGroupName = "DQgroupName";
	private TibrvQueue queue;
	private final Map<Thread, RcsDispatcher> dispatchers = new HashMap<>();
	private final SynchronousQueue<TibrvMsg> syncQueue = new SynchronousQueue<>();

	private final static int rv_threads = 1;
	private final static int threads = 5;
	private TibrvCmQueueTransport dq;

	public ListenDQ_RCS(final String service, final String network, final String daemon, final String subject) {

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
		try {
			transport = new TibrvRvdTransport(service, network, daemon);
		} catch (final TibrvException e) {
			System.err.println("Failed to create TibrvRvdTransport:");
			e.printStackTrace();
			System.exit(1);
		}

		try {
			queue = new TibrvQueue();

			dq = new TibrvCmQueueTransport(transport, dqGroupName);
			dq.setWorkerTasks(rv_threads);

			new TibrvCmListener(queue, new RvDispatcher(syncQueue), dq, subject, null);
			System.err.println("Listening on: " + subject);

		} catch (final TibrvException e) {
			System.err.println("Error setup distributed queue or listener");
			e.printStackTrace();
			System.exit(1);
		}

		// Init Thread to take msg from TbiRv and put them into syncQueue
		// Will block if no one is polling the queue
		for (int i = 1; i <= rv_threads; i++) {
			new TibrvDispatcher("RvDisp-" + i, queue);
		}

		// Process msgs form syncQueue
		for (int i = 1; i <= threads; i++) {
			RcsDispatcher dispatcher = new RcsDispatcher(syncQueue);
			final Thread thread = new Thread(dispatcher, "Worker-" + i);
			thread.start();
			dispatchers.put(thread, dispatcher);
		}
	}

	public void printDebugInfo() {
		while (true) {
			try {
				System.out.println(
						"QueueLength: " + queue.getCount() + " DQueueLength: " + dq.getUnassignedMessageCount());
				System.out.flush();

				LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(10));
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void setPerformDispatch(final boolean performDispatch) {
		if (performDispatch) {
			dispatchers.keySet().forEach(thread -> {
				System.out.println("Starting: " + thread.getName());
				RcsDispatcher dispatcher = dispatchers.get(thread);
				dispatcher.setRun(true);
			});
		} else {
			dispatchers.keySet().forEach(thread -> {
				System.out.println("Stopping: " + thread.getName());
					RcsDispatcher dispatcher = dispatchers.get(thread);
					dispatcher.setRun(false);
			});
		}
	}

	public static void main(final String args[]) {
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
			System.out.flush();

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
		while (true ) {
			if (!run) {
				LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(500));
				continue;
			}
			
			try {
				final TibrvMsg msg = syncQueue.poll(500, TimeUnit.MILLISECONDS);
				if (msg != null) {
					onMsg(msg);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void onMsg(final TibrvMsg msg) throws InterruptedException, TibrvException {
		System.out.println((new Date()).toString() + " " + Thread.currentThread().getName() + " START " //
				+ "subject=" + msg.getSendSubject() + ", message=" + msg.toString() + ", seqno="
				+ TibrvCmMsg.getSequence(msg));
		System.out.flush();

		LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));

		msg.dispose();

		System.out.println((new Date()).toString() + " " + Thread.currentThread().getName() + " FINISHED");
		System.out.flush();
	}
	
	public void setRun(boolean run) {
		this.run = run;
	}
}
