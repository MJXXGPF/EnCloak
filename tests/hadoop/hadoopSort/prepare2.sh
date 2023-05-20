hadoop dfs -rmr terasort_input01
#hadoop jar ../lib/hadoop-examples-1.0.4.jar teragen 10737418 terasort_input01

hadoop dfs -rmr terasort_input02
hadoop jar ../lib/hadoop-examples-1.0.4.jar teragen 21474836 terasort_input02

hadoop dfs -rmr terasort_input03
#hadoop jar ../lib/hadoop-examples-1.0.4.jar teragen 32212254 terasort_input03

hadoop dfs -rmr terasort_input04
#hadoop jar ../lib/hadoop-examples-1.0.4.jar teragen 42949672 terasort_input04

hadoop dfs -rmr terasort_input05
#hadoop jar ../lib/hadoop-examples-1.0.4.jar teragen 53687090 terasort_input05

hadoop dfs -rmr terasort_input06
#hadoop jar ../lib/hadoop-examples-1.0.4.jar teragen 64424508 terasort_input06
#使用Teragen来产生数据，示例如下：
#hadoop jar hadoop-*-examples.jar teragen 参数1 参数2
#teragen的参数解释：
#  参数1：表示要产生的数据的行数。Teragen每行数据的大小是100B。
#  要产生1G的数据，需要的行数=
#1024*1024*1024/100=10737418行,21474836,32212254,42949672,53687090,64424508
#  参数2 : 产生的数据放置的文件夹地址

