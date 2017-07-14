# Setup

Die tibrvnative.jar für den Java Teil
- Muss im classpath zu finden sein.

Die tibrvnative64.(dll|so) für den Native Teil
- Muss in java.library.path zu finden sein.
- Diese erbt unter Linux von ENV: LD_LIBRARY_PATH
- z.B: in /etc/profile   export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:/opt/tibco/tibrv/8.4/lib/"
- Diese erbt unter Windows von ENV: PATH


