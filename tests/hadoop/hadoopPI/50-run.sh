
#exec 3>&1 4>&2 1>> time_record/50.log 2>&1

I=1
II=1

while [ $I -le $II ]
do
	#echo $I>>time_record/replace.log
	echo $I
  	time hadoop jar 50/ReplaceTestForHadoopPI.jar 10 100000

I=`expr $I + 1`
done
