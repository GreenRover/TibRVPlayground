package ch.mtrail.tibrv.playground;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
				String paramName = args[i].substring(1);
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
		for (String paramName : requiredParameters) {
			if (!extractedParameter.containsKey(paramName)) {
				System.err.println("Missing parameter: -" + paramName + " " + paramName);
				printUsage();
			}
		}
	}

	private void validateArguments() {
		if (requiredArgs.size() > extractedArguments.size()) {
			int missingIndex = requiredArgs.size() - extractedArguments.size() - 1;
			System.err.println("Missing argument: <" + requiredArgs.get(missingIndex) + ">");
			printUsage();
		}

	}

	private void printUsage() {
		StringBuilder usageMsg = new StringBuilder();
		usageMsg.append("Usage: java " + programmName);

		for (String param : requiredParameters) {
			printToErrIfLineToLong(usageMsg);
			usageMsg.append(" -" + param + " " + param);
		}

		for (String param : optionalParameters) {
			printToErrIfLineToLong(usageMsg);
			usageMsg.append(" [-" + param + " " + param + "]");
		}

		for (String flag : flags) {
		        printToErrIfLineToLong(usageMsg);
			usageMsg.append(" [-" + flag + "]");
		}

		for (String arg : requiredArgs) {
			printToErrIfLineToLong(usageMsg);
			usageMsg.append(" <" + arg + ">");
		}

		for (String arg : optionalArgs) {
			printToErrIfLineToLong(usageMsg);
			usageMsg.append(" [" + arg + "]");
		}

		if (usageMsg.toString().trim().length() > 0) {
			System.err.println(usageMsg.toString());
		}

		System.exit(-1);
	}

	private void printToErrIfLineToLong(StringBuilder output) {
		if (output.length() > 65) {
			System.err.println(output.toString());
			output.setLength(0);
			output.append("\t\t");
		}
	}

	/**
	 * @return Can not be null
	 * @throws Exception
	 */
	public String getArgument(final String argName) {
		final int indexRequired = requiredArgs.indexOf(argName);
		if (indexRequired >= 0) {
			return extractedArguments.get(indexRequired);
		}

		final int indexOptional = optionalArgs.indexOf(argName);
		if (indexOptional >= 0) {
			try {
				return extractedArguments.get(indexOptional);
			} catch (IndexOutOfBoundsException e) {
				return null;
			}
		}

		throw new InvalidParameterException("Argument " + argName + " was not set as required or optional.");

	}

	/**
	 * @param parameterName
	 * @return Can be null for optional parameter.
	 */
	public String getParameter(final String parameterName) {
		return extractedParameter.get(parameterName);
	}
	
	public boolean isFlagSet(final String parameterName) {
		return extractedFlags.contains(parameterName);
	}
}