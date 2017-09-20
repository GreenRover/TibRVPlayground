package ch.mtrail.tibrv.playground;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import com.tibco.tibrv.Tibrv;
import com.tibco.tibrv.TibrvException;
import com.tibco.tibrv.TibrvFtMember;
import com.tibco.tibrv.TibrvFtMemberCallback;
import com.tibco.tibrv.TibrvListener;
import com.tibco.tibrv.TibrvMsg;
import com.tibco.tibrv.TibrvMsgCallback;
import com.tibco.tibrv.TibrvRvdTransport;

public class ListenFT implements TibrvMsgCallback {

	private boolean performDispatch = true;
	private final String ftGroupName = "FT_group_Name";
	private TibrvFtMember ftMember;
	private int ftStatus;

	public ListenFT(final String service, final String network, final String daemon, final String subject) {

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

		// create listener using default queue
		try {
			new TibrvListener(Tibrv.defaultQueue(), this, transport, subject, null);
			System.err.println("Listening on: " + subject);
		} catch (final TibrvException e) {
			System.err.println("Failed to create listener:");
			e.printStackTrace();
			System.exit(1);
		}

		// FaultTolerance
		final TibrvFtMemberCallback callback = new TibrvFtMemberCallback() {
			@Override
			public void onFtAction(final TibrvFtMember member, final String groupName, final int action) {
				ftStatus = action;
				System.err.println(System.currentTimeMillis() + " " + groupName + " -> " + //
						getFtStatus(action) + " [" + action + "]");
			}
		};

		/**
		 * TibrvFtMember( 
		 *   TibrvQueue queue, 
		 *   TibrvFtMemberCallback callback,
		 *   TibrvTransport transport, 
		 *   String groupName, 
		 *   int weight, 
		 *   int activeGoal, 
		 *   
		 *   double heartbeatInterval, 
		 *   double preparationInterval,
		 *   double activationInterval, 
		 *   Object closure 
		 * )
		 */
		try {
			final int defaultWeight = 50;
			final int activePrcocesses = 2;
			ftMember = new TibrvFtMember(Tibrv.defaultQueue(), callback, transport, ftGroupName, defaultWeight, activePrcocesses, //
					0.5, 0, 1.0, null);
		} catch (final TibrvException e) {
			System.err.println("Failed to create FT member:");
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void dispatch() {
		while (true) {
			if (performDispatch) {
				// dispatch Tibrv events
				try {
					// Wait max 0.5 sec, to listen on keyboard.
					Tibrv.defaultQueue().timedDispatch(0.5d);
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
		System.out.println((new Date()).toString() + " " + getFtStatus(ftStatus) + "(" + ftMember.getWeight() + "): " //
				+ "subject=" + msg.getSendSubject() + ", message=" + msg.toString());
		System.out.flush();

		msg.dispose();
	}

	private static String getFtStatus(final int ftStatus) {
		switch (ftStatus) {
		case TibrvFtMember.ACTIVATE:
			return "ACTIVE";
		case TibrvFtMember.DEACTIVATE:
			return "DEACTIVE";
		case TibrvFtMember.PREPARE_TO_ACTIVATE:
			return "PREPARE_TO_ACTIVATE";
		case TibrvFtMember.PREPARE_AND_ACTIVATE:
			return "PREPARE_AND_ACTIVATE";
		}
		return "";
	}

	public boolean isPerformDispatch() {
		return performDispatch;
	}

	public void setPerformDispatch(final boolean performDispatch) {
		this.performDispatch = performDispatch;
	}

	public static void main(final String args[]) {
		// Debug.diplayEnvInfo();

		final ArgParser argParser = new ArgParser("TibRvListenFT");
		argParser.setOptionalParameter("service", "network", "daemon");
		argParser.setRequiredArg("subject");
		argParser.parse(args);

		final ListenFT listen = new ListenFT(//
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
					final String line = input.readLine().trim();

					switch (line.charAt(0)) {
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

					default:
						if (line.matches("^\\d+$")) {
							try {
								ftMember.setWeight(Integer.parseInt(line));
								System.out.println("Set weight to " + line);
							} catch (TibrvException | NumberFormatException e) {
								System.err.println("Exception to set ft weight:");
								e.printStackTrace();
							}
						} else {
							printKeyUsage();
						}
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
