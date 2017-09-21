package ch.mtrail.tibrv.playground;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

public class ListenDQ implements TibrvMsgCallback {

	private final String dqGroupName = "DQgroupName";
	private TibrvQueue queue;
	private final List<RvDispatcher> dispatchers = new ArrayList<>();

	private final static int threads = 5;
	private TibrvCmQueueTransport dq;

	public ListenDQ(final String service, final String network, final String daemon, final String subject) {

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
			dq.setWorkerTasks(threads);

			new TibrvCmListener(queue, this, dq, subject, null);
			System.err.println("Listening on: " + subject);

		} catch (final TibrvException e) {
			System.err.println("Error setup distributed queue or listener");
			e.printStackTrace();
			System.exit(1);
		}

		for (int i = 0; i <= threads; i++) {
			final RvDispatcher dispatcher = new RvDispatcher(queue);
			dispatchers.add(dispatcher);
			final Thread thread = new Thread(dispatcher, "Dispatcher-" + i);
			thread.start();
		}
	}

	public void printDebugInfos() {
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

	@Override
	public void onMsg(final TibrvListener listener, final TibrvMsg msg) {
		long seqno = -1;
		try {
			seqno = TibrvCmMsg.getSequence(msg);
		} catch (final TibrvException e) {
			e.printStackTrace();
		}

		System.out.println((new Date()).toString() + " " + Thread.currentThread().getName() + " START " //
				+ "subject=" + msg.getSendSubject() + ", message=" + msg.toString() + ", seqno=" + seqno);
		System.out.flush();

		msg.dispose();

		LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));

		System.out.println((new Date()).toString() + " " + Thread.currentThread().getName() + " FINISHED");
		System.out.flush();
	}

	public void setPerformDispatch(final boolean performDispatch) {
		dispatchers.forEach(dispatcher -> {
			dispatcher.setRun(performDispatch);
		});
	}

	public static void main(final String args[]) {
		// Debug.diplayEnvInfo();

		final ArgParser argParser = new ArgParser("TibRvListenFT");
		argParser.setOptionalParameter("service", "network", "daemon");
		argParser.setRequiredArg("subject");
		argParser.parse(args);

		final ListenDQ listen = new ListenDQ(//
				argParser.getParameter("service"), //
				argParser.getParameter("network"), //
				argParser.getParameter("daemon"), //
				argParser.getArgument("subject"));

		listen.startKeyListener();

		listen.printDebugInfos();
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

	class RvDispatcher implements Runnable {
		
		private boolean performDispatch = true;
		private final TibrvQueue queue;
		
		public RvDispatcher(final TibrvQueue queue) {
			this.queue = queue;
		}
		
		@Override
		public void run() {
			while (true) {
				if (performDispatch) {
					// dispatch Tibrv events
					try {
						// Wait max 0.5 sec, to listen on keyboard.
						queue.timedDispatch(0.5d);
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
		
		public void setRun(final boolean performDispatch) {
			this.performDispatch = performDispatch;
		}
	}
}

