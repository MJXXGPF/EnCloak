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
import soot.SootField;
import soot.SootFieldRef;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.StaticFieldRef;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;


public class TaintAnalysisWrapper {
	 public TaintAnalysisWrapper(UnitGraph graph,String clsname,String name,List<Value> sourceList,Map<String, Map<String, Integer>> memberVariables,
			 Map<String, List<String>> staticmemberVariables,Map<String, Map<String, List<Value>>> CFMAP, Map<String, Map<String, int[]>> INVOKEMAP) {
		 
		 
			 G.v().out.println("clasname="+clsname.toString());
			 G.v().out.println("methodname="+name.toString());
		 
		    G.v().out.println("sourcelist2021="+sourceList.toString());
			TaintAnalysis analysis = new TaintAnalysis(graph,clsname,name,sourceList,CFMAP,INVOKEMAP);
			
			/**
			 * deal with the taint values, put them into box which we prepared
			 */
			 G.v().out.println("analysis.outSet.size():"+analysis.outSet.size());
			 
			 
			 
				if(analysis.outSet.size() > 0) {
					Iterator<Value> i = analysis.outSet.iterator();
					while(i.hasNext()){
						Value value = i.next();
						 G.v().out.println("[TAINT]out:"+value.toString()+" type:"+value.getType());
						 if (value instanceof StaticFieldRef) {
							 G.v().out.println("StaticFieldRef:********"+value.toString()+"*************");
							 SootField sField = ((StaticFieldRef) value).getField();
								String classname = sField.getDeclaringClass().getName();//classname
								if (staticmemberVariables.containsKey(classname)) {
									if (!staticmemberVariables.get(classname).contains(sField.getName())) {   //to avoid repeating
										staticmemberVariables.get(classname).add(sField.getName());
									}
								}else {
									List<String> memVariList = new ArrayList<>();
									memVariList.add(sField.getName());
									staticmemberVariables.put(classname, memVariList);
								}
						 }
						 /*else if(value instanceof ArrayRef){
							G.v().out.println("ArrayRef:********"+value.toString()+"*************");
						 }
						 */
						 else if(value instanceof FieldRef){
								SootField sField = ((FieldRef) value).getField();
								String classname = sField.getDeclaringClass().getName();//classname
								G.v().out.println("FieldRef:********"+value.toString()+"*************");
								G.v().out.println("sField:********"+sField.getName()+"*************");
								G.v().out.println("classname:********"+classname.toString()+"*************");
								if (memberVariables.containsKey(classname)) {
									if (!(memberVariables.get(classname).containsKey(sField.getName()))) {   //to avoid repeating
										int xx = TypeIndex(sField.getType());
										memberVariables.get(classname).put(sField.getName(), (xx>=7)?100*xx:10+10*xx+memberVariables.size());
									}
								}else {
									
									Map<String, Integer> memVar = new HashMap<>();
									int xx = TypeIndex(sField.getType());
									memVar.put(sField.getName(),(xx>=7)?100*xx:10+10*xx+memberVariables.size());
									memberVariables.put(classname, memVar);
								}
						 }else {
							 if (!sourceList.contains(value)|| sourceList.isEmpty()) {
								 sourceList.add(value);
							}
							 
						}
					}
				}
		    }
	 @SuppressWarnings("unused")
		private int TypeIndex(Type tValue){
			int typeIndex = -1;
			String typeStr = tValue.toString();
			G.v().out.println("<<<<<<>>>>>> in Function TypeIndex typeStr:********"+typeStr+"*************");
			
			if(typeStr.equals("int") || typeStr.equals("short")|| typeStr.equals("boolean")){
				typeIndex = 1;
			}else if(typeStr.equals("double")){
				typeIndex = 2;
			}else if(typeStr.equals("float")){
				typeIndex = 3;
			}else if(typeStr.equals("char")){
				typeIndex = 4;
			}else if(typeStr.equals("long")){
				typeIndex = 5;
			}else if(typeStr.equals("byte")){
				typeIndex = 6;
			}else if (typeStr.equals("int[]") || typeStr.equals("short[]") ||typeStr.equals("boolean[]")) {
				typeIndex = 7;
			}else if (typeStr.equals("double[]")) {
				typeIndex = 8;
			}else if (typeStr.equals("float[]")) {
				typeIndex = 9;
			}else if (typeStr.equals("char[]")) {
				typeIndex = 10;
			}else if (typeStr.equals("long[]")) {
				typeIndex = 11;
			}else if(typeStr.equals("byte[]")){
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
interface GetUseBoxes {
    public List<ValueBox> getUseBoxes();
}
class TaintAnalysis extends ForwardFlowAnalysis<Unit, Set<Value>> {
    public Map<Unit, Set<Set<Value>>> taintedSinks;
    public Set<Value> outSet;
    public String MethodName;
    public String ClassName;
    public Map<String, Map<String, List<Value>>> cfmap;
    public List<Value> SourceList;
    public Map<String, Map<String, int[]>> invokemap;
    public TaintAnalysis(UnitGraph graph,String calname,String name,List<Value> sourceList,Map<String, Map<String, List<Value>>> CFMAP, Map<String, Map<String, int[]>> INVOKEMAP) {
		super(graph);
		//tainted = taintMap;
		taintedSinks = new HashMap();
		outSet = new HashSet<>();
		MethodName = name;
		ClassName = calname;
		SourceList = sourceList;
		cfmap = CFMAP;
		invokemap = INVOKEMAP;
		doAnalysis();
		
		//taintMap.putAll(tainted);
    }

    protected Set<Value> newInitialFlow() {
	return new HashSet();
    }

    protected Set<Value> entryInitialFlow() {
	return new HashSet();
    }

    protected void copy(Set<Value> src, Set<Value> dest) {
		dest.removeAll(dest);
		dest.addAll(src);
    }

    // Called after if/else blocks, with the result of the analysis from both
    // branches.
    protected void merge(Set<Value> in1, Set<Value> in2, Set<Value> out) {
		out.removeAll(out);
		out.addAll(in1);
		out.addAll(in2);
    }

    protected void flowThrough(Set<Value> in, Unit node, Set<Value> out) {
    	G.v().out.println("20210427node :"+node.toString());
		Set<Value> filteredIn = stillTaintedValues(in, node);
		G.v().out.println("20210427node in :"+filteredIn);
		Set<Value> newOut = newTaintedValues(filteredIn, node);
		G.v().out.println("20210427node out :"+newOut);
		
	
		out.removeAll(out);
		out.addAll(filteredIn);
		out.addAll(newOut);
		if (in.size()>0) {
			G.v().out.println("flowThrough in :"+in);
			G.v().out.println("flowThrough node:"+node);
			G.v().out.println("flowThrough out:"+out);
		}
		if (!out.isEmpty()) {
			//for(Value value : out){
			//	outSet.add(value);
			//}
			outSet.addAll(out);
		}
		//tainted.put(methodname, out);
		
		if(isTaintedPublicSink(node, in)) {
		    if(!taintedSinks.containsKey(node))
		    	taintedSinks.put(node, new HashSet());
	
		    taintedSinks.get(node).add(in);
		}
    }

    protected Set<Value> stillTaintedValues(Set<Value> in, Unit node) {
    	return in;
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
	    // I'm so sorry.
	    Method m = s.getClass().getMethod("getUseBoxes");
	    for(ValueBox b : (Collection<ValueBox>) m.invoke(s))
		if(b.getValue().equals(v))
		    return true;
	    return false;
	} catch(Exception e) {
	    return false;
	}
    }

    protected Set<Value> newTaintedValues(Set<Value> in, Unit node) {
	Set<Value> out = new HashSet();

	if(containsValues(in, node)) {
	    if(node instanceof AssignStmt) {
	    	 AssignStmt assn = (AssignStmt) node;
	    	 G.v().out.println("A "+assn.getRightOp()+" "+assn.getLeftOp()+" "+out.contains(assn.getLeftOp()));
	    	 if (assn.getLeftOpBox().getValue() instanceof ArrayRef && !(assn.getRightOp() instanceof Constant)) {
			    	G.v().out.println("assn.getLeftOpBox().getValue() ArrayRef"+assn.getLeftOpBox().getValue());
			    	G.v().out.println("assn.getLeftOpBox().getValue() ArrayRef="+((ArrayRef) assn.getLeftOpBox().getValue()).getBase());
			    	out.add(((ArrayRef) assn.getLeftOpBox().getValue()).getBase());// mark the array name or'base'
			 }else if (assn.getRightOp() instanceof ArrayRef && TypeIndex((assn.getRightOp()).getType())==100) { //if the "$r15 = r7[$i13]"  r7 is a object
				 return out;
			 }else {
				G.v().out.println("wrong in here:"+((AssignStmt) node).getLeftOpBox().getValue());
				 out.add(((AssignStmt) node).getLeftOpBox().getValue());
			    	//G.v().out.println("======out====:"+((AssignStmt) node).getLeftOpBox().getValue());
			}
	    	 
	    } else if(node instanceof IfStmt) {
	    	IfStmt i = (IfStmt) node;
		if(i.getTarget() instanceof AssignStmt)
		    out.add(((AssignStmt) i.getTarget()).getLeftOpBox().getValue());
	    }
	} else if(node instanceof AssignStmt) {
	    AssignStmt assn = (AssignStmt) node;
	    //normal taint source
	    if(((AssignStmt) node).containsInvokeExpr()){ // add on 4.16 2021//调用语句
				SootMethod m = ((AssignStmt) node).getInvokeExpr().getMethod();
				G.v().out.println("调用语句赋值给变量:"+node.toString());
				
				if (invokemap.containsKey(m.getDeclaringClass().getName()) && invokemap.get(m.getDeclaringClass().getName()).containsKey(m.getName())) {
					G.v().out.println("Invokestmt:"+m.getName());
					for (int i = 0; i < 10; i++) {
					if (invokemap.get(m.getDeclaringClass().getName()).get(m.getName())[i] == 1) {
						Value value = ((AssignStmt) node).getInvokeExpr().getArg(i);
								if(cfmap.containsKey(ClassName) && cfmap.get(ClassName).containsKey(MethodName)){
									if (!cfmap.get(ClassName).get(MethodName).contains(value)) {
										cfmap.get(ClassName).get(MethodName).add(value);
									}
								}else {
									if (cfmap.containsKey(ClassName)) {
										List<Value> temList = new ArrayList<>();
										temList.add(value);
										cfmap.get(ClassName).put(MethodName, temList);
									}else {
										G.v().out.println("[Wrong on 5.26 for TeraSort]");
									}
								}
						
						out.add(value);
					}
				}
			}
			
			
	    }else{//普通复制语句
	    	G.v().out.println("普通复制语句1112:"+node.toString());
	    	
		    Iterator<ValueBox> ubIt=node.getUseBoxes().iterator();
			while(ubIt.hasNext()){
				ValueBox vBox = ubIt.next();
				Value tmpValue = vBox.getValue();
				if(isPrivateSource(tmpValue)){
					G.v().out.println("======out====:"+assn.getLeftOpBox().getValue());
					out.add(assn.getLeftOpBox().getValue());
			    	break;
			    }
			}    
	    }
	    
	    
	    
	} else if(node instanceof IdentityStmt){   //for invoke source
		IdentityStmt idStmt = (IdentityStmt)node;
		
		
		if (idStmt.getRightOp().toString().startsWith("@parameter")) {
			
		
			int index = Integer.parseInt(idStmt.getRightOp().toString().substring(10, 11));
			G.v().out.println("[idStmt]iiiiiii "+idStmt.toString()+"  index:"+index);
			G.v().out.println("ClassName:"+ClassName);
			G.v().out.println("MethodName:"+MethodName);
			if (invokemap.containsKey(ClassName) && invokemap.get(ClassName).containsKey(MethodName)) {
				
				G.v().out.println("aaaa:"+invokemap.get(ClassName).get(MethodName)[index]);
				if (invokemap.get(ClassName).get(MethodName)[index] == 1) {
					
					G.v().out.println("[taint328]MethodName "+MethodName+" index"+index);
					
					Value value = idStmt.getLeftOp();
					G.v().out.println("[taint328]value "+value);
					
//							if (!cfmap.get(ClassName).get(MethodName).contains(value)) {
//								
//								cfmap.get(ClassName).get(MethodName).add(value);
//								G.v().out.println("[taint328]iiiiiii "+cfmap.toString());
//							}
							if(cfmap.containsKey(ClassName) && cfmap.get(ClassName).containsKey(MethodName)){
								if (!cfmap.get(ClassName).get(MethodName).contains(value)) {
									cfmap.get(ClassName).get(MethodName).add(value);
									G.v().out.println("[taint328]iiiiiii "+cfmap.toString());
								}
							}else {
								G.v().out.println("Here");
								if (cfmap.containsKey(ClassName)) {
									List<Value> temList = new ArrayList<>();
									temList.add(value);
									cfmap.get(ClassName).put(MethodName, temList);
								}else {
									G.v().out.println("[Wrong on 5.26 for TeraSort]");
								}
							}
					
					out.add(idStmt.getLeftOp());
				}
			}
			
		}		
						 //
					
				
			
	}

	return out;
    }

    protected boolean isPrivateSource(Value u) {
    //G.v().out.println("[taint]Value:"+u.toString());
	/*if(u instanceof VirtualInvokeExpr) {
	    VirtualInvokeExpr e = (VirtualInvokeExpr) u;
	    SootMethod m = e.getMethod();
	    G.v().out.println("[taint]sootmethod:"+m.getName());
	    if(m.getName().equals("readLine") &&
	       m.getDeclaringClass().getName().equals("(java.io.Console"))
		return true;
	}*/
	    
	    /*if (u.toString().equals("")) {//<test.Sort_Quick: int a><test.BinarySearch: int a>
	    	G.v().out.println("[taint]real Value:"+u.toString());
	    	return true;
		}*/
    	//G.v().out.println("[taint source] SourceList:"+SourceList.toString());
    	G.v().out.println("[taint source] u:"+u.toString());
    	
    	G.v().out.println("SourceList:"+SourceList.toString());
	    if (SourceList!= null &&SourceList.contains(u)) {
	    		
		    G.v().out.println("[taint source] Value:"+u.toString());
			return true;
		}
	    /**
	     * add on 4.14 2021 for taint source
	     */
//	    if (ClassName.equals("test.Sort_Bubble") && MethodName.equals("bubbleSort") && u.toString().equals("i0")) {
//	    	return true;
//		}
	    
		return false;
    }

    protected boolean isTaintedPublicSink(Unit u, Set<Value> in) {
    	
//    	if (u instanceof IdentityStmt && ((IdentityStmt) u).getRightOp().toString().startsWith("@parameter")) {
//    		G.v().out.println("[taint527] identity "+u.toString());
//    		if (SourceList.contains(((IdentityStmt) u).getLeftOp())) {
//    			G.v().out.println("[taint527] identity senstivie"+((IdentityStmt) u).getLeftOp());
//    			int index = Integer.parseInt(((IdentityStmt) u).getRightOp().toString().substring(10, 11));
//    			int[] temp = new int[10];
//    			temp[index] = 1;
//    			if (invokemap.containsKey(ClassName)) {
//    			    if (invokemap.get(ClassName).containsKey(MethodName)) {  //if the method is exist
//    			    	invokemap.get(ClassName).put(MethodName, twoArrayadd(invokemap.get(ClassName).get(MethodName), temp));
//    				}else {
//    					invokemap.get(ClassName).put(MethodName,temp);
//    				}
//    			    //G.v().out.println("[invokemap]invoke:"+m.getName()+"  "+Arrays.toString(invokemap.get(m.getDeclaringClass().getName()).get(m.getName())));
//    		    }
//			}
//		}
    	
    	/**
    	 * add on 4.16 2021
    	 */
    	if (u instanceof IdentityStmt) {
    		G.v().out.println("currStmt20210423:"+u.toString());
    		IdentityStmt idStmt = (IdentityStmt)u;
    		Value left_value = idStmt.getLeftOp();
    		int[] temp = new int[10];
    		if (idStmt.getRightOp().toString().startsWith("@parameter")) {
	    		if (SourceList!= null && SourceList.contains(left_value)){
	    			int index = Integer.parseInt(idStmt.getRightOp().toString().substring(10, 11));
	    			G.v().out.println("this index is invalid and it's value:"+index);
	    			temp[index] = 1;
	    			
	    		}
    		
    		
	    		if (invokemap.containsKey(ClassName)) {
				    if (invokemap.get(ClassName).containsKey(MethodName)) {  //if the method is exist
				    	invokemap.get(ClassName).put(MethodName, twoArrayadd(invokemap.get(ClassName).get(MethodName), temp));
					}else {
						invokemap.get(ClassName).put(MethodName,temp);
					}
				   // G.v().out.println("[invokemap]invoke:"+m.getName()+"  "+Arrays.toString(invokemap.get(m.getDeclaringClass().getName()).get(m.getName())));
			    }
    		}
    	}
    	
		if(u instanceof InvokeStmt) {
		    InvokeExpr e = ((InvokeStmt) u).getInvokeExpr();
		    SootMethod m = e.getMethod();
		    G.v().out.println("[taint329]i invoke "+u.toString());
		    G.v().out.println("[taint328]i invoke "+m.toString());
		    G.v().out.println("[taint328]i invoke "+((InvokeStmt) u).getInvokeExpr().getArgCount());
		    
		    int[] temp = new int[10];
		    for (int i = 0; i < ((InvokeStmt) u).getInvokeExpr().getArgCount(); i++) {
				Value value = ((InvokeStmt) u).getInvokeExpr().getArg(i);
				G.v().out.println("value:"+value);
				
				
				for(Value v: outSet){
					if (v instanceof ArrayRef) {
						G.v().out.println("outSet:"+v);
						G.v().out.println("outSet base:"+((ArrayRef) v).getBase());
						if (value.equals(((ArrayRef) v).getBase())) {
							G.v().out.println("match value:"+v);
							temp[i] = 1;
							break;
						}
					}
				}
				
				G.v().out.println("isoutSetContains "+" "+outSet.contains(value) + " value:"+value+" index:"+i);
				
				if (outSet.contains(value)) {                // this invoke and this arg number is sensitive
					temp[i] = 1;
				}
				//G.v().out.println("1");
				if (SourceList!= null && SourceList.contains(value)){
					temp[i] = 1;
					G.v().out.println("isSourceListContains "+" "+SourceList.contains(value) + " value:"+value+" index:"+i);
				}
				
//				if (value instanceof Constant && TypeIndex(value.getType())!=100) {
//					temp[i] = 1;
//				}
				
				//G.v().out.println("2");
			}
		    //Map<String, int[]> tempMap = new HashMap<>();
		    //tempMap.put(m.getName(), temp);
		    //invokemap.put(m.getDeclaringClass().getName(), tempMap);
		    //G.v().out.println("invoke "+" "+m.getDeclaringClass().getName()+" "+invokemap.containsKey(m.getDeclaringClass().getName()));
		    
		    if (invokemap.containsKey(m.getDeclaringClass().getName())) {
			    if (invokemap.get(m.getDeclaringClass().getName()).containsKey(m.getName())) {  //if the method is exist
			    	invokemap.get(m.getDeclaringClass().getName()).put(m.getName(), twoArrayadd(invokemap.get(m.getDeclaringClass().getName()).get(m.getName()), temp));
				}else {
					invokemap.get(m.getDeclaringClass().getName()).put(m.getName(),temp);
				}
			    G.v().out.println("[invokemap]invoke:"+m.getName()+"  "+Arrays.toString(invokemap.get(m.getDeclaringClass().getName()).get(m.getName())));
		    }
		    
		    if(m.getName().equals("println") &&
		       m.getDeclaringClass().getName().equals("java.io.PrintStream") &&
		       containsValues(in, e))

			    G.v().out.println("[isTaintedPublicSink]invoke m ="+m.toString());
			return true;
		}
		if (u instanceof AssignStmt) {
			if(((AssignStmt) u).containsInvokeExpr()){
				SootMethod m = ((AssignStmt) u).getInvokeExpr().getMethod();
				
				G.v().out.println("[taint328]a invoke "+m.toString());
			    G.v().out.println("[taint328]a invoke "+((AssignStmt) u).getInvokeExpr().getArgCount());
			    
			    
			    int[] temp = new int[10];
			    for (int i = 0; i < ((AssignStmt) u).getInvokeExpr().getArgCount(); i++) {
					Value value = ((AssignStmt) u).getInvokeExpr().getArg(i);
					
					
					for(Value v: outSet){
						if (v instanceof ArrayRef) {
							G.v().out.println("outSet:"+v);
							G.v().out.println("outSet base:"+((ArrayRef) v).getBase());
							if (value.equals(((ArrayRef) v).getBase())) {
								G.v().out.println("match value:"+v);
								temp[i] = 1;
								break;
							}
						}
					}
					
					
					
					G.v().out.println("assi isoutSet "+" "+outSet.contains(value) + " value:"+value+" index:"+i);
					if (outSet.contains(value)) {                // this invoke and this arg number is sensitive
						temp[i] = 1;
					}
					if (SourceList!= null && SourceList.contains(value)){
						temp[i] = 1;
						G.v().out.println("assi isSourceListContains "+" "+SourceList.contains(value) + " value:"+value+" index:"+i);
					}
					
//					if (value instanceof Constant && TypeIndex(value.getType())!=100) {
//						temp[i] = 1;
//					}
				}
			    
			    if (invokemap.containsKey(m.getDeclaringClass().getName())) {
				    if (invokemap.get(m.getDeclaringClass().getName()).containsKey(m.getName())) {  //if the method is exist
				    	invokemap.get(m.getDeclaringClass().getName()).put(m.getName(), twoArrayadd(invokemap.get(m.getDeclaringClass().getName()).get(m.getName()), temp));
					}else {
						invokemap.get(m.getDeclaringClass().getName()).put(m.getName(),temp);
					}
				    G.v().out.println("[invokemap]invoke:"+m.getName()+"  "+Arrays.toString(temp));
			    }
				
				 if(containsValues(in, ((AssignStmt) u).getInvokeExpr()))
					 G.v().out.println("[isTaintedPublicSink]assign:"+m.toString());
						return true;
			}
			
		}
		//add new  2021
		if(u instanceof IfStmt){
			//IfStmt  判断if语句中变量是否有在污染变量数据集中的,如果存在则将if语句中的所有变量加入污染变量数据集合
    			G.v().out.println("Have");
    			G.v().out.print("Stmt if :"+u.toString());
    			Value orgIfCondition = ((IfStmt) u).getCondition();
    			//orgIfCondition.getUseBoxes().
    			G.v().out.print("Stmt if value:"+orgIfCondition.toString());
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
     			G.v().out.println("maintainValues.size"+maintainValues.size());
     			G.v().out.println("ifValues"+ifUnitValues.size());
     			if(maintainValues.size()>0){
     				for(Value v: ifUnitValues){
     					if(!SourceList.contains(v) && !(v instanceof Constant)){
     						G.v().out.println("add ifstmt value:"+v);
     						SourceList.add(v);
     					}
     				}
     			}
		}
		
		return false;
    }
    
    //tools 0404
    protected int[] twoArrayadd(int a[],int b[]){
    	int c[] = new int[10]; 
    	for (int i = 0; i < a.length; i++) {
    		c[i] = a[i] +b[i];
    		if (c[i] > 1) {
				c[i] = 1;
			}
		}
    	return c;
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
