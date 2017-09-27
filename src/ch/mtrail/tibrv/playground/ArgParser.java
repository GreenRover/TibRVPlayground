package ch.mtrail.tibrv.playground;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example command:
 *  myCommand -parameterA "foo" -parameterB "grr" -flagA -flagB "Argument A" "Argument B"
 *  
 * Parameter:
 *   Key value set
 *
 * Argument:
 *   Value list at the end of comment
 *
 * Flag:
 *   Keys that can be set or not
 *   
 * Example:
 * 	public static void main(final String args[]) {
 *		final ArgParser argParser = new ArgParser("TibRvListenFT");
 *		argParser.setOptionalParameter("service", "network", "daemon", "groupName");
 *		argParser.parse(args);
 *		
 *<		argParser.getParameter("service");
 *  
 * @author GreenRover
 */
public class ArgParser {
	private final List<String> optionalParameters = new ArrayList<>();
	private final List<String> requiredParameters = new ArrayList<>();
	private final List<String> requiredArgs = new ArrayList<>();
	private final List<String> optionalArgs = new ArrayList<>();
	private final List<String> flags = new ArrayList<>();
	private final String programmName;

	private final Map<String, String> extractedParameter = new HashMap<>();
	private final List<String> extractedArguments = new ArrayList<>();
	private final List<String> extractedFlags = new ArrayList<>();

	public ArgParser(final String programmName) {
		this.programmName = programmName;
	}

	public void setOptionalParameter(final String... parameter) {
		optionalParameters.addAll(Arrays.asList(parameter));
	}

	public void setRequiredParameter(final String... parameter) {
		requiredParameters.addAll(Arrays.asList(parameter));
	}

	public void setRequiredArg(final String... args) {
		requiredArgs.addAll(Arrays.asList(args));
	}

	public void setOptionalArg(final String... args) {
		optionalArgs.addAll(Arrays.asList(args));
	}

	public void setFlags(final String... possibleFlags) {
		flags.addAll(Arrays.asList(possibleFlags));
	}

	public void parse(final String[] args) {
		extract(args);
		validateParameters();
		validateArguments();
	}

	private void extract(final String[] args) {
		int i = 0;
		while (i < args.length) {
			if (args[i].startsWith("-")) {
				// Get parameter.
				final String paramName = args[i].substring(1);
				if (flags.contains(paramName)) {
					extractedFlags.add(paramName);
					i++;
				} else if (!optionalParameters.contains(paramName) && !requiredParameters.contains(paramName)) {
					System.err.println("Invalid parameter: -" + paramName + " " + paramName);
					printUsage();
				} else {
					extractedParameter.put(paramName, args[i + 1]);
					i += 2;
				}

			} else {
				// Get argument.
				extractedArguments.add(args[i]);
				i++;
			}
		}
	}

	private void validateParameters() {
		for (final String paramName : requiredParameters) {
			if (!extractedParameter.containsKey(paramName)) {
				System.err.println("Missing parameter: -" + paramName + " " + paramName);
				printUsage();
			}
		}
	}

	private void validateArguments() {
		if (requiredArgs.size() > extractedArguments.size()) {
			final int missingIndex = requiredArgs.size() - extractedArguments.size() - 1;
			System.err.println("Missing argument: <" + requiredArgs.get(missingIndex) + ">");
			printUsage();
		}

	}

	private void printUsage() {
		final StringBuilder usageMsg = new StringBuilder();
		usageMsg.append("Usage: java " + programmName);

		for (final String param : requiredParameters) {
			printToErrIfLineToLong(usageMsg);
			usageMsg.append(" -" + param + " " + param);
		}

		for (final String param : optionalParameters) {
			printToErrIfLineToLong(usageMsg);
			usageMsg.append(" [-" + param + " " + param + "]");
		}

		for (final String flag : flags) {
			printToErrIfLineToLong(usageMsg);
			usageMsg.append(" [-" + flag + "]");
		}

		for (final String arg : requiredArgs) {
			printToErrIfLineToLong(usageMsg);
			usageMsg.append(" <" + arg + ">");
		}

		for (final String arg : optionalArgs) {
			printToErrIfLineToLong(usageMsg);
			usageMsg.append(" [" + arg + "]");
		}

		if (usageMsg.toString().trim().length() > 0) {
			System.err.println(usageMsg.toString());
		}

		System.exit(-1);
	}

	private void printToErrIfLineToLong(final StringBuilder output) {
		if (output.length() > 65) {
			System.err.println(output.toString());
			output.setLength(0);
			output.append("\t\t");
		}
	}

	/**
	 * @return If Argument was not set the defaultValue will be returned
	 * @throws Exception
	 */
	public String getArgument(final String argName, final String defaultValue) {
		final int indexRequired = requiredArgs.indexOf(argName);
		if (indexRequired >= 0) {
			return extractedArguments.get(indexRequired);
		}

		final int indexOptional = requiredArgs.size() + optionalArgs.indexOf(argName);
		if (indexOptional >= 0) {
			try {
				return extractedArguments.get(indexOptional);
			} catch (final IndexOutOfBoundsException e) {
				return defaultValue;
			}
		}

		throw new InvalidParameterException("Argument " + argName + " was not set as required or optional.");
	}

	/**
	 * @return Can not be null
	 * @throws Exception
	 */
	public String getArgument(final String argName) {
		return getArgument(argName, null);
	}

	/**
	 * @param parameterName
	 * @return If Argument was not set the defaultValue will be returned
	 */
	public String getParameter(final String parameterName, final String defaultValue) {
		if (!extractedParameter.containsKey(parameterName)) {
			return defaultValue;
		}

		return extractedParameter.get(parameterName);
	}

	/**
	 * @param parameterName
	 * @return Can be null for optional parameter.
	 */
	public String getParameter(final String parameterName) {
		return getParameter(parameterName, null);
	}

	public boolean isFlagSet(final String parameterName) {
		return extractedFlags.contains(parameterName);
	}
}
