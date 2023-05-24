This folder contains two types of JNI header files, which are integral to Java calls to C++.
## hadoopJNI is the interface for hadoop tests to call the c++ shared object
### you can modify the interface, then  
    $ javac your-hadoop-invoker.java  
    $ javah your-hadoop-invoker

## javaJNI is the interface for java tests to call the c++ shared object  
### you can modify the interface, then  
    $ javac your-java-invoker.java
    $ javah your-java-invoker    
### The compiled \*.h file should be put into */SGX/EnhancedSGX/App/* 
