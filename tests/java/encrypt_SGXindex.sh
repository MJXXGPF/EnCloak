cd ../../SGX/func_call_v3.14_Java/
mv /tmp/SGXindex /tmp/SGXindex1
mv /tmp/SGXindex1 ./
./app SGXindex1 1234567812345678 SGXindex
cp SGXindex /tmp/

#cd ../../../SGX/func_call_v3.14_Java/
mv /tmp/SGXinvoke /tmp/SGXinvoke1
mv /tmp/SGXinvoke1 ./
./app SGXinvoke1 1234567812345678 SGXinvoke
cp SGXinvoke /tmp/
