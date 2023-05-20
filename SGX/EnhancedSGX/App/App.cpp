#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include <time.h>
# include <unistd.h>
# include <pwd.h>
#define MAX_PATH FILENAME_MAX

#include "sgx_urts.h"
#include "App.h"
#include "Enclave_u.h"
#include<fstream>
#include<iomanip>
#include <iostream>
#include <fstream>
#include <vector>
#include<time.h>
#include<ctime>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/ioctl.h>
using namespace std;
#include <cstdarg>
#include "invoker_sgx_invoker.h"

typedef unsigned char byte;
#define ArrayLen 10//10000


int sgx_use_flag=0;
/* Global EID shared by multiple threads */
sgx_enclave_id_t global_eid = 1;
sgx_enclave_id_t globalEnclaveID;
clock_t t1,t2;

int rei = -99;
int reu = -99;
int reb = -99;
int re_get = -99;

clock_t t3,t4;

int gnum = 0;
int inum = 0;
int dnum = 0;
int unum = 0;
int bnum = 0;

long temp = 0;
int tem=0;

ofstream outfile;

//------------------------------------------hotcalls-------------------------------------------
#include "common.h"
#include "hot_calls.h"
//#include "hot_calls1.h"
typedef sgx_status_t (*EcallFunction)(sgx_enclave_id_t, void* );

#define PERFORMANCE_MEASUREMENT_NUM_REPEATS 10000
#define MEASUREMENTS_ROOT_DIR               "measurments"

inline __attribute__((always_inline))  uint64_t rdtscp(void)
{
        unsigned int low, high;

        asm volatile("rdtscp" : "=a" (low), "=d" (high));

        return low | ((uint64_t)high) << 32;
}

void* EnclaveResponderThread( void* hotEcallAsVoidP )    //branch
{
    //To be started in a new thread
    HotCall *hotEcall = (HotCall*)hotEcallAsVoidP;
    EcallStartResponder( globalEnclaveID, hotEcall );

    return NULL;
}


void* EnclaveResponderThread3( void* hotEcall3AsVoidP ) //init_delete
{
    //To be started in a new thread
    HotCall *hotEcall3 = (HotCall*)hotEcall3AsVoidP;
    EcallStartResponder3( globalEnclaveID, hotEcall3 );

    return NULL;
}


	int         data            = 0;
	int intArray[ArrayLen]={0};
	double doubleArray[ArrayLen]={0};
	float floatArray[ArrayLen]={0};
	char charArray[ArrayLen]={NULL};
	long longArray[ArrayLen]={0};
	char byteArray[ArrayLen]={NULL};
	char uuid[33]={NULL};
	HotCall     hotEcall        = HOTCALL_INITIALIZER;

	char uuid3[33]={NULL};
	char uuid3c[33]={NULL};
	HotCall     hotEcall3        = HOTCALL_INITIALIZER;
    const uint16_t requestedCallID = 0;
	const uint16_t requestedCallID1 = 1;
	const uint16_t requestedCallID2 = 2;
    const uint16_t requestedCallID3 = 3;
    const uint16_t requestedCallID4 = 4;




JNIEXPORT jint JNICALL Java_invoker_sgx_1invoker_init(JNIEnv *env, jclass obj)
//int init_enclave(void)
{
t3=clock();
	if(sgx_use_flag){
		return 0;
	}
clock_t t1,t2;
t1=clock();
printf("Java_invoker_sgx_1invoker_init\n");
    if(initialize_enclave() < 0){
        printf("init Failed ...\n");
        getchar();
        return -1; 
    }
	printf("init ok\n");
	//------------load-----------------------
	int load_flag=-111;
	encall_table_load(global_eid,&load_flag);
	
	sgx_use_flag=1;
   
	//hotcall
	hotEcall.data               = &data;
	hotEcall.intArray               = intArray; 
	hotEcall.doubleArray               = doubleArray;
	hotEcall.floatArray               = floatArray;
	hotEcall.charArray               = charArray;
	hotEcall.longArray               = longArray;
	hotEcall.byteArray               = byteArray;	
	hotEcall.uuid               = uuid;

	hotEcall3.uuid               = uuid3;


        globalEnclaveID = global_eid;
        pthread_create(&hotEcall.responderThread, NULL, EnclaveResponderThread, (void*)&hotEcall);
       
 	pthread_create(&hotEcall3.responderThread, NULL, EnclaveResponderThread3, (void*)&hotEcall3);
t2=clock();
printf("initialize_enclave()_time:%lfs\n",((double)(t2 - t1)/CLOCKS_PER_SEC));
	printf("hotcall init ok\n");

	return load_flag;
}

JNIEXPORT jint JNICALL Java_invoker_sgx_1invoker_destroy(JNIEnv *env, jclass obj)
{

    //------------------------------destroy------------
    StopResponder( &hotEcall );    //StopResponder( &hotEcall2);
    StopResponder( &hotEcall3);
   
    if(SGX_SUCCESS==sgx_destroy_enclave(global_eid)){
	sgx_use_flag=0;
	printf("total_ecall=%d\n",gnum+unum+bnum+inum+dnum);
	printf("getint_ecall=%d\n",gnum);
	printf("update_ecall=%d\n",unum);
	printf("branch_ecall=%d\n",bnum);
	printf("init_ecall=%d\n",inum);
	printf("delete_ecall=%d\n",dnum);
    	printf("Enclave destroy success\n");
	return 0;
    }else{
	printf("Enclave destroy failure\n");
	return -1;
    }
}
JNIEXPORT void JNICALL Java_invoker_sgx_1invoker_initNode
  (JNIEnv *env, jclass obj, jstring uuidstring, jint type, jint size){

  	if(env -> MonitorEnter(obj)!= JNI_OK)
		printf("initArray enter wrong \n");

	const char* buf = env->GetStringUTFChars(uuidstring,false);
	strcpy(uuid,buf);
	env->ReleaseStringUTFChars(uuidstring, buf);

	encall_initNode(global_eid,uuid,type,size);

	if(env -> MonitorExit(obj)!= JNI_OK)
		printf("initArray exit wrong \n");
	return;

  }


JNIEXPORT jint JNICALL Java_invoker_sgx_1invoker_commitInt
  (JNIEnv *env, jclass obj, jlong counter, jintArray jintArray, jint intTail, jdoubleArray jdoubleArray, jint doubleTail, jfloatArray jfloatArray, jint floatTail,jlongArray jlongArray, jint longTail, jcharArray jcharArray, jint charTail,jbyteArray jbyteArray, jint byteTail, jstring uuidstring){


	
	if(env -> MonitorEnter(obj)!= JNI_OK)
		printf("Java_invoker_sgx_1invoker_commitInt enter wrong \n");

gnum++;
		jint *body_i = env->GetIntArrayElements(jintArray, 0);
			for (int i=0; i<intTail; i++)
			{
				intArray[i] = body_i[i];
			}
			env->ReleaseIntArrayElements(jintArray, body_i, 0);

		jdouble *body_d = env->GetDoubleArrayElements(jdoubleArray, 0);
		for (int i=0; i<doubleTail; i++)
		{
			doubleArray[i] = body_d[i];
		}
		env->ReleaseDoubleArrayElements(jdoubleArray, body_d, 0);

		jfloat *body_f = env->GetFloatArrayElements(jfloatArray, 0);
		for (int i=0; i<floatTail; i++)
		{
			floatArray[i] = body_f[i];
		}
		env->ReleaseFloatArrayElements(jfloatArray,body_f, 0);

		jlong *body_l = env->GetLongArrayElements(jlongArray, 0);
		for (int i=0; i<longTail; i++)
		{
			longArray[i] = body_l[i];
		}
		env->ReleaseLongArrayElements(jlongArray,body_l, 0);


		jchar *body_c = env->GetCharArrayElements(jcharArray, 0);
		for (int i=0; i<charTail; i++)
		{
			charArray[i] = body_c[i];
		}
		env->ReleaseCharArrayElements(jcharArray,body_c , 0);

		jbyte *body_b = env->GetByteArrayElements(jbyteArray, 0);
		for (int i=0; i<byteTail; i++)
		{
			byteArray[i] = body_b[i];
		}
		env->ReleaseByteArrayElements(jbyteArray,body_b, 0);

	int re=-99;
	const char* buf = env->GetStringUTFChars(uuidstring, false);
	char uuid[33] = {0};
	char cuuid[33] = {0};
	strcpy(uuid,buf);
	env->ReleaseStringUTFChars(uuidstring,buf);	
	HotCall_requestCall( &hotEcall,requestedCallID4, &counter,intArray,intTail,doubleArray,doubleTail,floatArray,floatTail,charArray,charTail,longArray,longTail,byteArray,byteTail,uuid,cuuid,&re_get);//printf("get Line=%ld ,uuid=%s\n",counter,uuid);
	
	re = re_get;

	if(env -> MonitorExit(obj)!= JNI_OK)
		printf("Java_invoker_sgx_1invoker_commitInt exit wrong \n");	
	
	return re;

	
	
}
/**
	commitDouble
*/
JNIEXPORT jdouble JNICALL Java_invoker_sgx_1invoker_commitDouble
   (JNIEnv *env, jclass obj, jlong counter, jintArray jintArray, jint intTail, jdoubleArray jdoubleArray, jint doubleTail, jfloatArray jfloatArray, jint floatTail,jlongArray jlongArray, jint longTail, jcharArray jcharArray, jint charTail,jbyteArray jbyteArray, jint byteTail, jstring uuidstring){
	if(env -> MonitorEnter(obj)!= JNI_OK)
		printf("double enter wrong \n");		
	printf("go to get double line=%ld\n",counter);	
	const char* buf = env->GetStringUTFChars(uuidstring, false);
	strcpy(uuid,buf);
	env->ReleaseStringUTFChars(uuidstring, buf);

	
	double red=0.0;
	
	sgx_status_t ret=encall_switch_get_d(global_eid,&red,counter,intArray,intTail,
			doubleArray,doubleTail,
			floatArray,floatTail,
			charArray,charTail,
			longArray,longTail,
			byteArray,byteTail,uuid);
	if(ret != SGX_SUCCESS){
		print_error_message(ret);
	}
	if(env -> MonitorExit(obj)!= JNI_OK)
		printf("Java_invoker_sgx_1invoker_commitdouble exit wrong \n");	
	return red;
}




/**
	commitLong
*/
JNIEXPORT jlong JNICALL Java_invoker_sgx_1invoker_commitLong
  (JNIEnv *env, jclass obj, jlong counter, jintArray jintArray, jint intTail, jdoubleArray jdoubleArray, jint doubleTail, jfloatArray jfloatArray, jint floatTail,jlongArray jlongArray, jint longTail, jcharArray jcharArray, jint charTail,jbyteArray jbyteArray, jint byteTail, jstring uuidstring){
	
	if(env -> MonitorEnter(obj)!= JNI_OK)
		printf("Long enter wrong \n");	
	long rel=999;	
	
	const char* buf = env->GetStringUTFChars(uuidstring, false);
	//char uuid[33] = {0};
	strcpy(uuid,buf);
	env->ReleaseStringUTFChars(uuidstring, buf);

	sgx_status_t ret=encall_switch_get_l(global_eid,&rel,counter,intArray,intTail,
			doubleArray,doubleTail,
			floatArray,floatTail,
			charArray,charTail,
			longArray,longTail,
			byteArray,byteTail,uuid);
	if(ret != SGX_SUCCESS){
		print_error_message(ret);
	}
//printf("get over %ld\n",rel);

	if(env -> MonitorExit(obj)!= JNI_OK)
		printf("branch exit wrong \n");	
	return rel;
}

JNIEXPORT jint JNICALL Java_invoker_sgx_1invoker_commitBranch
 (JNIEnv *env, jclass obj, jlong counter, jintArray jintArray, jint intTail, jdoubleArray jdoubleArray, jint doubleTail, jfloatArray jfloatArray, jint floatTail,jlongArray jlongArray, jint longTail, jcharArray jcharArray, jint charTail,jbyteArray jbyteArray, jint byteTail, jstring uuidstring){
	
	if(env -> MonitorEnter(obj)!= JNI_OK)
		printf("branch enter wrong \n");
 bnum++;
	int re = -98;
	const char* buf = env->GetStringUTFChars(uuidstring, false);
	char uuid[33] = {0};
	strncpy(uuid,buf,32);
	env->ReleaseStringUTFChars(uuidstring, buf);
	HotCall_requestCall( &hotEcall,requestedCallID, &counter,NULL,0,NULL,0,NULL,0,NULL,0,NULL,0,NULL,0,uuid,NULL,&reb);	
	
	
	
	re = reb;

	if(env -> MonitorExit(obj)!= JNI_OK)
		printf("branch exit wrong \n");	
	return re;

}

JNIEXPORT jint JNICALL Java_invoker_sgx_1invoker_commitUpdate
  (JNIEnv *env, jclass obj, jlong counter, jintArray jintArray, jint intTail, jdoubleArray jdoubleArray, jint doubleTail, jfloatArray jfloatArray, jint floatTail,jlongArray jlongArray, jint longTail, jcharArray jcharArray, jint charTail,jbyteArray jbyteArray, jint byteTail, jstring uuidstring){


	if(env -> MonitorEnter(obj)!= JNI_OK)
		printf("update enter wrong \n");

unum++;


	int* intArray = NULL;
	double* doubleArray = NULL;
	float* floatArray = NULL;
	long* longArray = NULL;
	char* charArray = NULL;
	char* byteArray = NULL;

	if(intTail > 0){
		intArray = new int[intTail];
	}
	if(doubleTail > 0){
		doubleArray = new double[doubleTail];
	}
	if(floatTail > 0){
		floatArray = new float[floatTail];
	}			
	if(longTail > 0){
		longArray = new long[longTail];
	}
	if(charTail > 0){
		charArray = new char[charTail];
	}
	if(byteTail > 0){
		byteArray = new char[byteTail];
	}
	if(intTail > 0){
		intArray = new int[intTail];
		jint *body_i = env->GetIntArrayElements(jintArray, 0);
		for (int i=0; i<intTail; i++)
		{
			intArray[i] = body_i[i];
		}
		env->ReleaseIntArrayElements(jintArray, body_i, 0);
	}
	if(doubleTail > 0){
		doubleArray = new double[doubleTail];
		jdouble *body_d = env->GetDoubleArrayElements(jdoubleArray, 0);
		for (int i=0; i<doubleTail; i++)
		{
			doubleArray[i] = body_d[i];
		}
		env->ReleaseDoubleArrayElements(jdoubleArray, body_d, 0);
	}
	if(floatTail > 0){
		floatArray = new float[floatTail];
		jfloat *body_f = env->GetFloatArrayElements(jfloatArray, 0);
		for (int i=0; i<floatTail; i++)
		{
			floatArray[i] = body_f[i];
		}
		env->ReleaseFloatArrayElements(jfloatArray,body_f, 0);
	}
	if(longTail > 0){
		longArray = new long[longTail];
		jlong *body_l = env->GetLongArrayElements(jlongArray, 0);
		for (int i=0; i<longTail; i++)
		{
			longArray[i] = body_l[i];
		}
		env->ReleaseLongArrayElements(jlongArray,body_l, 0);

	}
	if(charTail > 0){
		charArray = new char[charTail];
		jchar *body_c = env->GetCharArrayElements(jcharArray, 0);
		for (int i=0; i<charTail; i++)
		{
			charArray[i] = body_c[i];
		}
		env->ReleaseCharArrayElements(jcharArray,body_c , 0);
	}
	if(byteTail > 0){
		byteArray = new char[byteTail];
		jbyte *body_b = env->GetByteArrayElements(jbyteArray, 0);
		for (int i=0; i<byteTail; i++)
		{
			byteArray[i] = body_b[i];
		}
		env->ReleaseByteArrayElements(jbyteArray,body_b, 0);
	}

	int re=-97;

	const char* buf = env->GetStringUTFChars(uuidstring, false);
	char uuid[33] = {0};
	strncpy(uuid,buf,32);
	char cuuid[33] = {0};
	strncpy(cuuid,buf+32,32);
	env->ReleaseStringUTFChars(uuidstring,buf);

  
	HotCall_requestCall( &hotEcall,requestedCallID3,&counter,intArray,intTail,doubleArray,doubleTail,floatArray,floatTail,charArray,charTail,longArray,longTail,byteArray,byteTail,uuid,cuuid,&reu);
	
	if(intTail > 0){
		delete[] intArray;
	}

	if(doubleTail > 0){
		delete[] doubleArray;
	}
	if(floatTail > 0){
		delete[] floatArray;
	}			
	if(longTail > 0){
		delete[] longArray;
	}
	if(charTail > 0){
		delete[] charArray;
	}
	if(byteTail > 0){
		delete[] byteArray;
	}

	
	re = reu;
	if(env -> MonitorExit(obj)!= JNI_OK)
		printf("update exit wrong \n");

	return re;
	
}

JNIEXPORT jint JNICALL Java_invoker_sgx_1invoker_initValue
  (JNIEnv *env, jclass obj,jstring uuidstring,jstring calluuidstring, jlong lineno){

	if(env -> MonitorEnter(obj)!= JNI_OK)
		printf("initvalue enter wrong \n");

inum++;
	const char* buf = env->GetStringUTFChars(uuidstring, false);
	strcpy(uuid3,buf);
	env->ReleaseStringUTFChars(uuidstring, buf);
	if(calluuidstring == NULL){
		HotCall_requestCall( &hotEcall3,requestedCallID1,&lineno,NULL,0,NULL,0,NULL,0,NULL,0,NULL,0,NULL,0,uuid3,NULL,&tem);
	}else{
		const char* callbuf = env->GetStringUTFChars(calluuidstring, false);
	
		strcpy(uuid3c,callbuf);
		env->ReleaseStringUTFChars(calluuidstring, callbuf);
		HotCall_requestCall( &hotEcall3,requestedCallID1,&lineno,NULL,0,NULL,0,NULL,0,uuid3c,32,NULL,0,NULL,0,uuid3,NULL,&tem);

	}
	if(env -> MonitorExit(obj)!= JNI_OK)
		printf("initvalue exit wrong \n");
	return 1;
}

JNIEXPORT jint JNICALL Java_invoker_sgx_1invoker_deleteValue
  (JNIEnv *env, jclass obj, jstring uuidstring,jstring deletecuuid,jlong status){


	if(env -> MonitorEnter(obj)!= JNI_OK)
		printf("deletevalue enter wrong \n");
dnum++;
	const char* buf = env->GetStringUTFChars(uuidstring, false);
	strcpy(uuid3,buf);
	if(status == 0){
		HotCall_requestCall( &hotEcall3,requestedCallID2, &status,NULL,0,NULL,0,NULL,0,NULL,0,NULL,0,NULL,0,uuid3,NULL,&tem);
	}else if(status == 1){
		const char* callbuf = env->GetStringUTFChars(deletecuuid, false);
		strcpy(uuid3c,callbuf);
		env->ReleaseStringUTFChars(deletecuuid, callbuf);
		HotCall_requestCall( &hotEcall3,requestedCallID2, &status,NULL,0,NULL,0,NULL,0,uuid3c,32,NULL,0,NULL,0,uuid3,NULL,&tem);
	}
	env->ReleaseStringUTFChars(uuidstring, buf);

	if(env -> MonitorExit(obj)!= JNI_OK)
		printf("deletevalue exit wrong \n");
	return 1;
}

JNIEXPORT void JNICALL Java_invoker_sgx_1invoker_initArray
  (JNIEnv *env, jclass obj, jstring uuidstring, jint index, jint size,jint isSens){
	if(env -> MonitorEnter(obj)!= JNI_OK)
		printf("initArray enter wrong \n");

	const char* buf = env->GetStringUTFChars(uuidstring, false);
	strcpy(uuid,buf);
	env->ReleaseStringUTFChars(uuidstring, buf);

	encall_initArray(global_eid,uuid,index,size,isSens);
	if(env -> MonitorExit(obj)!= JNI_OK)
		printf("initArray exit wrong \n");
	return;
}



JNIEXPORT jintArray JNICALL Java_invoker_sgx_1invoker_commitIntArray
  (JNIEnv *env, jclass obj, jlong counter, jstring uuidstring){
	

	if(env -> MonitorEnter(obj)!= JNI_OK)
		printf("initArray enter wrong \n");
	const char* buf = env->GetStringUTFChars(uuidstring, false);
	strcpy(uuid,buf);
	env->ReleaseStringUTFChars(uuidstring, buf);

	int size = 0;
	
	encall_getArraySize(global_eid,&size,counter,uuid);
	int *re = new int[size];
	//second return the array
	encall_getIntArray(global_eid,re,size,counter,uuid);
	
 	jintArray result = env->NewIntArray(size);
 	if (result == NULL) {
	    printf("APP.CPP OUT OF MEMORY!!!!!!!!\n");
 	    return NULL; /* out of memory error thrown */
 	}
	jint *get_i = env->GetIntArrayElements(result, 0);
	for(int i=0;i<size;i++){
			get_i[i] = re[i];
	}
	env->ReleaseIntArrayElements(result, get_i, 0);
	delete[] re;
	if(env -> MonitorExit(obj)!= JNI_OK)
		printf("initArray exit wrong \n");

 	return result;
}

JNIEXPORT jdoubleArray JNICALL Java_invoker_sgx_1invoker_commitDoubleArray
  (JNIEnv *env, jclass obj, jlong counter, jstring uuidstring){
	if(env -> MonitorEnter(obj)!= JNI_OK)
		printf("initArray enter wrong \n");
	const char* buf = env->GetStringUTFChars(uuidstring, false);
	strcpy(uuid,buf);
	env->ReleaseStringUTFChars(uuidstring, buf);
	int size = 0;
	
	//first return array size
	encall_getArraySize(global_eid,&size,counter,uuid);

	//printf("d size=%d\n",size);
	double *re = new double[size];
	//second return the array
	encall_getDoubleArray(global_eid,re,size,counter,uuid);
	

 	jdoubleArray result = env->NewDoubleArray(size);
 	if (result == NULL) {
	    printf("APP.CPP OUT OF MEMORY!!!!!!!!\n");
 	    return NULL; /* out of memory error thrown */
 	}

	// move from the temp structure to the java structure
	jdouble *get_d = env->GetDoubleArrayElements(result, 0);
	for(int i=0;i<size;i++){
			get_d[i] = re[i];
	}
	env->ReleaseDoubleArrayElements(result, get_d, 0);
	delete[] re;
	if(env -> MonitorExit(obj)!= JNI_OK)
		printf("initArray exit wrong \n");
 	return result;
}

JNIEXPORT jint JNICALL Java_invoker_sgx_1invoker_commitUpdateMutliArray
  (JNIEnv *env, jclass obj, jlong counter, jstring uuidstring, jstring cuuid){

	if(env -> MonitorEnter(obj)!= JNI_OK)
		printf("initArray enter wrong \n");
	const char* buf = env->GetStringUTFChars(uuidstring, false);
	strcpy(uuid3,buf);
	env->ReleaseStringUTFChars(uuidstring, buf);

	const char* callbuf = env->GetStringUTFChars(cuuid, false);
	strcpy(uuid3c,callbuf);
	env->ReleaseStringUTFChars(cuuid, callbuf);

	encall_initmultiArray(global_eid,counter,uuid3,uuid3c);
	if(env -> MonitorExit(obj)!= JNI_OK)
		printf("initArray exit wrong \n");
 	return 1;

}

int SGX_CDECL main(int argc, char *argv[])
{
clock_t t1,t2,t3,t4;
t1=clock();
initialize_enclave();
t2=clock();
printf("init time:%lf\n",((double)(t2 - t1)/CLOCKS_PER_SEC));
int re = 0;
int red =99;
t3=clock();
t4=clock();
printf("destory time:%lf\n",((double)(t4 - t3)/CLOCKS_PER_SEC));

return 0;
}
