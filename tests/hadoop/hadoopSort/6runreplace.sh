#hadoop dfs -rmr terasort_replace_output01

#time hadoop jar replaceOutput/ReplaceTestForHadoopTeraSort.jar terasort_input01 terasort_replace_output01

#exec 3>&1 4>&2 1>> time_record/9.23/s/replace6G.log 2>&1

I=1
II=1

while [ $I -le $II ]
do
	#echo $I>>time_record/replace.log
	echo $I
	hadoop dfs -rmr terasort_output06
  	time hadoop jar replaceOutput/ReplaceTestForHadoopTeraSort.jar terasort_input06 terasort_output06

I=`expr $I + 1`
done
