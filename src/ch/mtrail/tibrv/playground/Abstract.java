package ch.mtrail.tibrv.playground;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import com.tibco.tibrv.Tibrv;
import com.tibco.tibrv.TibrvDispatchable;
import com.tibco.tibrv.TibrvDispatcher;
import com.tibco.tibrv.TibrvException;
import com.tibco.tibrv.TibrvListener;
import com.tibco.tibrv.TibrvQueue;
import com.tibco.tibrv.TibrvRvdTransport;

public class Abstract {

	protected TibrvRvdTransport transport = null;
	protected List<TibrvListener> listeners = new ArrayList<>();
	protected List<TibrvDispatcher> dispatchers = new ArrayList<>();
	protected boolean performDispatch = true;
	protected boolean performDispose = false;

	public Abstract(final String service, final String network, final String daemon) throws TibrvException {
		// open Tibrv in native implementation
		Tibrv.open(Tibrv.IMPL_NATIVE);

		transport = new TibrvRvdTransport(service, network, daemon);
	}

	public void shutdown() {
		listeners.forEach(listener -> {
			listener.destroy();
		});
		dispatchers.forEach(dispatcher -> {
			dispatcher.destroy();
		});
		transport.destroy();
	}

	protected void dispatch(final TibrvDispatchable dispatchable) throws TibrvException, InterruptedException {
		while (true) {
			if (performDispatch) {
				// dispatch Tibrv events
				// Wait max 0.5 sec, to listen on keyboard.
				dispatchable.timedDispatch(0.5);

			} else {
				// Dispatch is disabled, just idle
				LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(500));
			}
		}
	}

	public void setPerformDispose() {
		setPerformDispose(true);
	}

	public void setPerformDispose(final boolean performDispose) {
		this.performDispose = performDispose;
	}

	public boolean isPerformDispatch() {
		return performDispatch;
	}

	public void setPerformDispatch(final boolean performDispatch) {
		this.performDispatch = performDispatch;
	}

	protected void startKeyListener() {
		printKeyUsage();

		new Thread(() -> {
			try (BufferedReader input = new BufferedReader(new InputStreamReader(System.in, "UTF-8"))) {
				while (true) {
					final String line = input.readLine().trim();

					switch (line.charAt(0)) {
					case 'd':
					case 'D':
						System.out.println("Dispatcher is DISABLED");
						setPerformDispatch(false);
						break;

					case 'e':
					case 'E':
						System.out.println("Dispatcher is ENABLED");
						setPerformDispatch(true);
						break;

					case '\r':
					case '\n':
						break;

					default:
						if (line.matches("^\\d+$")) {
							try {
								userHasEnteredANumber(Integer.parseInt(line));
							} catch (final Exception e) {
								System.err.println("Unexpected error");
								e.printStackTrace();
							}
							
						} else {
							printKeyUsage();
						}
						break;
					}

				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}).start();
	}

	protected void userHasEnteredANumber(final int number) throws TibrvException {
		// Can be implemented.
	}

	private void printKeyUsage() {
		System.out.println("Press\n\t\"D\" to disable Dispatcher\n\t\"E\" to enable Dispatcher ");
	}

	protected TibrvQueue createErrorHandler() throws TibrvException {
		// Create error listener
		final TibrvQueue errorQueue = new TibrvQueue();
		final ErrorLogger errorLogger = new ErrorLogger();
		listeners.add(new TibrvListener(errorQueue, errorLogger, transport, "_RV.ERROR.>", null));
		listeners.add(new TibrvListener(errorQueue, errorLogger, transport, "_RV.WARN.>", null));

		return errorQueue;
	}

}
