find 30/ -name "*.class" -type f -print -exec rm -rf {} \;
find 30/ -name "*.jar" -type f -print -exec rm -rf {} \;
echo "ok1"
java -Xms512M -Xmx1024M -cp ../../../soot-lib/symja-2015-09-26.jar:../../../soot-lib/jasminclasses-custom.jar:../../../soot-lib/polyglotclasses-1.3.5.jar:../../../soot-lib/soot-trunk.jar:../../../soot-lib/commons-io-2.4.jar:../../../soot-lib/log4j-1.2.11.jar:../../../soot-code/SGXBranchReplacer/testcase.jar  LoggerMain30 -allow-phantom-refs -cp .:$JAVA_HOME/jre/lib/rt.jar:$JAVA_HOME/jre/lib/jce.jar:../lib/hadoop-tools-1.0.4.jar:../../../soot-lib/commons-logging-1.1.3.jar:../lib/hadoop-core-1.0.4.jar:../lib/commons-cli-1.2.jar -src-prec c -f c -include-all -process-dir bin -output-dir 30
echo "ok2"

cd 30/

jar -cfm ReplaceTestForHadoopPI.jar ../META-INF/MANIFEST.MF cfhider/ invoker/
echo "ok3"

