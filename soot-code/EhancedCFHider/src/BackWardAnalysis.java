import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.G;
import soot.Local;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.BackwardFlowAnalysis;
import soot.util.Cons;

//import com.sun.corba.se.impl.orbutil.closure.Constant;



public class BackWardAnalysis{
	public BackWardAnalysis (UnitGraph graph,String clsname,String name,List<Value> sourceList,Map<String, Map<String, Integer>> memberVariables,
			 Map<String, List<String>> staticmemberVariables,Map<String, Map<String, List<Value>>> CFMAP, Map<String, Map<String, int[]>> INVOKEMAP) {
		G.v().out.println("The data isgggg ");
		G.v().out.println(graph.getBody().getMethod());
		BackWardFlow backWardFlow= new BackWardFlow(graph, clsname, name, sourceList, CFMAP, INVOKEMAP);
		G.v().out.println("come here");
		
		
	}

}
class BackWardFlow extends BackwardFlowAnalysis<Unit, Set<Value>>{
	 public Map<Unit, Set<Set<Value>>> taintedSinks;
	    public Set<Value> outSet;
	    public String MethodName;
	    public String ClassName;
	    public Map<String, Map<String, List<Value>>> cfmap;
	    public List<Value> SourceList;
	    public Map<String, Map<String, int[]>> invokemap;
	    public BackWardFlow(DirectedGraph<Unit> graph,String calname,String name,List<Value> sourceList,Map<String, Map<String, List<Value>>> CFMAP, Map<String, Map<String, int[]>> INVOKEMAP) {
	    	super(graph);
			G.v().out.println("The data isgggg "+calname+" "+name+" "+sourceList.toString()+ " "+CFMAP.toString()+"  "+INVOKEMAP.toString());

	    	G.v().out.println("Coming...");
			//tainted = taintMap;
			taintedSinks = new HashMap();
			outSet = new HashSet<>();
			MethodName = name;
			ClassName = calname;
			SourceList = sourceList;
			cfmap = CFMAP;
			invokemap = INVOKEMAP;
			G.v().out.println("doAnalysis Startingqqqqqqqq.....");
			doAnalysis();
			G.v().out.println("doAnalysis endqqqqqqq.....");
			
			//taintMap.putAll(tainted);
	
}
		@Override
		protected Set<Value> newInitialFlow() {
			// TODO Auto-generated method stub
			return new HashSet();
		}
		@Override
		protected Set<Value> entryInitialFlow() {
			// TODO Auto-generated method stub
			return new HashSet();
		}
		@Override
		protected void merge(
				Set<Value> src1,
				Set<Value> src2,
				Set<Value> dest) {
			// TODO Auto-generated method stub
			dest.clear();
			dest.addAll(src1);
			dest.addAll(src2);
			
			
		}
		@Override
		protected void copy(
				Set<Value> src,
				Set<Value> dest) {
			// TODO Auto-generated method stub
			dest.clear();
			dest.addAll(src);
			
			
		}
	

		@Override
		protected void flowThrough(	Set<Value> srcValue,Unit node,Set<Value> destValue) {
			G.v().out.println("srcValue="+srcValue.toString());
			G.v().out.println("destValue:"+destValue.toString());
			Set<Value> in = stillInsSet(node,srcValue);
			Set<Value> out = newTaintByBackWard(node,in);
//			G.v().out.println("in="+in.toString());
//			G.v().out.println("out:"+out.toString());
			
			G.v().out.println("taint data:"+SourceList.toString());
			// TODO Auto-generated method stub
			G.v().out.println("Coming in Backward Analysis");
			

			/*
			 * 通过前向分析得到的每个方法中的污染变量数据集来后向检查每个方法，只需要关注赋值语句和调用语句的定义语句
			 * 在通过前向分析下得到的污染数据集的基础上，若调用函数的定义语句变量在该函数中是控制流变量或者是被控制流变量污染
			 * 的变量则将调用方法的参数列表进行更新
			 */
				if(node instanceof IfStmt){
	    			//IfStmt  判断if语句中变量是否有在污染变量数据集中的,如果存在则将if语句中的所有变量加入污染变量数据集合
		    			G.v().out.println("Have");
		    			Value orgIfCondition = ((IfStmt) node).getCondition();
		    			//orgIfCondition.getUseBoxes().
		    			Iterator<ValueBox> ubIt=orgIfCondition.getUseBoxes().iterator();
		    			List<Value> ifUnitValues = new ArrayList<>();
		    			List<Value> maintainValues = new ArrayList<>();
		     			while(ubIt.hasNext()){
		    				ValueBox vBox = (ValueBox) ubIt.next();
		    				Value tValue = vBox.getValue();
		    				G.v().out.println("the value="+tValue);
		    				if (tValue instanceof Constant) {
								continue;
							}
		    				ifUnitValues.add(tValue);
		    			}
		     			maintainValues.addAll(ifUnitValues);
		     			maintainValues.retainAll(SourceList);
		     			if(maintainValues.size()>0){
		     				for(Value v: ifUnitValues){
		     					if(!SourceList.contains(v)){
		     						SourceList.add(v);
		     				}
		     			}
		    		}
				}
				if(node instanceof AssignStmt) {
					//调用语句为赋值语句
					AssignStmt assn = (AssignStmt)node;
					G.v().out.println("currStmt20210420:"+assn.toString());
					Value leftValue = assn.getLeftOp();
					G.v().out.println("currStmt left value20210420:"+leftValue);
					if(SourceList.contains(leftValue)){
					 if(((AssignStmt) node).containsInvokeExpr()){
						 G.v().out.println("BackWard 20210419==="+((AssignStmt) node).getInvokeExpr().toString());
						 InvokeExpr e = ((AssignStmt) node).getInvokeExpr();
//						 SootMethod m = e.getMethod();
						 SootMethod m = ((AssignStmt) node).getInvokeExpr().getMethod();
						    G.v().out.println("[BackWard taint329]i invoke "+node.toString());
						    G.v().out.println("[BackWard taint328]i invoke "+m.toString());
						   // G.v().out.println("[taint328]i invoke "+((InvokeStmt) node).getInvokeExpr().getArgCount());
						    
						    int[] temp = new int[10];
						    for (int i = 0; i < ((AssignStmt) node).getInvokeExpr().getArgCount(); i++) {
								Value value = ((AssignStmt) node).getInvokeExpr().getArg(i);
								G.v().out.println("value:"+value);
								G.v().out.println("value index:"+temp[i]);
								//调用方法参数列表中该参数在污染变量数据集中,将该变量存储到对应的污染变量数据集中
								if (invokemap.containsKey(m.getDeclaringClass().getName())) {
								    if (invokemap.get(m.getDeclaringClass().getName()).containsKey(m.getName())) {  //if the method is exist
								    	G.v().out.println("in here:"+temp.toString());
								    	//invokemap.get(m.getDeclaringClass().getName()).put(m.getName(), twoArrayadd(invokemap.get(m.getDeclaringClass().getName()).get(m.getName()), temp));
									if(invokemap.get(m.getDeclaringClass().getName()).get(m.getName())[i]==1){
										temp[i]=1;
										G.v().out.println("accessibility value"+value);
										if(!SourceList.contains(value)&&SourceList!=null&&!(value instanceof Constant)){
											
											SourceList.add(value);
										}
									  }
								    }
								    G.v().out.println("[invokemap]invoke:"+m.getName()+"  "+Arrays.toString(invokemap.get(m.getDeclaringClass().getName()).get(m.getName())));
							    }
								G.v().out.println("value index1:"+temp[i]);
								if (SourceList!= null && SourceList.contains(value)){
									temp[i] = 1;
									G.v().out.println("isSourceListContains "+" "+SourceList.contains(value) + " value:"+value+" index:"+i);
								}
							
//								if (value instanceof Constant && TypeIndex(value.getType())!=100) {
//									temp[i] = 1;
//								}
						    }
//						    if (invokemap.containsKey(m.getDeclaringClass().getName())) {
//							    if (invokemap.get(m.getDeclaringClass().getName()).containsKey(m.getName())) {  //if the method is exist
//							    	invokemap.get(m.getDeclaringClass().getName()).put(m.getName(), twoArrayadd(invokemap.get(m.getDeclaringClass().getName()).get(m.getName()), temp));
//								}else {
//									invokemap.get(m.getDeclaringClass().getName()).put(m.getName(),temp);
//								}
//							    G.v().out.println("[invokemap]invoke:"+m.getName()+"  "+Arrays.toString(invokemap.get(m.getDeclaringClass().getName()).get(m.getName())));
//						    }else{
//						    	Map<String, int[]> value = null;
//						    	value.put(m.getName(), temp);
//					
//								invokemap.put(m.getDeclaringClass().getName(), value);
//						    }
						    
						    if(m.getName().equals("println") &&
						       m.getDeclaringClass().getName().equals("java.io.PrintStream") &&
						       containsValues(in, e))

							    G.v().out.println("[isTaintedPublicSink]invoke m ="+m.toString());
								
					 }else{
						//普通赋值语句
						G.v().out.println("普通赋值语句:"+node.toString());
				    	G.v().out.println("调用语句赋值给变量:"+node.toString());
				    	
					    Iterator<ValueBox> ubIt=node.getUseBoxes().iterator();
						while(ubIt.hasNext()){
							ValueBox vBox = ubIt.next();
							Value tmpValue = vBox.getValue();
							G.v().out.println("2021="+SourceList.toString());
							G.v().out.println("2021tmp="+tmpValue);
							if(SourceList!=null&&!SourceList.contains(tmpValue)&&!(tmpValue instanceof Constant)&&TypeIndex(tmpValue.getType())!=100){
								//out.add(tmpValue);
								
								if(tmpValue instanceof Local){
									G.v().out.println("20210720==========tmp="+tmpValue);
									SourceList.add(tmpValue);
							    	break;
								}
						    }
						}    
					}
					}
				
				}
				//纯invoke语句
				if(node instanceof InvokeStmt){

				    InvokeExpr e = ((InvokeStmt) node).getInvokeExpr();
				    SootMethod m = e.getMethod();
				    G.v().out.println("[taint329]i invoke "+node.toString());
				    G.v().out.println("[taint328]i invoke "+m.toString());
				    G.v().out.println("[taint328]i invoke "+((InvokeStmt) node).getInvokeExpr().getArgCount());
				    
				    int[] temp = new int[10];
				    for (int i = 0; i < ((InvokeStmt) node).getInvokeExpr().getArgCount(); i++) {
						Value value = ((InvokeStmt) node).getInvokeExpr().getArg(i);
						G.v().out.println("value:"+value);
						//G.v().out.println("1");
						if (invokemap.containsKey(m.getDeclaringClass().getName())) {
						    if (invokemap.get(m.getDeclaringClass().getName()).containsKey(m.getName())) {  //if the method is exist
						    	G.v().out.println("in here:"+temp.toString());
						    	//invokemap.get(m.getDeclaringClass().getName()).put(m.getName(), twoArrayadd(invokemap.get(m.getDeclaringClass().getName()).get(m.getName()), temp));
							if(invokemap.get(m.getDeclaringClass().getName()).get(m.getName())[i]==1){
								temp[i]=1;
								if(!SourceList.contains(value)&&SourceList!=null&&!(value instanceof Constant)){
									SourceList.add(value);
								}
							  }
						    }
						    G.v().out.println("[invokemap]invoke:"+m.getName()+"  "+Arrays.toString(invokemap.get(m.getDeclaringClass().getName()).get(m.getName())));
					    }
						if (SourceList!= null && SourceList.contains(value)){
							temp[i] = 1;
							G.v().out.println("isSourceListContains "+" "+SourceList.contains(value) + " value:"+value+" index:"+i);
						}
						
//						if (value instanceof Constant && TypeIndex(value.getType())!=100) {
//							temp[i] = 1;
//						}
						
						
						//G.v().out.println("2");
					}
//				    if (invokemap.containsKey(m.getDeclaringClass().getName())) {
//					    if (invokemap.get(m.getDeclaringClass().getName()).containsKey(m.getName())) {  //if the method is exist
//					    	invokemap.get(m.getDeclaringClass().getName()).put(m.getName(), twoArrayadd(invokemap.get(m.getDeclaringClass().getName()).get(m.getName()), temp));
//						}else {
//							invokemap.get(m.getDeclaringClass().getName()).put(m.getName(),temp);
//						}
//					    G.v().out.println("[invokemap]invoke:"+m.getName()+"  "+Arrays.toString(invokemap.get(m.getDeclaringClass().getName()).get(m.getName())));
//				    }
				    
				    if(m.getName().equals("println") &&
				       m.getDeclaringClass().getName().equals("java.io.PrintStream") &&
				       containsValues(in, e))

					    G.v().out.println("[isTaintedPublicSink]invoke m ="+m.toString());
					
				
				}
			}
				

		
		
		
		private Set<Value> newTaintByBackWard(Unit node,Set<Value> in) {
			// TODO Auto-generated method stub
			if(!SourceList.isEmpty()&&SourceList.contains(in)){
				return in;
			}
			return null;
		}
		
		
		private Set<Value> stillInsSet(Unit node, Set<Value> srcValue) {
			// TODO Auto-generated method stub
			return srcValue;
		}
		
		
		protected boolean isPrivateTaint(Value value){
			
			if(!SourceList.isEmpty()&&SourceList.contains(value)){
				return true;
			}else {
				return false;
			}
			
		}
		
	    
	    // It would be sweet if java had a way to duck type, but it doesn't so we have to do this.
	    protected boolean containsValues(Collection<Value> vs, Object s) {
		for(Value v : vs)
		    if(containsValue(v, s))
			return true;
		return false;
	    }

	    
	    protected boolean containsValue(Value v, Object s) {
	    	try {
	    	    Method m = s.getClass().getMethod("getUseBoxes");
	    	    for(ValueBox b : (Collection<ValueBox>) m.invoke(s))
	    		if(b.getValue().equals(v))
	    		    return true;
	    	    return false;
	    	} catch(Exception e) {
	    	    return false;
	    	}
	    }
	    
	    private boolean HasIntersection(List<Value> list1,List<Value> list2) {
	    	
			List<Value> list = new ArrayList<>();
			list.addAll(list1);
			list.retainAll(list2);
 		
 			if(list.size()>0){
 				return true;
 			}
 			return false;
		}
	    
	    @SuppressWarnings("unused")
	    private static int TypeIndex(Type tValue){
			int typeIndex = -1;
			String typeStr = tValue.toString();
			G.v().out.println("<<<<<<ZYSTBLE>>>>>> in Function TypeIndex typeStr:********"+typeStr+"*************");
			
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
	  
	    
	    protected int[] twoArrayadd(int a[],int b[]){
	    	int c[] = new int[10]; 
	    	for (int i = 0; i < a.length; i++) {
	    		c[i] = a[i] +b[i];
	    		if (c[i] > 1) {
					c[i] = 1;
					
				}
	    		//G.v().out.println("temp value20210420:"+c[i]);
			}
	    	return c;
	    }
			
			
}