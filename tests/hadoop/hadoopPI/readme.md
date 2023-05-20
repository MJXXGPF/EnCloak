## Specify a sensitive variable
Refer to **soot-code/readme.md** to specify a sensitive variable.

## Buid and run original java files
    $ mkdir bin Origin replaceOutput
    $ ./build-origin.sh  
    $ ./origin-run.sh  

## transform origin class files to new ones with SGX
    $ ./replace-transforme.sh  

## encrypt SGXindex  
    $ ./encrypt_SXGindex.sh  
*the encrypted SGXindex file should be copy to */tmp/* on every node in the cluster.(scp)*  

## runing this new files
    $ ./replace-run.sh  

## NOTICE: BEFORE TRANSFORM THE CLASS FILES, TO CLEAN THE *SGXindex* AND *counter* IN THE */tmp*.
