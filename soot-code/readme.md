## This is the transform tool for both the hadoop tests and java tests.  
The tool needs to be rebuilt when it is used for the first time or after modification.

    $ cd EhancedCFHider/
    $ ./build-jar.sh

### The sensitive variable that needs to be protected needs to be specified in EhancedCFHider/src/MyMain.java (line 146), where 
- taintClassName：the class where the sensitive variable is located.
- taintMethodName：the method where the sensitive variable is located.
- taintSourceName：the name of the sensitive variable.
