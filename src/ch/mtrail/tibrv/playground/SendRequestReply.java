package ch.mtrail.tibrv.playground;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.tibco.tibrv.TibrvException;
import com.tibco.tibrv.TibrvMsg;

public class SendRequestReply extends Abstract {

	private final String subject;
	private int msgSend = 0;

	public SendRequestReply(final String service, final String network, final String daemon, final String subject)
			throws TibrvException {
		super(service, network, daemon);

		this.subject = subject;
	}

	public void send(final String msgString) throws TibrvException {
		// Create the message
		final TibrvMsg msg = new TibrvMsg();
		msg.setSendSubject(subject);

		msg.add("DATA", msgString);
		msg.add("INDEX", msgSend);
		System.out.println(
				(new Date()).toString() + " QUESTION: subject=" + msg.getSendSubject() + ", message=" + msg.toString());

		// Timeout 30sec
		final TibrvMsg replyMsg = transport.sendRequest(msg, 30);

		msg.dispose();

		if (replyMsg != null) {
			System.out.println((new Date()).toString() + " REPLY: subject=" + replyMsg.getSendSubject() + ", message="
					+ replyMsg.toString());

			replyMsg.dispose();
		} else {
			System.out.println((new Date()).toString() + " REPLY NO RECEIVED");
		}
		msgSend++;
	}

	public static void main(final String args[]) throws Exception {
		final ArgParser argParser = new ArgParser("SendRequestReply");
		argParser.setOptionalParameter("service", "network", "daemon", "interval");
		argParser.setRequiredArg("msg", "subject");
		argParser.parse(args);

		final SendRequestReply send = new SendRequestReply(//
				argParser.getParameter("service"), //
				argParser.getParameter("network"), //
				argParser.getParameter("daemon"), //
				argParser.getArgument("subject"));

		System.out.println("Submitting: " + argParser.getArgument("msg"));
		send.send(argParser.getArgument("msg"));

		final String intervalStr = argParser.getParameter("interval");
		if (Objects.nonNull(intervalStr) && !intervalStr.isEmpty()) {
			final int intervalMs = Integer.parseInt(intervalStr);
			while (true) {
				if (intervalMs > 0) {
					// -interval 0 == hard core stress test
					TimeUnit.MILLISECONDS.sleep(intervalMs);
				}
				send.send(argParser.getArgument("msg"));
			}
		}
	}
}
