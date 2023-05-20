/* Soot - a J*va Optimization Framework
 * Copyright (C) 2008 Eric Bodden
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.matheclipse.core.builtin.function.Continue;

import polyglot.types.Flags;
import java_cup.internal_error;
import soot.Body;
import soot.BodyTransformer;
import soot.G;
import soot.Local;
import soot.PackManager;
import soot.PatchingChain;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Transform;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.JastAddJ.Signatures.FieldSignature;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.StaticInvokeExpr;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.util.Chain;

public class MyMain {	
   public static Map<String, int[]> hash = new HashMap<>();
   
   
   
   static Map<String, Map<String, List<Value>>> CFMAP = new HashMap<>();  //for storing control-flow variables
   
   static Map<String, Map<String, Integer>> memberVariables = new HashMap<>();        //Class--memberVari
   
   static Map<String, List<String>> staticmemberVariables = new HashMap<>();        //Class--staticmemberVari
   
   static Map<String, Map<String, int[]>> INVOKEMAP = new HashMap<>();  //for sensitive invoke
   
   static Map<String, Map<String, int[]>> senstiveArrayMap = new HashMap<>();  //for sensitive Array
   
   static Map<SootField, Value> OriginFieldCuuidArray = new HashMap<>();
   
   
   public static void main(String[] args) {
	   List<String> argList = new ArrayList<String>(Arrays.asList(args));
	   argList.addAll(Arrays.asList(new String[]{
			"-w",
	   }));
	   
	   PackManager.v().getPack("wjtp").add(new Transform("wjtp.T", new SceneTransformer() {
		   @Override
			protected void internalTransform(String arg0,Map arg1) {
				// TODO Auto-generated method stub
				
			   //for input taint source
			   //Scanner in = new Scanner(System.in);
			
//			   String taintSourceName = "i1";
//			   String taintClassName = "test.TestSort";
//			   String taintMethodName = "sort";
			   
//			   String taintSourceName = "i2";
//			   String taintClassName = "test.Sort_Quick";
//			   String taintMethodName = "quickSort";
//			   String taintSourceName = "$l4";
//			   String taintClassName = "cfhider.WordCount";
//			   String taintMethodName = "main";
//			   String taintSourceName = "i4";
//			   String taintClassName = "cfhider.TeraInputFormat";
//			   String taintMethodName = "writePartitionFile";
//			   
//			   String taintSourceName = "r3";
//			   String taintClassName = "test.ArrayTest";
//			   String taintMethodName = "main";
			   
//			   String taintSourceName = "i0";
//			   String taintClassName = "test.fibb";
//			   String taintMethodName = "fib";
			   
//			   String taintSourceName = "i2";
//			   String taintClassName = "test.Sort_Bubble";
//			   String taintMethodName = "bubbleSort";
			   
//			   String taintSourceName = "i4";
//			   String taintClassName = "test.BinarySearch";
//			   String taintMethodName = "binarySearch";
			   
//			   String taintSourceName = "$z0";
//			   String taintClassName = "cfhider.WordCount$TokenizerMapper";
//			   String taintMethodName = "map";
//			   //String taintSourceName = "i4";
			   //String taintClassName = "cfhider.TeraInputFormat";
			   //String taintMethodName = "writePartitionFile";
			  // String taintSourceName = "d1";
//			   String taintClassName = "cfhider.PiEstimator";
//			   String taintMethodName = "estimate";
//			   
			   
			   //pagerank pegasus.PagerankInitVector$MapStage1  map  $z0
			   //wordcount cfhider.WordCount$TokenizerMapper map $z0
			   //tearsort cfhider.TeraInputFormat writePartitionFile i4
			   
			  
			   String taintClassName = "test.BinarySearch";
			   String taintMethodName = "main";
			   //bubbleSort   binarySearch   quickSort   heapSort   Kmp
			   String taintSourceName = "r1";			   
			   //countPI
//			   String taintSourceName = "d1";			   
			   //fastPower
//			   String taintSourceName = "i2";
			   
//			   String taintClassName = "test.ArrayTest";
//			   String taintMethodName = "main";
//			   String taintSourceName = "r3";

			   
			   /**
			    * for variables
			    */
			   for(SootClass cls:Scene.v().getApplicationClasses()){
				   G.v().out.println("[cf]SooClass:"+cls.toString());
				   
				   Map<String, List<Value>> map = new HashMap<>(); 
				   
				   if (cls.toString().equals("invoker.sgx_invoker") || cls.toString().equals("pegasus.PagerankNaive$PrCounters")) {
						continue;
					}
				   G.v().out.println("[cf] class: "+cls.toString());
				   List<SootMethod> sootMethods = cls.getMethods();
				   
				 // if(cls.toString().equals(taintClassName)){//200
				// G.v().out.println("[cf] success class: "+cls.toString());
				   for (SootMethod sootMethod : sootMethods) { 
					   G.v().out.println("2021class name :"+sootMethod.getName());
					// if (sootMethod.getName().equals(taintMethodName)) {
						 if(!sootMethod.hasActiveBody()){
							   G.v().out.println("method name="+sootMethod.getName());
							   G.v().out.println("[cf] sootMethod:hasNOTActiveBody "+sootMethod.getName());
							   if(sootMethod.getName().equals("findPartition") && cls.toString().equals("cfhider.TeraSort$TotalOrderPartitioner$TrieNode")){
								   continue;
							   }
							   if(sootMethod.getName().equals("print") && cls.toString().equals("cfhider.TeraSort$TotalOrderPartitioner$TrieNode")){
								   continue;
							   }
							  
							   Body body = sootMethod.retrieveActiveBody();
							   List<Value> list = new ArrayList<>();
							   //access the variable of ifStmt
	                          //new GetContrlFlowVariables(body,list);
	                           G.v().out.println("[cf]");
	                           
	                           //access the variable of data analysis
	                           new SetTaintSources(body,list,taintSourceName);
	                           if (!list.isEmpty()) {
	                        	   map.put(sootMethod.getName(), list);
	                           }
	                           //G.v().out.println("[cf] sootMethod-list:"+list.toString());
							}else {
								Body body = sootMethod.getActiveBody();
								//do somethings
								 G.v().out.println("[cf] sootMethod a:"+sootMethod.getName());
								
								List<Value> list = new ArrayList<>();
								G.v().out.println("[cf] sootMethod");
	                           //new GetContrlFlowVariables(body,list);
	                            
	                           new SetTaintSources(body,list,taintSourceName);
	                            //G.v().out.println("[cf] sootMethod-list:"+list.toString());
	                            if (!list.isEmpty()) {
	                         	   map.put(sootMethod.getName(), list);
	                            }
							}
					  }
					// }//198
				   //}//200

                   CFMAP.put(cls.getName(), map);
                   Map<String, int[]> initnullMap = new HashMap<>();
                   INVOKEMAP.put(cls.getName(),initnullMap);   //for init 
                   
                   
                   
                   
                   
                   // init Cuuid
                   SootField sField = new SootField("Cuuid", RefType.v("java.lang.String"),1);
				   cls.addField(sField);
				   
			   }
			   
			   G.v().out.println("[CFMAP]2021:"+CFMAP.toString());
			   
			   Iterator<Map.Entry<String, Map<String, int[]>>> invokeIterator = INVOKEMAP.entrySet().iterator();
			   
			   G.v().out.println("[INVOKEMAP]2021size:"+INVOKEMAP.size());
			   while (invokeIterator.hasNext()) {
				   Map.Entry<String, Map<String, int[]>> entry = invokeIterator.next();
				   Iterator<Map.Entry<String, int[]>> entries1 = entry.getValue().entrySet().iterator();
				   while (entries1.hasNext()) {
					   Map.Entry<String, int[]> entry1 = entries1.next();
					   /*if (entry1.getKey().equals("buildTrie")) {
						   entry1.getValue()[1] =0;
						   entry1.getValue()[4] =0;
					   }*/
					   G.v().out.println("[========INVOKE==before taint========] class = "+entry.getKey()+" method = " + entry1.getKey() + ", Value1 = " + Arrays.toString(entry1.getValue()));
					   
				   }
			   }
			   
			   /**
			    * for taint
			    * 2020/03/27
			    */
			   Chain<SootClass> sList = Scene.v().getApplicationClasses();
			   
			   for (int j = 0; j < sList.size(); j++) {
				   Iterator<SootClass> iterator = sList.iterator();
				   while(iterator.hasNext()){
					   SootClass cls = iterator.next();
						G.v().out.println("[taint]SooClass:"+cls.toString());
						if (cls.toString().equals("invoker.sgx_invoker") || cls.toString().equals("pegasus.PagerankNaive$PrCounters")) {
							continue;
						}
						G.v().out.println("[taint] class: "+cls.toString());
						List<SootMethod> sootMethods = cls.getMethods();
						
						//get all the sensitive method variables list, this class by CFMAP
						Map<String, List<Value>> methList = CFMAP.get(cls.getName());
						G.v().out.println("methList :"+methList.toString());
						
						
						for (int i = 0; i <sootMethods.size(); i++) {
							for (int x= 0; x<sootMethods.size();x++) {
								
								
								SootMethod sootMethod = sootMethods.get(x);
								
									
									if(!sootMethod.hasActiveBody()){
										G.v().out.println("[taint not hasActiveBody] sootMethod: "+sootMethod.getName());
										if(sootMethod.getName().equals("findPartition") && cls.toString().equals("cfhider.TeraSort$TotalOrderPartitioner$TrieNode")){
											   continue;
										   }
										if(sootMethod.getName().equals("print") && cls.toString().equals("cfhider.TeraSort$TotalOrderPartitioner$TrieNode")){
											   continue;
										   }
										Body body = sootMethod.retrieveActiveBody();
										
										if (methList.containsKey(sootMethod.getName())) {
											G.v().out.println(methList.get(sootMethod.getName()).toString());
										}else {
											methList.put(sootMethod.getName(), new ArrayList<Value>());
										}
										
										new TaintAnalysisWrapper(new ExceptionalUnitGraph(body),cls.getName(),sootMethod.getName(),methList.get(sootMethod.getName()),
		                                		memberVariables,staticmemberVariables,CFMAP,INVOKEMAP);
//										new TaintForBackward(new ExceptionalUnitGraph(body),cls.getName(),sootMethod.getName(),methList.get(sootMethod.getName()),
//		                                		memberVariables,staticmemberVariables,CFMAP,INVOKEMAP);
		                                new BackWardAnalysis(new ExceptionalUnitGraph(body),cls.getName(),sootMethod.getName(),methList.get(sootMethod.getName()),
		                                		memberVariables,staticmemberVariables,CFMAP,INVOKEMAP);
									}else {
												G.v().out.println("[taint into] sootMethod: "+sootMethod.getName());
												Body body = sootMethod.getActiveBody();
												//new TaintAnalysisMain(g);
												//G.v().out.println("[taint] hashfortaint: "+hashfortaint.size() +" sMethodsList");
												//List<Value> values = CFLIST.
												if (methList.containsKey(sootMethod.getName())) {
													G.v().out.println(methList.get(sootMethod.getName()).toString());
												}else {
													methList.put(sootMethod.getName(), new ArrayList<Value>());
												}
												G.v().out.println("class name :"+cls.getName());
												G.v().out.println("method name :"+sootMethod.getName());
												G.v().out.println("method list :"+methList.get(sootMethod.getName()));
				                                new TaintAnalysisWrapper(new ExceptionalUnitGraph(body),cls.getName(),sootMethod.getName(),methList.get(sootMethod.getName()),
				                                		memberVariables,staticmemberVariables,CFMAP,INVOKEMAP);
//				                                new TaintForBackward(new ExceptionalUnitGraph(body),cls.getName(),sootMethod.getName(),methList.get(sootMethod.getName()),
//				                                		memberVariables,staticmemberVariables,CFMAP,INVOKEMAP);
				                                G.v().out.println("[CFMAP]2021 After taintAnalysis:"+CFMAP.toString());
				                                new BackWardAnalysis(new ExceptionalUnitGraph(body),cls.getName(),sootMethod.getName(),methList.get(sootMethod.getName()),
				                                		memberVariables,staticmemberVariables,CFMAP,INVOKEMAP);
				                                G.v().out.println("[CFMAP]2021 After BackwardAnalysis:"+CFMAP.toString());
												
									}
								
							}
						}
				   }
			   }
			   
			   
			   //CFMAP.get(taintClassName).get(taintMethodName).remove(4);
			   
			   G.v().out.println("[========CFMAP=======after taint===]:"+CFMAP.toString());
			   G.v().out.println("[==========memberVariables==after taint=========]:"+memberVariables.toString());
			   G.v().out.println("[============staticmemberVariables=====after taint=======]:"+staticmemberVariables.toString());
			   
			   Iterator<Map.Entry<String, Map<String, int[]>>> invokeIterator1 = INVOKEMAP.entrySet().iterator();
			   while (invokeIterator1.hasNext()) {
				   Map.Entry<String, Map<String, int[]>> entry = invokeIterator1.next();
				   Iterator<Map.Entry<String, int[]>> entries1 = entry.getValue().entrySet().iterator();
				   while (entries1.hasNext()) {
					   Map.Entry<String, int[]> entry1 = entries1.next();
					   /*if (entry1.getKey().equals("buildTrie")) {
						   entry1.getValue()[1] =0;
						   entry1.getValue()[4] =0;
					   }*/
					   G.v().out.println("[========INVOKE==========] class = "+entry.getKey()+" method = " + entry1.getKey() + ", Value1 = " + Arrays.toString(entry1.getValue()));
					   
				   }
			   }
			   
			   //removeDuplicationByHashSet
			   //CFMAPmark init
			   Iterator<Map.Entry<String, Map<String, List<Value>>>> entries = CFMAP.entrySet().iterator();
			   while (entries.hasNext()) {
				   Map.Entry<String, Map<String, List<Value>>> entry = entries.next();
				   G.v().out.println("Key class = " + entry.getKey() + ", Value = " + entry.getValue());
				
				   Iterator<Map.Entry<String, List<Value>>> entries1 = entry.getValue().entrySet().iterator();
				   
				   //init
				   
				   while (entries1.hasNext()) {
					   Map.Entry<String, List<Value>> entry1 = entries1.next();
					   G.v().out.println("Key1 method = " + entry1.getKey() + ", Value1 = " + entry1.getValue());
					   
					   removeDuplicationByHashSet(entry1.getValue());
					   
					   Iterator<Value> it_b=CFMAP.get(entry.getKey()).get(entry1.getKey()).iterator();
					   
					   List<Value> temp = new ArrayList<>();
					   while(it_b.hasNext()){
			                Value tValue=it_b.next();
			                
			                /**
			                 * delete not 18 types values
			                 */
			                if (TypeIndex(tValue.getType()) == 100) {
			                	it_b.remove();
			                	continue;
							}
			                
			                String type = tValue.getType().toString();
			          	  	//G.v().out.println("20210720tvalue:"+tValue+"   type:"+type);
			          	  	//过滤掉污染变量数据集合中的常数
			          	  	if(tValue instanceof Constant){
			          	  	G.v().out.println("20210720tvalue:"+tValue+"   type:"+type);
			          	  		it_b.remove();
			          	  	G.v().out.println("tvalue is Constant:"+tValue);
			          	  	}
			          	  	if (tValue instanceof ArrayRef && (type.equals("int")||type.equals("double") || type.equals("float")|| type.equals("long")||type.equals("char") )) {
			          	  		G.v().out.println("want to delete tvalue:"+tValue+"   type:"+type+" "+tValue.getUseBoxes().toString());
			          	  		
			          	  		Iterator<ValueBox> vbIterator =  tValue.getUseBoxes().iterator();
			          	  		while(vbIterator.hasNext()){
			          	  			Value tV = vbIterator.next().getValue();
			          	  			G.v().out.println("v "+tV);
			          	  			if (!entry1.getValue().contains(tV)) {   //r1[i0], r1 is not exist
			          	  			    G.v().out.println("yeah");
			          	  			    temp.add(tV);
			          	  			    break;
			          	  				//entry1.getValue().add(tV);
			          	  			}
			          	  		}	
			          	  		it_b.remove();
			          	  	} 
			            }
					   CFMAP.get(entry.getKey()).get(entry1.getKey()).addAll(temp);
					  
				   }
			   }
			   G.v().out.println("[========CFMAP=======after delete constant===]:"+CFMAP.toString());
			   for(SootClass cls:Scene.v().getApplicationClasses()){
				   G.v().out.println("[add member]SooClass:"+cls.toString());
				   
				   if (cls.toString().equals("invoker.sgx_invoker") || cls.toString().equals("pegasus.PagerankNaive$PrCounters")) {
						continue;
					}
				   G.v().out.println("[add member] class: "+cls.toString());
				   List<SootMethod> sootMethods = cls.getMethods();
				  
				   for (SootMethod sootMethod : sootMethods) {  //foreach every method
					   if(!sootMethod.hasActiveBody()){
						   G.v().out.println("[add member] sootMethod:hasNOTActiveBody "+sootMethod.getName());
						   if(sootMethod.getName().equals("findPartition") && cls.toString().equals("cfhider.TeraSort$TotalOrderPartitioner$TrieNode")){
							   continue;
						   }
						   if(sootMethod.getName().equals("print") && cls.toString().equals("cfhider.TeraSort$TotalOrderPartitioner$TrieNode")){
							   continue;
						   }
						  
						   Body body = sootMethod.retrieveActiveBody();
						   String ClassName = body.getMethod().getDeclaringClass().getName();
							String MethodName = body.getMethod().getName();
							Unit currStmt = null;
							PatchingChain<Unit> units = body.getUnits();//all statements
							Iterator<Unit> scanIt1 = units.snapshotIterator();
							
							while (scanIt1.hasNext()) {//stmt
					    		currStmt = scanIt1.next();
					    		if (currStmt instanceof AssignStmt) {
					    			Value leValue = ((AssignStmt)currStmt).getLeftOp();
					    			Value riValue = ((AssignStmt)currStmt).getRightOp();
									if (CFMAP.get(ClassName).containsKey(MethodName)&&CFMAP.get(ClassName).get(MethodName).contains(leValue)) {
										
										if (TypeIndex(leValue.getType())>6 && TypeIndex(leValue.getType())<19 && riValue instanceof FieldRef) {//don't solve static
											SootField sField = ((FieldRef) riValue).getField();
											Value sValue=riValue;
											G.v().out.println("sField 1:"+sField.getName());
											if (memberVariables.containsKey(ClassName)) {
												if (!memberVariables.get(ClassName).containsKey(sField.getName())) {
													memberVariables.get(ClassName).put(sField.getName(), 100*TypeIndex(sField.getType()));
													CFMAP.get(ClassName).get("<init>").add(sValue);
												}
												//G.v().out.println("sField 2:"+sField);
											}else {
												G.v().out.println("sField 3:"+sField);
												Map<String, Integer> temMap = new HashMap<>();
												temMap.put(sField.getName(), 100*TypeIndex(sField.getType()));
												G.v().out.println("sField 4:"+sField);
												memberVariables.put(ClassName, temMap);
												G.v().out.println("sField 5:"+sField);
											}
										}
									}
								}
							}
						}else {
							Body body = sootMethod.getActiveBody();
							String ClassName = body.getMethod().getDeclaringClass().getName();
							String MethodName = body.getMethod().getName();
							Unit currStmt = null;
							PatchingChain<Unit> units = body.getUnits();//all statements
							Iterator<Unit> scanIt1 = units.snapshotIterator();
							
							while (scanIt1.hasNext()) {//stmt
					    		currStmt = scanIt1.next();
					    		if (currStmt instanceof AssignStmt) {
					    			Value leValue = ((AssignStmt)currStmt).getLeftOp();
					    			Value riValue = ((AssignStmt)currStmt).getRightOp();
									if (CFMAP.get(ClassName).containsKey(MethodName)&&CFMAP.get(ClassName).get(MethodName).contains(leValue)) {
										boolean b1 = TypeIndex(leValue.getType())>6;
										boolean b2 = TypeIndex(leValue.getType())<19;
										boolean b3 = riValue instanceof FieldRef;
										G.v().out.println("===b1  :"+b1 +"  b2 :" + b2 + "  b3:"+ b3);
										if (TypeIndex(leValue.getType())>6 && TypeIndex(leValue.getType())<19 && riValue instanceof FieldRef) {//don't solve static
											SootField sField = ((FieldRef) riValue).getField();
											Value sValue=riValue;
											G.v().out.println("sField 1:"+sField.getName());
											if (memberVariables.containsKey(ClassName)) {
												if (!memberVariables.get(ClassName).containsKey(sField.getName())) {
													int xx = TypeIndex(sField.getType());
													memberVariables.get(ClassName).put(sField.getName(), 100*xx);
													CFMAP.get(ClassName).get("<init>").add(sValue);
												}
												//G.v().out.println("sField 2:"+sField);
											}else {
												Map<String, Integer> temMap = new HashMap<>();
												temMap.put(sField.getName(), 100*TypeIndex(sField.getType()));
												memberVariables.put(ClassName, temMap);
											}
										}
									}
								}
							}
						}
				   }
			   }
			   
			   /**
			    * new design 0503
			    * 
			    * like a statement : "$r9 = <test.test3: int[] P>"
			    * if $r9 is sensitive, We would think the "<test.test3: int[] P>" also is sensitive
			    */
			   
			   //Chain<SootClass> sootList = Scene.v().getApplicationClasses();
			   //new SensitivememberVariables(sootList,CFMAP,memberVariables,staticmemberVariables);
			   
			   
			   
			   G.v().out.println("[========CFMAP2022==========]:"+CFMAP.toString());
			   
			   G.v().out.println("[========INVOKEMAP==========]:"+INVOKEMAP.toString());
			   
			   G.v().out.println("[==========memberVariables===========]:"+memberVariables.toString());
			   
			   G.v().out.println("[============staticmemberVariables============]:"+staticmemberVariables.toString());
			   
//			   Iterator<Map.Entry<String, Map<String, int[]>>> invokeIterator = INVOKEMAP.entrySet().iterator();
//			   while (invokeIterator.hasNext()) {
//				   Map.Entry<String, Map<String, int[]>> entry = invokeIterator.next();
//				   Iterator<Map.Entry<String, int[]>> entries1 = entry.getValue().entrySet().iterator();
//				   while (entries1.hasNext()) {
//					   Map.Entry<String, int[]> entry1 = entries1.next();
//					   if (entry1.getKey().equals("buildTrie")) {
//						   entry1.getValue()[1] =0;
//						   entry1.getValue()[4] =0;
//					   }
//					   G.v().out.println("[========INVOKE==========] class = "+entry.getKey()+" method = " + entry1.getKey() + ", Value1 = " + Arrays.toString(entry1.getValue()));
//					   
//				   }
//			   }
			   
			}
	}));
	    
		PackManager.v().getPack( "jtp" ).add(
				new Transform("jtp.LogInserter", new BodyTransformer() {
				      protected void internalTransform(Body body, String phase, Map options) {
					    phase = "zystble";
					    
					    G.v().out.println("classname20210512: 1");
					    String ClassName = body.getMethod().getDeclaringClass().getName();
					    G.v().out.println("classname20210512:"+ClassName);
						String MethodName = body.getMethod().getName();
						G.v().out.println("MethodName20210512:"+MethodName);
						
						    if(staticmemberVariables.containsKey(ClassName)&&!staticmemberVariables.get(ClassName).isEmpty()||
						    		(CFMAP.containsKey(ClassName)&&CFMAP.get(ClassName).containsKey(MethodName)&&!CFMAP.get(ClassName).get(MethodName).isEmpty())
						    		){
						    	
						    		new Transformer(body,phase,CFMAP,memberVariables,staticmemberVariables,INVOKEMAP,OriginFieldCuuidArray);
////merge update statement part
						    								    	    try {
									new MergeUpdate(body,phase,CFMAP,memberVariables,staticmemberVariables,INVOKEMAP,OriginFieldCuuidArray);
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} 

						    }
//						    else if("<init>".equals(MethodName)){
//						    	G.v().out.println("init menthod");
//						    	new Transformer(body,phase,CFMAP,memberVariables,staticmemberVariables,INVOKEMAP,OriginFieldCuuidArray);
//						    }
						
					  }
				    }) );
		args = argList.toArray(new String[0]);		
		soot.Main.main(args);
	}
   
   
   
   
   /**使用HashSet实现List去重(无序)
   *
   * @param list
   * */
  public static List removeDuplicationByHashSet(List<Value> list) {
      HashSet set = new HashSet(list);
      //把List集合所有元素清空
      list.clear();
      //把HashSet对象添加至List集合
      list.addAll(set);
      return list;
  }
   
  @SuppressWarnings("unused")
	private static int TypeIndex(Type tValue){
		int typeIndex = -1;
		String typeStr = tValue.toString();
		G.v().out.println("<<<<<<>>>>>> in Function TypeIndex typeStr:********"+typeStr+"*************");
		
		if(typeStr.equals("int") || typeStr.equals("short")|| typeStr.equals("boolean")
				||typeStr.equals("byte")||typeStr.equals("char")){
			typeIndex = 1;
		}else if(typeStr.equals("double")){
			typeIndex = 2;
		}else if(typeStr.equals("float")){
			typeIndex = 3;
		}else if(typeStr.equals("long")){
			typeIndex = 5;
		}else if (typeStr.equals("int[]") || typeStr.equals("short[]") ||typeStr.equals("boolean[]")
				||typeStr.equals("byte[]")||typeStr.equals("char[]")) {
			typeIndex = 7;
		}else if (typeStr.equals("double[]")) {
			typeIndex = 8;
		}else if (typeStr.equals("float[]")) {
			typeIndex = 9;
		}else if (typeStr.equals("long[]")) {
			typeIndex = 11;
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
		}else if(typeStr.equals("byte[][]")){
			typeIndex = 18;
		}else if (typeStr.equals("int[][][]")) {
			typeIndex = 13;
		}else if (typeStr.equals("double[][][]")) {
			typeIndex = 14;
		}else if (typeStr.equals("float[][][]")) {
			typeIndex = 15;
		}else if (typeStr.equals("char[][][]")) {
			typeIndex = 16;
		}else if (typeStr.equals("long[][][]")) {
			typeIndex = 17;
		}else if(typeStr.equals("byte[][][]")){
			typeIndex = 18;
		}else{ //TODO: contains type object , boolean , short
			typeIndex = 100;  //for hashcode
		}
		return typeIndex;
	}
}  















   
