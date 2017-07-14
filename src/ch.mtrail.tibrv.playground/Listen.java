package ch.mtrail.tibrv.playground;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.tibco.tibrv.Tibrv;
import com.tibco.tibrv.TibrvException;
import com.tibco.tibrv.TibrvListener;
import com.tibco.tibrv.TibrvMsg;
import com.tibco.tibrv.TibrvMsgCallback;
import com.tibco.tibrv.TibrvRvdTransport;
import com.tibco.tibrv.TibrvTransport;

public class Listen implements TibrvMsgCallback {

	private boolean performDispose = false;

	public Listen(final String service, final String network, final String daemon, final List<String> subjects) {

		// open Tibrv in native implementation
		try {
			Tibrv.open(Tibrv.IMPL_NATIVE);
		} catch (

		final TibrvException e) {
			System.err.println("Failed to open Tibrv in native implementation:");
			e.printStackTrace();
			System.exit(0);
		}

		// Create RVD transport
		TibrvTransport transport = null;
		try {
			transport = new TibrvRvdTransport(service, network, daemon);
		} catch (final TibrvException e) {
			System.err.println("Failed to create TibrvRvdTransport:");
			e.printStackTrace();
			System.exit(0);
		}

		for (String subject : subjects) {
			// create listener using default queue
			try {
				new TibrvListener(Tibrv.defaultQueue(), this, transport, subject, null);
				System.err.println("Listening on: " + subject);
			} catch (final TibrvException e) {
				System.err.println("Failed to create listener:");
				e.printStackTrace();
				System.exit(0);
			}
		}

		// dispatch Tibrv events
		while (true) {
			try {
				Tibrv.defaultQueue().dispatch();
			} catch (final TibrvException e) {
				System.err.println("Exception dispatching default queue:");
				e.printStackTrace();
				System.exit(0);
			} catch (final InterruptedException ie) {
				System.exit(0);
			}
		}
	}

	@Override
	public void onMsg(final TibrvListener listener, final TibrvMsg msg) {
		System.out.println((new Date()).toString() + ": subject=" + msg.getSendSubject() + ", reply="
				+ msg.getReplySubject() + ", message=" + msg.toString());
		System.out.flush();
		
		if (performDispose) {
			msg.dispose();
		}
	}

	public void setPerformDispose() {
		setPerformDispose(true);
	}

	public void setPerformDispose(final boolean performDispose) {
		this.performDispose = performDispose;
	}

	public static void main(final String args[]) {
		// Debug.diplayEnvInfo();

		final ArgParser argParser = new ArgParser("TibRvListen");
		argParser.setOptionalParameter("service", "network", "daemon");
		argParser.setRequiredArg("subject");
		argParser.setFlags("perform-dispose");
		argParser.setOptionalArg("addtional-subject1", "addtional-subject2", "addtional-subject3");
		argParser.parse(args);

		final List<String> subjects = new ArrayList<>();
		try {
			subjects.add(argParser.getArgument("subject"));

			for (int i = 1; i <= 3; i++) {
				String addSubject = argParser.getArgument("addtional-subject" + i);
				if (Objects.nonNull(addSubject)) {
					subjects.add(addSubject);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		final Listen listen = new Listen(argParser.getParameter("service"), argParser.getParameter("network"),
				argParser.getParameter("daemon"), subjects);

		if (argParser.isFlagSet("perform-dispose")) {
			listen.setPerformDispose();
		}
	}

}
