# hadoop dfs -rmr /user/xidian/wc_output

# time hadoop jar replaceOutput/ReplaceTestforHadoopWordCount.jar wc_input wc_output


#exec 3>&1 4>&2 1>> time_record/9.23/replace3g.log 2>&1

I=1
II=1

while [ $I -le $II ]
do
	echo $I
	hadoop dfs -rmr wc_output1G
	time hadoop jar replaceOutput/ReplaceTestforHadoopWordCount.jar wc_input1G wc_output1G

I=`expr $I + 1`
done
