package ch.mtrail.tibrv.playground;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.tibco.tibrv.Tibrv;
import com.tibco.tibrv.TibrvException;
import com.tibco.tibrv.TibrvMsg;
import com.tibco.tibrv.TibrvRvdTransport;
import com.tibco.tibrv.TibrvTransport;

public class Send {

	private TibrvTransport transport = null;
	private final List<String> subjects;
	private final String FIELD_NAME = "DATA";
	private int msgSend = 0;

	public Send(final String service, final String network, final String daemon, final List<String> subjects) {

		this.subjects = subjects;

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
		try {
			transport = new TibrvRvdTransport(service, network, daemon);
		} catch (final TibrvException e) {
			System.err.println("Failed to create TibrvRvdTransport:");
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void send(final String msgString) throws TibrvException {
		for (final String subject : subjects) {
	        // Create the message
	        final TibrvMsg msg = new TibrvMsg();

	        // Set send subject into the message
	        try
	        {
	            msg.setSendSubject(subject);
	        }
	        catch (final TibrvException e) {
	            System.err.println("Failed to set send subject:");
	            e.printStackTrace();
	            System.exit(0);
	        }
	        
	        msg.update(FIELD_NAME, msgString);
            transport.send(msg);
            
            msgSend  ++;
		}
	}
	
	public void printStatus() {
		final NumberFormat nf = NumberFormat.getInstance();
		System.out.print("Msg send: " + nf.format(msgSend) + "\r");
	}

	public static void main(final String args[]) {
		// Debug.diplayEnvInfo();

		final ArgParser argParser = new ArgParser("TibRvListen");
		argParser.setOptionalParameter("service", "network", "daemon", "intervall");
		argParser.setRequiredArg("msg", "subject");
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

		final Send send = new Send(argParser.getParameter("service"), argParser.getParameter("network"),
				argParser.getParameter("daemon"), subjects);

		try {
			send.send(argParser.getArgument("msg"));
			System.out.println("Submitted: " + argParser.getArgument("msg"));
			
			final String argument = argParser.getParameter("intervall");
			if (Objects.nonNull(argument) && !argument.isEmpty()) {
				final int intervallMs = Integer.parseInt(argument);
				while(true) {
					TimeUnit.MILLISECONDS.sleep(intervallMs);
					send.send(argParser.getArgument("msg"));
					send.printStatus();
				}
			}
		} catch (TibrvException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

}
