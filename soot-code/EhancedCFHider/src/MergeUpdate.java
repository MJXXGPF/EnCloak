import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Box;

import soot.Body;
import soot.BooleanType;
import soot.BriefUnitPrinter;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.G;
import soot.IntType;
import soot.LabeledUnitPrinter;
import soot.Local;
import soot.LongType;
import soot.PatchingChain;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.ShortType;
import soot.SootClass;
import soot.SootField;
import soot.SootFieldRef;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.UnitBox;
import soot.Value;
import soot.ValueBox;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.DoubleConstant;
import soot.jimple.FieldRef;
import soot.jimple.FloatConstant;
import soot.jimple.GotoStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.LongConstant;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NullConstant;
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.toolkits.callgraph.Units;
import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

public class MergeUpdate {
	
	Map<Long,Integer> mergeIndex = new LinkedHashMap<>(); //<is , nums>
	static final int N=10;
	static long counter = 0;
	static Writer indexWriter=null;
    final static double ratio = 0.5;
    
    Map<Long,List<Integer>> addIndex = new HashMap<>();  //<is , List<index,index>>
    Map<String, Integer> typeNum = new HashMap<>(); //<type , nums>
    Map<Long, String> counterType = new HashMap<>(); //<type , nums>
    
    
	static Writer getWriter(){
		String filename = "/tmp/MuSGXindex";
	    if(indexWriter==null){
			try{
				indexWriter = new PrintWriter(filename, "UTF-8");

			} catch (IOException e) {
			   // do something
			}
	    }
		return indexWriter;
	}

	static void closeWriter(){
		if(indexWriter !=null){
			try {
				indexWriter.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			indexWriter = null;
		}
	}
	public static void indexwriter(String content) {
		String file="/tmp/MuSGXindex";
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)));
			out.write(content+"\n");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void updatewriter(Long long1, int num) {
		String file="/tmp/mergeIndex";
		BufferedWriter out = null;
		try {
			G.v().out.println("updatewriter log:" + long1 + " nums:" + num);
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)));
			out.write(long1+"," +num+"\n");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * for invokeSGX index writer
	 */
	static long invokecounter = 1;
	static Writer invokeWriter=null;
	public static void invokeWriter(String content) {
		String file="/tmp/muSGXinvoke";
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)));
			out.write(content+"\n");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
	
	

	@SuppressWarnings("unchecked")
	public MergeUpdate(Body aBody,String phase,Map<String, Map<String, List<Value>>> CFMAP,Map<String,Map<String, Integer>> memberVariables,
			Map<String, List<String>> staticmemberVariables,Map<String, Map<String, int[]>> INVOKEMAP,Map<SootField, Value> OriginFieldCuuidArray) throws IOException
    {
	String declaredClassName = "";
	declaredClassName = aBody.getMethod().getDeclaringClass().getName().toString();
	String declaredFunction = aBody.getMethod().toString();
	String declaredName = aBody.getMethod().getName();
	if(declaredClassName.contains("sgx_invoker")){
		return;
	}
	
	/**
	 * new test , we don't care about clinit
	 */
	if (declaredName.equals("<clinit>")) {
		return;
	}
	
	G.v().out.println("<<!!!!!!START!!!!!!>>20210610start insertting at class: "+declaredClassName);
	G.v().out.println("<<!!!!!!START!!!!!!>>20210610start processing function: "+declaredFunction+";");
	
	PatchingChain<Unit> units = aBody.getUnits();//all statements
//	G.v().out.println("units:"+units.toString());
	Local invokeUUIDLocal = Jimple.v().newLocal("invokeUUIDTest", RefType.v("java.lang.String"));
	Local invokeLineNo = Jimple.v().newLocal("invokeLineNoTest",LongType.v());
    Local getUUIDLocal = Jimple.v().newLocal("getUUIDTest", RefType.v("java.lang.String"));
    Local branchResultLocal = Jimple.v().newLocal("branchInvokeResultTest", BooleanType.v());
    Local sgxObjLocal = Jimple.v().newLocal("sgxInvokerTest", RefType.v("invoker.sgx_invoker"));//sgx object
    aBody.getLocals().add(invokeLineNo);
    aBody.getLocals().add(getUUIDLocal);
    aBody.getLocals().add(invokeUUIDLocal);
    aBody.getLocals().add(branchResultLocal);  //1.insert local boolean branchInvokeResultLocal
    aBody.getLocals().add(sgxObjLocal); //2.insert local reftype invokerLocal
    
    
    
   // LocalGenerator localGenerator = new LocalGenerator(aBody);
   //    Local invokeUUIDLocal = localGenerator.generateLocal
   // 			(RefType.v("java.lang.String")); 
    //aBody.getLocals().add(invokeUUIDLocal);  
    
    
	Unit currProStmt = null;
	
	boolean isInitValueInSgx = false;
	
	boolean flag = true;
	
    HashSet<Value> identifiedLocal = new HashSet<Value>();

    List<Local> localArray = new CopyOnWriteArrayList<Local>();//declaration valuables
    List<Local> tmpLocalArray = new CopyOnWriteArrayList<Local>();//declaration valuables
    Iterator<Local> locali = aBody.getLocals().iterator();
    

    G.v().out.println("**********************Line177");
    /**
     * 2020 04 04
     * First of all, CFMAP, memberVariables and staticmemberVariables let us know what variables are sensitive.
     * Then we will load them into some lists by type.
     */
    // pre deal with the identity
    Iterator<Unit> scanPre = units.snapshotIterator();
    Unit currScanPreStmt = null;
    int linenum=0;
    int start=0;
    int end=0;
    Tag tag = new UpdateTag();
    int currNums = 0; //当前update的add数量
    String currType = null;
    ArrayList<Integer> indexList = new ArrayList<>();
    //第一次循环,标记goto ,记录参数
    while (scanPre.hasNext()) {
    	currScanPreStmt = scanPre.next();
		G.v().out.println("====currScanPre==updatemerge====="+currScanPreStmt);
    	
    	String curStmtStr = currScanPreStmt.toString();
    	
    	

    	//add 语句
    	if(curStmtStr.contains("invoker.sgx_invoker: void add") ){
    		currNums++;
    		
    		int start_index = curStmtStr.indexOf('(')+1;
    		int end_index = curStmtStr.indexOf(')');
    		String type = curStmtStr.substring(start_index,end_index);
    		currType = new String(type);
    		G.v().out.println("====curr type 20210610====="+type);
    		if(typeNum.containsKey(type)){
    			int mapnum = typeNum.get(type);
    			G.v().out.println("====curr index====="+mapnum);
    			indexList.add(mapnum);
    			typeNum.put(type, mapnum+1);
    		}else {
    			typeNum.put(type, 1);
    			indexList.add(0);
			}
    		
    		continue;
    	}
    	
    	if(curStmtStr.contains("updateValueInEnclave") && currNums != 0){//update语句,且存在参数
    		
    		Long Is = Long.parseLong(curStmtStr.substring(curStmtStr.length()-3 , curStmtStr.length()-2));
    		G.v().out.println("----add index ,Is :"+Is);
    		
    		counterType.put(Is, currType);
    		G.v().out.println("====addIndex put:"+Is+" indexList:"+indexList.toString());
    		addIndex.put(Is, indexList);
    		currNums = 0;
    		currType = null;
    		indexList = new ArrayList<>();
//    		continue;
    	}

    	//goto测试
//    	if (currScanPreStmt instanceof GotoStmt) {
//    		UnitBox targetBoxList = ((GotoStmt) currScanPreStmt).getTargetBox();
//    		Unit unitlistList =  targetBoxList.getUnit();
//    		
//    		G.v().out.println("currStmt:"+currScanPreStmt);
//			G.v().out.println("++++GtoStmt==targetBoxList++++"+unitlistList.toString());
//			continue;
//		}
    	
		//goto语句标记
		if (currScanPreStmt instanceof GotoStmt || currScanPreStmt.toString().contains("goto")) {
			currScanPreStmt.addTag(tag);
			G.v().out.println("++++GtoStmt==has tag++++"+currScanPreStmt);
			continue;
		}	
		
		
//    	if(currScanPreStmt.toString().contains("updateValueInEnclave")){
//    		G.v().out.println("20210610meegeupdate="+currScanPreStmt.toString());
//    	}
//    	G.v().out.println("20210610="+currScanPreStmt.toString());
	  
    }
    
    G.v().out.println("++++updateMerge==add_index++++:"+addIndex.toString());
    LabeledUnitPrinter lPrinter = null;
    if (aBody != null) {
		lPrinter = new BriefUnitPrinter(aBody);
	}
    //第二次循环,开始合并
    scanPre = units.snapshotIterator();
    ArrayList<String> updateParamsArrayList = new ArrayList<>();
    ArrayList<String> updateParamsArrayList2 = new ArrayList<>();
    Unit firstStmt = null;  //第一个合并点
    Unit secondStmtUnit = null; //第二个合并点
    long firstIs = 0;
    while(scanPre.hasNext()){
    	currScanPreStmt = scanPre.next();
    	
    	G.v().out.println("----second while, currScanPreStmt: " + currScanPreStmt.toString());
    	
    	currScanPreStmt.toString(lPrinter);
    	String targetLabelString = (String) lPrinter.labels().get(currScanPreStmt);
    	String curStmtStr = currScanPreStmt.toString();
    	
    	//扫描到分段点
    	if (currScanPreStmt.hasTag("updateEnd")) {
    		G.v().out.println("----break point, has Tag: " + currScanPreStmt.toString());
    		firstStmt = null;
    		continue;
		} 	
    	if(targetLabelString != null){
    		G.v().out.println("----break point,label: "+ targetLabelString);
    		if (curStmtStr.contains("updateValueInEnclave")) {
    			firstStmt = currScanPreStmt;
        		int index1 = curStmtStr.lastIndexOf(',');
        		int index2 = curStmtStr.lastIndexOf('L');
        		firstIs = Long.parseLong(curStmtStr.substring(index1+1 , index2).trim());
        		G.v().out.println("----second while ,after break point firstIs :"+firstIs);
			}else {
				firstStmt = null;
			}
    		
//    		G.v().out.println("----second while ,stmt has tag:"+currScanPreStmt);
    		continue;
    	}
    	
    	//第一个合并点的add 语句
    	if(curStmtStr.contains("invoker.sgx_invoker: void add") && firstStmt == null){
    		 int start_index = curStmtStr.indexOf('>')+2;
    		 updateParamsArrayList.add(curStmtStr.substring(start_index, curStmtStr.length()-1));
    		 G.v().out.println("----second while ,updateParamsArrayList :"+updateParamsArrayList.toString());
    		 continue;
    	}
    	
    	
    	//第二个合并点的add语句
    	if(curStmtStr.contains("invoker.sgx_invoker: void add") && firstStmt != null){
   		 int start_index = curStmtStr.indexOf('>')+2;
   		 updateParamsArrayList2.add(curStmtStr.substring(start_index, curStmtStr.length()-1));
   		 G.v().out.println("----second while ,updateParamsArrayList2 :"+updateParamsArrayList2.toString());
   		 continue;
    	}
    	
    	//扫描到第一句update,存入第一个合并点
    	if(curStmtStr.contains("updateValueInEnclave") && firstStmt==null){
    		//临时处理:不合并带参数的update
//    		if (!updateParamsArrayList.isEmpty()) {
//    			updateParamsArrayList.clear();
//				continue;
//			}
    		
    		firstStmt = currScanPreStmt;
    		int index1 = curStmtStr.lastIndexOf(',');
    		int index2 = curStmtStr.lastIndexOf('L');
    		firstIs = Long.parseLong(curStmtStr.substring(index1+1 , index2).trim());
    		G.v().out.println("----second while ,firstIs :"+firstIs);
    		continue;
    	}
    	
    	//第一个合并点和第二个合并点之间存在其他语句  	
    	if((currScanPreStmt instanceof AssignStmt || currScanPreStmt instanceof IdentityStmt) && !updateParamsArrayList.isEmpty() ){
    		Iterator<ValueBox> ubIt=currScanPreStmt.getDefBoxes().iterator();
			while(ubIt.hasNext()){
				ValueBox vBox = ubIt.next();
				Value leftValue = vBox.getValue();
				G.v().out.println("----second while ,left Stmt :"+ currScanPreStmt);
				G.v().out.println("----second while ,leftOp : "+leftValue);
				if(updateParamsArrayList.contains(leftValue.toString())){//第一个合并点的参数在第二个合并点前被更改
					updateParamsArrayList.clear();					
					firstStmt = null;
					break;
				}
			}
			continue;
    	}
    	if(curStmtStr.contains("staticinvoke")){
    		G.v().out.println("staticinvoke in middle");
    		firstStmt = null;
    		continue;
    	}
    	
    	
    	
    	
    	
//    	G.v().out.println("----second while ,second merge :"+curStmtStr.contains("updateValueInEnclave") + " firStmt " + firstStmt);
    	//扫描到第二个合并点
    	if((curStmtStr.contains("updateValueInEnclave") || curStmtStr.contains("getBooleanValue") ||(curStmtStr.contains("get")&&curStmtStr.contains("Value(java.lang.String,"))) && firstStmt!=null){
    		int num = mergeIndex.get(firstIs)==null ? 2: mergeIndex.get(firstIs)+1;
//    		G.v().out.println("----second while ,merge num : "+ num);
//    		G.v().out.println("----first update stmt : "+ firstStmt.toString());
//    		G.v().out.println("----second update stmt : "+ curStmtStr);
    		//合并
    		if(curStmtStr.contains("updateValueInEnclave")){
    			
    			//!!!临时status = 1 的语句不合并
    			char c = curStmtStr.charAt(110);
    			if (c == '1') {
					firstStmt = null;
					continue;
				}
    		
    			G.v().out.println("-------start update merge----- ");
    			 
//    			SootMethod toCall = Scene.v().getMethod ("<invoker.sgx_invoker: void updateValueInEnclave(java.lang.String,int,long)>");
//    			Stmt newUpdateStmt = Jimple.v().newInvokeStmt(
//						Jimple.v().newVirtualInvokeExpr
//				           (sgxObjLocal, toCall.makeRef(), Arrays.asList(getUUIDLocal,IntConstant.v(0),LongConstant.v(firstIs))));
    			units.remove(firstStmt);
    			units.insertAfter(firstStmt, currScanPreStmt);
    			mergeIndex.put(firstIs, num); 
    			G.v().out.println("------merge complete : "+ firstIs +"L "+num);
    			updateParamsArrayList = updateParamsArrayList2;
    			updateParamsArrayList2.clear();
 
    			units.remove(currScanPreStmt);

//    			modifyInstruct(firstIs+num-1); //修改指令
    			
    			
    		}else if (curStmtStr.contains("getBooleanValue")) {
//    			G.v().out.println("-------start getBooleanValue merge----- ");
//    			SootMethod toCall = Scene.v().getMethod ("<invoker.sgx_invoker: boolean getBooleanValue(java.lang.String,long)>");
//    			DefinitionStmt assignStmt = Jimple.v().newAssignStmt(branchResultLocal,
//    					Jimple.v().newVirtualInvokeExpr
//    			           (sgxObjLocal, toCall.makeRef(), Arrays.asList(getUUIDLocal,LongConstant.v(firstIs))));
//    			units.insertBefore(assignStmt, currScanPreStmt);
//    			
////    			units.insertBefore(assignStmt, currScanPreStmt);
//    			mergeIndex.put(firstIs, num);
//    			G.v().out.println("------merge complete : "+ firstIs +"L "+num);
//    			units.remove(firstStmt);
//    			firstStmt = null;
//    			updateParamsArrayList = updateParamsArrayList2;
//    			updateParamsArrayList2.clear();
//    			units.remove(currScanPreStmt);
//    			modifyInstruct(firstIs, index);//修改指令
    			    			  			
			}else{//getValue
				G.v().out.println("-------start getValue merge----- ");
//				int indexStart = curStmtStr.indexOf('<');
//				int indexEnd = curStmtStr.lastIndexOf('>') + 1;
//				SootMethod toCall = Scene.v().getMethod (curStmtStr.substring(indexStart,indexEnd));
//				
//				DefinitionStmt assignStmt=null;
//				
//				Value leftOpValue = ((AssignStmt)currScanPreStmt).getLeftOp();
//				boolean LeftOpIsArrayRef = false;
//				
//				if(leftOpValue instanceof ArrayRef){
//		    		G.v().out.println("rrrrrrrrrrrrrrrrrrrrrrrrr");
//		    		LeftOpIsArrayRef = true;
//				
//		    		if(LeftOpIsArrayRef){
//		    			G.v().out.println("LeftOpIsArrayRef---------------zystble2:"+leftOpValue.toString());
//		    			/*contruct tmpRef */
//		    			Local tmpRef=Jimple.v().newLocal
//							("tmpArrayRef"+String.valueOf(firstIs),leftOpValue.getType());				 
//		    			aBody.getLocals().add(tmpRef);
//		    			localArray.add(tmpRef);    			
//		    			G.v().out.println("tmpValue: "+tmpRef.toString());    	        	
//				
//		    			/*tmpRef init stmt after all identitystmt*/
//		    			assignStmt = initAssignStmt(tmpRef);
//		    			G.v().out.println("newAssignStmt is: "+assignStmt.toString());	        							
//					
////						units.insertAfter(assignStmt, currScanPreStmt);
//
//		    			/*tmpRef assignstmt "tmpArrayRef=getIntValue()"*/
//		    			assignStmt = Jimple.v().newAssignStmt(tmpRef,
//							Jimple.v().newVirtualInvokeExpr
//						          (sgxObjLocal, toCall.makeRef(), Arrays.asList(getUUIDLocal)));
//		    			G.v().out.println("assignStmt===="+assignStmt.toString());
////						units.remove(currScanPreStmt);
////						units.insertBefore(assignStmt, currScanPreStmt);
//						
//		    			/*currstmt "leftop=tmpArrayRef"*/
//		    			((AssignStmt)currProStmt).setRightOp(tmpRef);
//		    		}else{
//		    			G.v().out.println("general stmt--------------");
//		    			G.v().out.println("general============leftOpValue is: "+leftOpValue.toString());	        			
//		    			G.v().out.println("general============curr AssignStmt is: "+currProStmt.toString());	        			
//
//		    			assignStmt = Jimple.v().newAssignStmt(leftOpValue,
//							Jimple.v().newVirtualInvokeExpr
//						          (sgxObjLocal, toCall.makeRef(), Arrays.asList(getUUIDLocal,IntConstant.v(1),LongConstant.v(firstIs))));//IntConstant.v((isNeedCuuidFlag)?1:0)
//		    			G.v().out.println("general============newAssignStmt is: "+assignStmt.toString());	        			
////						units.insertBefore(assignStmt, currScanPreStmt);
////						units.remove(currScanPreStmt);
//		    		}
//				
//				}
				firstStmt = null; 		
    	}	
    }
    }
    //将合并点和合并数量写入文件
  		for(Map.Entry<Long, Integer> entry : mergeIndex.entrySet()){
  			G.v().out.println("----write to file, Is:"+entry.getKey() + " , Nums:" + entry.getValue());
  			updatewriter(entry.getKey(), entry.getValue());
  		}
    
   //----------------------------------------------------------------------------------------------------------
    
    
  		G.v().out.println("**********************Line198");

	
    
    	G.v().out.println("<<!!!!!!END!!!!!!>>20210610start insertting at class: "+declaredClassName);
    	G.v().out.println("<<!!!!!!END!!!!!!>>20210610start processing function: "+declaredFunction+";");
    	
   }
	
	public void modifyInstruct(long is) throws IOException{
		G.v().out.println("<<!!!!!!enter!!!!!!>>start modifyInstruct "+is);
		G.v().out.println("<<modifyInstruct>> addIndex map :"+addIndex);
		//通过addIndex map获得参数位置
		List<Integer> indexList = addIndex.get(is);
		if (indexList != null) {
			int leftIndex = indexList.get(0);
			
			int rightIndex = -1;
			if (indexList.size() ==2) {
				rightIndex = indexList.get(1);
			}
			
//			int indexBound = TypeIndex1(counterType.get(is))*100;
			
			UpdateIndex updateIndex = new UpdateIndex();
			List<String> sgxIndexArrayList = updateIndex.getReaderIndex();
//			List<String> sgxIndexArrayList = getReaderIndex();
			
			G.v().out.println("<<modifyInstruct>> sgxIndexArrayList size:"+sgxIndexArrayList.size());
			
			if (leftIndex < 100 && leftIndex != -1) {
				//读SGXIndex文件,通过is定位到八元组,修改左操作数
				int isInt = (int) is -1;
				for(int i =isInt*8; i < isInt*8+8 ; i++){
					G.v().out.println("index:"+i+"  num:"+sgxIndexArrayList.get(i));
				}
				int left_pos = isInt*8 + 1;
				sgxIndexArrayList.set(left_pos, String.valueOf(leftIndex));
				G.v().out.println("<<modifyInstruct>> left_pos:"+left_pos+" leftIndex:"+leftIndex);
				updateIndex.WriterIndex(sgxIndexArrayList, "/tmp/testIndexOutput");
				
			}
			if (rightIndex != -1 && rightIndex < 100) {
				//读SGXIndex文件,通过is定位到八元组,修改右操作数
				int isInt = (int) is -1;
				for(int i =isInt*8; i < isInt*8+8 ; i++){
					G.v().out.println("index:"+i+"  num:"+sgxIndexArrayList.get(i));
				}
				int right_pos = isInt*8 + 3;
				sgxIndexArrayList.set(right_pos, String.valueOf(leftIndex));
				G.v().out.println("<<modifyInstruct>> left_pos:"+right_pos+" leftIndex:"+leftIndex);
				updateIndex.WriterIndex(sgxIndexArrayList, "/tmp/testIndexOutput");
				
			}
			
			
			
		}
	
	}
	
	
	//赋值语句中的变量赋初始值
		private AssignStmt initAssignStmt(Local l){
			Type t = l.getType();
			G.v().out.println("20210603="+l.toString());
			G.v().out.println("20210603="+t);
			soot.jimple.AssignStmt stmt = null;
			if(t instanceof RefLikeType){
				stmt = soot.jimple.Jimple.v().newAssignStmt(l, NullConstant.v());
				G.v().out.println("20210603="+stmt.toString());
			}
			else if(t instanceof IntType){
				stmt = soot.jimple.Jimple.v().newAssignStmt(l, IntConstant.v(0));
				G.v().out.println("20210603="+stmt.toString());
			}
			else if(t instanceof DoubleType){
				stmt = soot.jimple.Jimple.v().newAssignStmt(l, DoubleConstant.v(0));
			}
			else if(t instanceof FloatType){
				stmt = soot.jimple.Jimple.v().newAssignStmt(l, FloatConstant.v(0));
			}
			else if(t instanceof soot.LongType){
				stmt = soot.jimple.Jimple.v().newAssignStmt(l, LongConstant.v(0));
			}
			else if(t instanceof BooleanType){
				stmt = soot.jimple.Jimple.v().newAssignStmt(l, IntConstant.v(0));
			}
			else if(t instanceof ShortType){
				stmt = soot.jimple.Jimple.v().newAssignStmt(l, IntConstant.v(0));
			}
			else if(t instanceof CharType){
				stmt = soot.jimple.Jimple.v().newAssignStmt(l, IntConstant.v(0));
			}
			else if(t instanceof ByteType){
				stmt = soot.jimple.Jimple.v().newAssignStmt(l, IntConstant.v(0));
			}
			return stmt;
		}
		
		
		private int TypeIndex1(String typeStr){
			int typeIndex = -1;
//			String typeStr = tValue.getType().toString();
			G.v().out.println("in Function TypeIndex typeStr:********"+typeStr+"*************");
			
			if(typeStr.equals("int") || typeStr.equals("short")||typeStr.equals("java.lang.Integer")|| typeStr.equals("boolean")){
				typeIndex = 1;
			}else if(typeStr.equals("double")){
				typeIndex = 2;
			}else if(typeStr.equals("float")){
				typeIndex = 3;
			}else if(typeStr.equals("char")){
				typeIndex = 4;
			}else if(typeStr.equals("long")){
				typeIndex = 5;
			}else if (typeStr.equals("byte")) {
				typeIndex = 6;
			}else if (typeStr.equals("int[]")) {
				typeIndex = 7;
			}else if (typeStr.equals("double[]")) {
				typeIndex = 8;
			}else if (typeStr.equals("float[]")) {
				typeIndex = 9;
			}else if (typeStr.equals("char[]")) {
				typeIndex = 10;
			}else if (typeStr.equals("long[]")) {
				typeIndex = 11;
			}else if (typeStr.equals("byte[]")) {
				typeIndex = 12;
			}else if (typeStr.equals("int[][]")) {
				typeIndex = 13;
			}else if (typeStr.equals("double[][]")) {
				typeIndex = 14;
			}else if (typeStr.equals("float[][]")) {
				typeIndex = 15;
			}else if (typeStr.equals("char[][]")) {
				typeIndex = 16;
			}else if (typeStr.equals("long[][]")) {
				typeIndex = 17;
			}else if (typeStr.equals("byte[][]")) {
				typeIndex = 18;
			}else{ //TODO: contains type object , boolean , short
				G.v().out.println("other Value.getType():"+typeStr);
				typeIndex = -1;  //for hashcode
			}
			return typeIndex;
		}
		
		//读取SGXindex文件存入List
//	    private List<String> getReaderIndex() throws IOException {
//	    	G.v().out.println(">>>>>enter get reader index");
//	        List<String> readerIndex = new ArrayList<>();
//	        //BufferedReader是可以按行读取文件
//	        FileInputStream inputStream = new FileInputStream("/tmp/SGXindex");
//	        
//	        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
//
//	        String str = null;
//	        while((str = bufferedReader.readLine()) != null){
//	            readerIndex.add(str);
//	        }
//	        G.v().out.println(">>>>>enter reade end ");
//	        //close
//	        inputStream.close();
//	        bufferedReader.close();
//
//	        return readerIndex;
//	    }
}
	
	

class UpdateTag implements Tag{
	String name = "updateEnd";
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return this.name;
	}

	@Override
	public byte[] getValue()
			throws AttributeValueException {
		// TODO Auto-generated method stub
		return null;
	}
	
}
