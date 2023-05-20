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
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;


public class SetTaintSources {
	public SetTaintSources(
			Body body,
			List<Value> list,
			String taintSourceName) {
		// TODO Auto-generated constructor stub
		
			List<Value> newlist = new ArrayList<>();
			
			Unit currStmt = null;
			PatchingChain<Unit> units = body.getUnits();//all statements
			Iterator<Unit> scanIt1 = units.snapshotIterator();
			
			while (scanIt1.hasNext()) {
	
	    		currStmt = scanIt1.next();
	    		G.v().out.println("unit20210710 :"+currStmt.toString());
	    		if(currStmt instanceof IfStmt){
	    			//IfStmt
	    			G.v().out.println("If statment");
	    			
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
	    				if(!newlist.contains(tValue)&& tValue.toString().equals(taintSourceName)) {
	    					newlist.add(tValue);
	    					break;
						}
	    				
	    			}
	    		}
	    		if(currStmt instanceof AssignStmt){
	    			G.v().out.println("AssignStmt statment");
	    			
	    			Iterator<ValueBox> ubIt=currStmt.getUseAndDefBoxes().iterator();
	    			    			
	    			while(ubIt.hasNext()){
	    				ValueBox vBox = (ValueBox) ubIt.next();
	    				Value tValue = vBox.getValue();
	    				G.v().out.println("the value="+tValue);
	    				
	    				if (tValue instanceof Constant) {
	    					G.v().out.println("i0 is constant");
							continue;
						}
	    				G.v().out.println("2021="+newlist.contains(tValue)+" taintSourceName="+taintSourceName);
	    				if(!newlist.contains(tValue)&& tValue.toString().equals(taintSourceName)) {
	    					newlist.add(tValue);
	    					G.v().out.println("the value of newlist="+newlist.toString());
	    					break;
						}
	    			}
	    		}
	    		if(currStmt instanceof IdentityStmt){
	    			G.v().out.println("Identity statment");
	    			Iterator<ValueBox> ubIt=currStmt.getUseAndDefBoxes().iterator();
	    			    			
	    			while(ubIt.hasNext()){
	    				ValueBox vBox = (ValueBox) ubIt.next();
	    				Value tValue = vBox.getValue();
	    				G.v().out.println("the value="+tValue);
	    				if (tValue instanceof Constant) {
							continue;
						}
	    				if(!newlist.contains(tValue)&& tValue.toString().equals(taintSourceName)) {
	    					newlist.add(tValue);
	    					break;
						}
	    			}
	    		}
		}
		list.addAll(newlist);
	}
}
