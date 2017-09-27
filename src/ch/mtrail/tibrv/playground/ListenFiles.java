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

public class ListenFiles extends Abstract implements TibrvMsgCallback {

	private Path dstFolder = null;
	private final static short fileTimeType = TibrvMsg.USER_FIRST + 1;

	public ListenFiles(final String service, final String network, final String daemon, final String subject,
			final String folder) {
		super(service, network, daemon);
		
		try {
			final FileTimeEncoder fileTimeEncoder = new FileTimeEncoder();
			TibrvMsg.setHandlers(fileTimeType, fileTimeEncoder, fileTimeEncoder);
			
			if (folder != null && !folder.isEmpty()) {
				this.dstFolder = Paths.get(folder);
			}
	
			// create listener using default queue
			new TibrvListener(Tibrv.defaultQueue(), this, transport, subject, null);
			System.out.println("Listening on: " + subject);
		} catch (final TibrvException e) {
			handleFatalError(e);
		}
	}
	
	public void dispatch() {
		dispatch(Tibrv.defaultQueue());
	}

	@Override
	public void onMsg(final TibrvListener listener, final TibrvMsg msg) {
		try {
			System.out.println((new Date()).toString() + //
					": subject=" + msg.getSendSubject() + //
					", filename=" + msg.get("FILENAME") + //
					", size=" + msg.get("SIZE") + // 
					", mime=" + ((TibrvMsg)msg.get("META")).get("mimeType") + //
					", creationTime=" + ((TibrvMsg)msg.get("META")).get("creationTime")
					);
			System.out.flush();

			if (dstFolder != null) {
				try {
					storeFile(msg);
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}

		} catch (final TibrvException e) {
			handleFatalError(e);
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