javac -classpath ../lib/hadoop-core-1.0.4.jar:../lib/commons-cli-1.2.jar:../lib/commons-logging-1.2.jar -d ./bin ./src/*.java
cp -r bin/cfhider bin/invoker Origin/
cd Origin
jar -cfm TestForHadoopTeraSort.jar ../META-INF/MANIFEST.MF  cfhider/ invoker/

