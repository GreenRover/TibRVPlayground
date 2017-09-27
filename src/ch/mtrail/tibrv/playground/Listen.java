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

public class Listen extends Abstract implements TibrvMsgCallback {

	public Listen(final String service, final String network, final String daemon, final List<String> subjects) {
		super(service, network, daemon);

		for (final String subject : subjects) {
			// create listener using default queue
			try {
				/**
				 * TibrvListener( TibrvQueue queue, TibrvMsgCallback callback,
				 * TibrvTransport transport, java.lang.String subject,
				 * java.lang.Object closure)
				 */
				new TibrvListener(Tibrv.defaultQueue(), this, transport, subject, null);
				System.out.println("Listening on: " + subject);
			} catch (final TibrvException e) {
				System.err.println("Failed to create listener:");
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	public void dispatch() {
		dispatch(Tibrv.defaultQueue());
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

	public static void main(final String args[]) {
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
				final String addSubject = argParser.getArgument("addtional-subject" + i);
				if (Objects.nonNull(addSubject)) {
					subjects.add(addSubject);
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		final Listen listen = new Listen(//
				argParser.getParameter("service"), //
				argParser.getParameter("network"), //
				argParser.getParameter("daemon"), //
				subjects);

		if (argParser.isFlagSet("perform-dispose")) {
			listen.setPerformDispose();
		}

		listen.startKeyListener();

		listen.dispatch();
	}
}
