***
## func\_call\_v3.14\_JAVA
### This part is used when you encrypt the matrix(SGXindex), remember to build it before use.
    $ cd func_call_v3.14_JAVA/
    $ make  
    $ ./app input_file_path 128bits-key output_file_path #(optional)
***

## EnhancedSGX
### This part will build the enclave.so enclave.signed.so libSGX.so which will read the matrix in /tmp/SGXindex, remember to build it before use. 
    $ cd EnhancedSGX/  
    $ make
    $ ./app #(optional)
    # for hadoop case    
    $ cp libSGX.so ~/hadoop/lib/native/Linux-amd64-64  
    $ cp enclave.signed.so ~/hadoop/lib/native/Linux-amd64-64  
    $ cp enclave.so ~/hadoop/lib/native/Linux-amd64-64  
NOTE: The value of **tmpe** in line 167 of **EnhancedSGX/App/SGX_setting/SGX_setting.cpp** should be consistent with the actual location in the project. And *.so should be in the same dir on every nodes in the cluster.(scp)  
