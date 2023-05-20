hadoop dfs -rmr wc_input1G

hadoop fs -mkdir wc_input1G
hadoop fs -put 1G/* wc_input1G
