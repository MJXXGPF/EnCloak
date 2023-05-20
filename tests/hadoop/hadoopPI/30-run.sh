
#exec 3>&1 4>&2 1>> time_record/30.log 2>&1

I=1
II=1 #11

while [ $I -le $II ]
do
	#echo $I>>time_record/replace.log
	echo $I
	hadoop dfs -rmr PiEstimator_TMP_3_141592654
  	time hadoop jar 30/ReplaceTestForHadoopPI.jar 10 100000

I=`expr $I + 1`
done
