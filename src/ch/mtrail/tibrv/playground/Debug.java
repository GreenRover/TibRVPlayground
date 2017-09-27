package ch.mtrail.tibrv.playground;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

public class Debug {

	public static void diplayEnvInfo() {
		// Debug ClassPath
		final ClassLoader cl = ClassLoader.getSystemClassLoader();

		final URL[] urls = ((URLClassLoader) cl).getURLs();

		for (final URL url : urls) {
			System.out.println("ClassLoaderFile: " + url.getFile());
		}

		// Debug ENV
		final Map<String, String> envVars = System.getenv();
		System.out.println("EnvPath: " + envVars.get("Path"));

		// Debug LibPath
		final String javaLibPath = System.getProperty("java.library.path");
		System.out.println("java.library.path: " + javaLibPath);
		for (final String javaLibPathPart : javaLibPath.split(":")) {
			System.out.println("path Part: " + javaLibPathPart);
		}
		/*
		 * for (final String var : envVars.keySet()) {
		 * System.err.println("examining " + var); if
		 * (envVars.get(var).equals(javaLibPath)) { System.out.println(var); } }
		 */

		System.out.flush();
	}

}
