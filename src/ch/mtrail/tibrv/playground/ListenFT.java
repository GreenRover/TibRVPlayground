package ch.mtrail.tibrv.playground;

import java.util.Date;

import com.tibco.tibrv.Tibrv;
import com.tibco.tibrv.TibrvException;
import com.tibco.tibrv.TibrvFtMember;
import com.tibco.tibrv.TibrvFtMemberCallback;
import com.tibco.tibrv.TibrvListener;
import com.tibco.tibrv.TibrvMsg;
import com.tibco.tibrv.TibrvMsgCallback;

public class ListenFT extends Abstract implements TibrvMsgCallback {

	private final String ftGroupName = "FT_group_Name";
	private TibrvFtMember ftMember;
	
	// Wenn man dir nichts anderes sagt, bist du PASIVE
	private int ftStatus = TibrvFtMember.DEACTIVATE;

	public ListenFT(final String service, final String network, final String daemon, final String subject) throws TibrvException {
		super(service, network, daemon);
		
		// create listener using default queue
		new TibrvListener(Tibrv.defaultQueue(), this, transport, subject, null);
		System.err.println("Listening on: " + subject);

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
		 *          When this member is active, it sends heartbeat messages at this interval (in seconds).
		 *   double preparationInterval,
		 *          When the heartbeat signal from one or more active members has been
		 *          silent for this interval (in seconds), Rendezvous fault tolerance software
		 *          issues an early warning hint (TibrvFtMember.PREPARE_TO_ACTIVATE)
		 *          to the ranking inactive member. This warning lets the inactive member
		 *          prepare to activate, for example, by connecting to a database server, or
		 *          allocating memory.
		 *   double activationInterval, 
		 *          When the heartbeat signal from one or more active members has been
		 *          silent for this interval (in seconds), Rendezvous fault tolerance software
		 *          considers the silent member to be lost, and issues the instruction to
		 *          activate (TibrvFtMember.ACTIVATE) to the ranking inactive member
		 *   Object closure 
		 * )
		 */
		final int defaultWeight = 50;
		final int activePrcocesses = 2;
		ftMember = new TibrvFtMember(Tibrv.defaultQueue(), callback, transport, ftGroupName, defaultWeight,
				activePrcocesses, //
				0.5, 0, 1.0, null);
	}

	public void dispatch() throws TibrvException, InterruptedException {
		dispatch(Tibrv.defaultQueue());
	}

	@Override
	public void onMsg(final TibrvListener listener, final TibrvMsg msg) {
		System.out.println((new Date()).toString() + " " + getFtStatus(ftStatus) + "(" + ftMember.getWeight() + "): " //
				+ "subject=" + msg.getSendSubject() + ", message=" + msg.toString());

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

	@Override
	protected void userHasEnteredANumber(final int number) throws TibrvException {
		ftMember.setWeight(number);
		System.out.println("Set weight to " + number);
	}

	public static void main(final String args[]) throws Exception {
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
}
