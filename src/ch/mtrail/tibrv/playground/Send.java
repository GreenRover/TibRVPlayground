package ch.mtrail.tibrv.playground;

import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.tibco.tibrv.Tibrv;
import com.tibco.tibrv.TibrvException;
import com.tibco.tibrv.TibrvMsg;
import com.tibco.tibrv.TibrvRvdTransport;
import com.tibco.tibrv.TibrvTransport;

public class Send {

	private TibrvTransport transport = null;
	private final Set<String> subjects;
	private int msgCount = 0;

	public Send(final String service, final String network, final String daemon, final Set<String> subjects) {

		this.subjects = subjects;

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
		try {
			transport = new TibrvRvdTransport(service, network, daemon);
		} catch (final TibrvException e) {
			System.err.println("Failed to create TibrvRvdTransport:");
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void send(final String msgString) throws TibrvException {
		for (final String subject : subjects) {
			// Create the message
			final TibrvMsg msg = new TibrvMsg();

			// Set send subject into the message
			try {
				msg.setSendSubject(subject);
			} catch (final TibrvException e) {
				System.err.println("Failed to set send subject:");
				e.printStackTrace();
				System.exit(1);
			}

			msg.add("DATA", msgString);
			msg.add("INDEX", msgCount);
			transport.send(msg);
			msg.dispose();

		}
		msgCount++;
	}

	public void printStatus() {
		final NumberFormat nf = NumberFormat.getInstance();
		System.out.print("Msg send: " + nf.format(msgCount) + "\n");
	}

	public static void main(final String args[]) {
		// Debug.diplayEnvInfo();

		final ArgParser argParser = new ArgParser("TibRvListen");
		// Interval milli seconds to repeat message
		argParser.setOptionalParameter("service", "network", "daemon", "interval");
		argParser.setRequiredArg("msg", "subject");
		argParser.setOptionalArg("addtional-subject1", "addtional-subject2", "addtional-subject3");
		argParser.parse(args);

		final Set<String> subjects = new HashSet<>();
		try {
			subjects.add(argParser.getArgument("subject"));

			for (int i = 1; i <= 3; i++) {
				final String addSubject = argParser.getArgument("addtional-subject" + i);
				if (Objects.nonNull(addSubject) && !addSubject.isEmpty()) {
					subjects.add(addSubject);
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		final Send send = new Send(//
				argParser.getParameter("service"), //
				argParser.getParameter("network"), //
				argParser.getParameter("daemon"), //
				subjects);

		try {
			send.send(argParser.getArgument("msg"));
			System.out.println("Submitted: " + argParser.getArgument("msg"));

			final String intervalStr = argParser.getParameter("interval");
			if (Objects.nonNull(intervalStr) && !intervalStr.isEmpty()) {
				final int intervalMs = Integer.parseInt(intervalStr);
				while (true) {
					if (intervalMs > 0) {
						// -interval 0  == hard core stress test
						TimeUnit.MILLISECONDS.sleep(intervalMs);
					}
					send.send(argParser.getArgument("msg"));
					send.printStatus();
				}
			}
		} catch (TibrvException | InterruptedException e) {
			e.printStackTrace();
		}

	}

}
