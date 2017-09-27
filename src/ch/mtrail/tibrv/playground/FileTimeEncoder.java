package ch.mtrail.tibrv.playground;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.attribute.FileTime;

import com.tibco.tibrv.TibrvMsgDecoder;
import com.tibco.tibrv.TibrvMsgEncoder;

/**
 * En/Decoder for custom Tibco Rendevouz type {@link FileTime}.
 *
 */
public class FileTimeEncoder implements TibrvMsgEncoder, TibrvMsgDecoder {

	@Override
	public Object decode(final short type, final byte[] bytes) {
		try {
			final ByteArrayInputStream bytein = new ByteArrayInputStream(bytes);
			final DataInputStream in = new DataInputStream(bytein);
			final long unixTime = in.readLong();

			return FileTime.fromMillis(unixTime);
		} catch (final IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
	}

	@Override
	public boolean canEncode(final short type, final Object data) {
		return (data instanceof FileTime);
	}

	@Override
	public byte[] encode(final short type, final Object data) {
		try {
			final long unixTime = ((FileTime) data).toMillis();
			final ByteArrayOutputStream byteout = new ByteArrayOutputStream();
			final DataOutputStream out = new DataOutputStream(byteout);
			out.writeLong(unixTime);
			out.close();
			return byteout.toByteArray();
		} catch (final IOException ioe) {
			ioe.printStackTrace();
			return null;
		}

	}

}