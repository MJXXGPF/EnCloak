#time /home/xidian/hadoop/bin/hadoop jar /home/xidian/hadoop/hadoop-examples-1.0.4.jar pi 10 100000

javac -classpath ../lib/hadoop-core-1.0.4.jar:../lib/commons-cli-1.2.jar:../lib/commons-logging-1.2.jar -d ./bin ./src/PiEstimator.java ./src/sgx_invoker.java
cp -r bin/cfhider bin/invoker Origin/
cd Origin
jar -cfm TestForHadoopPI.jar ../META-INF/MANIFEST.MF  cfhider/ invoker/

