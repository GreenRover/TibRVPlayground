# Setup

Die tibrvnative.jar für den Java Teil
- Muss im classpath zu finden sein.

Die tibrvnative64.(dll|so) für den Native Teil
- Muss in java.library.path zu finden sein.
- Diese erbt unter Linux von ENV: LD_LIBRARY_PATH
- z.B: in /etc/profile   export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:/opt/tibco/tibrv/9.0/lib/"
- Diese erbt unter Windows von ENV: PATH

## Maven build

- The project now includes `pom.xml` and expects `lib/tibrvnative.jar` to exist.
- Build with `mvn clean package`.
- Per-launch executable jars are created in `target/launch-jars/`.

## Running the JARs

The JARs in `target/launch-jars/` now include `tibrvnative.jar`. To run them, you only need to point to the native TIBCO RV libraries.

### Example (Linux)
```bash
java -Djava.library.path=/opt/tibco/tibrv/9.0/bin \
     -jar target/launch-jars/Listen.jar -service 7500 -network ";" -daemon "tcp:7500"
```

### Example (Windows)
```powershell
java -Djava.library.path="C:\tibco\tibrv\9.0\lib" `
     -jar target/launch-jars\Listen.jar -service 7500 -network ";" -daemon "tcp:7500"
```

Note: The `Main-Class` is set in the manifest of each JAR, allowing you to use the `-jar` option directly. The native TIBCO RV libraries (e.g., `tibrvnative64.dll` or `libtibrvnative.so`) must still be accessible via `java.library.path`.


