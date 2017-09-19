package ch.mtrail.tibrv.playground;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
	private List<TibrvDispatcher> dispatchers = new ArrayList<>();

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
			dispatchers.add(new TibrvDispatcher("Dispatcher-" + i, queue));
		}
	}

	public void dispatch() {
		while (true) {
			try {
				System.out.println("QueueLength: " + dq.getUnassignedMessageCount());
				System.out.flush();
				
				TimeUnit.SECONDS.sleep(10);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onMsg(final TibrvListener listener, final TibrvMsg msg) {
		long seqno = -1;
		try {
			seqno = TibrvCmMsg.getSequence(msg);
		} catch (TibrvException e) {
			e.printStackTrace();
		}

		System.out.println((new Date()).toString() + " " + Thread.currentThread().getName() + " START " //
				+ "subject=" + msg.getSendSubject() + ", message=" + msg.toString() + ", seqno=" + seqno);
		System.out.flush();

		msg.dispose();

		try {
			TimeUnit.SECONDS.sleep(2);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println((new Date()).toString() + " " + Thread.currentThread().getName() + " FINISHED");
		System.out.flush();
	}

	public void setPerformDispatch(final boolean performDispatch) {
		dispatchers.forEach(dispatcher -> {
			try {
				if (performDispatch) {
					if (!dispatcher.isAlive()) {
						dispatcher.start();
					}
				} else {
					if (dispatcher.isAlive()) {
						dispatcher.join();
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
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
