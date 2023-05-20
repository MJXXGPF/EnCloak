
## What is this repository for?  
This repository contains the necessary files for a prototype of our proposed data flow protection scheme.


## FOLDERS  
### JNI  
This folder contains the JNI (cpp headers) for the java project to call the c++ shared object.

### SGX  
This folder contains the source code to build the c++ shared object.
##### Note: config the sgx in Enclave/Enclave.config.xml, especially for the heap and stack.

### soot-code  
Use soot to transform the java or hadoop code into a data flow version.

### tests  
Benchmark test cases for the projects.

## How do I get set up?  
* Install JDK and SGX on your computer
* Build the c++ shared object in SGX folder
* Bulid the transform tool in soot-code folder
* Test the benchmark test cases
#### For specific instructions, please refer to the readme.md in the corresponding folder.
