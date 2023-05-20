package invoker;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class sgx_invoker{
	//public native int varargsMethod( int... no,float... fl,double... dl,long... lo );
  	public static final int N=20;
  	//public static final int Temp=100;
	public static native int print_ms();
	public static native int init();
	public static native int destroy();
	public static native int commitInt(long counter, int[] intArray, int intTail, double[] doubleArray, int doubleTail,float[] floatArray, int floatTail, long[] longArray, int longTail, char[] charArray,int charTail, byte[] byteArray,int byteTail, String uuid);
	public static native float commitFloat(long counter, int[] intArray, int intTail, double[] doubleArray, int doubleTail,float[] floatArray, int floatTail, long[] longArray, int longTail, char[] charArray,int charTail, byte[] byteArray,int byteTail, String uuid);
	public static native double commitDouble(long counter, int[] intArray, int intTail, double[] doubleArray, int doubleTail,float[] floatArray, int floatTail, long[] longArray, int longTail, char[] charArray,int charTail, byte[] byteArray,int byteTail, String uuid);
	public static native char commitChar(long counter, int[] intArray, int intTail, double[] doubleArray, int doubleTail,float[] floatArray, int floatTail, long[] longArray, int longTail, char[] charArray,int charTail, byte[] byteArray,int byteTail, String uuid);
	public static native byte commitByte(long counter, int[] intArray, int intTail, double[] doubleArray, int doubleTail,float[] floatArray, int floatTail, long[] longArray, int longTail, char[] charArray,int charTail, byte[] byteArray,int byteTail, String uuid);
	public static native long commitLong(long counter, int[] intArray, int intTail, double[] doubleArray, int doubleTail,float[] floatArray, int floatTail, long[] longArray, int longTail, char[] charArray,int charTail, byte[] byteArray,int byteTail, String uuid);
	public static native int commitBranch(long counter, int[] intArray, int intTail, double[] doubleArray, int doubleTail,float[] floatArray, int floatTail, long[] longArray, int longTail, char[] charArray,int charTail, byte[] byteArray,int byteTail, String uuid);
	public static native int commitUpdate(long counter, int[] intArray,int intTail, double[] doubleArray,int doubleTail,float[] floatArray, int floatTail, long[] longArray, int longTail, char[] charArray, int charTail,byte[] byteArray,int byteTail, String uuid);

	public static native int[] commitIntArray(long counter,String uuid);
	public static native double[] commitDoubleArray(long counter,String uuid);
	public static native byte[] commitByteArray(long counter,String uuid);
	
	public static native int commitUpdateMutliArray(long counter,String uuid,String cuuid);

	public static native int initValue(String uuid,String calluuid,long LineNo);
	public static native int deleteValue(String uuid,String cuuid,long status);
	
	public static native void initArray(String uuid,int index,int size,int isSens);
	public static native void initNode(String uuid,int type,int size);
    
	
	static {
		try{
//			System.out.println("invoker"+System.getProperty("java.library.path"));
			System.loadLibrary("SGX");
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("invoker"+System.getProperty("java.library.path"));
			e.printStackTrace();
		}
	} 
	
	/*public static class TempUpdate {
	    long Counter;
	    int[] IntArray;
	    double[] DoubleArray;
	    float[] FloatArray;
	    long[] LongArray;
	    char[] CharArray;
	    byte[] ByteArray;
	    
	    int inttail;
	    int doubletail;
	    int floattail;
	    int longtail;
	    int chartail;
	    int bytetail;
	    
	    String uuid;
	    public TempUpdate(long Counter,int[] IntArray,double[] DoubleArray,
	    		float[] FloatArray,long[] LongArray,char[] CharArray,byte[] ByteArray,int inttail,
	    		int doubletail,int floattail, int longtail,int chartail, int bytetail,String uuid){
	    	this.Counter = Counter;
	    	this.IntArray = IntArray;
	    	this.DoubleArray = DoubleArray;
	    	this.FloatArray = FloatArray;
	    	this.LongArray = LongArray;
	    	this.CharArray = CharArray;
	    	this.ByteArray = ByteArray;
	    	
	    	this.inttail = inttail;
	    	this.doubletail = doubletail;
	    	this.floattail = floattail;
	    	this.longtail = longtail;
	    	this.chartail = chartail;
	    	this.bytetail = bytetail;
	    	
	    	this.uuid = uuid;
	    }
	}

	//public static List<TempUpdate> tempUpdatesList = new ArrayList<>();
    //TODO: may potentially reduce the performance, can be improved later
	//ArrayList<Object> objects = null;
	
	long[] counterArr = new long[10];
	int[] temintarray = new int[Temp];
	double[] temdoublearray = new double[Temp];
	float[] temfloatarray = new float[Temp];
	char[] temchararray = new char[Temp];
	long[] temlongarray = new long[Temp];
	byte[] tembytearray = new byte[Temp];
*/
	int[] intArray = new int[N];
	int intTail = 0;
	
	double[] doubleArray = new double[N];
	int doubleTail = 0;
	
	float[] floatArray = new float[N];
	int floatTail = 0;
	
	long[] longArray = new long[N];
	int longTail = 0;
	
	char[] charArray = new char[N];
	int charTail = 0;
	
	byte[] byteArray = new byte[N];
	int byteTail = 0;
	
	int[] iarr = new int[100];
	double[] darr = new double[100];
	float[] farr = new float[100];
	char[] carr = new char[100];
	long[] larr = new long[100];
	byte[] barr = new byte[100];
	
	int[][] miarr = new int[100][100];
	double[][] mdarr = new double[100][100];
	float[][] mfarr = new float[100][100];
	char[][] mcarr = new char[100][100];
	long[][] mlarr = new long[100][100];
	byte[][] mbarr = new byte[100][100];
	
	int size_i=0;
	int size_d=0;
	int size_f=0;
	int size_c=0;
	int size_l=0;
	int size_b=0;
	
	long counter = -1;
	String cuuid = null;
	//long invokecounter = -1;
	//static SnowFlake snowFlake = new SnowFlake(2, 3);
	public void sgx_invoker(){
		
		//objects = new ArrayList<Object>();
	}
	
	//long time_init = 0;
	//long time_update = 0;
	//long time_get = 0;
	//long time_branch = 0;
	//long time_delete = 0;
	
	//static long request = 1;
	//static long hitNum = 0;
	
	public void clear(){
		intTail = 0;
		doubleTail = 0;
		floatTail = 0;
		longTail = 0;
		charTail = 0;
		byteTail = 0;
		
		size_i=0;
		size_d=0;
		size_f=0;
		size_c=0;
		size_l=0;
		size_b=0;
	}
	
	public void add(Object o){
		if(o==null)
			intArray[intTail++]=0;
		else
			intArray[intTail++]=o.hashCode();
		//objArray[objTail++] = o;
	}

	public void add(int o){
		intArray[intTail++] = o;
//		System.out.println(String.valueOf(o)+" is added to list;");
	}
	
	public void add(double o){
		doubleArray[doubleTail++] = o;
	}
	public void add(float o){
		floatArray[floatTail++] = o;
	}
	
	public void add(long o){
		longArray[longTail++] = o;
	}
	public void add(char o){
		charArray[charTail++] = o;
	}
	public void add(byte o){
		intArray[intTail++] = o;
	}
	
	public void add(int[] o){
		//iarr = o;
		size_i = o.length;
		System.arraycopy(o, 0, iarr, 0, size_i);
	}
	public void add(double[] o){
		//darr = o;
		size_d = o.length;
		System.arraycopy(o, 0, darr, 0, size_d);
	}
	public void add(float[] o){
		//farr = o;
		size_f = o.length;
		System.arraycopy(o, 0, farr, 0, size_f);
	}
	public void add(char[] o){
		//carr = o;
		size_c = o.length;
		System.arraycopy(o, 0, carr, 0, size_c);
	}
	public void add(long[] o){
		//larr = o;
		size_l = o.length;
		System.arraycopy(o, 0, larr, 0, size_l);
	}
	public void add(byte[] o){
		//barr = o;
		size_b = o.length;
		System.arraycopy(o, 0, barr, 0, size_b);
	}
	
	public void add(int[][] o){
	}
	public void add(double[][] o){
	}
	public void add(float[][] o){
	}
	public void add(char[][] o){
	}
	public void add(long[][] o){
	}
	public void add(byte[][] o){
		
	}
	public void add(int[][][] o){
	}
	public void add(double[][][] o){
	}
	public void add(float[][][] o){
	}
	public void add(char[][][] o){
	}
	public void add(long[][][] o){
	}
	public void add(byte[][][] o){
		
	}
	
	public void setCounter(long counter){
		this.counter = counter;
	}
	
	public void setCuuid(String uuid){
		this.cuuid = uuid;
	}
	
//	public void setInvokeCounter(long counter){
//		this.invokecounter = counter;
//	}
	
	public boolean initenclave(){
		//System.out.println("init in java");
		//init_total++;
		if(1==init())
		return true;
		else
		return false;
	}
	
	public boolean closeenclave(){
	
		if(0==destroy())
		return true;
		else
		return false;
		
	}
	
	public java.lang.String getUUID() {
		String idsString = UUID.randomUUID().toString().replace("-", "").toLowerCase();
		//System.out.println(idsString);
		return idsString;
	}
	
	//新的生成uuid方法
//		public java.lang.String getUUID() {
//			
//			//System.out.println("cominnnnnnnnnnnnnnnnnnnnnnnnnng");
//			//String idsString = String.valueOf(snowFlake.nextId());
//			//System.out.println(idsString);
//			//return idsString;
//			return String.valueOf(SnowFlake.nextId());
//		}
	
	/*public void loadTempTail(int index,TempUpdate t){
		if (t.inttail>0) {
			for (int i = 0; i < t.inttail; i++) {
				temintarray[index*10+i] = t.IntArray[i];
				inttailT = 1;
			}
		}
		if (t.doubletail>0) {
			for (int i = 0; i < t.doubletail; i++) {
				temdoublearray[index*10+i] = t.DoubleArray[i];
				doubletailT = 1;
			}
		}
		if (t.floattail>0) {
			for (int i = 0; i < t.floattail; i++) {
				temfloatarray[index*10+i] = t.FloatArray[i];
				floattailT = 1;
			}
		}
		if (t.longtail>0) {
			for (int i = 0; i < t.longtail; i++) {
				temlongarray[index*10+i] = t.LongArray[i];
				longtailT = 1;
			}
		}
		if (t.chartail>0) {
			for (int i = 0; i < t.chartail; i++) {
				temchararray[index*10+i] = t.CharArray[i];
				chartailT = 1;
			}
		}
		if (t.bytetail>0) {
			for (int i = 0; i < t.bytetail; i++) {
				tembytearray[index*10+i] = t.ByteArray[i];
				bytetailT = 1;
			}
		}
	}
	*/
	
	/**
	 * new solution for array init
	 * @param uuid
	 * @param index
	 * @param size
	 * @return
	 */
	public void initArrayInEnclave(String uuid,int index,int size,int isSens){
		
		initArray(uuid,index,size,isSens);
	}
	
	//add gpf
	public void initNodeInEnclave(String uuid,int type,int size){
		
 	//7: int type array  8: double type array
		initNode(uuid, type,size);
	
		
	}
	
	
	public boolean initValueInEnclave(String uuid,String calluuid,long LineNO){
		/*if (!tempUpdatesList.isEmpty()) {
			int index = 0;
			
			//String uuidList = new String();
			for(TempUpdate t:tempUpdatesList){
				counterArr[index] = t.Counter;
				//uuidT = t.uuid;
				loadTempTail(index, t);
				index++;
			}
			
			int ret = commitUpdate(counterArr,temintarray,inttailT,temdoublearray,doubletailT,temfloatarray,floattailT,
					temlongarray,longtailT,temchararray,chartailT,tembytearray,bytetailT,tempUpdatesList.get(0).uuid,tempUpdatesList.size());
			tempUpdatesList.clear();
		}*/
		
		//System.out.println("uuid="+uuid);
		if(1==initValue(uuid,calluuid,LineNO)){
			//System.out.println("initvalue true=");
			return true;
		}
		
		else
			//initvaluefalse++;
		    //System.out.println("initvalue false=");
			return false;
	}
	
	public boolean deleteValueInEnclave(String getuuid,String cuuid,long status){
		/*if (!tempUpdatesList.isEmpty()) {
			int index = 0;
			for(TempUpdate t:tempUpdatesList){
				counterArr[index] = t.Counter;
				loadTempTail(index, t);
				index++;
			}
			int ret = commitUpdate(counterArr,temintarray,inttailT,temdoublearray,doubletailT,temfloatarray,floattailT,
					temlongarray,longtailT,temchararray,chartailT,tembytearray,bytetailT,tempUpdatesList.get(0).uuid,tempUpdatesList.size());
			tempUpdatesList.clear();
		}*/

		//System.out.println("[invoke] status:"+status);
		if(1==deleteValue(getuuid,cuuid,status)){
			return true;
		}
		else
			return false;
	}
	
	
	public void updateValueInEnclave(String uuid,int status,long counter){
		//edit 20210526gxc merge continuous update function
		
		//SSSSSystem.out.println("java update stmt==========counter==="+counter);
		//System.out.println("update stmt==========counter==="+counter);
		
		int ret = -1;
		if (status == 0) {
			ret = commitUpdate(counter,intArray,intTail,doubleArray,doubleTail,floatArray,floatTail,
					longArray,longTail,(cuuid==null)?charArray:cuuid.toCharArray(),(cuuid==null)?charTail:cuuid.toCharArray().length,byteArray,byteTail,uuid);
		}else if (status == 1) {
			int[] newi = new int[size_i];
			double [] newd = new double[size_d];
			float [] newf = new float[size_f];
			//char [] newc = new char[size_c];
			long [] newl = new long[size_l];
			byte [] newb = new byte[size_b];
			
			for (int i = 0; i < size_i; i++) {
				//System.out.println("[SGXinvoke]iarr["+i+"]:"+iarr[i]);
				newi[i] = iarr[i];
			}
			for (int i = 0; i < size_d; i++) {
				newd[i] = darr[i];
			}
			for (int i = 0; i < size_f; i++) {
				newf[i] = farr[i];
			}
//			for (int i = 0; i < size_c; i++) {
//				newc[i] = carr[i];
//			}
			for (int i = 0; i < size_l; i++) {
				newl[i] = larr[i];
			}
			for (int i = 0; i < size_b; i++) {
				newb[i] = barr[i];
			}
			ret = commitUpdate(counter,newi,size_i,newd,size_d,newf,size_f,
					newl,size_l,(cuuid==null)?charArray:cuuid.toCharArray(),(cuuid==null)?charTail:cuuid.toCharArray().length,newb,size_b,uuid);
		}
		
		if (ret != 1000) {
			System.out.println("update wrong:"+ret);
		}
		clear();
	}
	
	
	public void updateMultArray(String uuid,int width,int high,long counter) {
		commitUpdateMutliArray(counter,uuid,cuuid);
		clear();
	}
	
	public boolean getBooleanValue(String uuid,long counter){ 
		//clear();
		//System.out.println("branch stmt==========counter==="+counter);
		/*if (!tempUpdatesList.isEmpty()) {
			//System.out.println("update exe in branch size:"+tempUpdatesList.size());
			int index = 0;
			for(TempUpdate t:tempUpdatesList){
				counterArr[index] = t.Counter;
				loadTempTail(index, t);
				index++;
			}
			int ret = commitUpdate(counterArr,temintarray,inttailT,temdoublearray,doubletailT,temfloatarray,floattailT,
					temlongarray,longtailT,temchararray,chartailT,tembytearray,bytetailT,tempUpdatesList.get(0).uuid,tempUpdatesList.size());
			tempUpdatesList.clear();
		}*/
		
		int ret = -1;
		ret = commitBranch(counter, intArray,intTail,doubleArray,doubleTail,floatArray,floatTail,longArray,longTail,charArray,charTail,byteArray,byteTail,uuid);
		if(ret == 1){
			//System.out.println("get is okay!");
			clear();
			return true;
		}else if(ret == 0){
			clear();
			return false;
		}else{
			//throw new Exception("error");
			System.out.println("branch ret:"+ret);
			System.out.println("branch error");
			System.out.println("ret");
			System.exit(1);
		}
		clear();
		return false;
	}
	public int getIntValue(String uuid,int status,long counter){ 
		//System.out.println("get stmt==========counter==="+counter);
		/*if (!tempUpdatesList.isEmpty()) {
			int index = 0;
			for(TempUpdate t:tempUpdatesList){
				counterArr[index] = t.Counter;
				loadTempTail(index, t);
				index++;
			}
			int ret = commitUpdate(counterArr,temintarray,inttailT,temdoublearray,doubletailT,temfloatarray,floattailT,
					temlongarray,longtailT,temchararray,chartailT,tembytearray,bytetailT,tempUpdatesList.get(0).uuid,tempUpdatesList.size());
			tempUpdatesList.clear();
		}*/

		int ret = -1;
		
		ret = commitInt(counter, intArray,intTail,doubleArray,doubleTail,floatArray,floatTail,longArray,longTail,charArray,charTail,byteArray,byteTail,uuid);
		//System.out.println("get ret in jni:"+ret);
		clear();
		return ret;
	}
	
	public int[] getIntArray(String uuid,int status,long counter){ 
		
		int[] ret;
		if (status==1) {
			//System.out.println("[invoke]cuuid:"+cuuid);
			ret = commitIntArray(counter,cuuid);
		}else {
			ret = commitIntArray(counter,uuid);
		}
		//System.out.println("[invoke]get success!");
		//System.out.println("[invoke i]"+Arrays.toString(ret));
		clear();
		return ret;
	}
	
	public double[] getDoubleArray(String uuid,int status,long counter){ 
		
		double[] ret;
		if (status==1) {
			ret = commitDoubleArray(counter, cuuid);
		}else{
			ret = commitDoubleArray(counter, uuid);
		}
		clear();
		return ret;
	}
	
	public byte[] getByteArray(String uuid,int status,long counter){ 
		
		byte[] ret;
		
		ret = commitByteArray(counter, uuid);
		clear();
		return ret;
	}

	public float getFloatValue(String getuuid,int status,long counter){ 
		float ret = -1;
		//ret = commitFloat(counter, intArray,intTail,doubleArray,doubleTail,floatArray,floatTail,longArray,longTail,charArray,charTail,byteArray,byteTail);
		clear();
		return ret;
	
	}

	public double getDoubleValue(String getuuid,int status,long counter){ 
		
		double ret = -1;
		ret = commitDouble(counter, intArray,intTail,doubleArray,doubleTail,floatArray,floatTail,longArray,longTail,charArray,charTail,byteArray,byteTail,getuuid);
		clear();
		return ret;
	}

	public char getCharValue(String getuuid,int status,long counter){ 
		char ret=0;
		//ret = commitChar(counter, intArray,intTail,doubleArray,doubleTail,floatArray,floatTail,longArray,longTail,charArray,charTail,byteArray,byteTail);
		clear();
		return ret;
	}
	
	public long getLongValue(String getuuid,int status,long counter){ 
		long ret = -1;
		ret = commitLong(counter, intArray,intTail,doubleArray,doubleTail,floatArray,floatTail,longArray,longTail,charArray,charTail,byteArray,byteTail,getuuid);
		clear();
		return ret;
	
	}

	public byte getByteValue(String getuuid,int status,long counter){ 
		byte ret=0;
		//ret = commitByte(counter, intArray,intTail,doubleArray,doubleTail,floatArray,floatTail,longArray,longTail,charArray,charTail,byteArray,byteTail);
		clear();
		return ret;
	}
	
	
}
