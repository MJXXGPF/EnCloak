cd ../../../SGX/func_call_v3.14_Java/
mv /tmp/SGXindex /tmp/SGXindex1
mv /tmp/SGXindex1 ./

mv /tmp/SGXinvoke /tmp/SGXinvoke1
mv /tmp/SGXinvoke1 ./

./app SGXindex1 1234567812345678 SGXindex
./app SGXinvoke1 1234567812345678 SGXinvoke
cp SGXindex /tmp/
cp SGXinvoke /tmp/
#cd ../../../SGX/func_call_v3.14_Java/
#mv /tmp/SGXinvoke /tmp/SGXinvoke1
#mv /tmp/SGXinvoke1 ./
#./app SGXinvoke1 1234567812345678 SGXinvoke
#cp SGXinvoke /tmp/
