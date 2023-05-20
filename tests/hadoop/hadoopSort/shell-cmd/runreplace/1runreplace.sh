#hadoop dfs -rmr terasort_replace_output01

#time hadoop jar replaceOutput/ReplaceTestForHadoopTeraSort.jar terasort_input01 terasort_replace_output01

exec 3>&1 4>&2 1>> time_record/4.17/replace1G.log 2>&1

I=1
II=3

while [ $I -le $II ]
do
	#echo $I>>time_record/replace.log
	echo $I
	hadoop dfs -rmr terasort_output01
  	time hadoop jar replaceOutput/ReplaceTestForHadoopTeraSort.jar terasort_input01 terasort_output01

I=`expr $I + 1`
done
