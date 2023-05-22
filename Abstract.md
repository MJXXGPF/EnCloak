# Abstract
In conjunction with Intel SGX technology, we have developed a prototype system, EnCloak, to secure Java applications.
We have provided source code related to this paper.
Section 4.2 of the paper regarding the theory of backward and forward item taint analysis is implemented in BackWardAnalysis.java in EnhancedCFHider in the source code we have provided; the code implementation of 4.3 and 4.4 of the thesis regarding basic and advanced transformations is implemented in EnhancedCFHider in Transformer.java; the implementation of the EI instructions (Table 1: Enclave Instructions) and the Cloak Enclave structure (Figure 2: Function Node in Cloak Enclave) is in SGX/EnhancedSGX/Enclave.cpp. The use case code for the experimental part of the paper is in the tests file and is divided into a hadoop application and a Java application.




## Environmental requirements

ubuntu 16.04

jdk1.7

SGX SDK 1.9

gcc 8.1
