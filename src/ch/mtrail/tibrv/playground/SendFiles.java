package ch.mtrail.tibrv.playground;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import com.tibco.tibrv.Tibrv;
import com.tibco.tibrv.TibrvException;
import com.tibco.tibrv.TibrvListener;
import com.tibco.tibrv.TibrvMsg;
import com.tibco.tibrv.TibrvRvdTransport;
import com.tibco.tibrv.TibrvTransport;

public class SendFiles {

	private TibrvTransport transport = null;
	private final String subject;
	private final static short fileTimeType = TibrvMsg.USER_FIRST + 1;

	public SendFiles(final String service, final String network, final String daemon, final String subject) {
		this.subject = subject;

		// open Tibrv in native implementation
		try {
			Tibrv.open(Tibrv.IMPL_NATIVE);
			TibrvMsg.setStringEncoding("UTF-8");

			final FileTimeEncoder fileTimeEncoder = new FileTimeEncoder();
			TibrvMsg.setHandlers(fileTimeType, fileTimeEncoder, fileTimeEncoder);
		} catch (

		final TibrvException | UnsupportedEncodingException e) {
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
		
		// Create error listener
		try {
			final ErrorLogger errorLogger = new ErrorLogger();
			new TibrvListener(Tibrv.defaultQueue(), errorLogger, transport, "_RV.ERROR.>", null);
			new TibrvListener(Tibrv.defaultQueue(), errorLogger, transport, "_RV.WARN.>", null);
		} catch (final TibrvException e) {
			System.err.println("Failed to create ErrorHandler:");
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void send(final Path file) throws TibrvException, IOException {
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

		final byte[] content = Files.readAllBytes(file);
		final BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);

		// Without toString we will run into "TibrvException[error=34,message=Invalid type of data object]"
		msg.add("FILENAME", file.getFileName().toString());
		msg.add("SIZE", attributes.size());
		msg.add("CONTENT", content);
		final TibrvMsg metaMsg = new TibrvMsg();
		// Without 3rd parameter, we wil run into "TibrvException[error=34,message=Invalid type of data object]"
		metaMsg.add("creationTime", attributes.creationTime(), fileTimeType);
		metaMsg.add("lastAccessTime", attributes.lastAccessTime(), fileTimeType);
		metaMsg.add("lastModifiedTime", attributes.lastModifiedTime(), fileTimeType);
		// TibrvMsg.DATETIME
		// new TibrvDate()
		final String mimeType = Files.probeContentType(file);
		if (mimeType != null) {
			// NULL is not permitted: java.lang.IllegalArgumentException: Field data is null
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

		final SendFiles send = new SendFiles(//
				argParser.getParameter("service"), //
				argParser.getParameter("network"), //
				argParser.getParameter("daemon"), //
				argParser.getArgument("subject"));

		try {
			final Path path = Paths.get(argParser.getParameter("folder"));
			try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
				for (final Path child : ds) {
					if (Files.isRegularFile(child)) {
						send.send(child);
						System.out.println("Submitted: " + child);
					}
				}
			}
		} catch (final TibrvException | IOException e) {
			e.printStackTrace();
		}

	}

}
