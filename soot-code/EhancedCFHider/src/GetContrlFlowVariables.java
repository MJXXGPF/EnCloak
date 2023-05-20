import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import soot.Body;
import soot.G;
import soot.PatchingChain;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.IfStmt;
import soot.jimple.InvokeStmt;


public class GetContrlFlowVariables {
	
	public GetContrlFlowVariables(Body aBody,List<Value> cfvariables){
		
		//G.v().out.println("[GetContrlFlowVariables] cfvariables:"+cfvariables.getClassName()+" "+cfvariables.getMethodNameString());
		
		List<Value> list = new ArrayList<>();
		
		Unit currStmt = null;
		PatchingChain<Unit> units = aBody.getUnits();//all statements
		Iterator<Unit> scanIt1 = units.snapshotIterator();
		
		while (scanIt1.hasNext()) {

    		currStmt = scanIt1.next();
    		G.v().out.println("If statment");
    		if(currStmt instanceof IfStmt){
    			//IfStmt
    			G.v().out.println("Have");
    			Value orgIfCondition = ((IfStmt) currStmt).getCondition();
    			//orgIfCondition.getUseBoxes().
    			Iterator<ValueBox> ubIt=orgIfCondition.getUseBoxes().iterator();
    			    			
    			while(ubIt.hasNext()){
    				ValueBox vBox = (ValueBox) ubIt.next();
    				Value tValue = vBox.getValue();
    				G.v().out.println("the value="+tValue);
    				if (tValue instanceof Constant) {
						continue;
					}
    				if(!list.contains(tValue)) {
    					list.add(tValue);
					}
    				
    			}
    		}
    		/**
    		 * we think the Invokestmts which we defined, these "int~long[]" type variables are sensitive
    		 */
//    		if (currStmt instanceof InvokeStmt) {
//    			List<Value> values = ((InvokeStmt) currStmt).getInvokeExpr().getArgs();
//    			for(Value v:values){
//    				if (TypeIndex(v) != 100 &&!(v instanceof Constant)) {
//    					if (!list.contains(v)) {
//    						G.v().out.println("[W GET]:"+v);
//							list.add(v);
//						}
//					}
//    			}
//			}
//    		if (currStmt instanceof AssignStmt) {
//    			if(((AssignStmt)currStmt).containsInvokeExpr()){
//	    			List<Value> values = ((AssignStmt) currStmt).getInvokeExpr().getArgs();
//	    			for(Value v:values){
//	    				if (TypeIndex(v) != 100 && !(v instanceof Constant)) {
//	    					if (!list.contains(v)) {
//	    						G.v().out.println("[W a GET]:"+v);
//								list.add(v);
//							}
//						}
//	    			}
//    			}
//			}
    		
    		
		}
		cfvariables.addAll(list);
	}
	
	
}
