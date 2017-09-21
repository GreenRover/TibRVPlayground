package ch.mtrail.tibrv.playground;

import com.tibco.tibrv.Tibrv;
import com.tibco.tibrv.TibrvException;
import com.tibco.tibrv.TibrvFtMonitor;
import com.tibco.tibrv.TibrvFtMonitorCallback;
import com.tibco.tibrv.TibrvRvdTransport;

public class MonitorFT implements TibrvFtMonitorCallback {

	private double lostInterval = 4.8; // matches tibrvfttime
	private static int oldNumActive = 0;

	public MonitorFT(final String service, final String network, final String daemon, final String ftGroupName) {

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
		TibrvRvdTransport transport = null;
		try {
			transport = new TibrvRvdTransport(service, network, daemon);
		} catch (final TibrvException e) {
			System.err.println("Failed to create TibrvRvdTransport:");
			e.printStackTrace();
			System.exit(1);
		}

		// create listener using default queue
		try {
			new TibrvFtMonitor(Tibrv.defaultQueue(), this, transport, ftGroupName,
					lostInterval, null);
			System.err.println("tibrvftmon: Waiting for group information... " + ftGroupName);
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
			} catch (TibrvException e) {
				System.err.println("Exception dispatching default queue:");
				e.printStackTrace();
				System.exit(0);
			} catch (InterruptedException ie) {
				System.exit(0);
			}
		}
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
