package ch.mtrail.tibrv.playground;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import com.tibco.tibrv.TibrvDispatcher;
import com.tibco.tibrv.TibrvException;
import com.tibco.tibrv.TibrvMsg;
import com.tibco.tibrv.TibrvQueueGroup;

public class SendFiles extends Abstract {

	private final String subject;
	private final static short fileTimeType = TibrvMsg.USER_FIRST + 1;

	public SendFiles(final String service, final String network, final String daemon, final String subject) {
		super(service, network, daemon);
		this.subject = subject;

		try {
			final FileTimeEncoder fileTimeEncoder = new FileTimeEncoder();
			TibrvMsg.setHandlers(fileTimeType, fileTimeEncoder, fileTimeEncoder);
			
			// Create error listener
			final TibrvQueueGroup group = new TibrvQueueGroup();
			group.add(createErrorHandler());
			dispatchers.add(new TibrvDispatcher(group));
		} catch (final TibrvException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void send(final Path file) throws TibrvException, IOException {
		// Create the message
		final TibrvMsg msg = new TibrvMsg();

		msg.setSendSubject(subject);

		final byte[] content = Files.readAllBytes(file);
		final BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);

		// Without toString we will run into
		// "TibrvException[error=34,message=Invalid type of data object]"
		msg.add("FILENAME", file.getFileName().toString());
		msg.add("SIZE", attributes.size());
		msg.add("CONTENT", content);
		final TibrvMsg metaMsg = new TibrvMsg();
		// Without 3rd parameter, we will run into
		// "TibrvException[error=34,message=Invalid type of data object]"
		metaMsg.add("creationTime", attributes.creationTime(), fileTimeType);
		metaMsg.add("lastAccessTime", attributes.lastAccessTime(), fileTimeType);
		metaMsg.add("lastModifiedTime", attributes.lastModifiedTime(), fileTimeType);
		// TibrvMsg.DATETIME
		// new TibrvDate()
		final String mimeType = Files.probeContentType(file);
		if (mimeType != null) {
			// NULL is not permitted: java.lang.IllegalArgumentException: Field
			// data is null
			metaMsg.add("mimeType", mimeType);
		}
		msg.add("META", metaMsg);

		transport.send(msg);
		msg.dispose();
	}

	public static void main(final String args[]) {
		final ArgParser argParser = new ArgParser("SendFiles");
		argParser.setOptionalParameter("service", "network", "daemon");
		argParser.setRequiredParameter("folder");
		argParser.setRequiredArg("subject");
		argParser.parse(args);

		final SendFiles sender = new SendFiles(//
				argParser.getParameter("service"), //
				argParser.getParameter("network"), //
				argParser.getParameter("daemon"), //
				argParser.getArgument("subject"));

		try {
			final Path path = Paths.get(argParser.getParameter("folder"));
			try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
				for (final Path child : ds) {
					if (Files.isRegularFile(child)) {
						sender.send(child);
						System.out.println("Submitted: " + child);
					}
				}
			}
			
			System.out.println("DONE");
			sender.shutdown();
		} catch (final TibrvException | IOException e) {
			e.printStackTrace();
		}

	}

}
