package ch.mtrail.tibrv.playground;

import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.tibco.tibrv.TibrvException;
import com.tibco.tibrv.TibrvMsg;

public class Send extends Abstract {
	private final Set<String> subjects;
	private int msgCount = 0;

	public Send(final String service, final String network, final String daemon, final Set<String> subjects)
			throws TibrvException {
		super(service, network, daemon);

		this.subjects = subjects;
	}

	public void send(final String msgString) throws TibrvException {
		for (final String subject : subjects) {
				// Create the message
				final TibrvMsg msg = new TibrvMsg();
				msg.setSendSubject(subject);
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

	public static void main(final String args[]) throws Exception {
		// Debug.diplayEnvInfo();

		final ArgParser argParser = new ArgParser("TibRvListen");
		// Interval milli seconds to repeat message
		argParser.setOptionalParameter("service", "network", "daemon", "interval");
		argParser.setRequiredArg("msg", "subject");
		argParser.setOptionalArg("addtional-subject1", "addtional-subject2", "addtional-subject3");
		argParser.parse(args);

		final Set<String> subjects = new HashSet<>();

		subjects.add(argParser.getArgument("subject"));

		for (int i = 1; i <= 3; i++) {
			final String addSubject = argParser.getArgument("addtional-subject" + i);
			if (Objects.nonNull(addSubject) && !addSubject.isEmpty()) {
				subjects.add(addSubject);
			}
		}

		final Send sender = new Send(//
				argParser.getParameter("service"), //
				argParser.getParameter("network"), //
				argParser.getParameter("daemon"), //
				subjects);

		sender.send(argParser.getArgument("msg"));
		System.out.println("Submitted: " + argParser.getArgument("msg"));

		final String intervalStr = argParser.getParameter("interval");
		if (Objects.nonNull(intervalStr) && !intervalStr.isEmpty()) {
			final int intervalMs = Integer.parseInt(intervalStr);
			while (true) {
				if (intervalMs > 0) {
					// -interval 0 == hard core stress test
					TimeUnit.MILLISECONDS.sleep(intervalMs);
				}
				sender.send(argParser.getArgument("msg"));
				sender.printStatus();
			}
		}

	}

}
