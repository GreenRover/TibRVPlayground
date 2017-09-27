package ch.mtrail.tibrv.playground;

import com.tibco.tibrv.Tibrv;
import com.tibco.tibrv.TibrvException;
import com.tibco.tibrv.TibrvFtMonitor;
import com.tibco.tibrv.TibrvFtMonitorCallback;

public class MonitorFT extends Abstract implements TibrvFtMonitorCallback {

	private double lostInterval = 4.8; // matches tibrvfttime
	private static int oldNumActive = 0;

	public MonitorFT(final String service, final String network, final String daemon, final String ftGroupName) {
		super(service, network, daemon);

		// create listener using default queue
		try {
			new TibrvFtMonitor(Tibrv.defaultQueue(), this, transport, ftGroupName, lostInterval, null);
			System.err.println("tibrvftmon: Waiting for group information... " + ftGroupName);
		} catch (final TibrvException e) {
			System.err.println("Failed to create listener:");
			handleFatalError(e);
		}
	}

	public void dispatch() {
		dispatch(Tibrv.defaultQueue());
	}

	/**
	 * Fault tolerance monitor callback called when TIBRVFT detects a change in
	 * the number of active members in group TIBRVFT_TIME_EXAMPLE.
	 */
	@Override
	public void onFtMonitor(TibrvFtMonitor ftMonitor, String ftgroupName, int numActive) {
		System.out.println("Group [" + ftgroupName + "]: has " + numActive + " members (after "
				+ ((oldNumActive > numActive) ? "one deactivated" : "one activated") + ").");
		oldNumActive = numActive;

	}

	public static void main(final String args[]) {
		final ArgParser argParser = new ArgParser("TibRvListenFT");
		argParser.setOptionalParameter("service", "network", "daemon", "groupName");
		argParser.parse(args);

		final MonitorFT listen = new MonitorFT(//
				argParser.getParameter("service"), //
				argParser.getParameter("network"), //
				argParser.getParameter("daemon"), //
				argParser.getParameter("groupName", "FT_group_Name"));

		listen.dispatch();
	}

}
