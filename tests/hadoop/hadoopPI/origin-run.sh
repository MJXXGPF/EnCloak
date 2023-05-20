#time /home/xidian/hadoop/bin/hadoop jar /home/xidian/hadoop/hadoop-examples-1.0.4.jar pi 10 100000

#time hadoop jar Origin/TestForHadoopPI.jar 10 100000


#exec 3>&1 4>&2 1>> time_record/origin.log 2>&1

I=1
II=1 #11

while [ $I -le $II ]
do
	#echo $I>>time_record/replace.log
	echo $I
	hadoop dfs -rmr PiEstimator_TMP_3_141592654
  	time hadoop jar Origin/TestForHadoopPI.jar 10 100000

I=`expr $I + 1`
done
