# Preparation for Hadoop testcases  
### Install and configure the hadoop 1.0.4 in a cluster. Add the HAOOP_HOME and PATH to the sys ~/.bashrc.  
    export HADOOP_HOME=/YOUR/HADOOP/PATH  
    export PATH=$PATH:$HAOOP_HOME/bin    
### Start Hadoop service
    $ hadoop namenode -format  
    $ start-all.sh  
