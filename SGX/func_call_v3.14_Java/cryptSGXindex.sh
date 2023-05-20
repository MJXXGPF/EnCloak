mv /tmp/SGXindex SGXindex
./app SGXindex 1234567812345678 SGXindex1
mv SGXindex SGXindex-`date +'%Y%m%d%H%M%S'`
mv SGXindex1 /tmp/SGXindex
