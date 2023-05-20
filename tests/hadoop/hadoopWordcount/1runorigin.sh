# hadoop dfs -rmr wc_output1G

# time hadoop jar Origin/TestForHadoopWordCount.jar wc_input1G wc_output1G


#exec 3>&1 4>&2 1>> time_record/origin1G.log 2>&1

I=1
II=1

while [ $I -le $II ]
do
	#echo $I>>time_record/replace.log
	echo $I
	hadoop dfs -rmr wc_output1G
	time hadoop jar Origin/TestForHadoopWordCount.jar wc_input1G wc_output1G

I=`expr $I + 1`
done
