package ch.mtrail.tibrv.playground;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import com.tibco.tibrv.Tibrv;
import com.tibco.tibrv.TibrvException;
import com.tibco.tibrv.TibrvListener;
import com.tibco.tibrv.TibrvMsg;
import com.tibco.tibrv.TibrvMsgCallback;
import com.tibco.tibrv.TibrvRvdTransport;
import com.tibco.tibrv.TibrvTransport;

public class ListenFiles implements TibrvMsgCallback {

	private boolean performDispose = false;
	private Path dstFolder = null;

	public ListenFiles(final String service, final String network, final String daemon, final String subject,
			final String folder) {

		if (folder != null && !folder.isEmpty()) {
			this.dstFolder = Paths.get(folder);
		}

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
		TibrvTransport transport = null;
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
	}
	
	public void dispatch() {
		// dispatch Tibrv events
		while (true) {
			try {
				Tibrv.defaultQueue().dispatch();
			} catch (final TibrvException e) {
				System.err.println("Exception dispatching default queue:");
				e.printStackTrace();
				System.exit(1);
			} catch (final InterruptedException ie) {
				System.exit(1);
			}
		}
	}

	@Override
	public void onMsg(final TibrvListener listener, final TibrvMsg msg) {
		try {
			System.out.println((new Date()).toString() + //
					": subject=" + msg.getSendSubject() + //
					", filename=" + msg.get("FILENAME") + //
					", size=" + msg.get("SIZE") + // 
					", mime=" + ((TibrvMsg)msg.get("META")).get("mimeType"));
			System.out.flush();

			if (dstFolder != null) {
				try {
					storeFile(msg);
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}

		} catch (final TibrvException e) {
			e.printStackTrace();
		}

		if (performDispose) {
			msg.dispose();
		}
	}

	private void storeFile(final TibrvMsg msg) throws TibrvException, IOException {
		final String fileName = (String) msg.get("FILENAME");
		final byte[] content = (byte[]) msg.get("CONTENT");

		final File file = new File(dstFolder.toAbsolutePath().toString(), fileName);
		Files.write(file.toPath(), content);
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
		// If folder is not set, we perform a dry run and just print out what we received.
		argParser.setOptionalParameter("service", "network", "daemon", "folder");
		argParser.setRequiredArg("subject");
		argParser.setFlags("perform-dispose");
		argParser.parse(args);

		final ListenFiles listen = new ListenFiles(//
				argParser.getParameter("service"), //
				argParser.getParameter("network"), //
				argParser.getParameter("daemon"), //
				argParser.getArgument("subject"), //
				argParser.getParameter("folder"));

		if (argParser.isFlagSet("perform-dispose")) {
			listen.setPerformDispose();
		}
		
		listen.dispatch();
	}

}
