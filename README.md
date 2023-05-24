# Contents
Four classes of artifacts are included in this release:
1. JNI folder: This folder contains header files for Java programs calling C++ functions, which can be subdivided into header files for general Java program calls and header files for Hadoop program calls.
2. SGX folder: This folder contains the code implementation of the SGX section. The implementation of the EI instructions (Table 1: Enclave Instructions) and the Cloak Enclave structure (Figure 2: Function Node in Cloak Enclave) is in SGX/EnhancedSGX/Enclave.cpp.

3. soot-code folder: This file is mainly the code implementation of the conversion program and contains the jar packages needed for the soot framework. Section 4.2 of the paper regarding the theory of backward and forward item taint analysis is implemented in BackWardAnalysis.java ; the code implementation of 4.3 and 4.4 of the thesis regarding basic and advanced transformations is implemented  Transformer.java.
4.  tests folder: This folder contains all the test cases for the experimental part of the thesis, for more information on the test cases click on this link： https://github.com/MJXXGPF/EnCloak/blob/master/tests/readme.md.

# Environmental requirements
ubuntu 16.04

jdk1.7

SGX SDK 1.9

gcc 8.1


# Run Code Steps 
* Install JDK and SGX on your computer
* Build the c++ shared object in SGX folder
* Bulid the transform tool in soot-code folder
* Test the benchmark test cases
#### For specific instructions, please refer to the readme.md in the corresponding folder.
