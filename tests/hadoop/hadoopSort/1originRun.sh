#hadoop dfs -rmr terasort_output01

#hadoop jar Origin/TestForHadoopTeraSort.jar terasort_input01 terasort_output01

#exec 3>&1 4>&2 1>> time_record/4.17/origin1G.log 2>&1

I=1
II=1

while [ $I -le $II ]
do
	#echo $I>>time_record/replace.log
	echo $I
	hadoop dfs -rmr terasort_output01
  	time hadoop jar Origin/TestForHadoopTeraSort.jar terasort_input01 terasort_output01

I=`expr $I + 1`
done
