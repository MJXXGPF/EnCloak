#include <unistd.h>
#include <string.h>
#include <sgx_cpuid.h>
#include <stdlib.h>
#include <string>
#include <string.h>
#define Table_Len 10000
#define MAX 100
#include "io/fcntl.h"
#include "io/mman.h"
#include "io/stat.h"
#include "io/stdio.h"
#include "io/stdlib.h"
#include "io/time.h"
#include "io/unistd.h"
#include <sgx_tcrypto.h>
#include <ctype.h>
#include <map>
#include<time.h>
#include<ctime>
using namespace std;
#include "Enclave.h"
#include "Enclave_t.h"  /* print_string */
#include "common.h"


clock_t t1,t2;
void printf(const char *fmt, ...)
{
    char buf[BUFSIZ] = {'\0'};
    va_list ap;
    va_start(ap, fmt);
    vsnprintf(buf, BUFSIZ, fmt, ap);
    va_end(ap);
    ocall_print_string(buf);
}

//----------------struct--------------
struct Table_meta{
	int type;
	int p1;//index 0-100 from array  ,100-200 hash-int
	int p1_i; 
	int p2;
	int p2_i;
	int op; 
	int para_name;
	int para_i;
};

typedef struct Node {
	int v_int[20];
	double v_double[20];
	float v_float[10];
	char v_char[20];
	int v_byte[20];
	long v_long[20];
	
	char calluuid[33];

	char array[10][33];
	int  arrayIndex[10];
	long lineNo;

	int re[3];
}*SNODE, Node;

typedef struct ArrayNode {
	int* arr_int[10];
	double* arr_double[10];
	char* arr_char[10];
	long* arr_long[10];
	int* arr_byte[10];

	int intsize[10];
	int doublesize[10];
	int charsize[10];
	int longsize[10];
	int bytesize[10];
}*ANODE, ArrayNode;
typedef struct ArrayNode2{

	IntArrayNode * int_arrNodes[30];
	CharArrayNode * char_arrNodes[30];
	DoubleArrayNode * double_arrNodes[30];
	int double_sz;
	int int_sz;
	int char_sz;
}*ANODE2,ArrayNode2;
typedef struct MultiArrayNode{
	int arr_int[10][100];
	double arr_double[10][100];
	long arr_long[10][100];

	int intsize[10];
	int doublesize[10];
}*MNODE, MultiArrayNode;

typedef struct PublicVariableNode{
	
	int v_i;	
	double v_d;
	float v_f;
	char v_c;
	int v_b;
	long v_l;	

	int arr_int[10];
	double arr_double[10];	
	long arr_long[10];	
	
	int arr_multi_int[100][100];
	double arr_multi_double[100][100];
	long arr_multi_long[2][2];
	int intsize;
	int doublesize;
	int longsize;

	int intmultisize[10];
	int doublemultisize[10];
	int longemultisize[10];
}*PNODE, PublicVariableNode;

//------------==================--------HASHMAP-----------========================-------------------
template<class Key, class Value>
class HashNode
{
public:
	Key    _key;
	Value  _value;
	HashNode *next;

	HashNode(Key key, Value value)
	{
		_key = key;
		_value = value;
		next = NULL;
	}
	~HashNode()
	{

	}
	HashNode& operator=(const HashNode& node)
	{
		_key = node.key;
		//_value = node.key;
		_value = node.value;
		next = node.next;
		return *this;
	}
};

template <class Key, class Value, class HashFunc, class EqualKey>
class HashMap
{
public:
	HashMap(int size);
	~HashMap();
	bool insert(const Key& key, const Value& value);
	bool del(const Key& key);
	Value& find(const Key& key);
	Value& operator [](const Key& key);

private:
	HashFunc hash;
	EqualKey equal;
	HashNode<Key, Value> **table;
	unsigned int _size;
	Value ValueNULL;
};


template <class Key, class Value, class HashFunc, class EqualKey>
HashMap<Key, Value, HashFunc, EqualKey>::HashMap(int size) : _size(size)
{
	hash = HashFunc();
	equal = EqualKey();
	table = new HashNode<Key, Value> *[_size];
	for (unsigned i = 0; i < _size; i++)
		table[i] = NULL;
}



template <class Key, class Value, class HashFunc, class EqualKey>
HashMap<Key, Value, HashFunc, EqualKey>::~HashMap()
{
	for (unsigned i = 0; i < _size; i++)
	{
		HashNode<Key, Value> *currentNode = table[i];
		while (currentNode)
		{
			HashNode<Key, Value> *temp = currentNode;
			currentNode = currentNode->next;
			delete temp;
		}
	}
	delete table;
}


template <class Key, class Value, class HashFunc, class EqualKey>
bool HashMap<Key, Value, HashFunc, EqualKey>::insert(const Key& key, const Value& value)
{
	int index = hash(key) % _size;
	HashNode<Key, Value> *node = new HashNode<Key, Value>(key, value);
	node->next = table[index];
	table[index] = node;
	return true;
}
template <class Key, class Value, class HashFunc, class EqualKey>
bool HashMap<Key, Value, HashFunc, EqualKey>::del(const Key& key)
{
	unsigned index = hash(key) % _size;
	HashNode<Key, Value> * node = table[index];
	HashNode<Key, Value> * prev = NULL;
	while (node)
	{
		if (node->_key == key)
		{
			if (prev == NULL)
			{
				table[index] = node->next;
			}
			else
			{
				prev->next = node->next;
			}
			delete node;
			return true;
		}
		prev = node;
		node = node->next;
	}
	return false;
}


template <class Key, class Value, class HashFunc, class EqualKey>
Value& HashMap<Key, Value, HashFunc, EqualKey>::find(const Key& key)
{
	unsigned  index = hash(key) % _size;
	if (table[index] == NULL){
		return ValueNULL;
	}
	else
	{	
		HashNode<Key, Value> * node = table[index];
		
		while (node)
		{
			
			if (node->_key == key){
				
				return node->_value;
			}
				
			node = node->next;
		}
		
		printf("key is not find! index :%u\n",index);
		
		return ValueNULL;
	}
}


template <class Key, class Value, class HashFunc, class EqualKey>
Value& HashMap<Key, Value, HashFunc, EqualKey>::operator [](const Key& key)
{
	return find(key);
}

class HashFunc
{
public:
	int operator()(const string & key)
	{
		int hash = 0;
		for (int i = 0; i < key.length(); ++i)
		{
			hash = hash << 7 ^ key[i];
		}
		return (hash & 0x7FFFFFFF);


		
	}
};

class EqualKey
{
public:
	bool operator()(const string & A, const string & B)
	{
		if (A.compare(B) == 0)
			return true;
		else
			return false;
	}
};

HashMap<string, SNODE, HashFunc, EqualKey> hashmap(30); // HashMap

HashMap<string, ANODE, HashFunc, EqualKey> hashmapArray(30); // HashMap
HashMap<string, ANODE2, HashFunc, EqualKey> hashmapArray2(30); // HashMap

HashMap<string, MNODE, HashFunc, EqualKey> hashmapMultiArray(1); // HashMap

HashMap<string, PNODE, HashFunc, EqualKey> hashmapPublicV(30); // HashMap

//------------==================--------HASHMAP-----------========================-------------------


//--------------------------------------------------------------

//char file[500]="/home/xidian/CF/MatrixEncrypt/SGXindex1";


char updateFile[50]="/tmp/mergeIndex";


char file[50]="/tmp/SGXindex";
int hash_int[Table_Len];
double hash_double[Table_Len];
float hash_float[Table_Len];
char hash_char[Table_Len];
long hash_long[Table_Len];
char hash_byte[Table_Len];
int *table=(int*)malloc(sizeof(int)*10000);
int hash_index[Table_Len]; //for constant if it is int or double...

//invoke file
char invokefile[50]="/tmp/SGXinvoke";
int hash_invoke_int[Table_Len];
double hash_invoke_double[Table_Len];
char hash_invoke_char[Table_Len];
long hash_invoke_long[Table_Len];
int hash_invoke_byte[Table_Len];
int *invoketable=(int*)malloc(sizeof(int)*1000);
int hash_invoke_index[Table_Len]; //for constant if it is int or double...
int lineIndex[100];



int ecall_ctr_decrypt(uint8_t *sql, 
	const char *sgx_ctr_key, uint8_t *p_dst,int len)    //ecall_ctr_decrypt(c,key_t,ppp,64);
{
	const uint32_t src_len = len;
	uint8_t p_ctr[16]= {'0'};
	const uint32_t ctr_inc_bits = 128;
	uint8_t *sgx_ctr_keys = (uint8_t *)malloc(16*sizeof(char));
	memcpy(sgx_ctr_keys,sgx_ctr_key,16);

	
	sgx_status_t rc;
	uint8_t *p_dsts2 = (uint8_t *)malloc(src_len*sizeof(char));
	rc = sgx_aes_ctr_decrypt((sgx_aes_gcm_128bit_key_t *)sgx_ctr_keys, sql, src_len, p_ctr, ctr_inc_bits, p_dsts2);

	for(int i=0; i<src_len; i++){
		p_dst[i] = p_dsts2[i];
		
	}

	free(sgx_ctr_keys);
	return 0;
}



int encall_hash_readin(char* buf,long line)
{
	char buffer[50];
	char c=*buf;
	switch(c)
	{
		case 'i':strncpy(buffer,buf+4,44);//int_
			int int_data;
			int_data=atoi(buffer);
			hash_int[line]=int_data;
			hash_index[line] = 1;
			break;
		case 'd':strncpy(buffer,buf+7,44);//double_
			double double_data;
			double_data=atof(buffer);
			hash_double[line]=double_data;
			hash_index[line] = 2;
			break;
		case 'f':strncpy(buffer,buf+6,44);//float_
			float float_data;
			float_data=atof(buffer);
			hash_float[line]=(float)float_data;
			hash_index[line] = 3;
			break;
		case 'c':strncpy(buffer,buf+5,44);//char_
			char char_data;
			char_data=*buffer;
			hash_char[line]=char_data;
			hash_index[line] = 4;
			break;
		case 'l':strncpy(buffer,buf+5,44);//long_
			long long_data;
			long_data=atol(buffer);
			hash_long[line]=long_data;
			hash_index[line] = 5;
			break;
		case '\0':
			break;
		default:
			hash_int[line]=0;
			hash_double[line]=0;
			hash_float[line]=0;
			hash_char[line]=0;
			hash_long[line]=0;
			return -6;
	}
	return 1;
}
int encall_hash_invoke_readin(char* buf,long line)
{
	char buffer[50];
	//return -10;
	char c=*buf;
	ocall_print_string("read value-------------------------\n");
	switch(c)
	{
		case 'i':strncpy(buffer,buf+4,44);//int_
			int int_data;
			int_data=atoi(buffer);
			hash_invoke_int[line]=int_data;
			hash_invoke_index[line] = 1;
			break;
		case 'd':strncpy(buffer,buf+7,44);//double_
			double double_data;
			double_data=atof(buffer);
			hash_invoke_double[line]=double_data;
			hash_invoke_index[line] = 2;
			break;
		case 'c':strncpy(buffer,buf+5,44);//char_
			char char_data;
			char_data=*buffer;
			hash_invoke_char[line]=char_data;
			hash_invoke_index[line] = 4;
			break;
		case 'l':strncpy(buffer,buf+5,44);//long_
			printf("$$$$$$$$$$$$$$$$$ %s\n", buffer);
			long long_data;
			long_data=atol(buffer);
			hash_invoke_long[line]=long_data;
			hash_invoke_index[line] = 5;
			break;
		case '\0':
			break;
		default:
			return -6;
	}
	return 1;
}
Table_meta get_table_meta(long Line)
{
	Table_meta meta;
	meta.type=*(table+Line*8);
	meta.p1=*(table+Line*8+1);
	meta.p1_i=*(table+Line*8+2);
	meta.p2=*(table+Line*8+3);
	meta.p2_i=*(table+Line*8+4);
	meta.op=*(table+Line*8+5);
	meta.para_name=*(table+Line*8+6);
	meta.para_i=*(table+Line*8+7);
	return meta;
}
char ret[50000];
long ret_len=0;
long g_line_num=0;
int split_file(int isIndex)
{
	char line[50]={0};
	int k=0;
	long line_num=0;
	ocall_print_string("splitting ret_len:\n");
	ocall_print_long(ret_len);

	
	for(long i=0;i<ret_len;i++){
		if(ret[i]=='\n'){
			line[k]=0;
			if(k==0){
				continue;
			}

			encall_read_line(line,k,line_num,isIndex);
			line_num++;
			k=0;
		}else{
			line[k]=ret[i];
			k++;
		}
	}
	g_line_num=line_num;	
	printf("line_num1: %ld \n",line_num);
	ret_len = 0;
	g_line_num = 0;
	return line_num;
}

map<int,int> mymap;

//read updateIndex file to hashmapUpdateIndex
int handleline(char * text) {

    char *p; 
    p = strtok(text, ",");
    bool flag = true;
    char *key;
    while(p){  
	    
	
        if(flag){
		key = p;
		flag=false;
    	}else{
		mymap[atoi(key)]=atoi(p);
	    	flag= true;
	}
	p = strtok(NULL, ",");  
	   
    }
    return 0;
}

void read_update_index_file()
{
	

	
	int reout=open(updateFile,O_RDONLY,S_IRUSR);

//------------read out
	long l=0;
	char sss[MAX];
	int k=0;
	memset(sss,0,MAX);
	unsigned char c[MAX];
	long loop2=0;
	long loop=0;
	while(1){
		l=read(reout,c,64);                                                //????????
	
		if(l<64){
			break;
		}
		if(64==l){
			c[64]=0;
			for(int i=0;i<l-1;i++){
				if(c[i]=='\n'){
					sss[k++]=',';
				}else{
					sss[k++]=c[i];
				}
			}
		
			l=0;
			memset(c,0,65);
		}
	}
	if(l<64){
		c[l]=0;
		for(int i=0;i<l-1;i++){
	
			if(c[i]=='\n'){
				sss[k++]=',';
			}else{
				sss[k++]=c[i];
			}
		}
		printf("\n");
		handleline(sss);
	
		l=0;
	}
	
}
//end read updatefile

int read_table(char* file,int isIndex)
{
	
	memset(ret,0,50000);
	char* key_t="1234567812345678";

	int reout=open(file,O_RDONLY,S_IRUSR);

	printf("reout:%d\n",reout);
//------------read out
	long l=0;
	unsigned char sss[MAX];

	memset(sss,0,MAX);
	unsigned char c[MAX];
	long loop2=0;
	long loop=0;
	while(1){
		loop++;
		if(loop%1000==0){
			sleep(0);
		}
		l=read(reout,c,64);                                                //????????
		if(l<64){
			break;
		}
		
		if(64==l){
			c[64]=0;
			unsigned char ppp[MAX];
			memset(ppp,0,MAX);
			ecall_ctr_decrypt(c,key_t,ppp,64);
			for(int i=0;i<l;i++){
				strncat(ret,(const char*)&ppp[i],1);
			}
			ret_len=ret_len+l;
			l=0;
			memset(c,0,65);
		}
	}
	if(l<64){
		
		c[l]=0;
		unsigned char ppp[MAX];
		memset(ppp,0,MAX);
		ecall_ctr_decrypt(c,key_t,ppp,l);
		for(int i=0;i<l;i++){
			strncat(ret,(const char*)&ppp[i],1);
		}
		ret_len=ret_len+l;
		l=0;
	}
	int lineno = split_file(isIndex);
	return lineno;
}

int encall_table_load(void)
{
	
	printf("Start to write the hashmap about updateIndex\n");
	read_update_index_file();
	int lineno = read_table(file,1);
	memset(invoketable, -1, sizeof(invoketable));
	lineno = read_table(invokefile,0);
	ocall_print_string("invoke read ok\n");
	memset(lineIndex, -1, sizeof(lineIndex));
	//set lineIndex
	int temp = 0;
	for(int j=0;j<lineno;j++){
		if(invoketable[j]==0 && j % 3 ==0){
			lineIndex[temp] = j;
			temp++;
		}
	}

	return 1;
}


int encall_read_line(char* in_buf,int buf_len,long line,int isIndex)
{
	int read_num=0;
	if(*in_buf>=48 && *in_buf<=57){// number
		read_num=atoi(in_buf);
	}else if(*in_buf == 45){
		read_num=atoi(in_buf);
	}else if (!strncmp(in_buf,"call_", 5)){ //call_
		char buffer[50];
		strncpy(buffer,in_buf+5,44);
		int call = atoi(buffer);
		read_num = call;

	}else if (!strncmp(in_buf,"re", 2)){ 
		read_num = -2;
	}else{
		read_num=0-line;
		if(isIndex == 1){
			encall_hash_readin(in_buf,line);
		}else{
			encall_hash_invoke_readin(in_buf,line);	
		}
	}
	if(isIndex == 1){
		table[line]=read_num;
	}else{
		invoketable[line] = read_num;
	}
	return 0;
}




//-------------hotcall---------------------
void EcallStartResponder( HotCall* hotEcall )
{
    void (*callbacks[1])(void*,void*,int*,int,double*,int,float*,int,char*,int,long*,int,char*,int,char*,char*);
    callbacks[0] = encall_switch_type_branch;
    callbacks[3] = encall_switch_type_update;
    callbacks[4] = encall_switch_type_get_i;
    HotCallTable callTable;
    callTable.numEntries = 1;
    callTable.callbacks  = callbacks;
    HotCall_waitForCall( hotEcall, &callTable );

}
void EcallStartResponder3( HotCall* hotEcall3 )
{
    void (*callback[1])(void*,char*,char*);
    callback[1] = encall_varible;
    callback[2] = encall_deleteValue;
    HotCallTable callTable;
    callTable.numEntries = 1;
    callTable.callback  = callback;
    HotCall_waitForCall( hotEcall3, &callTable );
}
//-----------------------------------------


void InitArray(ArrayNode *ANODE, int m){
	
	for(int i=0;i<m;i++){
		ANODE->arr_int[i] = NULL;
		ANODE->arr_double[i] = NULL;
		ANODE->arr_char[i] = NULL;
		ANODE->arr_long[i] = NULL;
		ANODE->arr_byte[i] = NULL;
	}
}

void initArrayRow(ArrayNode *ANODE,int type, int size) {
	switch (type/10){
		case 7:
			ANODE->arr_int[type % 10] = (int*)malloc(size * sizeof(int));
			memset(ANODE->arr_int[type % 10], 0, size);
			ANODE->intsize[type % 10] = size;
			break;
		case 8:
			ANODE->arr_double[type % 10] = (double*)malloc(size * sizeof(double));
			memset(ANODE->arr_double[type % 10], 0, size);
			ANODE->doublesize[type % 10] = size;
			break;
		case 10:
			ANODE->arr_char[type % 10] = (char*)malloc(size * sizeof(char));
			memset(ANODE->arr_char[type % 10], 0, size);
			ANODE->charsize[type % 10] = size;
			break;
		case 11:
			ANODE->arr_long[type % 10] = (long*)malloc(size * sizeof(long));
			memset(ANODE->arr_long[type % 10], 0, size);
			ANODE->longsize[type % 10] = size;
			break;
		case 12:
			ANODE->arr_byte[type % 10] = (int*)malloc(size * sizeof(int));
			memset(ANODE->arr_byte[type % 10], 0, size);
			ANODE->bytesize[type % 10] = size;
			break;
		default:
			break;
	}
}

void FreeArray(ArrayNode *ANODE,int m)
{
	for (int i = 0;i < m;i++) {
		if (ANODE->arr_int[i] != NULL) {
			free(ANODE->arr_int[i]);
		}else {
			break;
		}
	}
	for (int i = 0;i <m;i++) {
		if (ANODE->arr_double[i] != NULL) {
			free(ANODE->arr_double[i]);
		}else{
			break;
		}
	}
	for (int i = 0;i < m;i++) {
		if (ANODE->arr_char[i] != NULL) {
			free(ANODE->arr_char[i]);
		}else {
			break;
		}
	}
	for (int i = 0;i < m;i++) {
		if (ANODE->arr_long[i] != NULL) {
			free(ANODE->arr_long[i]);
		}else {
			break;
		}
	}
	for (int i = 0;i < m;i++) {
		if (ANODE->arr_byte[i] != NULL) {
			free(ANODE->arr_byte[i]);
		}else {
			break;
		}
	}
	free(ANODE->arr_double);
	free(ANODE->arr_int);
	free(ANODE->arr_long);
	free(ANODE->arr_char);
	free(ANODE->arr_byte);
}

void encall_varible(void* data,char* uuid,char* calluuid) { //int* k,

	long *data1 = (long*)data;
	long lineNo = *data1;

	SNODE s = (SNODE)malloc(sizeof(Node));

	if(lineNo != 0L){
		s-> lineNo = lineNo;
		memcpy(s->calluuid,calluuid,32);	
		int start = lineIndex[lineNo-1];
		int end = (lineIndex[lineNo]!=-1)?lineIndex[lineNo]:1000;
		int ii=0;
		int dd=0;
		int cc=0;
		int ll=0;
		int bb=0;
		for(int i=start;i<end;i=i+3){
			int paraindex = invoketable[i];
			int isFromSelf = invoketable[i+1];
			int index = invoketable[i+2];
			if(paraindex == 0 && isFromSelf==0 && index==0) break;//out			
			if(isFromSelf == 1 && paraindex !=-2){
				if(index < 0){ //constant
					switch (hash_invoke_index[-index]) {
						case 1:
							s->v_int[ii++] = hash_invoke_int[-index];
							break;
						case 2:
							s->v_double[dd++] = hash_invoke_double[-index];
							break;
						case 4:
							s->v_char[cc++] = hash_invoke_char[-index];
							break;
						case 5:
							s->v_long[ll++] = hash_invoke_long[-index];
							break;
						case 6://????
							printf("[ERROR] I don't meet this problem!\n");
							s->v_byte[bb++] = hash_invoke_byte[-index];
							break;
					}
				}else if(index<100){ //array
					memcpy(s->array[paraindex],calluuid,32);			
					s->arrayIndex[paraindex] = index;

				}else if(index>=100){           //variables
					switch (index/100) {
						case 1:
							s->v_int[ii++] = hashmap.find(calluuid)->v_int[index-100];
							break;
						case 2:
							s->v_double[dd++] = hashmap.find(calluuid)->v_double[index-200];
							break;
						case 4:
							s->v_char[cc++] = hashmap.find(calluuid)->v_char[index-400];
							break;
						case 5:
							s->v_long[ll++] = hashmap.find(calluuid)->v_long[index-500];
							break;
						case 6:
							printf("[ERROR] I don't meet this problem too!\n");
							s->v_byte[bb++] = hashmap.find(calluuid)->v_byte[index-600];
							break;
					}
					
				}		
			}else if(isFromSelf == 2){  //isFromSelf == 0 array
				memset(s->array[paraindex],0,32);
				memcpy(s->array[paraindex],hashmap.find(calluuid)->array[index],32);
				s->arrayIndex[paraindex] = hashmap.find(calluuid)->arrayIndex[index];
			}else{
				//printf("=%d is a return=\n",i);
				s->re[0] = paraindex;
				s->re[1] = isFromSelf;
				s->re[2] = index;
			}
		}
	}else{
	}
	

	//insert
	if(!hashmap.insert(uuid,s)){
		printf("insert fail!! %s\n",uuid);
	}
	ANODE a = (ANODE)malloc(sizeof(ArrayNode));
	InitArray(a, 10);
	if(!hashmapArray.insert(uuid,a)){
		printf("insert fail!! %s\n",uuid);
	}
	//多维数组注释1022-1026
	// struct MultiArrayNode m[10] = {NULL,NULL,NULL,NULL};
	// MNODE m = (MNODE)malloc(10*sizeof(MultiArrayNode));
	// if(!hashmapMultiArray.insert(uuid,m)){
	// 	printf("insert fail!! %s\n",uuid);
	// }

	
}

void encall_deleteValue(void* data,char* uuid,char* cuuid) {
	
	long *data1 = (long*)data;
	long status = *data1;
	if(status==1L){
		printf("It need to destory! %s\n",cuuid);
		free(hashmapPublicV.find(cuuid));
		hashmapPublicV.find(cuuid) = NULL;
		hashmapPublicV.del(cuuid);
	}
	//printf("1\n");
	free(hashmap.find(uuid));
	//printf("2\n");
	hashmap.find(uuid) = NULL;
	//printf("3\n");
	hashmap.del(uuid);
	//printf("4\n");

	if(!hashmapArray.find(uuid)){
		FreeArray(hashmapArray.find(uuid),10);
	}
	//printf("5\n");
	if(hashmapArray2.find(uuid)){
		int sz=hashmapArray2.find(uuid)->int_sz;
		//printf("int_sz=%d\n", sz);
		for(int i=0;i<sz;i++){
			//printf("i=%d\n", i);
			if(hashmapArray2.find(uuid)->int_arrNodes[i]!=NULL){
				//printf("int_arrNodes[i]!=NULL\n");
				if(hashmapArray2.find(uuid)->int_arrNodes[i]->sz!=0){
					//printf("data sz=%d\n",hashmapArray2.find(uuid)->int_arrNodes[i]->sz);
					free(hashmapArray2.find(uuid)->int_arrNodes[i]->data);

				}
				//printf("free\n");
				//printf("d=%d\n",hashmapArray2.find(uuid)->int_arrNodes[i]->d);
				free(hashmapArray2.find(uuid)->int_arrNodes[i]);
				hashmapArray2.find(uuid)->int_arrNodes[i]=NULL;
					
					
				
			}
		}
	}
	//printf("6\n");
//污染变量中没有数组将916-922进行注释可以提高效率
	free(hashmapArray.find(uuid));   // I don't know if it will success free 0508
	hashmapArray.find(uuid) = NULL;
	hashmapArray.del(uuid);
	free(hashmapArray2.find(uuid));  
	hashmapArray2.find(uuid) = NULL;
	hashmapArray2.del(uuid);
}

void encall_initmultiArray(long line,char* uuid,char* cuuid){
	
	
	if(!hashmapPublicV.find(cuuid)){
		printf("init PNODE cuuid=%s\n",cuuid);
	
		PNODE p = (PNODE)malloc(10*sizeof(PublicVariableNode));
		if(!hashmapPublicV.insert(cuuid,p)){
			printf("insert fail!! %s\n",cuuid);
		}
	}
}




int encall_getArraySize(long Line,char* uuid){

	Table_meta meta=get_table_meta(Line);
	int p1 = meta.p1;
	int p1_i = meta.p1_i;
	int p2 = meta.p2;
	int p2_i = meta.p2_i;
	int op = meta.op;
	int para_name = meta.para_name;
	int para_i = meta.para_i;	

	int return_size=0;
	if(p1_i>=700 && p1_i<=1200){ // this uuid is cuuid
		switch(p1_i/100){
			case 7:return_size = hashmapPublicV.find(uuid)[p1_i%10].intsize;break;
			case 8:return_size = hashmapPublicV.find(uuid)[p1_i%10].doublesize;break;
		}
	}else if(p1_i>=70 && p1_i<=120){
		
		switch(p1_i/10){
			case 7:
				if(hashmapArray.find(uuid)->intsize[p1_i%10]<0){
					int a = -hashmapArray.find(uuid)->intsize[p1_i%10];
					int b = hashmap.find(uuid)->v_int[a-100];
					return_size = hashmapMultiArray.find(uuid)[p1_i%10].intsize[b];
				}else{
					return_size = hashmapArray.find(uuid)->intsize[p1_i%10];
				}
				break;
			case 8:
				if(hashmapArray.find(uuid)->doublesize[p1_i%10]<0){
					int a = -hashmapArray.find(uuid)->doublesize[p1_i%10];
					int b = hashmap.find(uuid)->v_int[a-100];
					return_size = hashmapMultiArray.find(uuid)[p1_i%10].doublesize[b];
				}else{
					return_size = hashmapArray.find(uuid)->doublesize[p1_i%10];
				}
				break;

			case 10:return_size = hashmapArray.find(uuid)->charsize[p1_i%10];break;
			case 11:return_size = hashmapArray.find(uuid)->longsize[p1_i%10];break;
			case 12:return_size = hashmapArray.find(uuid)->bytesize[p1_i%10];break;
		}
	}else if(p1_i<10){
		int index = hashmap.find(uuid)->arrayIndex[p1_i];
		switch(index/10){
			case 7:return_size = hashmapArray.find(hashmap.find(uuid)->array[p1_i])->intsize[index%10];break;
			case 8:return_size = hashmapArray.find(hashmap.find(uuid)->array[p1_i])->doublesize[index%10];break;
			case 10:return_size = hashmapArray.find(hashmap.find(uuid)->array[p1_i])->charsize[index%10];break;
			case 11:return_size = hashmapArray.find(hashmap.find(uuid)->array[p1_i])->longsize[index%10];break;
			case 12:return_size = hashmapArray.find(hashmap.find(uuid)->array[p1_i])->bytesize[index%10];break;
		}
	}
	return return_size;
}


void encall_getIntArray(int* re,int size,long Line,char* uuid){
	
	Table_meta meta=get_table_meta(Line);
	int p1_i = meta.p1_i;
	if(p1_i >=700 && p1_i<=1200){ //this uuid is cuuid
		for(int i=0;i<size;i++){
			re[i] = hashmapPublicV.find(uuid)[p1_i%10].arr_int[i];
		}
	}else if(p1_i >= 70 && p1_i<=120){
		if(hashmapArray.find(uuid)->intsize[p1_i%10]<0){
			int a = -hashmapArray.find(uuid)->intsize[p1_i%10];
			int b = hashmap.find(uuid)->v_int[a-100];
			for(int i=0;i<size;i++){
				re[i] = hashmapMultiArray.find(uuid)[p1_i%10].arr_int[b][i];
			}
		}else{
			for(int i=0;i<size;i++){
				re[i] = hashmapArray.find(uuid)->arr_int[p1_i%10][i];
			}	
		}
	}else if(p1_i <10){
		for(int i=0;i<size;i++){
			re[i] = hashmapArray.find(hashmap.find(uuid)->array[p1_i])->arr_int[hashmap.find(uuid)->arrayIndex[p1_i]%10][i];
		}
	}
}

void encall_getDoubleArray(double* re,int size,long Line,char* uuid){
	
	Table_meta meta=get_table_meta(Line);
	int p1_i = meta.p1_i;
	if(p1_i >=700 && p1_i<=1200){ //this uuid is cuuid
		for(int i=0;i<size;i++){
			re[i] = hashmapPublicV.find(uuid)[p1_i%10].arr_double[i];
		}

	}else if(p1_i >= 70 && p1_i<=120){
		if(hashmapArray.find(uuid)->doublesize[p1_i%10]<0){
			int a = -hashmapArray.find(uuid)->doublesize[p1_i%10];
			int b = hashmap.find(uuid)->v_int[a-100];
			for(int i=0;i<size;i++){
				re[i] = hashmapMultiArray.find(uuid)[p1_i%10].arr_double[b][i];
			}
		}else{
			for(int i=0;i<size;i++){
				re[i] = hashmapArray.find(uuid)->arr_double[p1_i%10][i];
			}	
		}
	}else if(p1_i <10){
		for(int i=0;i<size;i++){
			re[i] = hashmapArray.find(hashmap.find(uuid)->array[p1_i])->arr_double[hashmap.find(uuid)->arrayIndex[p1_i]%10][i];
		}
	}
}


void encall_initArray(char* uuid,int index,int size,int isSens){
	int realsize = 0;
	//get real size
	if(isSens == 0){
		realsize = size;
	}else if(isSens == 1){
		if(size>99 && size <200){
			realsize = hashmap.find(uuid)->v_int[size-100];
		}else{
			printf("Something Wrong in initArray Index Size! 0527\n");
		}
	}
	initArrayRow(hashmapArray.find(uuid), index, realsize);
}
void encall_initNode(char* uuid,int type,int size){
		
	 ArrayNode2* node=hashmapArray2.find(uuid);
	 if(node==NULL){
	 	printf("null\n");
	 	node=(ArrayNode2*)malloc(sizeof(ArrayNode2));
	 }
	 if(!hashmapArray2.insert(uuid,node)){
	 	printf("insert fail\n");
	 }
	 
	 if(type==7||type==13){
	 	 for(int i=0;i<size;i++){
	 	 	node->int_arrNodes[i]=(IntArrayNode*)malloc(sizeof(IntArrayNode));
	 	 	for(int j=0;j<5;j++){
	 	 		node->int_arrNodes[i]->index[j]=-1;
	 	 		node->int_arrNodes[i]->dimensions[j]=-1;
	 	 	}
	 	 	node->int_arrNodes[i]->paramLoc=-1;
	 	 	node->int_arrNodes[i]->sz=0;
	 	 }
	 	 node->int_sz=size;
	 }
	 if(node->int_arrNodes[0]==NULL){
	 	printf("null\n");
	 }else{
	 	printf("not null\n");
	 }
	  if(type==10){
	 	 for(int i=0;i<size;i++){
	 	 	node->char_arrNodes[i]=(CharArrayNode*)malloc(sizeof(CharArrayNode));
	 	 	for(int j=0;j<5;j++){
	 	 		node->char_arrNodes[i]->index[j]=-1;
	 	 		node->char_arrNodes[i]->dimensions[j]=-1;
	 	 	}
	 	 	node->char_arrNodes[i]->paramLoc=-1;
	 	 	node->char_arrNodes[i]->sz=0;
	 	 }
	 	 node->char_sz=size;
	 }
	
}

void encall_switch_type_get_i(void* data,void* rei,int* int_array,int int_tail,double* double_array,int double_tail,float* float_array,int float_tail,char* char_array,int char_tail,long* long_array, int long_tail,char* byte_array, int byte_tail,char* uuid,char* cuuid) {
	long *data1 = (long*)data;
        long Line = *data1;
	int return_flag = -1;

	switch (*(table+Line*8)) {
		case 1:return_flag = print_int(Line, int_array,uuid,cuuid);break;
		case 2:return_flag = print_double(Line, double_array,int_array,uuid,cuuid);break;
		case 4:return_flag = print_char(Line, char_array,uuid);break;
		case 5:return_flag = print_long(Line, long_array,int_array,uuid,cuuid);break;
		case 6:return_flag = print_byte(Line, byte_array,int_array,uuid);break;
		default:return_flag = -5;
	}
       int *re = (int*)rei;
       *re = return_flag;
}

void encall_switch_type_branch(void* data,void* rei,int* int_array,int int_tail,double* double_array,int double_tail,float* float_array,int float_tail,char* char_array,int char_tail,long* long_array, int long_tail,char* byte_array, int byte_tail,char* uuid,char* cuuid) {

	long *data1 = (long*)data;
        long Line = *data1;
	int return_flag = -1;

	switch (*(table+Line*8)) {
		case 1:return_flag = print_int(Line, int_array,uuid,cuuid);break;
		case 2:return_flag = print_double(Line, double_array,int_array,uuid,cuuid);break;
		case 3:return_flag = print_float(Line, float_array,uuid);break;
		case 4:return_flag = print_char(Line, char_array,uuid);break;
		case 5:return_flag = print_long(Line, long_array,int_array,uuid,cuuid);break;
		case 6:return_flag = print_byte(Line, byte_array,int_array,uuid);break;
		default:
			//printf("brach");
			return_flag = -5;
	}

       int *re = (int*)rei;
       *re = return_flag;
}

void encall_switch_type_update(void* data,void* rei,int* int_array,int int_tail,double* double_array,int double_tail,float* float_array,int float_tail,char* char_array,int char_tail,long* long_array, int long_tail,char* byte_array, int byte_tail,char* uuid,char* cuuid) {
	long *data1 = (long*)data;
        long Line = *data1;
	int return_flag = -1;
	
	
	int numbers = mymap[Line];
	int count = 0;
	if(numbers==0){
		switch (*(table+Line*8)) {
			case 1:return_flag = print_int(Line, int_array,uuid,cuuid);break;
			case 2:return_flag = print_double(Line, double_array,int_array,uuid,cuuid);break;
			case 3:return_flag = print_float(Line, float_array,uuid);break;
			case 4:return_flag = print_char(Line, char_array,uuid);break;
			case 5:return_flag = print_long(Line, long_array,int_array,uuid,cuuid);break;
			case 6:return_flag = print_byte(Line, byte_array,int_array,uuid);break;

			case 7:return_flag = print_array_i(Line, int_array,int_tail,uuid,cuuid);break;
			case 8:return_flag = print_array_d(Line, double_array,double_tail,uuid,cuuid);break;
			case 10:return_flag = print_array_c(Line, char_array,char_tail,uuid,cuuid);break;
			case 13:return_flag = print_array_i(Line, int_array,int_tail,uuid,cuuid);break;
			case 14:return_flag = print_array_d(Line, double_array,double_tail,uuid,cuuid);break;
				default:return_flag = -5;
		}
	
	}
	while(count<numbers){
		
		if(count>0){
			Line = Line+1;
		}

		switch (*(table+Line*8)) {
			case 1:return_flag = print_int(Line, int_array,uuid,cuuid);break;
			case 2:return_flag = print_double(Line, double_array,int_array,uuid,cuuid);break;
			case 3:return_flag = print_float(Line, float_array,uuid);break;
			case 4:return_flag = print_char(Line, char_array,uuid);break;
			case 5:return_flag = print_long(Line, long_array,int_array,uuid,cuuid);break;
			case 6:return_flag = print_byte(Line, byte_array,int_array,uuid);break;

			case 7:return_flag = print_array_i(Line, int_array,int_tail,uuid,cuuid);break;
			case 8:return_flag = print_array_d(Line, double_array,double_tail,uuid,cuuid);break;
			case 10:return_flag = print_array_c(Line, char_array,char_tail,uuid,cuuid);break;
			case 13:return_flag = print_array_i(Line, int_array,int_tail,uuid,cuuid);break;
			case 14:return_flag = print_array_d(Line, double_array,double_tail,uuid,cuuid);break;
				default:return_flag = -5;
		}
		

	count++;
 	
	}
	int *re = (int*)rei;
        *re = return_flag;
}


double encall_switch_get_d(long Line, int* int_array, int lenint,double* double_array, int lendouble,float* float_array, int lenfloat,char* char_array, int lenchar,long* long_array, int lenlong,char* byte_array, int lenbyte,char* uuid) {
	int type=*(table+Line*8);
	double return_flag = -1;
	switch (type) {
		case 2:return_flag = print_double(Line, double_array,int_array,uuid,NULL);break;
		default:return_flag = -5;
		}
	return return_flag;
}

long encall_switch_get_l(long Line, int* int_array, int lenint,double* double_array, int lendouble,float* float_array, int lenfloat,char* char_array, int lenchar,long* long_array, int lenlong,char* byte_array, int lenbyte,char* uuid) {
	int type=*(table+Line*8);
	long return_flag = -1;
	switch (type) {
		case 5:return_flag = print_long(Line, long_array,int_array,uuid,NULL);break;
		default:return_flag = -5;
		}
	return return_flag;
}


int print_int(long Line,int* int_array,char* uuid,char* cuuid)//---------------------------int
{
	
		Table_meta meta=get_table_meta(Line);
		int p1 = meta.p1;
		int p1_i = meta.p1_i;
		int p2 = meta.p2;
		int p2_i = meta.p2_i;
		int op = meta.op;
		int para_name = meta.para_name;
		int para_i = meta.para_i;	
		int return_flag = -999;
		int para1,para2;
				if(p1==-2 && p1_i==-2 && p2==-2 && p2_i==-2 && op==-2){
			if(para_i != -1){
				printf("I don't think this if will active 0509\n");
			}else if(para_name>=100 && para_i == -1){ //only variables
				hashmap.find(hashmap.find(uuid)->calluuid)->v_int[hashmap.find(uuid)->re[2]-100] = hashmap.find(uuid)->v_int[para_name-100];
			}else if(para_name<0 && para_i == -1){ //constant
				hashmap.find(hashmap.find(uuid)->calluuid)->v_int[hashmap.find(uuid)->re[2]-100] = hash_int[0-para_name];
			}
			return 1000;
			
		}

		/**
			para1 
		*/
		if(p1_i != -1){   //array
			if (p1 < 0 && hash_index[0-p1] != 0){  //consants
				if(p1_i>=70 && p1_i<=120){
					para1 = hashmapArray.find(uuid)->arr_int[p1_i%10][hash_int[0-p1]];
				}else if(p1_i < 10){
					para1 = hashmapArray.find(hashmap.find(uuid)->array[p1_i])->arr_int[hashmap.find(uuid)->arrayIndex[p1_i]%10][hash_int[0-p1]];
				}else if(p1_i>=700 && p1_i<=1200){ //array field
					if(hashmapPublicV.find(cuuid)==NULL){printf("[ERROR get cuuid a!]\n");}
					para1 = hashmapPublicV.find(cuuid)[p1_i%10].arr_int[hash_int[0-p1]];
				}else if(p1_i>=1300 && p1_i<=1800){ // multi array field, we need p2 as one of index
					if(hashmapPublicV.find(cuuid)==NULL){printf("[ERROR get cuuid b!]\n");}
					para1 = hashmapPublicV.find(cuuid)[p1_i%10].arr_multi_int[hashmap.find(uuid)->v_int[p2-100]][hash_int[0-p1]];
				}else{
					//do nothing
				}
				
			}else if(p1<10 && p1>=0){ //list
				if(p1_i>=70 && p1_i<=120){
					para1 = hashmapArray.find(uuid)->arr_int[p1_i%10][int_array[p1]];
				}else if(p1_i < 10){
					para1 = hashmapArray.find(hashmap.find(uuid)->array[p1_i])->arr_int[hashmap.find(uuid)->arrayIndex[p1_i]%10][int_array[p1]];
				}else if(p1_i>=700 && p1_i<=1200){ //array field
					if(hashmapPublicV.find(cuuid)==NULL){printf("[ERROR get cuuid c!]\n");}
					para1 = hashmapPublicV.find(cuuid)[p1_i%10].arr_int[int_array[p1]];
				}else if(p1_i>=1300 && p1_i<=1800){ // multi array field, we need p2 as one of index
					if(hashmapPublicV.find(cuuid)==NULL){printf("[ERROR get cuuid d!]\n");}
					para1 = hashmapPublicV.find(cuuid)[p1_i%10].arr_multi_int[hashmap.find(uuid)->v_int[p2-100]][int_array[p1]];
				}else{
					//do nothing
				}

			}else if(p1 >=100 && p1<=699){ //encalve int~byte
				if(p1_i>=70 && p1_i<=120){
					para1 = hashmapArray.find(uuid)->arr_int[p1_i%10][hashmap.find(uuid)->v_int[p1-100]];
				}else if(p1_i < 10){
					para1 =hashmapArray.find(hashmap.find(uuid)->array[p1_i])->arr_int[hashmap.find(uuid)->arrayIndex[p1_i]%10][hashmap.find(uuid)->v_int[p1-100]];
				}else if(p1_i>=700 && p1_i<=1200){ //array field
					if(hashmapPublicV.find(cuuid)==NULL){printf("[ERROR get cuuid e!]\n");}
					para1 = hashmapPublicV.find(cuuid)[p1_i%10].arr_int[hashmap.find(uuid)->v_int[p1-100]];
				}else if(p1_i>=1300 && p1_i<=1800){ // multi array field, we need p2 as one of index
					if(hashmapPublicV.find(cuuid)==NULL){
						printf("[ERROR get cuuid f! cuuid:%s Line=%ld]\n",cuuid,Line);
					}
					para1 = hashmapPublicV.find(cuuid)[p1_i%10].arr_multi_int[hashmap.find(uuid)->v_int[p2-100]][hashmap.find(uuid)->v_int[p1-100]];
				}else{
					//do nothing
				}
			}else if(p1 == 10000){ //lengof method
				if(p1_i>10){
					if(hashmapArray.find(uuid)->intsize[p1_i%10]<0){
						printf("[ERROR] I have not do this\n");
					}
					switch(p1_i/10){
						case 7:para1 = hashmapArray.find(uuid)->intsize[p1_i%10];break;
						case 8:para1 = hashmapArray.find(uuid)->doublesize[p1_i%10];break;
						case 10:para1 = hashmapArray.find(uuid)->charsize[p1_i%10];break;
						case 11:para1 = hashmapArray.find(uuid)->longsize[p1_i%10];break;
					}
				}else{
					int index = hashmap.find(uuid)->arrayIndex[p1_i];
					switch(index/10){
						case 7:para1 = hashmapArray.find(hashmap.find(uuid)->array[p1_i])->intsize[index%10];break;
						case 8:para1 = hashmapArray.find(hashmap.find(uuid)->array[p1_i])->doublesize[index%10];break;
						case 10:para1 = hashmapArray.find(hashmap.find(uuid)->array[p1_i])->charsize[index%10];break;
						case 11:para1 = hashmapArray.find(hashmap.find(uuid)->array[p1_i])->longsize[index%10];break;
					}
				}
			
			}else{
				//this is a array, but index is null
			}
		}else{	// no an array ,but possiable it is public variables

			if (p1 < 0 && hash_index[0-p1] != 0){  //consants

				para1 = hash_int[0-p1];

			}else if(p1<10 && p1>=0){ //list

				para1 = int_array[p1];
			}else if(p1>=20 && p1<=70){ // Public Variables , add on 0610/2020
				if(hashmapPublicV.find(cuuid) != NULL){
					para1 = hashmapPublicV.find(cuuid)[p1%10].v_i;
				}else{//init
					printf("[ERROR] print_int I don't think this will occur 6.10/2020\n");
				}
			}else{ //encalve
				switch(p1/100){    //maybe type cast
					case 1:para1 = hashmap.find(uuid)->v_int[p1-100];break;
					case 2:para1 = (int)hashmap.find(uuid)->v_double[p1-200];break;
					case 4:para1 = (int)hashmap.find(uuid)->v_char[p1-400];break;
					case 5:para1 = (int)hashmap.find(uuid)->v_long[p1-500];break;
					case 6:para1 = (int)hashmap.find(uuid)->v_byte[p1-600];break;
				}
				
				int ttpara1 = hashmap.find(uuid)->v_int[p1-100];   
			}
		}
		/**
			para2 
		*/
		if(p2_i != -1){   //array
			if (p2 < 0 && hash_index[0-p2] != 0){  //consants
				if(p2_i>10){
					para2 = hashmapArray.find(uuid)->arr_int[p2_i%10][hash_int[0-p2]];
				}else{
					para2 = hashmapArray.find(hashmap.find(uuid)->array[p2_i])->arr_int[hashmap.find(uuid)->arrayIndex[p2_i]%10][hash_int[0-p2]];
				}
			}else if(p2<10 && p2>=0){ //list

				if(p2_i>10){
					para2 = hashmapArray.find(uuid)->arr_int[p2_i%10][int_array[p2]];
				}else{
					para2 = hashmapArray.find(hashmap.find(uuid)->array[p2_i])->arr_int[hashmap.find(uuid)->arrayIndex[p2_i]%10][int_array[p2]];
				}

			}else if(p2 >=100 && p2<=500){ //encalve int~long
				if(p2_i>10){
					para2 = hashmapArray.find(uuid)->arr_int[p2_i%10][hashmap.find(uuid)->v_int[p2-100]];
				}else{
					para2 =hashmapArray.find(hashmap.find(uuid)->array[p2_i])->arr_int[hashmap.find(uuid)->arrayIndex[p2_i]%10][hashmap.find(uuid)->v_int[p2-100]];
				}
			}
		}else{	// no an array

			if (p2 < 0 && hash_index[0-p2] != 0){  //consants

				para2 = hash_int[0-p2];

			}else if(p2<10 && p2>=0){ //list

				para2 = int_array[p2];
			}else{ //encalve
	
				switch(p2/100){    //maybe type cast
					case 1:para2 = hashmap.find(uuid)->v_int[p2-100];break;
					case 2:para2 = (int)hashmap.find(uuid)->v_double[p2-200];break;
					case 4:para2 = (int)hashmap.find(uuid)->v_char[p2-400];break;
					case 5:para2 = (int)hashmap.find(uuid)->v_long[p2-500];break;
					case 6:para2 = (int)hashmap.find(uuid)->v_byte[p2-600];break;
				}
			}
		}
		switch (op) {
			case -1:return_flag = para1;break;
			case 1:return_flag = para1 + para2;break; //+
			case 2:return_flag = para1 - para2;break; //-
			case 3:return_flag = para1 * para2;break; //*
			case 4:return_flag = para1 / para2;break; // /
			case 5:return_flag = para1 % para2;break; // %
			case 6:return_flag =( para1== para2?1:0);break;
	 		case 7:return_flag =( para1!= para2?1:0);break;
	  		case 8:return_flag =( para1> para2?1:0);break;
	  		case 9:return_flag =( para1< para2?1:0);break;
	  		case 10:return_flag =(para1>=para2?1:0);break;
	  		case 11:return_flag =(para1<=para2?1:0);break;
			case 12:return_flag = para1 & para2;break;
			case 13:return_flag = para1 | para2;break;
			case 14:return_flag = para1 ^ para2;break;
			case 15:return_flag = para1 << para2;break;
			case 16:return_flag = para1 >> para2;break;
			case 17:return_flag = (unsigned int)para1 >> para2;break;
			default:return_flag = -11;
		}



		if(para_i != -1){ // update to array
			if (para_name < 0 && hash_index[0-para_name] != 0){ //constant
				if(para_i>=70 && para_i<=120){
					hashmapArray.find(uuid)->arr_int[para_i%10][hash_int[0-para_name]] = return_flag;
					return_flag = 1000;
				}else if(para_i<10){
					hashmapArray.find(hashmap.find(uuid)->array[para_i])->arr_int[hashmap.find(uuid)->arrayIndex[para_i]%10][hash_int[0-para_name]] = return_flag;
					return_flag = 1000;
				}else if(para_i>=700 && para_i<=1200){ //array field
					if(hashmapPublicV.find(cuuid)==NULL){printf("[ERROR get cuuid g!]\n");}
					hashmapPublicV.find(cuuid)[para_i%10].arr_int[hash_int[0-para_name]] = return_flag;
					return_flag = 1000;	
				}else if(para_i>=1300 && para_i<=1800){ // multi array field, we need p2 as one of index
					if(hashmapPublicV.find(cuuid)==NULL){printf("[ERROR get cuuid h!]\n");}
					hashmapPublicV.find(cuuid)[para_i%10].arr_multi_int[hashmap.find(uuid)->v_int[p2_i-100]][hash_int[0-para_name]] = return_flag;
					return_flag = 1000;	
				}else{
					//do nothing
				}
			
			}else if(para_name >=100 ){ //encalve index
				if(para_i>=70 && para_i<=120){
					hashmapArray.find(uuid)->arr_int[para_i%10][hashmap.find(uuid)->v_int[para_name-100]] = return_flag;
					return_flag = 1000;
				}else if(para_i<10){
					hashmapArray.find(hashmap.find(uuid)->array[para_i])->arr_int[hashmap.find(uuid)->arrayIndex[para_i]%10][hashmap.find(uuid)->v_int[para_name-100]] = return_flag;
					
					return_flag = 1000;
				}else if(para_i>=700 && para_i<=1200){ //array field
					if(hashmapPublicV.find(cuuid)==NULL){printf("[ERROR get cuuid i!]\n");}
					hashmapPublicV.find(cuuid)[para_i%10].arr_int[hashmap.find(uuid)->v_int[para_name-100]] = return_flag;
					return_flag = 1000;	
				}else if(para_i>=1300 && para_i<=1800){ // multi array field, we need p2_i as one of index
					if(hashmapPublicV.find(cuuid)==NULL){printf("[ERROR get cuuid j! cuuid:%s Line=%ld]\n",cuuid,Line);}
					hashmapPublicV.find(cuuid)[para_i%10].arr_multi_int[hashmap.find(uuid)->v_int[p2_i-100]][hashmap.find(uuid)->v_int[para_name-100]] = return_flag;
					/*if(Line==49){
						printf("d[i][j]:%d\n",hashmapPublicV.find(cuuid)[para_i%10].arr_multi_int[hashmap.find(uuid)->v_int[p2_i-100]][hashmap.find(uuid)->v_int[para_name-100]]);
					}*/
					return_flag = 1000;	
				}else{
					//do nothing
				}
			}
		}else if(para_name>=20 && para_name<=70){ // Public Variables , add on 0610/2020
				if(hashmapPublicV.find(cuuid) != NULL){
					switch(para_name/10){    //maybe type cast
						case 2:hashmapPublicV.find(cuuid)[para_name%10].v_i = return_flag;break;
						case 3:hashmapPublicV.find(cuuid)[para_name%10].v_d = return_flag;break;
						case 5:hashmapPublicV.find(cuuid)[para_name%10].v_c = return_flag;break;
						case 6:hashmapPublicV.find(cuuid)[para_name%10].v_l = return_flag;break;
						case 7:hashmapPublicV.find(cuuid)[para_name%10].v_b = return_flag;break;
					}
				}else{//init
					PNODE p = (PNODE)malloc(10*sizeof(PublicVariableNode));
					if(!hashmapPublicV.insert(cuuid,p)){
						printf("insert fail!! %s\n",cuuid);
					}
					switch(para_name/10){    //maybe type cast
						case 2:hashmapPublicV.find(cuuid)[para_name%10].v_i = return_flag;break;
						case 3:hashmapPublicV.find(cuuid)[para_name%10].v_d = return_flag;break;
						case 5:hashmapPublicV.find(cuuid)[para_name%10].v_c = return_flag;break;
						case 6:hashmapPublicV.find(cuuid)[para_name%10].v_l = return_flag;break;
						case 7:hashmapPublicV.find(cuuid)[para_name%10].v_b = return_flag;break;
					}
				}
				return_flag = 1000;	
		}else if(para_name>0 && para_i == -1){  // update to variables
			switch(para_name/100){    //maybe type cast
				case 1:hashmap.find(uuid)->v_int[para_name-100] = return_flag;break;
				case 2:hashmap.find(uuid)->v_double[para_name-200] = (int)return_flag;break;
				case 4:hashmap.find(uuid)->v_char[para_name-400] = (int)return_flag;break;
				case 5:hashmap.find(uuid)->v_long[para_name-500] = (long)return_flag;break;
				case 6:hashmap.find(uuid)->v_byte[para_name-600] = return_flag;break;
			}
			return_flag = 1000;	
		}
		return return_flag;

}
int calIntArrayIndex(INODE node){
	int d=0;
	while(node->dimensions[d]!=-1){
		d++;
	}
	int re=0;
	for(int i=0;i<d-1;i++){
		re+=(node->index[i]*node->dimensions[i+1]);
	}
	return re;
}
int calDoubleArrayIndex(DNODE node){
	int d=0;
	while(node->dimensions[d]!=-1){
		d++;
	}
	int re=0;
	for(int i=0;i<d-1;i++){
		re+=(node->index[i]*node->dimensions[i+1]);
	}
	return re;
}
int calCharArrayIndex(CNODE node){
	int d=0;
	while(node->dimensions[d]!=-1){
		d++;
	}
	int re=0;
	for(int i=0;i<d-1;i++){
		re+=(node->index[i]*node->dimensions[i+1]);
	}
	return re;
}


int print_array_d(long Line, double* double_array,int double_tail,char* uuid,char* cuuid){
	Table_meta meta=get_table_meta(Line);
	int type=meta.type;
	int p1 = meta.p1;
	int p1_i = meta.p1_i;
	int p2 = meta.p2;
	int p2_i = meta.p2_i;
	int op = meta.op;
	int para_name = meta.para_name;
	int para_i = meta.para_i;	
		char *tmpuuid=(char*)malloc(33*sizeof(char));
		memcpy(tmpuuid,uuid,32);
		if(p1==-2&&p1_i==-2&&p2==-2&&p2_i==-2&&op==-2){
				printf("return array\n");
				DNODE node1=hashmapArray2.find(hashmap.find(uuid)->calluuid)->double_arrNodes[hashmap.find(uuid)->re[2]%10];
				DNODE node2=hashmapArray2.find(uuid)->double_arrNodes[para_i%10];
				node2=hashmapArray2.find(uuid)->double_arrNodes[node2->oriLocation%10];
				if(node1->location==0){
					node1->location=hashmap.find(uuid)->re[2]%10;
					node1->oriLocation=hashmap.find(uuid)->re[2]%10;
					node1->data=(double*)malloc(sizeof(double)*node2->sz);
					node1->d=node2->d;
					for(int i=0;i<3;i++){
						node1->dimensions[i]=node2->dimensions[i];
					}
					for(int i=0;i<node2->sz;i++){
					node1->data[i]=node2->data[i];
				}
					return 1000;
				}
				node1=hashmapArray2.find(hashmap.find(uuid)->calluuid)->double_arrNodes[node1->oriLocation%10];			
				for(int i=0;i<node2->sz;i++){
					node1->data[i]=node2->data[i];
				}

		}else if(p2==0){
			bool flag=true;
			int dim[3]={0};//dim[0] dim[1] dim[2] represent array's 1st 2nd 3th dimension 
			if(p1<0&&hash_index[0-p1]!=0){
				dim[0]=hash_double[0-p1];

			}else if(p1>=100&&p1<700){
				dim[0]=hashmap.find(uuid)->v_double[p1-100];
				
			}else{
				flag=false;
			}
			if(flag&&p1_i<0&&hash_index[0-p1_i]!=0){
				dim[1]=hash_double[0-p1_i];
			}else if(flag&&p1_i>=100&&p1_i<700){
				dim[1]=hashmap.find(uuid)->v_double[p1_i-100];
			}else{
				flag=false;
			}
			if(flag&&p2_i<0&&hash_index[0-p2_i]!=0){
				dim[2]=hash_double[0-p2_i];
			}else if(flag&&p2_i>=100&&p2_i<700){
				dim[2]=hashmap.find(uuid)->v_double[p2_i-100];
			}else{
				flag=false;
			}
			int d=0;// how many dimensions
			int sz=1;//array size
			for(int i=0;i<3;i++){
				if(dim[i]!=0){
					d++;
					sz*=dim[i];
				}
			}
			DNODE node=hashmapArray2.find(uuid)->double_arrNodes[para_i%10];
			node->d=d;//update d
			for(int i=0;i<d;i++){
				node->dimensions[i]=dim[i];//update dimensions
			}
			node->paramLoc=-1;
			node->data=(double*)malloc(sz*sizeof(double));//malllo space for data 
			node->sz=sz;

			return 1000;

		}else if(p2==1){
			int oriLoc=p1_i;
			DNODE node=hashmapArray2.find(uuid)->double_arrNodes[para_i%10];
			node->oriLocation=oriLoc;
		
		}else if(p2==2){
	
			int loc=-1;
			int k=-1;
			if(para_i==-1){//int a=arr[0] left is variable
				DNODE node1=hashmapArray2.find(uuid)->double_arrNodes[p1_i%10];
				int idx=calDoubleArrayIndex(node1);
				if(node1->paramLoc!=-1){//is formular array param
					for(k=0;k<10;k++){
						if(k==node1->paramLoc){
							loc=hashmap.find(uuid)->arrayIndex[k];
							memcpy(tmpuuid,hashmap.find(uuid)->array[k],32);
							break;
						}
					}
				}
				

				if(loc!=-1){
					node1=hashmapArray2.find(hashmap.find(uuid)->array[k])->double_arrNodes[loc%10];
					
				}
							
				int index=-1;//array index
				if(p1<0&&hash_index[0-p1]!=0){
					index=hash_double[0-p1];
				}else if(p1>=100&&p1<700){
					index=hashmap.find(uuid)->v_double[p1-100];
				}
				idx+=index;//
				DNODE node2=NULL;
				if(k!=-1){
					node2=hashmapArray2.find(hashmap.find(uuid)->array[k])->double_arrNodes[node1->oriLocation%10];
				}else{
					node2=hashmapArray2.find(uuid)->double_arrNodes[node1->oriLocation%10];
				}
				
			
				hashmap.find(uuid)->v_double[para_name-100]=node2->data[idx];//int a=arr[1][2][3]
			}else{//arr[0]=3
				int loc=-1;
				int k=-1;
				
				DNODE node1=hashmapArray2.find(uuid)->double_arrNodes[para_i%10];
				int idx=calDoubleArrayIndex(node1);
				if(node1->paramLoc!=-1){//is formular array param
					for(k=0;k<10;k++){
						if(k==node1->paramLoc){
							loc=hashmap.find(uuid)->arrayIndex[k];
							memcpy(tmpuuid,hashmap.find(uuid)->array[k],32);
							break;
						}
					}
				}
				if(loc!=-1){
					node1=hashmapArray2.find(hashmap.find(uuid)->array[k])->double_arrNodes[loc%10];
					
				}
				
				
				int index=-1;//arr[index]=num
				int num=-1;
				if(para_name<0&&hash_index[0-para_name]!=0){
					index=hash_double[0-para_name];
				}else if(para_name>=100&&para_name<700){
					index=hashmap.find(uuid)->v_double[para_name-100];
				}
				if(p1<0&&hash_index[0-p1]!=0){
					num=hash_double[0-p1];
				}else if(p1>=100&&p1<700){
					num=hashmap.find(uuid)->v_double[p1-100];
				}
				idx+=index;
				DNODE node2=NULL;
				if(k!=-1){
					node2=hashmapArray2.find(hashmap.find(uuid)->array[k])->double_arrNodes[node1->oriLocation%10];
				}else{
					node2=hashmapArray2.find(uuid)->double_arrNodes[node1->oriLocation%10];
				}
				node2->data[idx]=num;
				

			}
		
		}else if(p2==3){//arr2=arr1 arr2=arr1[0]
			 
			 int loc=-1;
			 int k=-1;
			 
			DNODE node1=hashmapArray2.find(uuid)->double_arrNodes[p1_i%10];

			node2->d=node1->d;//update d
			for(int i=0;i<5;i++){
				node2->dimensions[i]=node1->dimensions[i];//update dimesions
				node2->index[i]=node1->index[i];//update index
			}
			node2->oriLocation=node1->oriLocation;//update oriLocation
		
		}else if(p2==4){// append index
			 int loc=-1;
			 int i=-1;
			 int k=-1;
			if(p1<0&&hash_index[0-p1]!=0){
				i=hash_double[0-p1];
			}else if(p1>=100&&p1<700){
				i==hashmap.find(uuid)->v_double[p1-100];
			}
			DNODE node=hashmapArray2.find(uuid)->double_arrNodes[para_i%10];

			SNODE snode=hashmap.find(uuid);
			node->paramLoc=p1_i;
			if(p1==0&&p1_i==0){
				for(k=0;k<10;k++){
					if(k==node->paramLoc){
						loc=snode->arrayIndex[k];
						memcpy(tmpuuid,hashmap.find(uuid)->array[k],32);
						break;
					}
				}
				
				if(hashmapArray2.find(snode->array[k])!=NULL){
					DNODE node2=hashmapArray2.find(snode->array[k])->double_arrNodes[loc%10];
					node->d=node2->d;//update d
					for(int i=0;i<5;i++){
					node->dimensions[i]=node2->dimensions[i];//update dimesions
					node->index[i]=node2->index[i];//update index
				}
				node->oriLocation=node2->oriLocation;//update oriLocation
				}
				
			}
			int j=0;
			
			while(node->index[j]!=-1){
				j++;
				
			}
			
			node->index[j]=i;
		}else if(p2==5){//get array length
				DNODE node1=hashmapArray2.find(uuid)->double_arrNodes[p1_i%10];
			    int d=0;
			    while(node1->index[d]!=-1){
			    	d++;
			    }
			    int loc=-1;
			    int k=-1;
			 	if(node1->paramLoc!=-1){//is formular array param
					for(k=0;k<10;k++){
						if(k==node1->paramLoc){
							loc=hashmap.find(uuid)->arrayIndex[k];
							memcpy(tmpuuid,hashmap.find(uuid)->array[k],32);
							break;
						}
					}
				}
				if(loc!=-1){
					node1=hashmapArray2.find(hashmap.find(uuid)->array[k])->double_arrNodes[loc%10];
				}
			    hashmap.find(uuid)->v_double[para_name-100]=node1->dimensions[d];

				
		}
		else if(p2==-1){
			
		}
		free(tmpuuid);
		return 1000;
		


	
}
double print_double(long Line, double* double_array,int* int_array,char* uuid,char* cuuid)//---------------------------double
{
		Table_meta meta=get_table_meta(Line);
		double return_flag = -999;
		double para1,para2;

		int p1 = meta.p1;
		int p1_i = meta.p1_i;
		int p2 = meta.p2;
		int p2_i = meta.p2_i;
		int op = meta.op;
		int para_name = meta.para_name;
		int para_i = meta.para_i;

		//return statement replacce! 0509
		if(p1==-2 &&  p1_i==-2 && p2==-2 && p2_i==-2 && op==-2){
			
			if(para_i != -1){
				printf("I don't think this if will active 0509\n");
			}else if(para_name>0 && para_i == -1){ //only variables
				hashmap.find(hashmap.find(uuid)->calluuid)->v_double[hashmap.find(uuid)->re[2]-100] = hashmap.find(uuid)->v_double[para_name-100];
			}else if(para_name<0 && para_i == -1){ //constant
				hashmap.find(hashmap.find(uuid)->calluuid)->v_double[hashmap.find(uuid)->re[2]-100] = hash_double[0-para_name];
			}
			return 1000;
		}

		/**
			para1 
		*/
		if(p1_i != -1){   //array
			if (p1 < 0 && hash_index[0-p1] != 0){  //consants

				if(p1_i>=70 && p1_i<=120){
					para1 = hashmapArray.find(uuid)->arr_double[p1_i%10][hash_int[0-p1]];
				}else if(p1_i < 10){
					para1 = hashmapArray.find(hashmap.find(uuid)->array[p1_i])->arr_double[hashmap.find(uuid)->arrayIndex[p1_i]%10][hash_int[0-p1]];
				}else if(p1_i>=700 && p1_i<=1200){ //array field
					if(hashmapPublicV.find(cuuid)==NULL){printf("[ERROR get cuuid k!]\n");}
					para1 = hashmapPublicV.find(cuuid)[p1_i%10].arr_double[hash_int[0-p1]];
				}else if(p1_i>=1300 && p1_i<=1800){ // multi array field, we need p2 as one of index
					if(hashmapPublicV.find(cuuid)==NULL){printf("[ERROR get cuuid l!]\n");}
					para1 = hashmapPublicV.find(cuuid)[p1_i%10].arr_multi_double[hashmap.find(uuid)->v_int[p2-100]][hash_int[0-p1]];
				}else{
					//do nothing
				}

			}else if(p1<10 && p1>=0){ //list
				if(p1_i>=70 && p1_i<=120){
					para1 = hashmapArray.find(uuid)->arr_double[p1_i%10][int_array[p1]];
				}else if(p1_i < 10){
					para1 = hashmapArray.find(hashmap.find(uuid)->array[p1_i])->arr_double[hashmap.find(uuid)->arrayIndex[p1_i]%10][int_array[p1]];
				}else if(p1_i>=700 && p1_i<=1200){ //array field
					if(hashmapPublicV.find(cuuid)==NULL){printf("[ERROR get cuuid m!]\n");}
					para1 = hashmapPublicV.find(cuuid)[p1_i%10].arr_double[int_array[p1]];
				}else if(p1_i>=1300 && p1_i<=1800){ // multi array field, we need p2 as one of index
					if(hashmapPublicV.find(cuuid)==NULL){printf("[ERROR get cuuid n!]\n");}
					para1 = hashmapPublicV.find(cuuid)[p1_i%10].arr_multi_double[hashmap.find(uuid)->v_int[p2-100]][int_array[p1]];
				}else{
					//do nothing
				}
			}else if(p1 >=100 && p1<=500){ //encalve int~long
				//printf("I think this if will ... 0508 d  p1=%d Line=%ld\n",p1,Line);
				if(p1_i>=70 && p1_i<=120){
					para1 = hashmapArray.find(uuid)->arr_double[p1_i%10][hashmap.find(uuid)->v_int[p1-100]];
				}else if(p1_i < 10){
					para1 =hashmapArray.find(hashmap.find(uuid)->array[p1_i])->arr_double[hashmap.find(uuid)->arrayIndex[p1_i]%10][hashmap.find(uuid)->v_int[p1-100]];
				}else if(p1_i>=700 && p1_i<=1200){ //array field
					if(hashmapPublicV.find(cuuid)==NULL){printf("[ERROR get cuuid o!]\n");}
					para1 = hashmapPublicV.find(cuuid)[p1_i%10].arr_double[hashmap.find(uuid)->v_int[p1-100]];
				}else if(p1_i>=1300 && p1_i<=1800){ // multi array field, we need p2 as one of index
					if(hashmapPublicV.find(cuuid)==NULL){printf("[ERROR get cuuid p!]\n");}
					para1 = hashmapPublicV.find(cuuid)[p1_i%10].arr_multi_double[hashmap.find(uuid)->v_int[p2-100]][hashmap.find(uuid)->v_int[p1-100]];
				}else{
					//do nothing
				}
			}else{
				//this is a array, but index is null
			}
		}else{	// no an array

			if (p1 < 0 && hash_index[0-p1] != 0){  //consants

				para1 = hash_double[0-p1];

			}else if(p1<10 && p1>=0){ //list

				para1 = double_array[p1];
			}else{ //encalve
				switch(p1/100){    //maybe type cast
					case 1:para1 = (double)hashmap.find(uuid)->v_int[p1-100];break;
					case 2:para1 = hashmap.find(uuid)->v_double[p1-200];break;
					case 4:para1 = (double)hashmap.find(uuid)->v_char[p1-400];break;
					case 5:para1 = (double)hashmap.find(uuid)->v_long[p1-500];break;
					case 6:para1 = (double)hashmap.find(uuid)->v_byte[p1-600];break;
				}
			}
		}

		/**
			para2 
		*/
		if(p2_i != -1){   //array
			if (p2 < 0 && hash_index[0-p2] != 0){  //consants

				if(p2_i>10){
					para2 = hashmapArray.find(uuid)->arr_double[p2_i%10][hash_int[0-p2]];
				}else{
					para2 = hashmapArray.find(hashmap.find(uuid)->array[p2_i])->arr_double[hashmap.find(uuid)->arrayIndex[p2_i]%10][hash_int[0-p2]];
				}

			}else if(p2<10 && p2>=0){ //list
				if(p2_i>10){
					para2 = hashmapArray.find(uuid)->arr_double[p2_i%10][int_array[p2]];
				}else{
					para2 = hashmapArray.find(hashmap.find(uuid)->array[p2_i])->arr_double[hashmap.find(uuid)->arrayIndex[p2_i]%10][int_array[p2]];  //may be no use
				}
				printf("I think this if will be no use!!!!!0508 d p2=%d\n",p2);
			}else if(p2 >=100 && p2<=500){ //encalve int~long
				if(p2_i>10){
					if(hashmapArray.find(uuid)->doublesize[p2_i%10]<0){
						int a = -hashmapArray.find(uuid)->doublesize[p2_i%10];
						para2 = hashmapMultiArray.find(uuid)[p2_i%10].arr_double[hashmap.find(uuid)->v_int[a-100]][hashmap.find(uuid)->v_int[p2-100]];
					}else{
						para2 = hashmapArray.find(uuid)->arr_double[p2_i%10][hashmap.find(uuid)->v_int[p2-100]];                                      ////may be ......
					}
				}else{
					para2 = hashmapArray.find(hashmap.find(uuid)->array[p2_i])->arr_double[hashmap.find(uuid)->arrayIndex[p2_i]%10][hashmap.find(uuid)->v_int[p2-100]];
				}
			}else{
				//this is a array, but index is null
			}
		}else{	// no an array

			if (p2 < 0 && hash_index[0-p2] != 0){  //consants

				para2 = hash_double[0-p2];

			}else if(p2<10 && p2>=0){ //list

				para2 = double_array[p2];
			}else{ //encalve
	
				switch(p2/100){    //maybe type cast
					case 1:para2 = (double)hashmap.find(uuid)->v_int[p2-100];break;
					case 2:para2 = hashmap.find(uuid)->v_double[p2-200];break;
					case 4:para2 = (double)hashmap.find(uuid)->v_char[p2-400];break;
					case 5:para2 = (double)hashmap.find(uuid)->v_long[p2-500];break;
					case 6:para2 = (double)hashmap.find(uuid)->v_byte[p2-600];break;
				}
			}
		}
	switch (op) {
			case -1:return_flag = para1;break;
			case 1:return_flag = para1 + para2;break; //+
			case 2:return_flag = para1 - para2;break; //-
			case 3:return_flag = para1 * para2;break; //*
			case 4:return_flag = para1 / para2;break; // /
			//case 5:return_flag = para1 % para2;break; // %
			case 6:return_flag =( para1== para2?1:0);break;
	 		case 7:return_flag =( para1!= para2?1:0);break;
	  		case 8:return_flag =( para1> para2?1:0);break;
	  		case 9:return_flag =( para1< para2?1:0);break;
	  		case 10:return_flag =(para1>=para2?1:0);break;
	  		case 11:return_flag =(para1<=para2?1:0);break;
			//case 12:return_flag = para1 & para2;break;
			default:return_flag = -11;
		}
		if(para_i != -1){ // update to array
			if (para_name < 0 && hash_index[0-para_name] != 0){ //constant
				if(para_i>=70 && para_i<=120){
					hashmapArray.find(uuid)->arr_double[para_i%10][hash_int[0-para_name]] = return_flag;
					return_flag = 1000;
				}else if(para_i<10){
					hashmapArray.find(hashmap.find(uuid)->array[para_i])->arr_double[hashmap.find(uuid)->arrayIndex[para_i]%10][hash_int[0-para_name]] = return_flag;
					return_flag = 1000;
				}else if(para_i>=700 && para_i<=1200){ //array field
					hashmapPublicV.find(cuuid)[para_i%10].arr_double[hash_int[0-para_name]] = return_flag;
					return_flag = 1000;
				}else if(para_i>=1300 && para_i<=1800){ // multi array field, we need p2_i as one of index
					hashmapPublicV.find(cuuid)[para_i%10].arr_multi_double[hashmap.find(uuid)->v_int[p2_i-100]][hash_int[0-para_name]] = return_flag;
					return_flag = 1000;
				}else{
					//do nothing
				}
			
			}else if(para_name >=100 ){ //encalve
				if(para_i>=70 && para_i<=120){
					hashmapArray.find(uuid)->arr_double[para_i%10][hashmap.find(uuid)->v_int[para_name-100]] = return_flag;
					return_flag = 1000;
				}else if(para_i<10){
					hashmapArray.find(hashmap.find(uuid)->array[para_i])->arr_double[hashmap.find(uuid)->arrayIndex[para_i]%10][hashmap.find(uuid)->v_int[para_name-100]] = return_flag;
					
					return_flag = 1000;
				}else if(para_i>=700 && para_i<=1200){ //array field
					if(hashmapPublicV.find(cuuid)==NULL){printf("[ERROR get cuuid q!]\n");}
					hashmapPublicV.find(cuuid)[para_i%10].arr_double[hashmap.find(uuid)->v_int[para_name-100]] = return_flag;
					return_flag = 1000;
				}else if(para_i>=1300 && para_i<=1800){ // multi array field, we need p2_i as one of index
					if(hashmapPublicV.find(cuuid)==NULL){printf("[ERROR get cuuid r!]\n");}
					hashmapPublicV.find(cuuid)[para_i%10].arr_multi_double[hashmap.find(uuid)->v_int[p2_i-100]][hashmap.find(uuid)->v_int[para_name-100]] = return_flag;
					return_flag = 1000;
				}else{
					//do nothing
				}
			}
	
		}else if(para_name>0 && para_i == -1){  // update to variables
			
			switch(para_name/100){    //maybe type cast
				case 1:hashmap.find(uuid)->v_int[para_name-100] = (double)return_flag;break;
				case 2:hashmap.find(uuid)->v_double[para_name-200] = return_flag;break;
				case 4:hashmap.find(uuid)->v_char[para_name-400] = (char)return_flag;break;
				case 5:hashmap.find(uuid)->v_long[para_name-500] = (long)return_flag;break;
				case 6:hashmap.find(uuid)->v_byte[para_name-600] = (return_flag<0)?-1:1;
				       //printf("[Warning]I think only in Pi will counter this case!! 0603/2020\n");
				       break;
			}
			return_flag = 1000;	
		}
		return return_flag;
}


float print_float(long Line, float* float_array,char* uuid)//---------------------------float
{
		Table_meta meta=get_table_meta(Line);
		float return_flag = -999;
		float para1,para2;
		if (meta.p1 < 0){  //consants
			para1 = hash_float[0-meta.p1];
		}else if(meta.p1<10 && meta.p1>=0){ //list
			para1 = float_array[meta.p1];
		}else{ //encalve
//			para1 = get_stacktop(s)->v_float[meta.p1 % 10];
		}
		
		if (meta.p2 < 0){  //consants
			para2 = hash_float[0-meta.p2];
		}else if(meta.p2<10 && meta.p2>=0){ //list
			para2 = float_array[meta.p2];
		}else{ //encalve
//			para2 = get_stacktop(s)->v_float[meta.p2 % 10];
		}
		switch (meta.op) {
			case -1:return_flag = para1;break;   //x=2; or x=y;
			case 1:return_flag = para1 + para2;break; //+
			case 2:return_flag = para1 - para2;break; //-
			case 3:return_flag = para1 * para2;break; //*
			case 4:return_flag = para1 / para2;break; // /
			//case 5:return_flag = para1 % para2;break; // %
			case 6:return_flag=( para1==para2?1:0);break;
	 		case 7:return_flag=( para1!=para2?1:0);break;
	  		case 8:return_flag=( para1>para2?1:0);break;
	  		case 9:return_flag=( para1<para2?1:0);break;
	  		case 10:return_flag=( para1>=para2?1:0);break;
	  		case 11:return_flag=( para1<=para2?1:0);break;
			default:return_flag = -11;
		}
		if (meta.para_name>0) { 
			return_flag = 1000;
		}
ocall_print_string("f success\n");
		return return_flag;
}

int print_char(long Line, char* char_array,char* uuid)//---------------------------char
{		
		Table_meta meta=get_table_meta(Line);
		int return_flag = -999;
		char para1,para2;

		int p1 = meta.p1;
		int p1_i = meta.p1_i;
		int p2 = meta.p2;
		int p2_i = meta.p2_i;
		int op = meta.op;
		int para_name = meta.para_name;
		int para_i = meta.para_i;



		if(p1==-2 &&  p1_i==-2 && p2==-2 && p2_i==-2 && op==-2){
			
			if(para_i != -1){
				printf("[print_char]I don't think this if will active 0509\n");
			}else if(para_name>0 && para_i == -1){ //only variables
				hashmap.find(hashmap.find(uuid)->calluuid)->v_char[hashmap.find(uuid)->re[2]-100] = hashmap.find(uuid)->v_char[para_name-100];
			}else if(para_name<0 && para_i == -1){ //constant
				hashmap.find(hashmap.find(uuid)->calluuid)->v_char[hashmap.find(uuid)->re[2]-100] = hash_int[0-para_name];
			}
			return 1000;
		}

		/**
			para1 
		*/
		if(p1_i != -1){   //array
			if (p1 < 0 && hash_index[0-p1] != 0){  //consants
				printf("[print_char]I don't think this if will active 0605\n");
			
			}else if(p1<10 && p1>=0){ //list
				printf("[print_char]I don't think this if will active 0605\n");
			
			}else if(p1 >=100 && p1<=500){ //encalve int~long
				printf("[print_char]I don't think this if will active 0605\n");
				
			}else{
				//this is a array, but index is null
			}
		}else{	// no an array

			if (p1 < 0 && hash_index[0-p1] != 0){  //consants
				
				para1 = hash_int[0-p1];

			}else if(p1<10 && p1>=0){ //list

				para1 = char_array[p1];
			}else{ //encalve
				switch(p1/100){    //maybe type cast
					case 1:para1 = (char)hashmap.find(uuid)->v_int[p1-100];break;
					case 2:para1 = (char)hashmap.find(uuid)->v_double[p1-200];break;
					case 4:para1 = hashmap.find(uuid)->v_char[p1-400];break;
					case 5:para1 = (char)hashmap.find(uuid)->v_long[p1-500];break;
					case 6:para1 = (char)hashmap.find(uuid)->v_byte[p1-600];break;
				}
			
			}
		}

		/**
			para2 
		*/
		if(p2_i != -1){   //array
			if (p2 < 0 && hash_index[0-p2] != 0){  //consants
				printf("[print_char]I don't think this if will active 0605\n");
			

			}else if(p2<10 &&p2>=0){ //list
				printf("[print_char]I don't think this if will active 0605\n");
			
			}else if(p2 >=100 ){ //encalve
				printf("[print_char]I don't think this if will active 0605\n");
			
			}else{
				//this is a array, but index is null
			}
		}else{	// no an array

			if (p2 < 0 && hash_index[0-p2] != 0){  //consants

				para2 = hash_int[0-p2];

			}else if(p2<10 && p2>=0){ //list

				para2 = char_array[p2];
			}else{ //encalve
	
				switch(p2/100){    //maybe type cast
					case 1:para2 = (char)hashmap.find(uuid)->v_int[p2-100];break;
					case 2:para2 = (char)hashmap.find(uuid)->v_double[p2-200];break;
					case 4:para2 = hashmap.find(uuid)->v_char[p2-400];break;
					case 5:para2 = (char)hashmap.find(uuid)->v_long[p2-500];break;
					case 6:para2 = (char)hashmap.find(uuid)->v_byte[p2-600];break;
				}
			}
		}

		switch (op) {
			case -1:return_flag = para1;break;
			case 1:return_flag = para1 + para2;break; //+
			case 2:return_flag = para1 - para2;break; //-
			case 3:return_flag = para1 * para2;break; //*
			case 4:return_flag = para1 / para2;break; // /
			case 5:return_flag = para1 % para2;break; // %
			case 6:return_flag =( para1== para2?1:0);break;
	 		case 7:return_flag =( para1!= para2?1:0);break;
	  		case 8:return_flag =( para1> para2?1:0);break;
	  		case 9:return_flag =( para1< para2?1:0);break;
	  		case 10:return_flag =(para1>=para2?1:0);break;
	  		case 11:return_flag =(para1<=para2?1:0);break;
			//case 12:return_flag = para1 & para2;break;
			default:return_flag = -11;
		}

		if(para_i != -1){ // update to array
			printf("[print_char]I don't think this if will active 0605\n");
	
		}else if(para_name>0 && para_i == -1){  // update to variables			
			switch(para_name/100){    //maybe type cast
				case 1:hashmap.find(uuid)->v_int[para_name-100] = (char)return_flag;break;
				case 2:hashmap.find(uuid)->v_double[para_name-200] = (char)return_flag;break;
				case 4:hashmap.find(uuid)->v_char[para_name-400] = return_flag;break;
				case 5:hashmap.find(uuid)->v_long[para_name-500] = (char)return_flag;break;
				case 6:hashmap.find(uuid)->v_byte[para_name-600] = (char)return_flag;break;
			}
			return_flag = 1000;	
		}
		return return_flag;
}

long print_long(long Line,long* long_array,int* int_array,char* uuid,char* cuuid)//---------------------------int
{
	
		Table_meta meta=get_table_meta(Line);
		int p1 = meta.p1;
		int p1_i = meta.p1_i;
		int p2 = meta.p2;
		int p2_i = meta.p2_i;
		int op = meta.op;
		int para_name = meta.para_name;
		int para_i = meta.para_i;	
		long return_flag = -999;
		long para1,para2;
		
		//return statement replacce! 0509
		if(p1==-2 && p1_i==-2 && p2==-2 && p2_i==-2 && op==-2){
			if(para_i != -1){
				printf("I don't think this if will active 0509\n");
			}else if(para_name>=100 && para_i == -1){ //only variables
				hashmap.find(hashmap.find(uuid)->calluuid)->v_long[hashmap.find(uuid)->re[2]-100] = hashmap.find(uuid)->v_long[para_name-100];
			}else if(para_name<0 && para_i == -1){ //constant
				hashmap.find(hashmap.find(uuid)->calluuid)->v_long[hashmap.find(uuid)->re[2]-100] = hash_long[0-para_name];
			}
			return 1000;
			
		}

		/**
			para1 
		*/
		if(p1_i != -1){   //array
			if (p1 < 0 && hash_index[0-p1] != 0){  //consants
				if(p1_i>=70 && p1_i<=120){
					para1 = hashmapArray.find(uuid)->arr_long[p1_i%10][hash_long[0-p1]];
				}else if(p1_i < 10){
					para1 = hashmapArray.find(hashmap.find(uuid)->array[p1_i])->arr_long[hashmap.find(uuid)->arrayIndex[p1_i]%10][hash_long[0-p1]];
				}else if(p1_i>=700 && p1_i<=1200){ //array field
					if(hashmapPublicV.find(cuuid)==NULL){printf("[ERROR get cuuid a!]\n");}
					para1 = hashmapPublicV.find(cuuid)[p1_i%10].arr_long[hash_long[0-p1]];
				}else if(p1_i>=1300 && p1_i<=1800){ // multi array field, we need p2 as one of index
					if(hashmapPublicV.find(cuuid)==NULL){printf("[ERROR get cuuid b!]\n");}
					para1 = hashmapPublicV.find(cuuid)[p1_i%10].arr_multi_long[hashmap.find(uuid)->v_long[p2-100]][hash_long[0-p1]];
				}else{
					//do nothing
				}
				printf("list 1562");
				
			}else if(p1<10 && p1>=0){ //list
				//printf("list 1564");
				if(p1_i>=70 && p1_i<=120){
					para1 = hashmapArray.find(uuid)->arr_long[p1_i%10][int_array[p1]];
				}else if(p1_i < 10){
					para1 = hashmapArray.find(hashmap.find(uuid)->array[p1_i])->arr_long[hashmap.find(uuid)->arrayIndex[p1_i]%10][int_array[p1]];
				}else if(p1_i>=700 && p1_i<=1200){ //array field
					if(hashmapPublicV.find(cuuid)==NULL){printf("[ERROR get cuuid c!]\n");}
					para1 = hashmapPublicV.find(cuuid)[p1_i%10].arr_long[int_array[p1]];
				}else if(p1_i>=1300 && p1_i<=1800){ // multi array field, we need p2 as one of index
					if(hashmapPublicV.find(cuuid)==NULL){printf("[ERROR get cuuid d!]\n");}
					para1 = hashmapPublicV.find(cuuid)[p1_i%10].arr_multi_long[hashmap.find(uuid)->v_long[p2-100]][int_array[p1]];
				}else{
					//do nothing
				}

			}else if(p1 >=100 && p1<=699){ //encalve int~byte
				if(p1_i>=70 && p1_i<=120){
					para1 = hashmapArray.find(uuid)->arr_long[p1_i%10][hashmap.find(uuid)->v_long[p1-100]];
				}else if(p1_i < 10){
					para1 =hashmapArray.find(hashmap.find(uuid)->array[p1_i])->arr_long[hashmap.find(uuid)->arrayIndex[p1_i]%10][hashmap.find(uuid)->v_long[p1-100]];
				}else if(p1_i>=700 && p1_i<=1200){ //array field
					if(hashmapPublicV.find(cuuid)==NULL){printf("[ERROR get cuuid e!]\n");}
					para1 = hashmapPublicV.find(cuuid)[p1_i%10].arr_long[hashmap.find(uuid)->v_long[p1-100]];
				}else if(p1_i>=1300 && p1_i<=1800){ // multi array field, we need p2 as one of index
					if(hashmapPublicV.find(cuuid)==NULL){
						printf("[ERROR get cuuid f! cuuid:%s Line=%ld]\n",cuuid,Line);
					}
					para1 = hashmapPublicV.find(cuuid)[p1_i%10].arr_multi_long[hashmap.find(uuid)->v_long[p2-100]][hashmap.find(uuid)->v_long[p1-100]];
				}else{
					//do nothing
				}
			}else if(p1 == 10000){ //lengof method
				if(p1_i>10){
					if(hashmapArray.find(uuid)->longsize[p1_i%10]<0){
						printf("[ERROR] I have not do this\n");
					}
					switch(p1_i/10){
						case 7:para1 = hashmapArray.find(uuid)->intsize[p1_i%10];break;
						case 8:para1 = hashmapArray.find(uuid)->doublesize[p1_i%10];break;
						case 10:para1 = hashmapArray.find(uuid)->charsize[p1_i%10];break;
						case 11:para1 = hashmapArray.find(uuid)->longsize[p1_i%10];break;
					}
				}else{
					int index = hashmap.find(uuid)->arrayIndex[p1_i];
					switch(index/10){
						case 7:para1 = hashmapArray.find(hashmap.find(uuid)->array[p1_i])->intsize[index%10];break;
						case 8:para1 = hashmapArray.find(hashmap.find(uuid)->array[p1_i])->doublesize[index%10];break;
						case 10:para1 = hashmapArray.find(hashmap.find(uuid)->array[p1_i])->charsize[index%10];break;
						case 11:para1 = hashmapArray.find(hashmap.find(uuid)->array[p1_i])->longsize[index%10];break;
					}
				}
			
			}else{
				//this is a array, but index is null
			}
		}else{	// no an array ,but possiable it is public variables

			if (p1 < 0 && hash_index[0-p1] != 0){  //consants

				para1 = hash_long[0-p1];

			}else if(p1<10 && p1>=0){ //list

				para1 = int_array[p1];
			}else if(p1>=20 && p1<=70){ // Public Variables , add on 0610/2020
				if(hashmapPublicV.find(cuuid) != NULL){
					para1 = hashmapPublicV.find(cuuid)[p1%10].v_i;
				}else{//init
					printf("[ERROR] print_int I don't think this will occur 6.10/2020\n");
				}
			}else{ //encalve
				switch(p1/100){    //maybe type cast
					case 1:para1 = hashmap.find(uuid)->v_int[p1-100];break;
					case 2:para1 = (long)hashmap.find(uuid)->v_double[p1-200];break;
					case 4:para1 = (long)hashmap.find(uuid)->v_char[p1-400];break;
					case 5:para1 = (long)hashmap.find(uuid)->v_long[p1-500];break;
					case 6:para1 = (long)hashmap.find(uuid)->v_byte[p1-600];break;
				}
				
				int ttpara1 = hashmap.find(uuid)->v_long[p1-100];   
			}
		}
		/**
			para2 
		*/
		if(p2_i != -1){   //array
			if (p2 < 0 && hash_index[0-p2] != 0){  //consants
				if(p2_i>10){
					para2 = hashmapArray.find(uuid)->arr_long[p2_i%10][hash_long[0-p2]];
				}else{
					para2 = hashmapArray.find(hashmap.find(uuid)->array[p2_i])->arr_long[hashmap.find(uuid)->arrayIndex[p2_i]%10][hash_long[0-p2]];
				}
			}else if(p2<10 && p2>=0){ //list

				if(p2_i>10){
					para2 = hashmapArray.find(uuid)->arr_long[p2_i%10][int_array[p2]];
				}else{
					para2 = hashmapArray.find(hashmap.find(uuid)->array[p2_i])->arr_long[hashmap.find(uuid)->arrayIndex[p2_i]%10][int_array[p2]];
				}

			}else if(p2 >=100 && p2<=500){ //encalve int~long
				if(p2_i>10){
					para2 = hashmapArray.find(uuid)->arr_long[p2_i%10][hashmap.find(uuid)->v_long[p2-100]];
				}else{
					para2 =hashmapArray.find(hashmap.find(uuid)->array[p2_i])->arr_long[hashmap.find(uuid)->arrayIndex[p2_i]%10][hashmap.find(uuid)->v_long[p2-100]];
				}
			}
		}else{	// no an array

			if (p2 < 0 && hash_index[0-p2] != 0){  //consants

				para2 = hash_long[0-p2];

			}else if(p2<10 && p2>=0){ //list

				para2 = int_array[p2];
			}else{ //encalve
	
				switch(p2/100){    //maybe type cast
					case 1:para2 = hashmap.find(uuid)->v_int[p2-100];break;
					case 2:para2 = (long)hashmap.find(uuid)->v_double[p2-200];break;
					case 4:para2 = (long)hashmap.find(uuid)->v_char[p2-400];break;
					case 5:para2 = (long)hashmap.find(uuid)->v_long[p2-500];break;
					case 6:para2 = (long)hashmap.find(uuid)->v_byte[p2-600];break;
				}
			}
		}
		switch (op) {
			case -1:return_flag = para1;break;
			case 1:return_flag = para1 + para2;break; //+
			case 2:return_flag = para1 - para2;break; //-
			case 3:return_flag = para1 * para2;break; //*
			case 4:return_flag = para1 / para2;break; // /
			case 5:return_flag = para1 % para2;break; // %
			case 6:return_flag =( para1== para2?1:0);break;
	 		case 7:return_flag =( para1!= para2?1:0);break;
	  		case 8:return_flag =( para1> para2?1:0);break;
	  		case 9:return_flag =( para1< para2?1:0);break;
	  		case 10:return_flag =(para1>=para2?1:0);break;
	  		case 11:return_flag =(para1<=para2?1:0);break;
			case 12:return_flag = para1 & para2;break;
			case 13:return_flag = para1 | para2;break;
			case 14:return_flag = para1 ^ para2;break;
			case 15:return_flag = para1 << para2;break;
			case 16:return_flag = para1 >> para2;break;
			case 17:return_flag = (unsigned long)para1 >> para2;break;
			default:return_flag = -11;
		}

		if(para_i != -1){ // update to array
			if (para_name < 0 && hash_index[0-para_name] != 0){ //constant
				if(para_i>=70 && para_i<=120){
					hashmapArray.find(uuid)->arr_long[para_i%10][hash_long[0-para_name]] = return_flag;
					return_flag = 1000;
				}else if(para_i<10){
					hashmapArray.find(hashmap.find(uuid)->array[para_i])->arr_long[hashmap.find(uuid)->arrayIndex[para_i]%10][hash_long[0-para_name]] = return_flag;
					return_flag = 1000;
				}else if(para_i>=700 && para_i<=1200){ //array field
					if(hashmapPublicV.find(cuuid)==NULL){printf("[ERROR get cuuid g!]\n");}
					hashmapPublicV.find(cuuid)[para_i%10].arr_long[hash_long[0-para_name]] = return_flag;
					return_flag = 1000;	
				}else if(para_i>=1300 && para_i<=1800){ // multi array field, we need p2 as one of index
					if(hashmapPublicV.find(cuuid)==NULL){printf("[ERROR get cuuid h!]\n");}
					hashmapPublicV.find(cuuid)[para_i%10].arr_multi_long[hashmap.find(uuid)->v_long[p2_i-100]][hash_long[0-para_name]] = return_flag;
					return_flag = 1000;	
				}else{
					//do nothing
				}
			
			}else if(para_name >=100 ){ //encalve index
				if(para_i>=70 && para_i<=120){
					hashmapArray.find(uuid)->arr_long[para_i%10][hashmap.find(uuid)->v_long[para_name-100]] = return_flag;
					return_flag = 1000;
				}else if(para_i<10){
					hashmapArray.find(hashmap.find(uuid)->array[para_i])->arr_long[hashmap.find(uuid)->arrayIndex[para_i]%10][hashmap.find(uuid)->v_long[para_name-100]] = return_flag;
					
					return_flag = 1000;
				}else if(para_i>=700 && para_i<=1200){ //array field
					if(hashmapPublicV.find(cuuid)==NULL){printf("[ERROR get cuuid i!]\n");}
					hashmapPublicV.find(cuuid)[para_i%10].arr_long[hashmap.find(uuid)->v_long[para_name-100]] = return_flag;
					return_flag = 1000;	
				}else if(para_i>=1300 && para_i<=1800){ // multi array field, we need p2_i as one of index
					if(hashmapPublicV.find(cuuid)==NULL){printf("[ERROR get cuuid j! cuuid:%s Line=%ld]\n",cuuid,Line);}
					hashmapPublicV.find(cuuid)[para_i%10].arr_multi_long[hashmap.find(uuid)->v_long[p2_i-100]][hashmap.find(uuid)->v_long[para_name-100]] = return_flag;
					return_flag = 1000;	
				}else{
					//do nothing
				}
			}
		}else if(para_name>=20 && para_name<=70){ // Public Variables , add on 0610/2020
				if(hashmapPublicV.find(cuuid) != NULL){
					switch(para_name/10){    //maybe type cast
						case 2:hashmapPublicV.find(cuuid)[para_name%10].v_i = return_flag;break;
						case 3:hashmapPublicV.find(cuuid)[para_name%10].v_d = return_flag;break;
						case 5:hashmapPublicV.find(cuuid)[para_name%10].v_c = return_flag;break;
						case 6:hashmapPublicV.find(cuuid)[para_name%10].v_l = return_flag;break;
						case 7:hashmapPublicV.find(cuuid)[para_name%10].v_b = return_flag;break;
					}
				}else{//init
					PNODE p = (PNODE)malloc(10*sizeof(PublicVariableNode));
					if(!hashmapPublicV.insert(cuuid,p)){
						printf("insert fail!! %s\n",cuuid);
					}
					switch(para_name/10){    //maybe type cast
						case 2:hashmapPublicV.find(cuuid)[para_name%10].v_i = return_flag;break;
						case 3:hashmapPublicV.find(cuuid)[para_name%10].v_d = return_flag;break;
						case 5:hashmapPublicV.find(cuuid)[para_name%10].v_c = return_flag;break;
						case 6:hashmapPublicV.find(cuuid)[para_name%10].v_l = return_flag;break;
						case 7:hashmapPublicV.find(cuuid)[para_name%10].v_b = return_flag;break;
					}
				}
				return_flag = 1000;	
		}else if(para_name>0 && para_i == -1){  // update to variables
			switch(para_name/100){    //maybe type cast
				case 1:hashmap.find(uuid)->v_long[para_name-100] = (int)return_flag;break;
				case 2:hashmap.find(uuid)->v_double[para_name-200] = (double)return_flag;break;
				case 4:hashmap.find(uuid)->v_char[para_name-400] = (char)return_flag;break;
				case 5:hashmap.find(uuid)->v_long[para_name-500] = return_flag;break;
				case 6:hashmap.find(uuid)->v_byte[para_name-600] = (int)return_flag;break;
			}
			return_flag = 1000;	
		}
		return return_flag;

}

int print_byte(long Line, char* byte_array,int* int_array,char* uuid)
{
	    printf("byte\n");
		Table_meta meta=get_table_meta(Line);
		int p1 = meta.p1;
		int p1_i = meta.p1_i;
		int p2 = meta.p2;
		int p2_i = meta.p2_i;
		int op = meta.op;
		int para_name = meta.para_name;
		int para_i = meta.para_i;

		
		int return_flag = -999;
		int para1,para2;
		if(p1==-2 && p1_i==-2 && p2==-2 && p2_i==-2 && op==-2){
			if(para_i != -1){
				printf("I don't think this if will active 0509 byte\n");
			}else if(para_name>=600 && para_i == -1){ //only variables
				hashmap.find(hashmap.find(uuid)->calluuid)->v_byte[hashmap.find(uuid)->re[2]-100] = hashmap.find(uuid)->v_int[para_name-100];
			}else if(para_name<0 && para_i == -1){ //constant
				hashmap.find(hashmap.find(uuid)->calluuid)->v_byte[hashmap.find(uuid)->re[2]-100] = hash_int[0-para_name];
			}
			return 1000;
		}

		/**
			para1 
		*/
		if(p1_i != -1){   //array
			if (p1 < 0 && hash_index[0-p1] != 0){  //consants

				para1 = (p1_i>10)?
				hashmapArray.find(uuid)->arr_byte[p1_i%10][hash_int[0-p1]]:
				hashmapArray.find(hashmap.find(uuid)->array[p1_i])->arr_byte[hashmap.find(uuid)->arrayIndex[p1_i]%10][hash_int[0-p1]];

			}else if(p1<10 && p1>=0){ //list

				para1 = (p1_i>10)?
				hashmapArray.find(uuid)->arr_byte[p1_i%10][int_array[p1]]:
				hashmapArray.find(hashmap.find(uuid)->array[p1_i])->arr_byte[hashmap.find(uuid)->arrayIndex[p1_i]%10][int_array[p1]];

			}else if(p1 >=600 && p1<=700){ //encalve byte
				
				para1 = (p1_i>10)?
				hashmapArray.find(uuid)->arr_byte[p1_i%10][hashmap.find(uuid)->v_int[p1-100]]:
				hashmapArray.find(hashmap.find(uuid)->array[p1_i])->arr_byte[hashmap.find(uuid)->arrayIndex[p1_i]%10][hashmap.find(uuid)->v_int[p1-100]];
				
			}
		}else{	// no an array

			if (p1 < 0 && hash_index[0-p1] != 0){  //consants

				para1 = hash_int[0-p1];

			}else if(p1<10 && p1>=0){ //list

				para1 = byte_array[p1];
			}else{ //encalve
				switch(p1/100){    //maybe type cast
					case 1:para1 = hashmap.find(uuid)->v_int[p1-100];break;
					case 2:para1 = (int)hashmap.find(uuid)->v_double[p1-200];break;
					case 4:para1 = (int)hashmap.find(uuid)->v_char[p1-400];break;
					case 5:para1 = (int)hashmap.find(uuid)->v_long[p1-500];break;
					case 6:para1 = hashmap.find(uuid)->v_byte[p1-600];break;
				}
			}
		}
		/**
			para2 
		*/
		if(p2_i != -1){   //array
			if (p2 < 0 && hash_index[0-p2] != 0){  //consants

				para2 = (p2_i>10)?
				hashmapArray.find(uuid)->arr_byte[p2_i%10][hash_int[0-p2]]:
				hashmapArray.find(hashmap.find(uuid)->array[p2_i])->arr_byte[hashmap.find(uuid)->arrayIndex[p2_i]%10][hash_int[0-p2]];

			}else if(p2<10 &&p2>=0){ //list

				para2 = (p2_i>10)?
				hashmapArray.find(uuid)->arr_byte[p2_i%10][byte_array[p2]]:
				hashmapArray.find(hashmap.find(uuid)->array[p2_i])->arr_byte[hashmap.find(uuid)->arrayIndex[p2_i]%10][int_array[p2]];

			}else if(p2 >=100 ){ //encalve
	
				para2 = (p2_i>10)?
				hashmapArray.find(uuid)->arr_byte[p2_i%10][hashmap.find(uuid)->v_int[p2-100]]:
				hashmapArray.find(hashmap.find(uuid)->array[p2_i])->arr_byte[hashmap.find(uuid)->arrayIndex[p2_i]%10][hashmap.find(uuid)->v_int[p2-100]];
			}
		}else{	// no an array

			if (p2 < 0 && hash_index[0-p2] != 0){  //consants

				para2 = hash_int[0-p2];

			}else if(p2<10 && p2>=0){ //list

				para2 = byte_array[p2];
			}else{ //encalve
	
				switch(p2/100){    //maybe type cast
					case 1:para2 = hashmap.find(uuid)->v_int[p2-100];break;
					case 2:para2 = (int)hashmap.find(uuid)->v_double[p2-200];break;
					case 4:para2 = (int)hashmap.find(uuid)->v_char[p2-400];break;
					case 5:para2 = (int)hashmap.find(uuid)->v_long[p2-500];break;
					case 6:para2 = (int)hashmap.find(uuid)->v_byte[p2-600];break;
				}
			}
		}

		switch (op) {
			case -1:return_flag = para1;break;
			case 1:return_flag = para1 + para2;break; //+
			case 2:return_flag = para1 - para2;break; //-
			case 3:return_flag = para1 * para2;break; //*
			case 4:return_flag = para1 / para2;break; // /
			case 5:return_flag = para1 % para2;break; // %
			case 6:return_flag =( para1== para2?1:0);break;
	 		case 7:return_flag =( para1!= para2?1:0);break;
	  		case 8:return_flag =( para1> para2?1:0);break;
	  		case 9:return_flag =( para1< para2?1:0);break;
	  		case 10:return_flag =(para1>=para2?1:0);break;
	  		case 11:return_flag =(para1<=para2?1:0);break;
			case 12:return_flag = para1 & para2;break;
			default:return_flag = -11;
		}
	
		if(para_i != -1){ // update to array
			if (para_name < 0 && hash_index[0-para_name] != 0){ //constant
				if(para_i>10){
					hashmapArray.find(uuid)->arr_byte[para_i%10][hash_int[0-para_name]] = return_flag;
					return_flag = 1000;
				}else{
					hashmapArray.find(hashmap.find(uuid)->array[para_i])->arr_byte[hashmap.find(uuid)->arrayIndex[para_i]%10][hash_int[0-para_name]] = return_flag;
					return_flag = 1000;
				}
			
			}else if(para_name >=100 ){ //encalve
				if(para_i>10){
					hashmapArray.find(uuid)->arr_byte[para_i%10][hashmap.find(uuid)->v_int[para_name-100]] = return_flag;
					return_flag = 1000;
				}else{
					hashmapArray.find(hashmap.find(uuid)->array[para_i])->arr_byte[hashmap.find(uuid)->arrayIndex[para_i]%10][hashmap.find(uuid)->v_int[para_name-100]] = return_flag;
					
					return_flag = 1000;
				}
			}
		}else if(para_name>0 && para_i == -1){  // update to variables
			switch(para_name/100){    //maybe type cast
				case 1:hashmap.find(uuid)->v_int[para_name-100] = return_flag;break;
				case 2:hashmap.find(uuid)->v_double[para_name-200] = (double)return_flag;break;
				case 4:hashmap.find(uuid)->v_char[para_name-400] = (char)return_flag;break;
				case 5:hashmap.find(uuid)->v_long[para_name-500] = (int)return_flag;break;
				case 6:hashmap.find(uuid)->v_byte[para_name-600] = return_flag;break;
			}
			
			return_flag = 1000;	
		}
		printf("return_flag=%d\n",return_flag);
		return return_flag;
}



int print_array_i(long Line, int* int_array,int int_tail,char* uuid,char* cuuid){


	Table_meta meta=get_table_meta(Line);
	int type=meta.type;
	int p1 = meta.p1;
	int p1_i = meta.p1_i;
	int p2 = meta.p2;
	int p2_i = meta.p2_i;
	int op = meta.op;
	int para_name = meta.para_name;
	int para_i = meta.para_i;	
		char *tmpuuid=(char*)malloc(33*sizeof(char));
		memcpy(tmpuuid,uuid,32);
		if(p1==-2&&p1_i==-2&&p2==-2&&p2_i==-2&&op==-2){
				INODE node1=hashmapArray2.find(hashmap.find(uuid)->calluuid)->int_arrNodes[hashmap.find(uuid)->re[2]%10];
				INODE node2=hashmapArray2.find(uuid)->int_arrNodes[para_i%10];
				node2=hashmapArray2.find(uuid)->int_arrNodes[node2->oriLocation%10];
				if(node1->location==0){
					node1->location=hashmap.find(uuid)->re[2]%10;
					node1->oriLocation=hashmap.find(uuid)->re[2]%10;
					node1->data=(int*)malloc(sizeof(int)*node2->sz);
					node1->d=node2->d;
					for(int i=0;i<3;i++){
						node1->dimensions[i]=node2->dimensions[i];
					}
					for(int i=0;i<node2->sz;i++){
					node1->data[i]=node2->data[i];
				}
					return 1000;
				}
				node1=hashmapArray2.find(hashmap.find(uuid)->calluuid)->int_arrNodes[node1->oriLocation%10];
			
				for(int i=0;i<node2->sz;i++){
					node1->data[i]=node2->data[i];
				}
				

		}else if(p2==0){

			bool flag=true;
			int dim[3]={0};//dim[0] dim[1] dim[2] represent array's 1st 2nd 3th dimension 
			if(p1<0&&hash_index[0-p1]!=0){
				dim[0]=hash_int[0-p1];

			}else if(p1>=100&&p1<700){
				dim[0]=hashmap.find(uuid)->v_int[p1-100];
				
			}else{
				flag=false;
			}
			if(flag&&p1_i<0&&hash_index[0-p1_i]!=0){
				dim[1]=hash_int[0-p1_i];
			}else if(flag&&p1_i>=100&&p1_i<700){
				dim[1]=hashmap.find(uuid)->v_int[p1_i-100];
			}else{
				flag=false;
			}
			if(flag&&p2_i<0&&hash_index[0-p2_i]!=0){
				dim[2]=hash_int[0-p2_i];
			}else if(flag&&p2_i>=100&&p2_i<700){
				dim[2]=hashmap.find(uuid)->v_int[p2_i-100];
			}else{
				flag=false;
			}
		
			int d=0;// how many dimensions
			int sz=1;//array size
			for(int i=0;i<3;i++){
				if(dim[i]!=0){
					d++;
					sz*=dim[i];
				}
			}
			INODE node=hashmapArray2.find(uuid)->int_arrNodes[para_i%10];
			
			node->d=d;//update d
			for(int i=0;i<d;i++){
				node->dimensions[i]=dim[i];//update dimensions
			}
			node->paramLoc=-1;
			node->data=(int*)malloc(sz*sizeof(int));//malllo space for data 
			node->sz=sz;

			return 1000;

		}else if(p2==1){
			int oriLoc=p1_i;
			INODE node=hashmapArray2.find(uuid)->int_arrNodes[para_i%10];
			node->oriLocation=oriLoc;
		
		}else if(p2==2){
			int loc=-1;
			int k=-1;
			if(para_i==-1){//int a=arr[0] left is variable
				INODE node1=hashmapArray2.find(uuid)->int_arrNodes[p1_i%10];
				int idx=calIntArrayIndex(node1);
				//printf("idx=%d\n", idx);
				if(node1->paramLoc!=-1){//is formular array param
					for(k=0;k<10;k++){
						if(k==node1->paramLoc){
							loc=hashmap.find(uuid)->arrayIndex[k];
							memcpy(tmpuuid,hashmap.find(uuid)->array[k],32);
							break;
						}
					}
				}
				

				if(loc!=-1){
					
					node1=hashmapArray2.find(hashmap.find(uuid)->array[k])->int_arrNodes[loc%10];
					
				}
				
				
				int index=-1;//array index
				if(p1<0&&hash_index[0-p1]!=0){
					index=hash_int[0-p1];
				}else if(p1>=100&&p1<700){
					index=hashmap.find(uuid)->v_int[p1-100];
				}
				idx+=index;//
				INODE node2=NULL;
				if(k!=-1){
					node2=hashmapArray2.find(hashmap.find(uuid)->array[k])->int_arrNodes[node1->oriLocation%10];
				}else{
					node2=hashmapArray2.find(uuid)->int_arrNodes[node1->oriLocation%10];
				}
				
				hashmap.find(uuid)->v_int[para_name-100]=node2->data[idx];//int a=arr[1][2][3]
			}else{//arr[0]=3
				int loc=-1;
				int k=-1;
				
				INODE node1=hashmapArray2.find(uuid)->int_arrNodes[para_i%10];
				int idx=calIntArrayIndex(node1);
				if(node1->paramLoc!=-1){//is formular array param
					for(k=0;k<10;k++){
						if(k==node1->paramLoc){
							loc=hashmap.find(uuid)->arrayIndex[k];
							memcpy(tmpuuid,hashmap.find(uuid)->array[k],32);
							break;
						}
					}
				}
				if(loc!=-1){
					node1=hashmapArray2.find(hashmap.find(uuid)->array[k])->int_arrNodes[loc%10];
					
				}
				
				
				int index=-1;//arr[index]=num
				int num=-1;
				if(para_name<0&&hash_index[0-para_name]!=0){
					index=hash_int[0-para_name];
				}else if(para_name>=100&&para_name<700){
					index=hashmap.find(uuid)->v_int[para_name-100];
				}
				if(p1<0&&hash_index[0-p1]!=0){
					num=hash_int[0-p1];
				}else if(p1>=100&&p1<700){
					num=hashmap.find(uuid)->v_int[p1-100];
				}
				idx+=index;
				INODE node2=NULL;
				if(k!=-1){
					node2=hashmapArray2.find(hashmap.find(uuid)->array[k])->int_arrNodes[node1->oriLocation%10];
				}else{
					node2=hashmapArray2.find(uuid)->int_arrNodes[node1->oriLocation%10];
				}
		
				node2->data[idx]=num;
			

			}
		
		}else if(p2==3){//arr2=arr1 arr2=arr1[0]
			 
			 int loc=-1;
			 int k=-1;
			 
			INODE node1=hashmapArray2.find(uuid)->int_arrNodes[p1_i%10];
			
			INODE node2=hashmapArray2.find(uuid)->int_arrNodes[para_i%10];
			node2->d=node1->d;//update d
			for(int i=0;i<5;i++){
				node2->dimensions[i]=node1->dimensions[i];//update dimesions
				node2->index[i]=node1->index[i];//update index
			}
			node2->oriLocation=node1->oriLocation;//update oriLocation
		
			
		}else if(p2==4){// append index

			 int loc=-1;
			 int i=-1;
			 int k=-1;
			if(p1<0&&hash_index[0-p1]!=0){
				i=hash_int[0-p1];
			}else if(p1>=100&&p1<700){
				i==hashmap.find(uuid)->v_int[p1-100];
			}
			INODE node=hashmapArray2.find(uuid)->int_arrNodes[para_i%10];

			SNODE snode=hashmap.find(uuid);
			node->paramLoc=p1_i;
			if(p1==0&&p1_i==0){
				for(k=0;k<10;k++){
					if(k==node->paramLoc){
						loc=snode->arrayIndex[k];
						memcpy(tmpuuid,hashmap.find(uuid)->array[k],32);
						break;
					}
				}
				
			
				if(hashmapArray2.find(snode->array[k])!=NULL){
					INODE node2=hashmapArray2.find(snode->array[k])->int_arrNodes[loc%10];
					node->d=node2->d;//update d
					for(int i=0;i<5;i++){
					node->dimensions[i]=node2->dimensions[i];//update dimesions
					node->index[i]=node2->index[i];//update index
				}
				node->oriLocation=node2->oriLocation;//update oriLocation
				}
				
			}
			int j=0;
		
			while(node->index[j]!=-1){
				j++;
				
			}
			
			node->index[j]=i;
			
		

			
		}else if(p2==5){//get array length
				INODE node1=hashmapArray2.find(uuid)->int_arrNodes[p1_i%10];
				if(node1==NULL){
					printf("null\n");
				}
			    int d=0;
			    while(node1->index[d]!=-1){
			    	printf("indxex[i]=%d\n", node1->index[d]);
			    	d++;
			    }
			    int loc=-1;
			    int k=-1;
			 	if(node1->paramLoc!=-1){//is formular array param
					for(k=0;k<10;k++){
						if(k==node1->paramLoc){
							loc=hashmap.find(uuid)->arrayIndex[k];
							memcpy(tmpuuid,hashmap.find(uuid)->array[k],32);
							break;
						}
					}
				}
				if(loc!=-1){
					node1=hashmapArray2.find(hashmap.find(uuid)->array[k])->int_arrNodes[loc%10];
				}
			    hashmap.find(uuid)->v_int[para_name-100]=node1->dimensions[d];

				
		}
		else if(p2==-1){
			
		}
		free(tmpuuid);
		return 1000;
		


	if(para_i >=700 && para_i<=1200){  //field array
		if(hashmapPublicV.find(cuuid) == NULL){
			PNODE p = (PNODE)malloc(10*sizeof(PublicVariableNode));
			if(!hashmapPublicV.insert(cuuid,p)){
				printf("insert fail!! %s\n",cuuid);
			}
		}	
		if(p1_i <=10){
			for(int i=0;i<int_tail;i++){
				hashmapPublicV.find(cuuid)[para_i%10].arr_int[i] = int_array[i];
			}
			hashmapPublicV.find(cuuid)[para_i%10].intsize = int_tail;
		}else{
			printf("[print_array_i] I don't deal with this case I. 0601/2020\n");
		}
	}else if(para_i>=1300 && para_i<=1800){ //field multi array
		if(hashmapPublicV.find(cuuid) == NULL){
			printf("[print_array_i] This is first create Cuuid multi. cuuid=%s\n",cuuid);
			PNODE p = (PNODE)malloc(10*sizeof(PublicVariableNode));
			if(!hashmapPublicV.insert(cuuid,p)){
				printf("insert fail!! %s\n",cuuid);
			}
		}
		if(para_name != -1){
			if(para_name >=100 && para_name<=200){ //this is index
				if(p1_i <= 10){

				}else if(p1_i >=70 && p1_i<=120){ //from ArrayNode
					int size = hashmapArray.find(uuid)->intsize[p1_i%10];
					for(int i=0;i<size;i++){
					hashmapPublicV.find(cuuid)[para_i%10].arr_multi_int[hashmap.find(uuid)->v_int[para_name-100]][i] = hashmapArray.find(uuid)->arr_int[p1_i%10][i];
					}
					hashmapPublicV.find(cuuid)[para_i%10].intmultisize[hashmap.find(uuid)->v_int[para_name-100]] = size;
				}
			}else {
				printf("[print_array_i] I don't deal with this case III. 0601/2020\n");
			}
		}else{
			//do nothing
		}
			
	}else if(para_i >= 70 && para_i<=120){//70 80
		if(p1_i > 10){
			for(int i=0;i<hashmapArray.find(uuid)->intsize[p1_i%10];i++){
				hashmapArray.find(uuid)->arr_int[para_i%10][i] = hashmapArray.find(uuid)->arr_int[p1_i%10][i];
			}		
		}else{
			if(p1 == -1){ //array
				if(hashmapArray.find(uuid)->arr_int[para_i%10] == NULL){
					encall_initArray(uuid,para_i,int_tail,0);
					for(int i=0;i<int_tail;i++){
						hashmapArray.find(uuid)->arr_int[para_i%10][i] = int_array[i];
					}
				
				}else{
					for(int i=0;i<int_tail;i++){
						hashmapArray.find(uuid)->arr_int[para_i%10][i] = int_array[i];
					}
				}
			}else if(p1>=100){ //multiarray 
					for(int i=0;i<int_tail;i++){
						hashmapMultiArray.find(uuid)[para_i%10].arr_int[hashmap.find(uuid)->v_int[p1-100]][i] = int_array[i];
					}
					hashmapMultiArray.find(uuid)[para_i%10].intsize[hashmap.find(uuid)->v_int[p1-100]] = int_tail;
					hashmapArray.find(uuid)->intsize[para_i%10] = -p1; //flag
			}else if(p1<0){
				printf("[ERROR] print_array_i  int_number type\n");
			}
		}
		
	}else{
		if(p1_i > 10){
			memcpy(hashmapArray.find(hashmap.find(uuid)->array[p1_i])->arr_int[hashmap.find(uuid)->arrayIndex[p1_i]%10],hashmapArray.find(uuid)->arr_int[p1_i%10],hashmapArray.find(uuid)->intsize[p1_i%10]);
		}else{
			memcpy(hashmapArray.find(hashmap.find(uuid)->array[p1_i])->arr_int[hashmap.find(uuid)->arrayIndex[p1_i]%10],int_array,int_tail);
		}
	}
	return 1000;
}




int print_array_c(long Line, char* char_array,int char_tail,char* uuid,char* cuuid){

	Table_meta meta=get_table_meta(Line);
	int type=meta.type;
	int p1 = meta.p1;
	int p1_i = meta.p1_i;
	int p2 = meta.p2;
	int p2_i = meta.p2_i;
	int op = meta.op;
	int para_name = meta.para_name;
	int para_i = meta.para_i;	
		char *tmpuuid=(char*)malloc(33*sizeof(char));
		memcpy(tmpuuid,uuid,32);
		if(p1==-2&&p1_i==-2&&p2==-2&&p2_i==-2&&op==-2){
				printf("return array\n");
				CNODE node1=hashmapArray2.find(hashmap.find(uuid)->calluuid)->char_arrNodes[hashmap.find(uuid)->re[2]%10];
				CNODE node2=hashmapArray2.find(uuid)->char_arrNodes[para_i%10];
				node2=hashmapArray2.find(uuid)->char_arrNodes[node2->oriLocation%10];
				if(node1->location==0){
					node1->location=hashmap.find(uuid)->re[2]%10;
					node1->oriLocation=hashmap.find(uuid)->re[2]%10;
					node1->data=(char*)malloc(sizeof(char)*node2->sz);
					node1->d=node2->d;
					for(int i=0;i<3;i++){
						node1->dimensions[i]=node2->dimensions[i];
					}
					for(int i=0;i<node2->sz;i++){
					node1->data[i]=node2->data[i];
				}
					return 1000;
				}
				node1=hashmapArray2.find(hashmap.find(uuid)->calluuid)->char_arrNodes[node1->oriLocation%10];			
				for(int i=0;i<node2->sz;i++){
					node1->data[i]=node2->data[i];
				}

		}else if(p2==0){

			bool flag=true;
			int dim[3]={0};//dim[0] dim[1] dim[2] represent array's 1st 2nd 3th dimension 
			if(p1<0&&hash_index[0-p1]!=0){
				dim[0]=hash_int[0-p1];
				printf("1 dimension constant: %d  int_%d\n", p1,hash_int[0-p1]);

			}else if(p1>=100&&p1<700){
				dim[0]=hashmap.find(uuid)->v_char[p1-100];
				printf("1 dimension variable:%d %d\n",p1, hashmap.find(uuid)->v_char[p1-100]);
				
			}else{
				flag=false;
			}
			if(flag&&p1_i<0&&hash_index[0-p1_i]!=0){
				dim[1]=hash_int[0-p1_i];
			}else if(flag&&p1_i>=100&&p1_i<700){
				dim[1]=hashmap.find(uuid)->v_char[p1_i-100];
			}else{
				flag=false;
			}
			if(flag&&p2_i<0&&hash_index[0-p2_i]!=0){
				dim[2]=hash_int[0-p2_i];
			}else if(flag&&p2_i>=100&&p2_i<700){
				dim[2]=hashmap.find(uuid)->v_char[p2_i-100];
			}else{
				flag=false;
			}
		
			int d=0;// how many dimensions
			int sz=1;//array size
			for(int i=0;i<3;i++){
				if(dim[i]!=0){
					d++;
					sz*=dim[i];
				}
			}
			CNODE node=hashmapArray2.find(uuid)->char_arrNodes[para_i%10];
			
			node->d=d;//update d
			for(int i=0;i<d;i++){
				node->dimensions[i]=dim[i];//update dimensions
			}
			printf("456\n");
			node->paramLoc=-1;
			node->data=(char*)malloc(sz*sizeof(char));//malllo space for data 
			node->sz=sz;

			return 1000;

		}else if(p2==1){
			int oriLoc=p1_i;
			CNODE node=hashmapArray2.find(uuid)->char_arrNodes[para_i%10];
			node->oriLocation=oriLoc;
		
		}else if(p2==2){

			
			int loc=-1;
			int k=-1;
			if(para_i==-1){//int a=arr[0] left is variable
				CNODE node1=hashmapArray2.find(uuid)->char_arrNodes[p1_i%10];
				int idx=calCharArrayIndex(node1);
				if(node1->paramLoc!=-1){//is formular array param
					for(k=0;k<10;k++){
						if(k==node1->paramLoc){
							loc=hashmap.find(uuid)->arrayIndex[k];
							memcpy(tmpuuid,hashmap.find(uuid)->array[k],32);
							break;
						}
					}
				}
				

				if(loc!=-1){
					
					node1=hashmapArray2.find(hashmap.find(uuid)->array[k])->char_arrNodes[loc%10];
					
				}				
				
				int index=-1;//array index
				if(p1<0&&hash_index[0-p1]!=0){
					index=hash_char[0-p1];
				}else if(p1>=100&&p1<700){
					index=hashmap.find(uuid)->v_char[p1-100];
				}
				idx+=index;//
				CNODE node2=NULL;
				if(k!=-1){
					node2=hashmapArray2.find(hashmap.find(uuid)->array[k])->char_arrNodes[node1->oriLocation%10];
				}else{
					node2=hashmapArray2.find(uuid)->char_arrNodes[node1->oriLocation%10];
				}
				hashmap.find(uuid)->v_char[para_name-100]=node2->data[idx];//int a=arr[1][2][3]
			}else{//arr[0]=3
				int loc=-1;
				int k=-1;
				
				CNODE node1=hashmapArray2.find(uuid)->char_arrNodes[para_i%10];
				
				int idx=calCharArrayIndex(node1);
				if(node1->paramLoc!=-1){//is formular array param
					for(k=0;k<10;k++){
						if(k==node1->paramLoc){
							loc=hashmap.find(uuid)->arrayIndex[k];
							memcpy(tmpuuid,hashmap.find(uuid)->array[k],32);
							break;
						}
					}
				}
				if(loc!=-1){
					node1=hashmapArray2.find(hashmap.find(uuid)->array[k])->char_arrNodes[loc%10];
					
				}
				
				
				int index=-1;//arr[index]=num
				int num=-1;
				if(para_name<0&&hash_index[0-para_name]!=0){
					index=hash_char[0-para_name];
				}else if(para_name>=100&&para_name<700){
					index=hashmap.find(uuid)->v_char[para_name-100];
				}
				if(p1<0&&hash_index[0-p1]!=0){
					num=hash_char[0-p1];
				}else if(p1>=100&&p1<700){
					num=hashmap.find(uuid)->v_char[p1-100];
				}
				idx+=index;
				CNODE node2=NULL;
				if(k!=-1){
					node2=hashmapArray2.find(hashmap.find(uuid)->array[k])->char_arrNodes[node1->oriLocation%10];
				}else{
					node2=hashmapArray2.find(uuid)->char_arrNodes[node1->oriLocation%10];
				}
			
				node2->data[idx]=num;
				
			}
		
		}else if(p2==3){//arr2=arr1 arr2=arr1[0]
			 
			 int loc=-1;
			 int k=-1;
			 
			CNODE node1=hashmapArray2.find(uuid)->char_arrNodes[p1_i%10];
			
			CNODE node2=hashmapArray2.find(uuid)->char_arrNodes[para_i%10];
			node2->d=node1->d;//update d
			for(int i=0;i<5;i++){
				node2->dimensions[i]=node1->dimensions[i];//update dimesions
				node2->index[i]=node1->index[i];//update index
			}
			node2->oriLocation=node1->oriLocation;//update oriLocation
		
			
		}else if(p2==4){// append index

			 int loc=-1;
			 int i=-1;
			 int k=-1;
			if(p1<0&&hash_index[0-p1]!=0){
				i=hash_char[0-p1];
			}else if(p1>=100&&p1<700){
				i==hashmap.find(uuid)->v_char[p1-100];
			}
			CNODE node=hashmapArray2.find(uuid)->char_arrNodes[para_i%10];

			SNODE snode=hashmap.find(uuid);
			node->paramLoc=p1_i;
			if(p1==0&&p1_i==0){
				for(k=0;k<10;k++){
					if(k==node->paramLoc){
						loc=snode->arrayIndex[k];
						memcpy(tmpuuid,hashmap.find(uuid)->array[k],32);
						break;
					}
				}
				
				if(hashmapArray2.find(snode->array[k])!=NULL){
					CNODE node2=hashmapArray2.find(snode->array[k])->char_arrNodes[loc%10];
					node->d=node2->d;//update d
					for(int i=0;i<5;i++){
					node->dimensions[i]=node2->dimensions[i];//update dimesions
					node->index[i]=node2->index[i];//update index
				}
				node->oriLocation=node2->oriLocation;//update oriLocation
				}
				
			}
			int j=0;
			while(node->index[j]!=-1){
				j++;
				
			}
			
			node->index[j]=i;
			
			
		}else if(p2==5){//get array length
				CNODE node1=hashmapArray2.find(uuid)->char_arrNodes[p1_i%10];
			    int d=0;
			    while(node1->index[d]!=-1){
			    	d++;
			    }
			    int loc=-1;
			    int k=-1;
			 	if(node1->paramLoc!=-1){//is formular array param
					for(k=0;k<10;k++){
						if(k==node1->paramLoc){
							loc=hashmap.find(uuid)->arrayIndex[k];
							memcpy(tmpuuid,hashmap.find(uuid)->array[k],32);
							break;
						}
					}
				}
				if(loc!=-1){
					node1=hashmapArray2.find(hashmap.find(uuid)->array[k])->char_arrNodes[loc%10];
				}
			    hashmap.find(uuid)->v_int[para_name-100]=node1->dimensions[d];

				
		}
		else if(p2==-1){
			
		}
		free(tmpuuid);
		return 1000;
		


	}
