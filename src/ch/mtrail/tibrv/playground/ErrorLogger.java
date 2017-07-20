package ch.mtrail.tibrv.playground;

import java.util.Date;

import com.tibco.tibrv.TibrvListener;
import com.tibco.tibrv.TibrvMsg;
import com.tibco.tibrv.TibrvMsgCallback;

/**
 * Falls eine Fehler zu spät auftritt, kann TibRv keine Exception mehr schmeissen.
 * Dann wird eine Fehlermeldung über diesen Weg gesendet.
 * Eine Zuordnung zu der verursachenden Msg ist nahezu unmöglich. 
 */
public class ErrorLogger implements TibrvMsgCallback {
	@Override
	public void onMsg(final TibrvListener listener, final TibrvMsg msg) {
		System.err.println("#FEHLER# " + (new Date()).toString() + ": subject=" + msg.getSendSubject() + ", reply="
				+ msg.getReplySubject() + ", message=" + msg.toString());
		System.err.flush();

		msg.dispose();
	}
}
