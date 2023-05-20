# Java Application Testing Guide
There are 7 java application tests in this folder. The source codes of the seven Java applications are in the **java/src/test/case/** directory.

## Add SGX path
Add LD_LIBRARY_PATH to your environment variable according to the project location. If your project location is **/home/sgx/SGX/EnCloak**, just add the following command to **~/.bashrc**.
    
    $ export LD_LIBRARY_PATH=/home/sgx/SGX/EnCloak/SGX/EnhancedSGX

## Select the Java application to test
Put the application to be tested in the **java/src/test/** directory, and specify the sensitive variable to be protected according to **soot-code/readme.md**. Don't forget to rebuild the transform tool. The currently specified sensitive variable is the array in the main method of the binary search application.

## Compile the source code
    $ ./build-origin.sh
  
## Clear last test cache
    $ ./rmtmp.sh
    
## Run the transform tool
    $ ./transformer.sh
 
## Encrypt SGXindex
    $ ./encrypt_SGXindex.sh

## Then run the transformed application
Here is an example of binary search:
    
    $ cd replaceOutput  
    $ java test.BinarySearch
