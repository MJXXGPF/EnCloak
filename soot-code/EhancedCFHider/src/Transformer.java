import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import java_cup.internal_error;

import org.matheclipse.core.interfaces.IRational;
import org.omg.CORBA.PRIVATE_MEMBER;

import com.sun.java.swing.plaf.windows.WindowsTreeUI.ExpandedIcon;

import soot.*;
import soot.JastAddJ.ArrayAccess;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.AddExpr;
import soot.jimple.AnyNewExpr;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;
import soot.jimple.CastExpr;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.DoubleConstant;
import soot.jimple.Expr;
import soot.jimple.FieldRef;
import soot.jimple.FloatConstant;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InstanceOfExpr;
import soot.jimple.IntConstant;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.LongConstant;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NopStmt;
import soot.jimple.NullConstant;
import soot.jimple.ParameterRef;
import soot.jimple.Ref;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.internal.AbstractBinopExpr;
import soot.jimple.internal.JEqExpr;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JLengthExpr;
import soot.jimple.internal.JStaticInvokeExpr;

/**
 * @author
 **/
public class Transformer {

	static final int N = 10;
	static long counter = 0;
	static Writer indexWriter = null;
	final static double ratio = 0.5;

	static Writer getWriter() {
		String filename = "/tmp/SGXindex";
		if (indexWriter == null) {
			try {
				indexWriter = new PrintWriter(filename, "UTF-8");

			} catch (IOException e) {
				// do something
			}
		}
		return indexWriter;
	}

	static void closeWriter() {
		if (indexWriter != null) {
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
		String file = "/tmp/SGXindex";
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(file, true)));
			out.write(content + "\n");
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
	static Writer invokeWriter = null;

	public static void invokeWriter(String content) {
		String file = "/tmp/SGXinvoke";
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(file, true)));
			out.write(content + "\n");
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

	Map<Value, String> identityArray = new HashMap<Value, String>();

	ArrayList<Value> InvokeVals = new ArrayList<Value>();

	ArrayList<Value> condVals = new ArrayList<Value>();
	ArrayList<Value> condValsInt = new ArrayList<Value>();
	ArrayList<Value> condValsDouble = new ArrayList<Value>();
	ArrayList<Value> condValsFloat = new ArrayList<Value>();
	ArrayList<Value> condValsChar = new ArrayList<Value>();
	ArrayList<Value> condValsLong = new ArrayList<Value>();
	ArrayList<Value> condValsByte = new ArrayList<Value>();

	ArrayList<Value> condValsArrayInt = new ArrayList<Value>();
	ArrayList<Value> condValsArrayDouble = new ArrayList<Value>();
	ArrayList<Value> condValsArrayFloat = new ArrayList<Value>();
	ArrayList<Value> condValsArrayChar = new ArrayList<Value>();
	ArrayList<Value> condValsArrayLong = new ArrayList<Value>();
	ArrayList<Value> condValsArrayByte = new ArrayList<Value>();

	ArrayList<Value> condValsMultiArrayInt = new ArrayList<Value>();
	ArrayList<Value> condValsMultiArrayDouble = new ArrayList<Value>();
	ArrayList<Value> condValsMultiArrayFloat = new ArrayList<Value>();
	ArrayList<Value> condValsMultiArrayChar = new ArrayList<Value>();
	ArrayList<Value> condValsMultiArrayLong = new ArrayList<Value>();
	ArrayList<Value> condValsMultiArrayByte = new ArrayList<Value>();

	Map<Value, Integer> MultiBaseMap = new HashMap<>();
	Map<Value, Integer> MultiIndexMap = new HashMap<>();

	Map<Value, Integer> SenstiveFieldArray = new HashMap<>();
	Map<Value, Integer> SenstiveFieldIndexArray = new HashMap<>();
	Map<Value, Value> SenstiveFieldCuuidArray = new HashMap<>();

	ArrayList<Value> condValsOtherType = new ArrayList<Value>(); // maybe no use
																	// too

	ArrayList<Value> condValsTypeArray = new ArrayList<Value>();
	Unit lastIdentityStmt = null;

	@SuppressWarnings("unchecked")
	public Transformer(Body aBody, String phase,
			Map<String, Map<String, List<Value>>> CFMAP,
			Map<String, Map<String, Integer>> memberVariables,
			Map<String, List<String>> staticmemberVariables,
			Map<String, Map<String, int[]>> INVOKEMAP,
			Map<SootField, Value> OriginFieldCuuidArray) {
		
		String declaredClassName = "";
		declaredClassName = aBody.getMethod().getDeclaringClass().getName()
				.toString();
		String declaredFunction = aBody.getMethod().toString();
		String declaredName = aBody.getMethod().getName();
		G.v().out.println("<<!!!!!!START!!!!!!>>start processing function: "
				+ declaredFunction + ";");
		if (declaredClassName.contains("sgx_invoker")) {
			return;
		}

		/**
		 * new test , we don't care about clinit
		 */
		if (declaredName.equals("<clinit>")||declaredName.equals("estimate")) {
			
			return;
		}

		G.v().out.println("<<!!!!!!START!!!!!!>>start insertting at class: "
				+ declaredClassName);
		

		PatchingChain<Unit> units = aBody.getUnits();// all statements
		// G.v().out.println("units:"+units.toString());
		Local invokeUUIDLocal = Jimple.v().newLocal("invokeUUID",
				RefType.v("java.lang.String"));
		Local invokeLineNo = Jimple.v().newLocal("invokeLineNo", LongType.v());
		Local getUUIDLocal = Jimple.v().newLocal("getUUID",
				RefType.v("java.lang.String"));
		Local branchResultLocal = Jimple.v().newLocal("branchInvokeResult",
				BooleanType.v());
		Local sgxObjLocal = Jimple.v().newLocal("sgxInvoker",
				RefType.v("invoker.sgx_invoker"));// sgx object
		aBody.getLocals().add(invokeLineNo);
		aBody.getLocals().add(getUUIDLocal);
		aBody.getLocals().add(invokeUUIDLocal);
		aBody.getLocals().add(branchResultLocal); // 1.insert local boolean
													// branchInvokeResultLocal
		aBody.getLocals().add(sgxObjLocal); // 2.insert local reftype
											// invokerLocal
		// LocalGenerator localGenerator = new LocalGenerator(aBody);
		// Local invokeUUIDLocal = localGenerator.generateLocal
		// (RefType.v("java.lang.String"));
		// aBody.getLocals().add(invokeUUIDLocal);
		Unit currProStmt = null;

		boolean isInitValueInSgx = false;

		boolean flag = true;

		HashSet<Value> identifiedLocal = new HashSet<Value>();

		List<Local> localArray = new CopyOnWriteArrayList<Local>();// declaration
																	// valuables
		List<Local> tmpLocalArray = new CopyOnWriteArrayList<Local>();// declaration
																		// valuables
		Iterator<Local> locali = aBody.getLocals().iterator();

		while (locali.hasNext()) {
			Local tLocal = locali.next();
			G.v().out.println("tLocal=" + tLocal.toString());
			localArray.add(tLocal);
			tmpLocalArray.add(tLocal);
		}
		G.v().out.println("**********************Line376");
		/**
		 * 2020 04 04 First of all, CFMAP, memberVariables and
		 * staticmemberVariables let us know what variables are sensitive. Then
		 * we will load them into some lists by type.
		 */
		// pre deal with the identity
		Iterator<Unit> scanPre = units.snapshotIterator();
		Unit currScanPreStmt = null;
		while (scanPre.hasNext()) {
			currScanPreStmt = scanPre.next();
			G.v().out.println("====currScanPre==0404=====" + currScanPreStmt);
			if (currProStmt instanceof NopStmt) {
				G.v().out.println("20210626:" + currProStmt.toString());
			}
			if (currScanPreStmt.equals("label")) {
				G.v().out.println("20210604 label="
						+ currScanPreStmt.toString());
			}
			// G.v().out.println("202106041029="+currScanPreStmt.toString());
			if (currScanPreStmt instanceof IdentityStmt) {
				if (((IdentityStmt) currScanPreStmt).getRightOp().toString()
						.startsWith("@parameter")) {
					int index = Integer
							.parseInt(((IdentityStmt) currScanPreStmt)
									.getRightOp().toString().substring(10, 11));
					if (CFMAP.containsKey(declaredClassName)
							&& CFMAP.get(declaredClassName).containsKey(
									declaredName)) {
						if (CFMAP
								.get(declaredClassName)
								.get(declaredName)
								.contains(
										((IdentityStmt) currScanPreStmt)
												.getLeftOp()) // only deal with
																// CFMAP
								&& TypeIndex(((IdentityStmt) currScanPreStmt)
										.getLeftOp()) > 6) {
							identityArray.put(((IdentityStmt) currScanPreStmt)
									.getLeftOp(), "call_" + index);
							// condVals.add(((IdentityStmt)
							// currScanPreStmt).getLeftOp());
							continue;
						}
					}
				}
			}

			Iterator<ValueBox> ubIt = currScanPreStmt.getDefBoxes().iterator();
			while (ubIt.hasNext()) {
				ValueBox vBox = ubIt.next();
				Value tmpValue = vBox.getValue();
				G.v().out.println("def:" + tmpValue);
				if (CFMAP.containsKey(declaredClassName)
						&& CFMAP.get(declaredClassName).containsKey(
								declaredName)) {
					if (CFMAP.get(declaredClassName).get(declaredName)
							.contains(tmpValue)) {
						G.v().out.println("add def:" + tmpValue);
						preInitSensitiveVariables(tmpValue); // add
					}
				}
			}

			ubIt = currScanPreStmt.getUseBoxes().iterator();
			while (ubIt.hasNext()) {
				ValueBox vBox = ubIt.next();
				Value tmpValue = vBox.getValue();
				G.v().out.println("use:" + tmpValue);
				if (CFMAP.containsKey(declaredClassName)
						&& CFMAP.get(declaredClassName).containsKey(
								declaredName)) {
					if (CFMAP.get(declaredClassName).get(declaredName)
							.contains(tmpValue)) {
						G.v().out.println("add use:" + tmpValue);
						preInitSensitiveVariables(tmpValue); // add
					}
				}
			}
		}
		// ----------------------------------------------------------------------------------------------------------

		// deal with Cuuid
		if (declaredName.equals("<init>")
				&& !declaredClassName.equals("invoker.sgx_invoker")
				&& !declaredClassName
						.equals("pegasus.PagerankNaive$PrCounters")) {
			G.v().out.println("Cuuid Classname:" + declaredClassName
					+ " declaredFunction:" + declaredName);
			if (!memberVariables.isEmpty() || !staticmemberVariables.isEmpty()) {

				Local uuidtemp = Jimple.v().newLocal("uuidtemp",
						RefType.v("java.lang.String"));
				aBody.getLocals().add(uuidtemp);// local variables
				// aBody.getLocals().add(sgxObjLocal); //2.insert local reftype
				// invokerLocal Value
				// G.v().out.println(" units:"+units.toString());
				Unit CuuidStmt = null;
				Iterator<Unit> scanIt1 = units.snapshotIterator();
				boolean cuuidflag = true;
				while (scanIt1.hasNext()) {
					// stmt
					CuuidStmt = scanIt1.next();
					if (CuuidStmt instanceof IdentityStmt) {
						continue;
					}
					if (cuuidflag) {
						SootMethod toCall1 = Scene
								.v()
								.getMethod(
										"<invoker.sgx_invoker: java.lang.String getUUID()>");
						VirtualInvokeExpr getuuidExpr = Jimple.v()
								.newVirtualInvokeExpr(sgxObjLocal,
										toCall1.makeRef());
						AssignStmt assignStmt1 = Jimple.v().newAssignStmt(
								uuidtemp, getuuidExpr);
						units.insertBefore(assignStmt1, CuuidStmt);
						G.v().out.println("3:" + assignStmt1.toString());
						SootFieldRef sootFieldRef = Scene.v().makeFieldRef(
								aBody.getMethod().getDeclaringClass(), "Cuuid",
								RefType.v("java.lang.String"), false);
						FieldRef fieldRef = Jimple.v().newInstanceFieldRef(
								aBody.getLocals().getFirst(), sootFieldRef);
						AssignStmt asStmt = Jimple.v().newAssignStmt(fieldRef,
								uuidtemp);
						units.insertBefore(asStmt, CuuidStmt);
						cuuidflag = false;
					}
				}
			}
		}

		G.v().out.println("**********************Line456");

		condValsTypeArray.add(IntConstant.v(condValsInt.size()));
		condValsTypeArray.add(IntConstant.v(condValsDouble.size()));
		condValsTypeArray.add(IntConstant.v(condValsFloat.size()));
		condValsTypeArray.add(IntConstant.v(condValsChar.size()));
		condValsTypeArray.add(IntConstant.v(condValsLong.size()));

		condValsTypeArray.add(IntConstant.v(condValsArrayInt.size()));
		condValsTypeArray.add(IntConstant.v(condValsArrayDouble.size()));
		condValsTypeArray.add(IntConstant.v(condValsArrayFloat.size()));
		condValsTypeArray.add(IntConstant.v(condValsArrayChar.size()));
		condValsTypeArray.add(IntConstant.v(condValsArrayLong.size()));

		condValsTypeArray.add(IntConstant.v(condValsMultiArrayInt.size()));
		condValsTypeArray.add(IntConstant.v(condValsMultiArrayDouble.size()));
		condValsTypeArray.add(IntConstant.v(condValsMultiArrayFloat.size()));
		condValsTypeArray.add(IntConstant.v(condValsMultiArrayChar.size()));
		condValsTypeArray.add(IntConstant.v(condValsMultiArrayLong.size()));
		G.v().out.print("the value of param list:");
		for (int i = 0; i < condValsInt.size(); i++) {
			G.v().out.print(condValsInt.get(i) + " ");
		}
		G.v().out.println("");
		boolean isInitSgxInvoker = false;

		
		G.v().out.println("senstive array: " + condValsArrayInt);
		G.v().out.println("member senstive array: " + condValsArrayInt);
		G.v().out.println("senstive array: " + condValsArrayChar);
		G.v().out.println("member senstive array: " + condValsArrayChar);
		G.v().out.println("senstive array size: " + condValsArrayInt.size());
		G.v().out.println("identity array: " + identityArray);
		
		Iterator<Unit> it = units.snapshotIterator();
		currProStmt = it.next();
		G.v().out.println("curStmt: " + currProStmt);

		Map<Value, SootClass> needToDestoryForMemberVari = new HashMap<>(); // add
																			// on
																			// 0601
																			// 2020
		// boolean isInitValueInSgx = false;
		// boolean isInitidentyLocal = false;
		// boolean isInitInvoker = false;
		lastIdentityStmt = units.getFirst();
		G.v().out.println("***+++lastIdentityStmt is： " + lastIdentityStmt.toString() + ";");

		G.v().out.println("localArray:" + localArray.toString());

		G.v().out.println("ok condVals" + condVals.size());

		ArrayList<Unit> tempStmts = new ArrayList<>(); // for <init> temp
														// identitystmt only
														// (int double... not
														// array)

		Iterator<Unit> scanIt2 = units.snapshotIterator();
		int index = 0;
		boolean isSenstiveflag = false;
		while (scanIt2.hasNext()) {
			currProStmt = scanIt2.next();
			if ((currProStmt instanceof IdentityStmt)) {
				G.v().out.println("currProStmt is IdentityStmt:"
						+ currProStmt.toString());
				if (((IdentityStmt) currProStmt).getRightOp().toString()
						.startsWith("@caughtexception")) {
					continue;
				}
				identifiedLocal.add(((IdentityStmt) currProStmt).getLeftOp());
				// lastIdentityStmt = currProStmt;
				// && INVOKEMAP.get(declaredClassName).containsKey(declaredName)
				// &&
				// INVOKEMAP.get(declaredClassName).get(declaredName)[Integer.parseInt(((IdentityStmt)
				// currProStmt).getRightOp().toString().substring(10, 11))]==1
				if (CFMAP.containsKey(declaredClassName)
						&& CFMAP.get(declaredClassName).containsKey(
								declaredName)) {
					if (CFMAP.get(declaredClassName).get(declaredName)
							.contains(((IdentityStmt) currProStmt).getLeftOp())
							&& INVOKEMAP.get(declaredClassName).containsKey(
									declaredName)
							&& INVOKEMAP.get(declaredClassName).get(
									declaredName)[Integer
									.parseInt(((IdentityStmt) currProStmt)
											.getRightOp().toString()
											.substring(10, 11))] == 1
							&& !declaredName.equals("buildTrie")&&
							!declaredName.equals("estimate")) {
						G.v().out.println("IdentityStmt: isSenstiveflag "
								+ isSenstiveflag + " declaredName:"
								+ declaredName);
						// if (declaredName.equals("<init>")) {
						// LocalGenerator localGenerator = new
						// LocalGenerator(aBody);
						// Local locali1 =
						// localGenerator.generateLocal(((IdentityStmt)
						// currProStmt).getRightOp().getType());
						// IdentityStmt identityStmt =
						// Jimple.v().newIdentityStmt(locali1, ((IdentityStmt)
						// currProStmt).getRightOp());
						// localArray.add(locali1);
						// identifiedLocal.add(locali1);
						// G.v().out.println("the new identityStmt is:"+identityStmt.toString());
						// lastIdentityStmt = identityStmt;
						// units.insertBefore(identityStmt, currProStmt);
						// AssignStmt assignStmt =
						// Jimple.v().newAssignStmt(((IdentityStmt)
						// currProStmt).getLeftOp(), locali1);
						// G.v().out.println("the new assignStmt is:"+assignStmt.toString());
						// tempStmts.add(assignStmt);
						// //DefValue transformation
						// //
						// G.v().out.println("***++++++currProStmt is:++++++++++"+currProStmt.toString());
						// units.remove(currProStmt);
						// }else {
						isSenstiveflag = true;
						// if (TypeIndex(((IdentityStmt)
						// currProStmt).getLeftOp())<6) { //delete
						// tempStmts.add(currProStmt); //need to update because
						// these are normal sensitive variables
						condVals.add(((IdentityStmt) currProStmt).getLeftOp());
						// }
						units.remove(currProStmt);
						// lastIdentityStmt = currProStmt;
						// }
					} else if (((IdentityStmt) currProStmt).getRightOp()
							.toString().startsWith("@parameter")) { // no change
						ParameterRef ref = Jimple.v().newParameterRef(
								((IdentityStmt) currProStmt).getLeftOp()
										.getType(), index);
						index++;

						if (CFMAP
								.get(declaredClassName)
								.get(declaredName)
								.contains(
										((IdentityStmt) currProStmt)
												.getLeftOp())) {
							LocalGenerator localGenerator = new LocalGenerator(
									aBody);
							Local locali1 = localGenerator
									.generateLocal(((IdentityStmt) currProStmt)
											.getRightOp().getType());
							IdentityStmt identityStmt = Jimple.v()
									.newIdentityStmt(
											locali1,
											((IdentityStmt) currProStmt)
													.getRightOp());
							localArray.add(locali1);
							identifiedLocal.add(locali1);
							lastIdentityStmt = identityStmt;
							units.insertBefore(identityStmt, currProStmt);
							AssignStmt assignStmt = Jimple.v().newAssignStmt(
									((IdentityStmt) currProStmt).getLeftOp(),
									locali1);
							G.v().out.println("the new assignStmt is:"
									+ assignStmt.toString());
							tempStmts.add(assignStmt);
							units.remove(currProStmt);
						} else {
							IdentityStmt identity = Jimple.v().newIdentityStmt(
									((IdentityStmt) currProStmt).getLeftOp(),
									ref);
							units.insertBefore(identity, currProStmt);
							units.remove(currProStmt);
							G.v().out.println("0424 identity Vals "
									+ identity.toString());
							lastIdentityStmt = identity;
						}
					}
				}
				continue;
			}
			G.v().out.println("line 701 current stmt is: ----------#"
					+ currProStmt + "#----------------");

			// After IndetityStmt
			if (isSenstiveflag) { // we should add two parameters, calluuid and
									// LineNo
				G.v().out.println("isSenstiveflag " + isSenstiveflag);
				G.v().out.println(" this method " + declaredName);
				ParameterRef ref = Jimple.v().newParameterRef(
						invokeUUIDLocal.getType(), index);
				IdentityStmt identity = Jimple.v().newIdentityStmt(
						invokeUUIDLocal, ref);
				G.v().out.println("units.insertBefore identity invokeUUIDLocal"
						+ identity.toString());
				units.insertBefore(identity, currProStmt);

				ParameterRef reflineno = Jimple.v().newParameterRef(
						invokeLineNo.getType(), index + 1);
				IdentityStmt identitylineno = Jimple.v().newIdentityStmt(
						invokeLineNo, reflineno);
				G.v().out.println("units.insertBefore identity identitylineno"
						+ identitylineno.toString());
				units.insertBefore(identitylineno, currProStmt);

				identifiedLocal.add(invokeUUIDLocal);
				identifiedLocal.add(invokeLineNo);

				isSenstiveflag = false; // only add once

				lastIdentityStmt = identitylineno;
			}

			ArrayList<Value> currDefVals = new ArrayList<Value>();
			ArrayList<Value> currUseVals = new ArrayList<Value>();
			G.v().out.println(" line632 current stmt is: ----------#"
					+ currProStmt + "#----------------");
			// G.v().out.println("currProStmt.getUseAndDefBoxes().toString():#"+currProStmt.getUseAndDefBoxes()+"#");

			Iterator<ValueBox> ubIt = currProStmt.getDefBoxes().iterator();

			while (ubIt.hasNext()) {
				ValueBox vBox = ubIt.next();
				Value tmpValue = vBox.getValue();
				/**
				 * new add "tmpValue instanceof ArrayRef" for a[i0] = 1
				 */
				if (tmpValue instanceof ArrayRef) {
					if (!currDefVals
							.contains(((ArrayRef) ((AssignStmt) currProStmt)
									.getLeftOpBox().getValue()).getBase()))
						currDefVals.add(((ArrayRef) ((AssignStmt) currProStmt)
								.getLeftOpBox().getValue()).getBase());
				}
				if (!currDefVals.contains(tmpValue))
					currDefVals.add(tmpValue);
			}
			G.v().out.println("currDefVals:" + currDefVals.toString());// def
																		// number
																		// ===
																		// 1???
			currDefVals.retainAll(condVals);

			G.v().out.println("currDefVals after retainAll:"
					+ currDefVals.toString());// def number === 1???

			ubIt = currProStmt.getUseBoxes().iterator();
			while (ubIt.hasNext()) {
				ValueBox vBox = ubIt.next();
				Value tmpValue = vBox.getValue();

				if (!currUseVals.contains(tmpValue))
					currUseVals.add(tmpValue);
			}
			G.v().out.println("currUseVals:" + currUseVals.toString());
			currUseVals.retainAll(condVals);
			G.v().out.println("currUseVals after retainAll:"
					+ currUseVals.toString());

			// init sgx enclave
			if (!isInitSgxInvoker) {// && (!condVals.isEmpty())
				initidentyLocal(localArray, units, currProStmt, identifiedLocal); // edit
																					// on
																					// 20
																					// 04
																					// 22
				insertSgxInitStmt(aBody, sgxObjLocal, units, currProStmt,
						"invoker.sgx_invoker");
				isInitSgxInvoker = true;
				if (!isInitValueInSgx && (!condVals.isEmpty())) {
					insertValueInitStmt(aBody, sgxObjLocal, units, currProStmt,
							getUUIDLocal, invokeUUIDLocal, invokeLineNo);

					isInitValueInSgx = true;
				}
				// add temp 0610 2020
				if (!isInitValueInSgx && declaredName.equals("getSplits")
						|| declaredName.equals("run")) {
					insertValueInitStmt(aBody, sgxObjLocal, units, currProStmt,
							getUUIDLocal, invokeUUIDLocal, invokeLineNo);
					isInitValueInSgx = true;
				}
			}

			if (!tempStmts.isEmpty() && flag) {
				G.v().out.println("[0528]tempStmts is not empty!");
				for (Unit unit : tempStmts) {
					G.v().out
							.println("[0528]tempStmts unit:" + unit.toString());
					units.insertBefore(unit, currProStmt);
					replaceValueUpdateStmt(aBody, sgxObjLocal, units,
							localArray, unit, getUUIDLocal, memberVariables,
							staticmemberVariables, OriginFieldCuuidArray);
				}
				flag = false;
			}

			if (currProStmt instanceof InvokeStmt) { // deal with invoke
														// statement 1

				replaceInvokeStmtA(aBody, sgxObjLocal, units, localArray,
						currProStmt, getUUIDLocal, INVOKEMAP, memberVariables,
						staticmemberVariables);
			}

			if ((currProStmt instanceof AssignStmt)) {

				if (((AssignStmt) currProStmt).getRightOp() instanceof FieldRef
						&& memberVariables.containsKey(declaredClassName)) {// fixed
																			// the
																			// problem
																			// "$r17 = r0.<cfhider.PiEstimator$HaltonSequence: double[][] q>"
																			// the
																			// $r17
																			// is
																			// not
																			// a
																			// sensitive
																			// exactly
					SootField sField = ((FieldRef) ((AssignStmt) currProStmt)
							.getRightOp()).getField();
					if (memberVariables.get(declaredClassName).containsKey(
							sField.getName())
							&& TypeIndex(((AssignStmt) currProStmt).getLeftOp()) > 6) {
						if (!condVals.contains(((AssignStmt) currProStmt)
								.getLeftOp())) {
							G.v().out.println(((AssignStmt) currProStmt).getLeftOp());
							condVals.add(((AssignStmt) currProStmt).getLeftOp());
							currDefVals.add(((AssignStmt) currProStmt).getLeftOp());
						}
					}
				}

				/**
				 * Field init
				 */
				if (((AssignStmt) currProStmt).getLeftOp() instanceof FieldRef
						&& memberVariables.containsKey(declaredClassName)) {
					SootField sField = ((FieldRef) ((AssignStmt) currProStmt)
							.getLeftOp()).getField();
					G.v().out.println("field:"+sField.getName());
					if (memberVariables.get(declaredClassName).containsKey(
							sField.getName())) {// &&
												// TypeIndex(((AssignStmt)currProStmt).getLeftOp())>6
						// init fieldref array
						G.v().out.println("currProStmt is init fieldref : "
								+ currProStmt.toString() + ";");
						G.v().out.println("currProStmt is init fieldref : "
								+ sField.toString() + ";");
						// G.v().out.println("currProStmt is init fieldref array: "+((AssignStmt)currProStmt).getLeftOp().getUseBoxes().get(0)+";");
						Value ssValue = null;
						ubIt = ((AssignStmt) currProStmt).getLeftOp()
								.getUseBoxes().iterator();
						while (ubIt.hasNext()) {
							ValueBox vBox = ubIt.next();
							ssValue = vBox.getValue();
							break;
						}
						G.v().out.println("ssValue: " + ssValue.toString()
								+ ";");
						SootFieldRef sootFieldRef = Scene.v().makeFieldRef(
								sField.getDeclaringClass(), "Cuuid",
								RefType.v("java.lang.String"), false);
						G.v().out.println("sootFieldRef: "
								+ sootFieldRef.toString() + ";");
						FieldRef fieldRef = Jimple.v().newInstanceFieldRef(
								ssValue, sootFieldRef);
						G.v().out.println("fieldRef: " + fieldRef.toString()
								+ ";");
						Local tmpCuuid = Jimple.v().newLocal(
								"tmpCuuid" + Long.toString(counter),
								RefType.v("java.lang.String"));
						aBody.getLocals().add(tmpCuuid);
						localArray.add(tmpCuuid);
						AssignStmt asStmt = Jimple.v().newAssignStmt(tmpCuuid,
								fieldRef);
						G.v().out.println("asStmt: " + asStmt.toString() + ";");
						units.insertBefore(asStmt, currProStmt);
						replaceFieldArrayInitStmt(aBody, sgxObjLocal, units,
								localArray, currProStmt, getUUIDLocal,
								tmpCuuid, memberVariables, sField,
								OriginFieldCuuidArray);
					}
				} else if (((AssignStmt) currProStmt).getRightOp() instanceof NewArrayExpr) {
    				G.v().out.println("currProStmt is NewArrayExpr: "+currProStmt.toString()+";");
    				if (condVals.contains(((AssignStmt) currProStmt).getLeftOp())) {   // if this array is sensitive
    					if (((AssignStmt) currProStmt).getLeftOp().toString().startsWith("$")) {      // 数组静态初始化
    						G.v().out.println("currProStmt is StaticNewArrayExpr: "+currProStmt.toString()+";");
    						replaceArrayStaticInitStmt(aBody, sgxObjLocal, units, localArray, currProStmt,getUUIDLocal,scanIt2,memberVariables,staticmemberVariables, OriginFieldCuuidArray);
						}else {
							replaceArrayInitStmt(aBody, sgxObjLocal, units, localArray, currProStmt,getUUIDLocal);
						}
					}
				} else if (((AssignStmt) currProStmt).getRightOp() instanceof NewMultiArrayExpr) {
					G.v().out.println("currProStmt is NewArrayMultiExpr: "
							+ currProStmt.toString() + ";");
					if (condVals.contains(((AssignStmt) currProStmt)
							.getLeftOp())) { // if this array is sensitive
						replaceMultiArrayInitStmt(aBody, sgxObjLocal, units,
								localArray, currProStmt, getUUIDLocal);
					}
				} else if (((AssignStmt) currProStmt).getRightOp() instanceof NewExpr) {
					G.v().out.println("currProStmt is NewExpr: "
							+ currProStmt.toString() + ";");
					Value right = ((AssignStmt) currProStmt).getRightOp();

					G.v().out.println("currProStmt is NewExpr TypeString: "
							+ right.getType().toString() + ";");
					if (memberVariables.containsKey(right.getType().toString())) {
						if (!memberVariables.get(right.getType().toString())
								.isEmpty()) {
							G.v().out
									.println("=========The NewExpr is Senstive! for SootField! "
											+ currProStmt + "==========");
							needToDestoryForMemberVari.put(
									((AssignStmt) currProStmt).getLeftOp(),
									((NewExpr) right).getBaseType()
											.getSootClass());
						}
					}

				} else if (((AssignStmt) currProStmt).containsInvokeExpr()) { // deal
																				// with
																				// invoke
																				// statement
																				// 2
					G.v().out.println("currProStmt is InvokeExpr: "
							+ currProStmt.toString() + ";");
					replaceInvokeStmtB(aBody, sgxObjLocal, units, localArray,
							currProStmt, getUUIDLocal, INVOKEMAP,
							memberVariables, staticmemberVariables,
							OriginFieldCuuidArray);

				} else {
					G.v().out.println("currProStmt is AssignStmt: "
							+ currProStmt.toString() + ";");
					if (!currDefVals.isEmpty()) {// update
						G.v().out.println("toBeHiddenDefValues:"
								+ currDefVals.toString());
						replaceValueUpdateStmt(aBody, sgxObjLocal, units,
								localArray, currProStmt, getUUIDLocal,
								memberVariables, staticmemberVariables,
								OriginFieldCuuidArray);

					} else if (!currUseVals.isEmpty()) {// getLocal
						G.v().out.println("toBeHiddenUseValues:"
								+ currUseVals.toString());

						replaceValueGetStmt(aBody, sgxObjLocal, units,
								localArray, currProStmt, currUseVals,
								getUUIDLocal, memberVariables,
								staticmemberVariables);

					}
				}
			}

			if (currProStmt instanceof IfStmt) {
				// 对if语句进行处理

				// IfStmt 判断if语句中变量是否有在污染变量数据集中的,如果存在则将if语句中的所有变量加入污染变量数据集合
				String currentClsName = declaredClassName;
				String currentMethodName = declaredName;
				Value orgIfCondition = ((IfStmt) currProStmt).getCondition();
				// orgIfCondition.getUseBoxes().
				Iterator<ValueBox> ublt = orgIfCondition.getUseBoxes()
						.iterator();
				List<Value> ifUnitValues = new ArrayList<>();
				List<Value> maintainValues = new ArrayList<>();
				while (ublt.hasNext()) {
					ValueBox vBox = (ValueBox) ublt.next();
					Value tValue = vBox.getValue();
					G.v().out.println("the value=" + tValue);
					if (tValue instanceof Constant) {
						continue;
					}
					ifUnitValues.add(tValue);
				}
				G.v().out.println("the value if stmt:"
						+ ifUnitValues.toString());
				G.v().out.println("the method SourceList:"
						+ CFMAP.get(currentClsName).get(currentMethodName)
								.toString());
				maintainValues.addAll(ifUnitValues);
				maintainValues.retainAll(CFMAP.get(currentClsName).get(
						currentMethodName));
				if (maintainValues.size() > 0) {
					// for(Value v: ifUnitValues){
					// if(!SourceList.contains(v)){
					// SourceList.add(v);
					// }
					replaceBranchStmt(aBody, sgxObjLocal, branchResultLocal,
							units, localArray, currProStmt, getUUIDLocal);
				}

				G.v().out.println("currProStmt is IfStmt: "
						+ currProStmt.toString() + ";");
				// replaceBranchStmt(aBody, sgxObjLocal, branchResultLocal,
				// units, localArray, currProStmt,getUUIDLocal);

			}
			if ((currProStmt instanceof ReturnStmt)
					|| (currProStmt instanceof ReturnVoidStmt)) {
				/**
				 * if delete return?
				 */
				boolean isSenstive = false;
				if (currProStmt instanceof ReturnStmt) {
					G.v().out.println("[delete]currProStmt is returnstmt: "
							+ currProStmt.toString());
					Value reValue = ((ReturnStmt) currProStmt).getOp();
					if (condVals.contains(reValue)) {   //
						isSenstive = true;
						G.v().out.println("reValue is senstive");
					}
					if (INVOKEMAP.containsKey(declaredClassName)
							&& INVOKEMAP.get(declaredClassName).containsKey(
									declaredName)) {
						int[] tem = INVOKEMAP.get(declaredClassName).get(
								declaredName);
						for (int i = 0; i < tem.length; i++) {
							if (tem[i] == 1) {
								G.v().out.println("currPro method is sensitive");
								isSenstive = true;
								break;
							}
						}
					}
					

				}

				G.v().out
						.println("currProStmt return stmt before deleteValuestmt: "
								+ currProStmt.toString());
				G.v().out
						.println("<<!!!!!!return!!!!!!>>this processing function: "
								+ declaredFunction + ";");

				if (isSenstive) { // need to change to update
					replaceReturnStmt(aBody, sgxObjLocal, units, localArray,
							currProStmt, getUUIDLocal);
				}
				if (isInitValueInSgx) {
					insertDeletValueStmt(aBody, sgxObjLocal, units,
							currProStmt, getUUIDLocal,
							needToDestoryForMemberVari, localArray);
					// G.v().out.println("........................."+isInitSgxInvoker);
				}
				if (declaredFunction.contains("void main(java.lang.String[])")) {
					// G.v().out.print("asjfdbashklfbhsak"+currStmt.toString());
					insertCloseEnclaveStmt(sgxObjLocal, units, currProStmt,
							"invoker.sgx_invoker");
				}
				if (isSenstive) { // need to delete
					ReturnVoidStmt returnVoidStmt = Jimple.v()
							.newReturnVoidStmt();
					units.insertBefore(returnVoidStmt, currProStmt);
					units.remove(currProStmt);
				}
			}
		}
		G.v().out.println("***++++++lastIdentityStmt is:++++++++++"
				+ lastIdentityStmt.toString());
		
	}

	private void replaceFieldArrayInitStmt(Body aBody, Local sgxObjLocal,
			PatchingChain<Unit> units, List<Local> localArray,
			Unit currProStmt, Local getUUIDLocal, Local tempCuuid,
			Map<String, Map<String, Integer>> memberVariables,
			SootField sField, Map<SootField, Value> OriginFieldCuuidArray) {

		OriginFieldCuuidArray.put(sField, tempCuuid);

		int index = 0;
		String left_index = "-1";
		String right_index = "-1";
		String return_index = "-1";

		String return_flag_index = "-1"; // add on 4.46 for new solution about
											// array&class
		String left_flag_index = "-1"; // add on 4.46 for new solution about
										// array&class
		String right_flag_index = "-1"; // add on 4.46 for new solution about
										// array&class
		boolean setParam0 = false, setParam1 = false;
		String symbolString = null;
		int val_type = 0;
		int pos_index = 0;

		SootMethod toCall = Scene.v().getMethod(
				"<invoker.sgx_invoker: void clear()>");
		Stmt newInvokeStmt = Jimple.v().newInvokeStmt(
				Jimple.v().newVirtualInvokeExpr(sgxObjLocal, toCall.makeRef(),
						Arrays.asList()));

		// 0527new solution for merging update function
		// units.insertBefore(newInvokeStmt, currProStmt);
		/*
		 * 
		 * toCall =
		 * Scene.v().getMethod("<invoker.sgx_invoker: void setCounter(long)>");
		 * newInvokeStmt = Jimple.v().newInvokeStmt(
		 * Jimple.v().newVirtualInvokeExpr (sgxObjLocal, toCall.makeRef(),
		 * Arrays.asList(LongConstant.v(counter)))); G.v().out.println(
		 * "start insert before currStmt: ++++++++++++++++++++++++++ "
		 * +currProStmt+"++++++++++++++++++++++"); //0527new solution for
		 * merging update function units.insertBefore(newInvokeStmt,
		 * currProStmt);
		 */
		toCall = Scene.v().getMethod(
				"<invoker.sgx_invoker: void setCuuid(java.lang.String)>");
		newInvokeStmt = Jimple.v().newInvokeStmt(
				Jimple.v().newVirtualInvokeExpr(sgxObjLocal, toCall.makeRef(),
						Arrays.asList(tempCuuid)));
		G.v().out
				.println("field array init"
						+ currProStmt );
		units.insertBefore(newInvokeStmt, currProStmt);

		if (TypeIndex(((AssignStmt) currProStmt).getRightOp()) >= 7) {
			//get the sensitive field array's logical postion
			return_flag_index = memberVariables
					.get(aBody.getMethod().getDeclaringClass().getName()
							.toString()).get(sField.getName()).toString();
			G.v().out.println("[replaceFieldArrayInitStmt]return_flag_index:"
					+ return_flag_index);
		} else if (TypeIndex(((AssignStmt) currProStmt).getRightOp()) < 7
				&& TypeIndex(((AssignStmt) currProStmt).getRightOp()) != -1) {
			//get the sensitive field variabe logical postion
			return_index = memberVariables
					.get(aBody.getMethod().getDeclaringClass().getName()
							.toString()).get(sField.getName()).toString();
			G.v().out.println("[replaceFieldValueInitStmt]return_index:"
					+ return_index);
		}

		if (TypeIndex(((AssignStmt) currProStmt).getRightOp()) >= 7) {
			Local tmpUpdateArray = Jimple.v().newLocal(
					"tmpUpdateArray" + Long.toString(counter),
					((AssignStmt) currProStmt).getRightOp().getType());
			aBody.getLocals().add(tmpUpdateArray);
			localArray.add(tmpUpdateArray);
			DefinitionStmt assignStmts = initAssignStmt(tmpUpdateArray);
			units.insertAfter(assignStmts, lastIdentityStmt);
			G.v().out.println("c tmpUpdateArray: " + tmpUpdateArray.toString());
			AssignStmt assignStmt = Jimple.v().newAssignStmt(tmpUpdateArray,
					((AssignStmt) currProStmt).getRightOp());
			G.v().out.println("newAssignStmt is: " + assignStmt.toString());
			units.insertBefore(assignStmt, currProStmt);
			newInvokeStmt = prepareInsertStmt(tmpUpdateArray, sgxObjLocal,
					"invoker.sgx_invoker");// 只add类型相同的变量
			G.v().out.println("add: values.get(0) else array :"
					+ newInvokeStmt.toString() + "  index:" + index);
			left_flag_index = "0";
			units.insertBefore(newInvokeStmt, currProStmt);
		}
		if (condVals.contains(((AssignStmt) currProStmt).getRightOp())) {
			val_type = TypeIndex(((AssignStmt) currProStmt).getRightOp());// int
																			// or
																			// float
			pos_index = typeToList(val_type).indexOf(
					((AssignStmt) currProStmt).getRightOp());
			left_index = Integer.toString(val_type * 100 + pos_index);//
		}

		indexwriter(Integer.toString(TypeIndex(((AssignStmt) currProStmt)
				.getRightOp()))+"  lineNo: ");// tuple-1
		indexwriter(left_index);// tuple-1
		indexwriter(left_flag_index);// tuple-1
		indexwriter(right_index);// tuple-2
		indexwriter(right_flag_index);// tuple-2
		indexwriter("-1");
		indexwriter(return_index);
		indexwriter(return_flag_index);
		G.v().out.println("return_index:" + return_index);
		G.v().out.println("counter:" + counter);
		if (Integer.parseInt(return_flag_index) >= 1300) {
			toCall = Scene
					.v()
					.getMethod(
							"<invoker.sgx_invoker: void updateMultArray(java.lang.String,int,int,long)>");
			newInvokeStmt = Jimple.v()
					.newInvokeStmt(
							Jimple.v().newVirtualInvokeExpr(
									sgxObjLocal,
									toCall.makeRef(),
									Arrays.asList(getUUIDLocal,
											IntConstant.v(0), IntConstant.v(0),
											LongConstant.v(counter))));

			units.insertBefore(newInvokeStmt, currProStmt);
			units.remove(currProStmt);
		} else {
			toCall = Scene
					.v()
					.getMethod(
							"<invoker.sgx_invoker: void updateValueInEnclave(java.lang.String,int,long)>");
			newInvokeStmt = Jimple.v().newInvokeStmt(
					Jimple.v().newVirtualInvokeExpr(
							sgxObjLocal,
							toCall.makeRef(),
							Arrays.asList(getUUIDLocal, IntConstant.v(1),
									LongConstant.v(counter))));

			units.insertBefore(newInvokeStmt, currProStmt);
			units.remove(currProStmt);
		}
		counter++;
	}

	// 转换return语句
	private void replaceReturnStmt(Body aBody, Local sgxObjLocal,
			PatchingChain<Unit> units, List<Local> localArray,
			Unit currProStmt, Local getUUIDLocal) {
		Value reValue = ((ReturnStmt) currProStmt).getOp();
		G.v().out.println("replaceReturnStmt");
		SootMethod toCall = Scene.v().getMethod(
				"<invoker.sgx_invoker: void clear()>");
		Stmt newInvokeStmt = Jimple.v().newInvokeStmt(
				Jimple.v().newVirtualInvokeExpr(sgxObjLocal, toCall.makeRef(),
						Arrays.asList()));
		// 0527new solution for merging update function
		// units.insertBefore(newInvokeStmt, currProStmt);
		/*
		 * toCall =
		 * Scene.v().getMethod("<invoker.sgx_invoker: void setCounter(long)>");
		 * newInvokeStmt = Jimple.v().newInvokeStmt(
		 * Jimple.v().newVirtualInvokeExpr (sgxObjLocal, toCall.makeRef(),
		 * Arrays.asList(LongConstant.v(counter)))); G.v().out.println(
		 * "start insert before currStmt: ++++++++++++++++++++++++++ "
		 * +currProStmt+"++++++++++++++++++++++"); //0527new solution for
		 * merging update function units.insertBefore(newInvokeStmt,
		 * currProStmt);
		 */
		toCall = Scene
				.v()
				.getMethod(
						"<invoker.sgx_invoker: void updateValueInEnclave(java.lang.String,int,long)>");
		newInvokeStmt = Jimple.v().newInvokeStmt(
				Jimple.v().newVirtualInvokeExpr(
						sgxObjLocal,
						toCall.makeRef(),
						Arrays.asList(getUUIDLocal, IntConstant.v(0),
								LongConstant.v(counter))));
		units.insertBefore(newInvokeStmt, currProStmt);

		Value value = ((ReturnStmt) currProStmt).getOp();
		G.v().out.println("value:" + value);
		int pos = TypeIndex(value);
		if (identityArray.containsKey(value)) {
			indexwriter(Integer.toString(pos));// type
			indexwriter("-2");// left
			indexwriter("-2");// l
			indexwriter("-2");// right
			indexwriter("-2");// r
			indexwriter("-2");// op
			indexwriter("-2");// re
			indexwriter(identityArray.get(value)); // r
		} else if (condVals.contains(value)) {  // remove( && TypeIndex(value) <= 12)
			G.v().out.println("return sensitive array");
			indexwriter(Integer.toString(pos));// type
			indexwriter("-2");// left
			indexwriter("-2");// l
			indexwriter("-2");// right
			indexwriter("-2");// r
			indexwriter("-2");// op
			if (pos <= 6) {
				int pos_index = typeToList(pos).indexOf(value);
				int index = pos * 100 + pos_index;
				indexwriter(String.valueOf(index));// re
				indexwriter("-1");// r
			} else {
				int pos_index = typeToList(pos).indexOf(value);
				int index = pos * 10 + pos_index;
				indexwriter("-1");// re
				indexwriter(String.valueOf(index));// r
			}
		} else if (value instanceof Constant) {
			G.v().out.println("value is constant" + value.toString());
			indexwriter(Integer.toString(pos));// type
			indexwriter("-2");// left
			indexwriter("-2");// l
			indexwriter("-2");// right
			indexwriter("-2");// r
			indexwriter("-2");// op
			indexwriter(value.getType().toString() + "_" + value);// re
			indexwriter("-2");// r
		}
		// units.remove(currProStmt);
		counter++;
	}

	@SuppressWarnings("unused")
	private void replaceInvokeStmtA(Body aBody, Local sgxObjLocal,
			PatchingChain<Unit> units, List<Local> localArray,
			Unit currProStmt, Local getUUIDLocal,
			Map<String, Map<String, int[]>> INVOKEMAP,
			Map<String, Map<String, Integer>> memberVariables,
			Map<String, List<String>> staticmemberVariables) {

		String methodname = null;
		String classname = null;

		methodname = ((InvokeStmt) currProStmt).getInvokeExpr().getMethodRef()
				.name();
		G.v().out.println("20210618replaceInvokeStmtA methodname :"
				+ methodname);
		classname = ((InvokeStmt) currProStmt).getInvokeExpr().getMethodRef()
				.declaringClass().getName();
		G.v().out.println("20210618replaceInvokeStmtA classname :" + classname);
		G.v().out.println("20220605 replaceInvokeStmtA");

		int[] temp = null;
		boolean issensitive = false;
		if (INVOKEMAP.containsKey(classname)
				&& INVOKEMAP.get(classname).containsKey(methodname)) { // if
																		// sensitive
																		// return
																		// void
			int[] tem = INVOKEMAP.get(classname).get(methodname);
			for (int i = 0; i < tem.length; i++) {
				if (tem[i] == 1) {
					G.v().out.println("currProStmt is sensitive invokestmt A:"
							+ currProStmt.toString());
					temp = tem;
					issensitive = true;
					break;
				}
			}
		}
		G.v().out.println("20210618===");
		// we don't deal with init function with param value
		// if (methodname.equals("<init>") && issensitive) {
		// G.v().out.println("We will return!");
		// return;
		// }
		if (methodname.equals("buildTrie")||methodname.equals("estimate")) {
			issensitive = false;
		}
		// 当前语句存在敏感变量
		if (!issensitive) {
			G.v().out.println("currProStmt isn't sensitive invokestmt:"
					+ currProStmt.toString());
			List<Value> argList = ((InvokeStmt) currProStmt).getInvokeExpr()
					.getArgs();
			G.v().out.println("currProStmt isn't sensitive argList:"
					+ argList.size());
			int i = 0;
			if (argList.size() > 0) {
				for (Value v : argList) {
					if (condVals.contains(v) || identityArray.containsKey(v)) {
						// 插入add混淆
						Local tmpValue = Jimple.v().newLocal(
								"tmpResult" + Long.toString(counter)
										+ Integer.toString(i), v.getType());
						aBody.getLocals().add(tmpValue);
						localArray.add(tmpValue);

						DefinitionStmt assignStmt = initAssignStmt(tmpValue);
						// G.v().out.println("newAssignStmt is: "+assignStmt.toString());
						// G.v().out.println("lastIdentityStmt is: "+lastIdentityStmt.toString());
						units.insertAfter(assignStmt, lastIdentityStmt);

						AssignStmt newAssStmt = Jimple.v().newAssignStmt(
								tmpValue, v);
						G.v().out.println("new assi:" + newAssStmt.toString());
						G.v().out.println("20220605new assi:"
								+ newAssStmt.toString());
						units.insertBefore(newAssStmt, currProStmt);
						replaceValueGetStmt(aBody, sgxObjLocal, units,
								localArray, newAssStmt, null, getUUIDLocal,
								memberVariables, staticmemberVariables);

						((InvokeStmt) currProStmt).getInvokeExpr().setArg(i,
								tmpValue);
					}
					i++;
				}
			}
			return;
		}

		// G.v().out.println("20210618===2");

		SootMethod sootMethod = ((InvokeStmt) currProStmt).getInvokeExpr()
				.getMethodRef().declaringClass().getMethodByName(methodname);
		List<Type> oldtypes = sootMethod.getParameterTypes();
		// G.v().out.println("20210618===3");
		int size = ((InvokeStmt) currProStmt).getInvokeExpr().getArgCount();
		// G.v().out.println("[invoke]size:"+size+"  oldtypes.size()"+oldtypes.size()+" "+oldtypes.get(0)+" "+oldtypes.get(1));
		List<Value> newValues = new ArrayList<>();
		List<Type> newtypes = new ArrayList<>();
		G.v().out.println("20210618===4");
		for (int i = 0; i < size; i++) {
			Value qesValue = ((InvokeStmt) currProStmt).getInvokeExpr().getArg(
					i);
			// if (temp[i] == 1) { //sensitive
			if (identityArray.containsKey(qesValue)) {
				invokeWriter(String.valueOf(i)); // paraformINdex
				invokeWriter(String.valueOf(2)); // not from self
				invokeWriter(identityArray.get(qesValue)); // call_index
			} else if (condVals.contains(qesValue)) {
				G.v().out.println("20210618===2");
				invokeWriter(String.valueOf(i)); // paraformINdex
				invokeWriter(String.valueOf(1)); // is from self
				int val_type = TypeIndex(qesValue);
				int pos_index = typeToList(val_type).indexOf(qesValue);
				int index = val_type * (val_type > 6 ? 10 : 100) + pos_index;
				G.v().out.println("[invoke] index:" + index + "  i=" + i);
				G.v().out.println("arr:"+qesValue+"   "+typeToList(val_type));
				invokeWriter(String.valueOf(index)); // is from self
			} else if (qesValue instanceof Constant) { // constant
				invokeWriter(String.valueOf(i)); // paraformINdex
				invokeWriter(String.valueOf(1)); // is from self
				invokeWriter(qesValue.getType().toString() + "_" + qesValue); // is
																				// from
																				// self
			} else {
				newtypes.add(oldtypes.get(i));
				newValues.add(qesValue);
			}
		}
		// G.v().out.println("20210618===5");
		newtypes.add(getUUIDLocal.getType()); // after edit arg list
		newValues.add(getUUIDLocal); // after edit arg list
		newtypes.add(LongType.v()); // after edit arg list
		newValues.add(LongConstant.v(invokecounter)); // after edit arg list
		sootMethod.setParameterTypes(newtypes);
		G.v().out.println("after2: ++++++++++++++++++++++++++ " + currProStmt
				+ "++++++++++++++++++++++");

		// SootMethod toCall = Scene.v().getMethod
		// ("<invoker.sgx_invoker: void setInvokeCounter(long)>");
		// InvokeStmt newInvokeStmt = Jimple.v().newInvokeStmt(
		// Jimple.v().newVirtualInvokeExpr
		// (sgxObjLocal, toCall.makeRef(),
		// Arrays.asList(LongConstant.v(invokecounter))));
		// units.insertBefore(newInvokeStmt, currProStmt);

		if (sootMethod.isStatic()) {// 处理static方法
			G.v().out.println("static method:" + sootMethod.toString());
			InvokeExpr inc = Jimple.v().newStaticInvokeExpr(
					sootMethod.makeRef(), newValues);
			// InvokeExpr inc = Jimple.v().new
			Stmt inStmt = Jimple.v().newInvokeStmt(inc);
			units.insertBefore(inStmt, currProStmt);
			G.v().out.println("[insi]   inStmt:" + inStmt.toString());
			units.remove(currProStmt);
		} else {// 处理非static方法主要为init
			G.v().out.println("not static method:" + sootMethod.toString());
			((InvokeStmt) currProStmt).getInvokeExpr().setMethodRef(
					sootMethod.makeRef());
			G.v().out.println("c:" + currProStmt + " newValues"
					+ newValues.toString() + " argcount:"
					+ ((InvokeStmt) currProStmt).getInvokeExpr().getArgCount());
			int i = 0;
			G.v().out.println("20210618:"
					+ ((InvokeStmt) currProStmt).getInvokeExpr().getArgCount());
			G.v().out.println("20210618 newvlaues:" + newValues.size());
			for (Value argValue : newValues) {
				if (i < ((InvokeStmt) currProStmt).getInvokeExpr()
						.getArgCount()) {
					((InvokeStmt) currProStmt).getInvokeExpr().setArg(i,
							argValue);
					G.v().out.println("c1:" + currProStmt);

					i++;
				} else {
					break;
				}

			}
			G.v().out.println("c1:" + currProStmt);
		}
		invokecounter++;
	}

	// 转换invoke语句
	@SuppressWarnings("unused")
	private void replaceInvokeStmtB(Body aBody, Local sgxObjLocal,
			PatchingChain<Unit> units, List<Local> localArray,
			Unit currProStmt, Local getUUIDLocal,
			Map<String, Map<String, int[]>> INVOKEMAP,
			Map<String, Map<String, Integer>> memberVariables,
			Map<String, List<String>> staticmemberVariables,
			Map<SootField, Value> OriginFieldCuuidArray) {

		String methodname = null;
		String classname = null;

		methodname = ((AssignStmt) currProStmt).getInvokeExpr().getMethodRef()
				.name();
		G.v().out.println("20210618 assi methodname :" + methodname);
		if(methodname.equals("nextPoint")){
			return;
		}
		classname = ((AssignStmt) currProStmt).getInvokeExpr().getMethodRef()
				.declaringClass().getName();
		G.v().out.println("20210618 assi classname :" + classname);
		G.v().out.println("20220605 replaceInvokeStmtB");
		int[] temp = null;
		boolean issensitive = false;
		if (INVOKEMAP.containsKey(classname)
				&& INVOKEMAP.get(classname).containsKey(methodname)) { // if
																		// sensitive
																		// return
																		// void
			int[] tem = INVOKEMAP.get(classname).get(methodname);
			for (int i = 0; i < tem.length; i++) {
				if (tem[i] == 1) {
					G.v().out.println("currProStmt is sensitive assignment:"
							+ currProStmt.toString());
					temp = tem;
					issensitive = true;
					break;
				}
			}
		}

		// we don't deal with init function with param value
		// if (methodname.equals("<init>") && issensitive) {
		// return;
		// }
		if (methodname.equals("buildTrie")||methodname.equals("estimate")) {
			issensitive = false;
		}

		if (!issensitive) { // invoke is not sensitive
			G.v().out.println("currProStmt isn't sensitive:"
					+ currProStmt.toString());
			Value v = ((AssignStmt) currProStmt).getLeftOp();
			if (condVals.contains(v)) { // d0 = random(); d0 is sensitive
				G.v().out.println("currProStmt will change to GET:"
						+ currProStmt.toString());
				/**
				 * get temp = random(); d0 = temp;
				 */
				Local tmpValue = Jimple.v().newLocal(
						"tmpResult" + Long.toString(counter), v.getType());
				aBody.getLocals().add(tmpValue);
				localArray.add(tmpValue);

				DefinitionStmt assignStmt = initAssignStmt(tmpValue);
				// G.v().out.println("newAssignStmt is: "+assignStmt.toString());
				// G.v().out.println("lastIdentityStmt is: "+lastIdentityStmt.toString());
				units.insertAfter(assignStmt, lastIdentityStmt);

				((AssignStmt) currProStmt).setLeftOp(tmpValue);

				AssignStmt newAssStmt = Jimple.v().newAssignStmt(v, tmpValue);
				G.v().out.println("new assi:" + newAssStmt.toString());
				G.v().out.println("20220605new assi:" + newAssStmt.toString());
				units.insertAfter(newAssStmt, currProStmt);

				replaceValueUpdateStmt(aBody, sgxObjLocal, units, localArray,
						newAssStmt, getUUIDLocal, memberVariables,
						staticmemberVariables, OriginFieldCuuidArray);
			} else {

				int size = ((AssignStmt) currProStmt).getInvokeExpr()
						.getArgCount();
				for (int i = 0; i < size; i++) {
					Value qesValue = ((AssignStmt) currProStmt).getInvokeExpr()
							.getArg(i);
					if (condVals.contains(qesValue)
							|| identityArray.containsKey(qesValue)) {
						G.v().out
								.println("We need get this value :" + qesValue);
						Local tmpValue = Jimple.v().newLocal(
								"tmpResult" + Long.toString(counter)
										+ Integer.toString(i),
								qesValue.getType());
						aBody.getLocals().add(tmpValue);
						localArray.add(tmpValue);
						// 插入了混淆add
						DefinitionStmt assignStmt = initAssignStmt(tmpValue);
						// G.v().out.println("newAssignStmt is: "+assignStmt.toString());
						// G.v().out.println("lastIdentityStmt is: "+lastIdentityStmt.toString());
						units.insertAfter(assignStmt, lastIdentityStmt);

						AssignStmt newAssStmt = Jimple.v().newAssignStmt(
								tmpValue, qesValue);
						units.insertBefore(newAssStmt, currProStmt);
						replaceValueGetStmt(aBody, sgxObjLocal, units,
								localArray, newAssStmt, null, getUUIDLocal,
								memberVariables, staticmemberVariables);
						G.v().out.println("20210618 new assi:"
								+ newAssStmt.toString());
						G.v().out.println("20220605 new assi:"
								+ newAssStmt.toString());
						((AssignStmt) currProStmt).getInvokeExpr().setArg(i,
								tmpValue);
					}
				}
			}
			return;
		}

		/**
		 * senstive
		 */
		SootMethod sootMethod = ((AssignStmt) currProStmt).getInvokeExpr()
				.getMethodRef().declaringClass().getMethodByName(methodname);
		List<Type> oldtypes = sootMethod.getParameterTypes();

		int size = ((AssignStmt) currProStmt).getInvokeExpr().getArgCount();
		// G.v().out.println("[invoke]size:"+size+"  oldtypes.size()"+oldtypes.size()+" "+oldtypes.get(0)+" "+oldtypes.get(1));
		List<Value> newValues = new ArrayList<>();
		List<Type> newtypes = new ArrayList<>();

		for (int i = 0; i < size; i++) {
			Value qesValue = ((AssignStmt) currProStmt).getInvokeExpr().getArg(
					i);
			// if (temp[i] == 1) { //sensitive
			if (identityArray.containsKey(qesValue)) {
				invokeWriter(String.valueOf(i)); // paraformINdex
				invokeWriter(String.valueOf(2)); // not from self
				invokeWriter(identityArray.get(qesValue)); // call_index
			} else if (condVals.contains(qesValue)) {
				invokeWriter(String.valueOf(i)); // paraformINdex
				invokeWriter(String.valueOf(1)); // is from self
				int val_type = TypeIndex(qesValue);
				int pos_index = typeToList(val_type).indexOf(qesValue);
				int index = val_type * (val_type > 5 ? 10 : 100) + pos_index;
				G.v().out.println("[invoke] index:" + index + "  i=" + i);
				G.v().out.println(typeToList(val_type));
				 
				invokeWriter(String.valueOf(index)); // is from self
			} else if (qesValue instanceof Constant) { // constant
				invokeWriter(String.valueOf(i)); // paraformINdex
				invokeWriter(String.valueOf(1)); // is from self
				invokeWriter(qesValue.getType().toString() + "_" + qesValue); // is
																				// from
																				// self
			} else {
				newtypes.add(oldtypes.get(i));
				newValues.add(qesValue);
			}
		}

		/**
		 * deal with re like "d0"
		 */
		
		boolean needtobechange = false;
		Value re = ((AssignStmt) currProStmt).getLeftOp();
		G.v().out.println("re="+re+" condVals="+condVals);
		if (identityArray.containsKey(re)) {
			invokeWriter("re"); // paraformINdexinvokeWriter("re")
			invokeWriter(String.valueOf(0)); // not from self
			invokeWriter(identityArray.get(re)); // call_index
			needtobechange = true;
		} else if (condVals.contains(re)) {
			G.v().out.println("敏感数组接受返回值");
			invokeWriter("re"); // paraformINdex
			invokeWriter(String.valueOf(1)); // is from self
			int val_type = TypeIndex(re);
			int pos_index = typeToList(val_type).indexOf(re);
			int index = val_type * (val_type > 5 ? 10 : 100) + pos_index;
			invokeWriter(String.valueOf(index));
			needtobechange = true;
		}

		newtypes.add(getUUIDLocal.getType()); // after edit arg list
		newValues.add(getUUIDLocal); // after edit arg list
		newtypes.add(LongType.v()); // after edit arg list
		newValues.add(LongConstant.v(invokecounter)); // after edit arg list
		sootMethod.setParameterTypes(newtypes);

		// SootMethod toCall = Scene.v().getMethod
		// ("<invoker.sgx_invoker: void setInvokeCounter(long)>");
		// InvokeStmt newInvokeStmt = Jimple.v().newInvokeStmt(
		// Jimple.v().newVirtualInvokeExpr
		// (sgxObjLocal, toCall.makeRef(),
		// Arrays.asList(LongConstant.v(invokecounter))));
		// units.insertBefore(newInvokeStmt, currProStmt);

		if (needtobechange) {
			G.v().out.println("after3 sootMethod.getReturnType():"
					+ sootMethod.getReturnType());
			sootMethod.setReturnType(VoidType.v());
			G.v().out.println("after4 sootMethod.getReturnType():"
					+ sootMethod.getReturnType());

			if (sootMethod.isStatic()) {
				InvokeExpr inc = Jimple.v().newStaticInvokeExpr(
						sootMethod.makeRef(), newValues);
				// InvokeExpr inc = Jimple.v().new
				Stmt inStmt = Jimple.v().newInvokeStmt(inc);
				units.insertBefore(inStmt, currProStmt);
				G.v().out.println("[assi]   assin:" + inStmt.toString());
				G.v().out.println("20220605 out");
				units.remove(currProStmt);
			} else {
				((AssignStmt) currProStmt).getInvokeExpr().setMethodRef(
						sootMethod.makeRef());
				int i = 0;
				for (Value argValue : newValues) {
					((AssignStmt) currProStmt).getInvokeExpr().setArg(i,
							argValue);
					i++;
				}
			}
		}
		invokecounter++;
	}

	// 转换数组初始化语句
	@SuppressWarnings("unused")
	private void replaceArrayInitStmt(Body aBody, Local sgxObjLocal,
			PatchingChain<Unit> units, List<Local> localArray,
			Unit currProStmt, Local getUUIDLocal) {
		// TODO Auto-generated method stub

		// right
		Value right = ((AssignStmt) currProStmt).getRightOp();
		Value left = ((AssignStmt) currProStmt).getLeftOp();
		G.v().out.println("当前创建的数组是否是类数组");
		NewArrayExpr n = (NewArrayExpr) right;
		G.v().out.println("NewArrayExpr :" + n + "  " + n.getSize());
		// r1=new int[3]
		// left
		int type = TypeIndex(((AssignStmt) currProStmt).getLeftOp());// 7
		int val_type = TypeIndex(((AssignStmt) currProStmt).getLeftOp());// int
																			// or
																			// float
		int pos_index = typeToList(val_type).indexOf(
				((AssignStmt) currProStmt).getLeftOp());
		int index = val_type * 10 + pos_index;
		SootMethod toCall = Scene
				.v()
				.getMethod(
						"<invoker.sgx_invoker: void updateValueInEnclave(java.lang.String,int,long)>");

		// size
		VirtualInvokeExpr initValueExpr = null;
		Value sizevaValue = n.getSize();
		String sz = "";
		if (sizevaValue instanceof Constant) {
			sz = "int_" + sizevaValue.toString();
		} else if (condVals.contains(sizevaValue)) {
			G.v().out.println("ValueInitStmt is:condVals " + sizevaValue
					+ "#--");
			int val_type1 = TypeIndex(sizevaValue);
			int pos_index1 = typeToList(val_type1).indexOf(sizevaValue);
			int size_index = val_type1 * 100 + pos_index1;
			sz += size_index;
		}

		Stmt newInvokeStmt = Jimple.v().newInvokeStmt(
				Jimple.v().newVirtualInvokeExpr(
						sgxObjLocal,
						toCall.makeRef(),
						Arrays.asList(getUUIDLocal, IntConstant.v(0),
								LongConstant.v(counter))));
		units.insertBefore(newInvokeStmt, currProStmt);//insert first update
		counter++;
		newInvokeStmt = Jimple.v().newInvokeStmt(
				Jimple.v().newVirtualInvokeExpr(
						sgxObjLocal,
						toCall.makeRef(),
						Arrays.asList(getUUIDLocal, IntConstant.v(0),
								LongConstant.v(counter))));
		units.insertBefore(newInvokeStmt, currProStmt);//insert second update
		counter++;
		G.v().out.println("NewArrayExpr4");
		units.remove(currProStmt);

		// 以下八元组为更新维度和申请data空间
		indexwriter("" + type);
		indexwriter("" + sz);// tuple-1
		indexwriter("" + 0);// tuple-1
		indexwriter("" + 0);// tuple-2
		indexwriter("" + (-1));// tuple-2
		indexwriter("-1");
		indexwriter("" + (-1));
		indexwriter("" + index);
		
		// 以下八元组为更新维度loc和oriLoc

		indexwriter("" + type);
		indexwriter("" + index);// tuple-1
		indexwriter("" + index);// tuple-1
		indexwriter("" + 1);// tuple-2
		indexwriter("" + (-1));// tuple-2
		indexwriter("-1");
		indexwriter("" + (-1));
		indexwriter("" + index);
	}

	// 转换数组初始化语句
	@SuppressWarnings("unused")
	private void replaceMultiArrayInitStmt(Body aBody, Local sgxObjLocal,
			PatchingChain<Unit> units, List<Local> localArray,
			Unit currProStmt, Local getUUIDLocal) {
		// TODO Auto-generated method stub

		// right
		Value right = ((AssignStmt) currProStmt).getRightOp();
		NewMultiArrayExpr n = (NewMultiArrayExpr) right;
		G.v().out.println("dimeonsions :" + n + "  " + n.getSizeCount());
		G.v().out.println("size :" + n + "  " + n.getSizes());
		// left
		int val_type = TypeIndex(((AssignStmt) currProStmt).getLeftOp());// int
																			// or
																			// float
		int pos_index = typeToList(val_type).indexOf(
				((AssignStmt) currProStmt).getLeftOp());
		G.v().out.println("senseive array list:" + typeToList(7));
		int index = val_type * 10 + pos_index;
		G.v().out.println("leftOp: " + ((AssignStmt) currProStmt).getLeftOp()
				+ "val_type: " + val_type + " pos_index: " + pos_index
				+ " index: " + index);

		SootMethod toCall = Scene
				.v()
				.getMethod(
						"<invoker.sgx_invoker: void updateValueInEnclave(java.lang.String,int,long)>");
		Stmt newInvokeStmt = Jimple.v().newInvokeStmt(
				Jimple.v().newVirtualInvokeExpr(
						sgxObjLocal,
						toCall.makeRef(),
						Arrays.asList(getUUIDLocal, IntConstant.v(0),
								LongConstant.v(counter))));
		units.insertBefore(newInvokeStmt, currProStmt);
		counter++;
		newInvokeStmt = Jimple.v().newInvokeStmt(
				Jimple.v().newVirtualInvokeExpr(
						sgxObjLocal,
						toCall.makeRef(),
						Arrays.asList(getUUIDLocal, IntConstant.v(0),
								LongConstant.v(counter))));
		units.insertBefore(newInvokeStmt, currProStmt);
		units.remove(currProStmt);
		counter++;

		// units.insertBefore(newInvokeStmt, currProStmt);
		G.v().out.println("init multiarray  2022");
		// units.remove(currProStmt);
		String[] dimensions = new String[3];
		List<Value> list = n.getSizes();
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) instanceof Constant) {
				G.v().out.println("replaceMultiArrayInitStmt: constant:"
						+ Integer.parseInt(list.get(i).toString()));
				dimensions[i] = "int_" + list.get(i).toString();
				continue;
			}
			int val_type2 = TypeIndex(list.get(i));
			pos_index = typeToList(val_type2).indexOf(list.get(i));
			G.v().out.println("replaceMultiArrayInitStmt: variables: "
					+ (val_type * 10 + pos_index));
			dimensions[i] = val_type2 * 100 + pos_index + "";
		}
		for(int i =0;i<3;i++){
			if(dimensions[i]==null){
				dimensions[i]="0";
			}
		}
		G.v().out.println("dimsnesions: " + Arrays.toString(dimensions));

		// 以下八元组为更新维度和申请data空间
		indexwriter(""+val_type);
		indexwriter("" + dimensions[0]);// tuple-1
		indexwriter("" + dimensions[1]);// tuple-1
		indexwriter(""+0);// tuple-2
		indexwriter("" + dimensions[2]);// tuple-2
		indexwriter("-1");
		indexwriter("" + (-1));
		indexwriter("" + index);

		
		// 以下八元组为更新维度loc和oriLoc
		indexwriter("" + val_type);
		indexwriter("" + index);// tuple-1
		indexwriter("" + index);// tuple-1
		indexwriter("" + 1);// tuple-2
		indexwriter("" + (-1));// tuple-2
		indexwriter("-1");
		indexwriter("" + (-1));
		indexwriter("" + index);
	}

	// 转换get语句
	@SuppressWarnings("unused")
	private void replaceValueGetStmt(Body aBody, Local sgxObjLocal,
			PatchingChain<Unit> units, List<Local> localArray,
			Unit currProStmt, ArrayList<Value> currUseVals, Local getUUIDLocal,
			Map<String, Map<String, Integer>> memberVariables,
			Map<String, List<String>> staticmemberVariables) {
		// TODO Auto-generated method stub
		Value rightOp = null;
		Value leftOpValue = null;
		if (currProStmt instanceof AssignStmt) {
			rightOp = ((AssignStmt) currProStmt).getRightOp();
			leftOpValue = ((AssignStmt) currProStmt).getLeftOp();
			G.v().out
					.println("<<<<<<>>>>>>replaceValueGetStmt AssignStmt leftOpValue is: ++++++++++++++++++++++++++"
							+ leftOpValue.toString() + "++++++++++++++++++++++");
		} else if (currProStmt instanceof IdentityStmt) {
			rightOp = ((IdentityStmt) currProStmt).getRightOp();
			leftOpValue = ((IdentityStmt) currProStmt).getLeftOp();
			G.v().out
					.println("<<<<<<>>>>>> replaceValueGetStmt IdentityStmt leftOpValue is: ++++++++++++++++++++++++++"
							+ leftOpValue.toString() + "++++++++++++++++++++++");
		} else if (currProStmt instanceof InvokeStmt) {
			G.v().out.println(" currProStmt InvokeStmt IN GET: "
					+ currProStmt.toString() + ";");
			// rightOp = (Value) ((InvokeStmt)currProStmt);
		}
		ArrayList<Value> variable = new ArrayList<Value>();//
		ArrayList<Value> cons = new ArrayList<Value>();//
		ArrayList<Value> values = new ArrayList<Value>();
		ArrayList<String> operator = new ArrayList<String>();

		/* deal with there is conval in leftop(ArrayRef) */
		if (leftOpValue instanceof ArrayRef) {
			Value indexValue = ((ArrayRef) leftOpValue).getIndex();
			G.v().out.println("ArrayRef indexValue: " + indexValue + ";");

			/* just deal with baseValue, beacause baseValue maybe in condvalue */
			if (currUseVals.contains(indexValue)) {
				ArrayList<Value> oneValueList = new ArrayList<>();
				oneValueList.add(indexValue);

				Local tmpArrRefBase = Jimple.v().newLocal(
						"tmpArrRefBase" + Long.toString(counter),
						indexValue.getType());// leftOpValue
				aBody.getLocals().add(tmpArrRefBase);
				localArray.add(tmpArrRefBase);
				G.v().out.println("tmpArrRefBase: " + tmpArrRefBase.toString());

				/* insert tmpArrRefBase init stmt after all identitystmt */
				DefinitionStmt assignStmt = initAssignStmt(tmpArrRefBase);
				// G.v().out.println("newAssignStmt is: "+assignStmt.toString());
				// G.v().out.println("lastIdentityStmt is: "+lastIdentityStmt.toString());
				units.insertAfter(assignStmt, lastIdentityStmt);
				/* insert new assignstmt */
				assignStmt = Jimple.v()
						.newAssignStmt(tmpArrRefBase, indexValue);
				G.v().out.println("newAssignStmt is: " + assignStmt.toString());
				units.insertBefore(assignStmt, currProStmt);

				/* replace new assignstmt */
				replaceValueGetStmt(aBody, sgxObjLocal, units, localArray,
						assignStmt, oneValueList, getUUIDLocal,
						memberVariables, staticmemberVariables);

				/* replace leftOpValue */
				((ArrayRef) leftOpValue).setIndex(tmpArrRefBase);
				// G.v().out.println("<<<<<<>>>>>> new leftOpValue is: ++++++++++++++++++++++++++ "+leftOpValue+"++++++++++++++++++++++");

				/* replace currProstmt */
				((AssignStmt) currProStmt).setLeftOp(leftOpValue);
				// G.v().out.println("<<<<<<>>>>>> currProStmt is: ++++++++++++++++++++++++++ "+currProStmt+"++++++++++++++++++++++");

			}
		}

		analyzeExp(rightOp, values, operator, cons, variable);//

		G.v().out.println("values length:" + values.size());
		boolean rightOpIsInvoke = false;
		boolean rightOpHasArrRef = false;
		boolean leftOpHasArrRef = false;
		boolean rightCast = false;
		for (Value val : values) {
			G.v().out.println("<<<<<<>>>>>>the val is: " + val + ";");
			if (val instanceof InvokeExpr) {// ||(val instanceof ArrayRef)
				rightOpIsInvoke = true;
				G.v().out.println("InvokeExpr");
			} else if (val instanceof ArrayRef) {
				G.v().out.println("ArrayRef");
				rightOpHasArrRef = true;
			}

			if (val instanceof CastExpr) {
				G.v().out.println("CastExpr");
				rightCast = true;
			}
		}

		// leftop 不包含condval,可退出
		ArrayList<Value> testValuesArrayList = new ArrayList<Value>();
		for (Value v : values) {
			testValuesArrayList.add(v);
		}
		testValuesArrayList.retainAll(condVals);
		G.v().out.println("testValuesArrayList length is:"
				+ testValuesArrayList.size());
		if (testValuesArrayList.isEmpty()) { // add in 0613
			G.v().out
					.println("testValuesArrayList.retainAll(condVals) is null;");
			return;
		}

		int index = 0;

		String left_index = "-1";
		String left_flag_index = "-1";
		String right_index = "-1";
		String right_flag_index = "-1";
		String return_index = "-1";
		String return_flag_index = "-1";
		boolean setParam0 = false, setParam1 = false;
		String symbolString = null;
		int val_type = 0;
		int pos_index = 0;

		boolean isNeedCuuidFlag = false;
		Value tempCuuidValue = null;

		for (String local : operator) {
			symbolString = local;
			// G.v().out.println("operator:********"+local+"*************");
		}
		// insert stmt

		SootMethod toCall = Scene.v().getMethod(
				"<invoker.sgx_invoker: void clear()>");
		Stmt newInvokeStmt = Jimple.v().newInvokeStmt(
				Jimple.v().newVirtualInvokeExpr(sgxObjLocal, toCall.makeRef(),
						Arrays.asList()));
		G.v().out
				.println("newInvokeStmt to insert is: ++++++++++++++++++++++++++ "
						+ newInvokeStmt + "++++++++++++++++++++++");
		G.v().out
				.println("start insert before currStmt: ++++++++++++++++++++++++++ "
						+ currProStmt + "++++++++++++++++++++++");
		// 0527new solution for merging update function
		// units.insertBefore(newInvokeStmt, currProStmt);
		/*
		 * toCall = Scene.v().getMethod
		 * ("<invoker.sgx_invoker: void setCounter(long)>"); newInvokeStmt =
		 * Jimple.v().newInvokeStmt( Jimple.v().newVirtualInvokeExpr
		 * (sgxObjLocal, toCall.makeRef(),
		 * Arrays.asList(LongConstant.v(counter))));
		 * 
		 * // G.v().out.println(
		 * "newInvokeStmt to insert is: ++++++++++++++++++++++++++ "
		 * +newInvokeStmt+"++++++++++++++++++++++"); // G.v().out.println(
		 * "start insert before currStmt: ++++++++++++++++++++++++++ "
		 * +currProStmt+"++++++++++++++++++++++"); //0527new solution for
		 * merging update function units.insertBefore(newInvokeStmt,
		 * currProStmt);
		 */
		// if (identityArray.containsKey(leftOpValue)) {
		// return_flag_index = identityArray.get(leftOpValue);
		// }else {
		// int returnTypeIndex = TypeIndex(leftOpValue);//return value type
		// index
		// pos_index = typeToList(returnTypeIndex).indexOf(leftOpValue);
		// return_index =
		// Integer.toString(returnTypeIndex*(returnTypeIndex>=6?10:100)+pos_index);
		// }
		//

		int opTypeIndex = TypeIndex(values.get(0));
		indexwriter(Integer.toString(opTypeIndex));// tuple-0
		G.v().out
				.println("<<<<<<>>>>>> tuple-0 Get: ++++++++++++++++++++++++++ "
						+ Integer.toString(opTypeIndex)
						+ "++++++++++++++++++++++");
		int list_size = 0;
		int MaxSize = (localArray.size() > N) ? N : localArray.size();
		Random rand = new Random();

		if (values.size() == 1) {
			G.v().out.println("values.size()==1");
			if (condVals.contains(values.get(0))) {
				if (identityArray.containsKey(values.get(0))) {
					left_flag_index = identityArray.get(values.get(0));
				} else if (SenstiveFieldArray.containsKey(values.get(0))) {
					left_flag_index = SenstiveFieldArray.get(values.get(0))
							.toString();
					right_index = SenstiveFieldIndexArray.get(values.get(0))
							.toString();
					G.v().out.println("left_flag_index:" + left_flag_index
							+ " right_index:" + right_index);
					tempCuuidValue = SenstiveFieldCuuidArray.get(values.get(0));
					G.v().out.println("tempCuuidValue :" + tempCuuidValue);
					isNeedCuuidFlag = true;
				} else {
					val_type = TypeIndex(values.get(0));// int or float
					G.v().out.println("val_type:" + val_type);
					pos_index = typeToList(val_type).indexOf(values.get(0));
					G.v().out.println("pos_index:" + pos_index);
					if (val_type > 6) {   //MultiBaseMap don't need
//						if (MultiBaseMap.containsKey(values.get(0))) {
//							left_flag_index = Integer.toString(MultiBaseMap
//									.get(values.get(0)));
//							right_index = Integer.toString(MultiIndexMap
//									.get(values.get(0)));
//						} else {
//							left_flag_index = Integer.toString(val_type * 10
//									+ pos_index);
//						}
						left_flag_index = Integer.toString(val_type * 10
								+ pos_index);
					} else {
						left_index = Integer.toString(val_type * 100
								+ pos_index);
					}

				}
			} else {
				for (Local loc : localArray) {// 将variable随机插入localarray
					if ((loc.equals(values.get(0)))
							&& (list_size >= MaxSize - 1)) {
						int index_random = rand.nextInt(MaxSize - 1);
						localArray.remove(loc);
						localArray.add(index_random, loc);
					}
					list_size++;
				}
				for (Local loc : localArray) {
					if (!isTypeCompatible(values.get(0).getType(),
							loc.getType()))
						continue;
					if ((loc.equals(values.get(0)) || (rand.nextDouble() <= ratio))
							&& (index < N)) {
						if (loc.equals(values.get(0))) {
							// val_type = TypeIndex(values.get(0));//int or
							// float
							left_index = Integer.toString(index);
							setParam0 = true;
						}
						if (!condVals.contains(loc)) {
							newInvokeStmt = prepareInsertStmt(loc, sgxObjLocal,
									"invoker.sgx_invoker");// 只add类型相同的变量
							units.insertBefore(newInvokeStmt, currProStmt);
							index++;
						}
					}
				}
				if (!setParam0) {
					left_index = ((Value) (values.get(0))).getType().toString()
							+ "_" + values.get(0);
					setParam0 = true;
				}
			}
		} else if (values.size() == 2) { // we have no deal with [0429]
			G.v().out.println("values.size()==2");
			if (condVals.contains(values.get(0))) {
				val_type = TypeIndex(values.get(0));// int or float
				pos_index = typeToList(val_type).indexOf(values.get(0));
				left_index = Integer.toString(val_type * 100 + pos_index);
				setParam0 = true;
			}
			if (condVals.contains(values.get(1))) {
				val_type = TypeIndex(values.get(1));// int or float
				pos_index = typeToList(val_type).indexOf(values.get(1));
				right_index = Integer.toString(val_type * 100 + pos_index);
				setParam1 = true;
			}
			if (!setParam0 && !setParam1) {
				for (Value val : values) {// variable-tobehidden;
					for (Local loc : localArray) {// 将variable随机插入localarray
						if ((loc.equals(val)) && (list_size >= MaxSize - 1)) {
							int index_random = rand.nextInt(MaxSize - 1);
							localArray.remove(loc);
							localArray.add(index_random, loc);
						}
						list_size++;
					}
				}
				for (Local loc : localArray) {
					if (!isTypeCompatible(values.get(0).getType(),
							loc.getType()))
						continue;
					// if(isTypeCompatible(values.get(0).getType(),
					// values.get(1).getType())){
					if ((loc.equals(values.get(0)) || loc.equals(values.get(1)) || (rand
							.nextDouble() <= ratio)) && (index < N)) {
						if (loc.equals(values.get(0))) {
							// val_type = TypeIndex(values.get(0));//int or
							// float
							left_index = Integer.toString(index);
							setParam0 = true;
						}
						if (loc.equals(values.get(1))) {
							// val_type = TypeIndex(values.get(1));//int or
							// float
							right_index = Integer.toString(index);
							setParam1 = true;
						}
						if (!condVals.contains(loc)) {
							newInvokeStmt = prepareInsertStmt(loc, sgxObjLocal,
									"invoker.sgx_invoker");// 只add类型相同的变量
							units.insertBefore(newInvokeStmt, currProStmt);
							index++;
						}
					}
					// }
				}
			} else if (!setParam0) {
				for (Local loc : localArray) {// 将variable随机插入localarray
					if ((loc.equals(values.get(0)))
							&& (list_size >= MaxSize - 1)) {
						int index_random = rand.nextInt(MaxSize - 1);
						localArray.remove(loc);
						localArray.add(index_random, loc);
					}
					list_size++;
				}
				for (Local loc : localArray) {
					if (!isTypeCompatible(values.get(0).getType(),
							loc.getType()))
						continue;
					if ((loc.equals(values.get(0)) || (rand.nextDouble() <= ratio))
							&& (index < N)) {
						if (loc.equals(values.get(0))) {
							// val_type = TypeIndex(values.get(0));//int or
							// float
							left_index = Integer.toString(index);
							setParam0 = true;
						}
						if (condVals.contains(loc)) {
							newInvokeStmt = prepareInsertStmt(loc, sgxObjLocal,
									"invoker.sgx_invoker");// 只add类型相同的变量
							units.insertBefore(newInvokeStmt, currProStmt);
							index++;
						}
					}
				}
			} else if (!setParam1) {
				for (Local loc : localArray) {// 将variable随机插入localarray
					if ((loc.equals(values.get(1)))
							&& (list_size >= MaxSize - 1)) {
						int index_random = rand.nextInt(MaxSize - 1);
						localArray.remove(loc);
						localArray.add(index_random, loc);
					}
					list_size++;
				}
				for (Local loc : localArray) {
					if (!isTypeCompatible(values.get(1).getType(),
							loc.getType()))
						continue;
					if ((loc.equals(values.get(1)) || (rand.nextDouble() <= ratio))
							&& (index < N)) {
						if (loc.equals(values.get(1))) {
							// val_type = TypeIndex(values.get(1));//int or
							// float
							right_index = Integer.toString(index);
							setParam1 = true;
						}
						if (condVals.contains(loc)) {
							newInvokeStmt = prepareInsertStmt(loc, sgxObjLocal,
									"invoker.sgx_invoker");// 只add类型相同的变量
							units.insertBefore(newInvokeStmt, currProStmt);
							index++;
						}
					}
				}
			}
			if (!setParam0) {// constant
				left_index = ((Value) (values.get(0))).getType().toString()
						+ "_" + values.get(0);
				setParam0 = true;
			}
			if (!setParam1) {// constant
				right_index = ((Value) (values.get(1))).getType().toString()
						+ "_" + values.get(1);
				setParam1 = true;
			}
		} else {
			// G.v().out.println("********error: values size isnot 1 nor 2!********");
		}
		indexwriter(left_index);// tuple-1
		indexwriter(left_flag_index);// tuple-1
		indexwriter(right_index);// tuple-2
		indexwriter(right_flag_index);// tuple-2
		if (!operator.isEmpty()) {
			if (symbolString.equals(" + "))
				indexwriter("1");
			else if (symbolString.equals(" - ") || symbolString.equals(" cmp ")
					|| symbolString.equals(" cmpg "))
				indexwriter("2");
			else if (symbolString.equals(" * "))
				indexwriter("3");
			else if (symbolString.equals(" / "))
				indexwriter("4");
			else if (symbolString.equals(" % "))
				indexwriter("5");
			else if (symbolString.equals(" & ")) { 
				indexwriter("12");
			}
			else if (symbolString.equals(" | ")) {
				indexwriter("13");
			} else if (symbolString.equals(" ^ ")) { 
				indexwriter("14");
			}  else if (symbolString.equals(" << ")) {
				indexwriter("15");
			}  else if (symbolString.equals(" >>> ")) {
				indexwriter("16");
			}  else if (symbolString.equals(" >>> ")) {
				indexwriter("17");
			} 
			else
				indexwriter("-1");
		} else {
			indexwriter("-1");
		}
		indexwriter("-1");
		indexwriter("-1");
		G.v().out.println("stmt get first operand:********" + left_index
				+ "*************");
		G.v().out.println("stmt get second operand:********" + right_index
				+ "*************");
		// if(left_index == "-1")
		// G.v().out.println("stmt has no first operand:********"+left_index+"*************");
		// if(right_index == "-1")
		// G.v().out.println("A stmt has no second operand:********"+right_index+"*************");

		boolean LeftOpIsArrayRef = false;
		boolean LeftOpIsObject = false;

		G.v().out.println("curr stmt：" + currProStmt.toString());
		G.v().out.println("leftOpValue：" + leftOpValue.toString());
		if (leftOpValue instanceof ArrayRef) {
			G.v().out.println("rrrrrrrrrrrrrrrrrrrrrrrrr");
			LeftOpIsArrayRef = true;
		} else if (leftOpValue.getType().toString()
				.equals("org.apache.hadoop.mapred.JobConf")) {
			G.v().out.println("kkkkkkkkkkkkkkkkkkkkkkkkk");
			LeftOpIsObject = true;
		}

		if (isNeedCuuidFlag) {
			G.v().out.println("1111333311");
			toCall = Scene.v().getMethod(
					"<invoker.sgx_invoker: void setCuuid(java.lang.String)>");
			newInvokeStmt = Jimple.v().newInvokeStmt(
					Jimple.v().newVirtualInvokeExpr(sgxObjLocal,
							toCall.makeRef(), Arrays.asList(tempCuuidValue)));
			G.v().out
					.println("start insert before currStmt: ++++++++++++++++++++++++++ "
							+ currProStmt + "++++++++++++++++++++++");
			units.insertBefore(newInvokeStmt, currProStmt);
			G.v().out.println("1111444111");
		}

		G.v().out.println("start insert an un-invoke get");
		G.v().out.println("returnTypeIndexToCallFunc:"
				+ returnTypeIndexToCallFunc(TypeIndex(values.get(0))));
		toCall = Scene.v().getMethod(
				returnTypeIndexToCallFunc(TypeIndex(values.get(0))));

		DefinitionStmt assignStmt = null;


		if (LeftOpIsArrayRef) {
			G.v().out.println("LeftOpIsArrayRef-------:"+ leftOpValue.toString());
			/* contruct tmpRef */
			Local tmpRef = Jimple.v().newLocal(
					"tmpArrayRef" + String.valueOf(counter),
					leftOpValue.getType());
			aBody.getLocals().add(tmpRef);
			localArray.add(tmpRef);
			G.v().out.println("tmpValue: " + tmpRef.toString());

			/* tmpRef init stmt after all identitystmt */
			assignStmt = initAssignStmt(tmpRef);
			// G.v().out.println("newAssignStmt is: "+assignStmt.toString());
			// G.v().out.println("lastIdentityStmt is: "+lastIdentityStmt.toString());
			units.insertAfter(assignStmt, lastIdentityStmt);

			/* tmpRef assignstmt "tmpArrayRef=getIntValue()" */
			assignStmt = Jimple.v().newAssignStmt(
					tmpRef,
					Jimple.v().newVirtualInvokeExpr(sgxObjLocal,
							toCall.makeRef(), Arrays.asList(getUUIDLocal)));
			G.v().out.println("0603====" + assignStmt.toString());
			units.insertBefore(assignStmt, currProStmt);

			/* currstmt "leftop=tmpArrayRef" */
			((AssignStmt) currProStmt).setRightOp(tmpRef);
		}
		/*
		 * else if (LeftOpIsObject) { //contruct tmpRef
		 * G.v().out.println("LeftOpIsObject---------------"); Local
		 * tmpRef=Jimple.v().newLocal
		 * ("tmpObjectRef"+String.valueOf(counter),leftOpValue.getType());
		 * aBody.getLocals().add(tmpRef); localArray.add(tmpRef);
		 * G.v().out.println("tmpValue: "+tmpRef.toString());
		 * 
		 * //tmpRef init stmt after all identitystmt assignStmt =
		 * initAssignStmt(tmpRef);
		 * G.v().out.println("object newAssignStmt is: "+assignStmt.toString());
		 * G
		 * .v().out.println("object lastIdentityStmt is: "+lastIdentityStmt.toString
		 * ()); units.insertAfter(assignStmt, lastIdentityStmt);
		 * 
		 * //tmpRef assignstmt "tmpArrayRef=getIntValue()" assignStmt =
		 * Jimple.v().newAssignStmt(tmpRef, Jimple.v().newVirtualInvokeExpr
		 * (sgxObjLocal, toCall.makeRef(), Arrays.asList(getUUIDLocal)));
		 * units.insertBefore(assignStmt, currProStmt);
		 * 
		 * //currstmt "leftop=tmpArrayRef"
		 * ((AssignStmt)currProStmt).setRightOp(tmpRef);
		 * G.v().out.println("already set rightop"); }
		 */
		else {
			G.v().out.println("general stmt--------------");
			G.v().out.println("0611============leftOpValue is: "
					+ leftOpValue.toString());
			G.v().out.println("0611============curr AssignStmt is: "
					+ currProStmt.toString());

			assignStmt = Jimple.v().newAssignStmt(
					leftOpValue,
					Jimple.v().newVirtualInvokeExpr(
							sgxObjLocal,
							toCall.makeRef(),
							Arrays.asList(getUUIDLocal,
									IntConstant.v((isNeedCuuidFlag) ? 1 : 0),
									LongConstant.v(counter))));
			G.v().out.println("0611============newAssignStmt is: "
					+ assignStmt.toString());
			units.insertBefore(assignStmt, currProStmt);
			units.remove(currProStmt);
		}
		// G.v().out.println("");
		// InvokeExpr invokeExprtmpExpr = Jimple.v().newVirtualInvokeExpr
		// (sgxObjLocal, toCall.makeRef(), Arrays.asList());
		// G.v().out.println("invokeExprtmpExpr is:++++++"+invokeExprtmpExpr+"++++++++");
		// //
		// G.v().out.println("invokeExprtmpExpr type is:++++++"+invokeExprtmpExp+"++++++++");
		// ((AssignStmt)currProStmt).setRightOp((Value)invokeExprtmpExpr);

		// G.v().out.println("rightOpvalueOfAssignment is:++++++"+rightOp+"++++++++");
		// G.v().out.println("currProStmt units is: ++++ "+currProStmt.getUseBoxes()+"++++++++++++");
		G.v().out.println("get counter:" + counter);
		counter++;
	}

	// get的具体分类
	private String returnTypeIndexToCallFunc(int returnTypeIndex) {
		String funcString = new String();
		switch (returnTypeIndex) {
		case 1:
			funcString = "<invoker.sgx_invoker: int getIntValue(java.lang.String,int,long)>";// getIntValue
			break;
		case 2:
			funcString = "<invoker.sgx_invoker: double getDoubleValue(java.lang.String,int,long)>";
			break;
		case 3:
			funcString = "<invoker.sgx_invoker: float getFloatValue(java.lang.String,int,long)>";
			break;
		case 4:
			funcString = "<invoker.sgx_invoker: char getCharValue(java.lang.String,int,long)>";
			break;
		case 5:
			funcString = "<invoker.sgx_invoker: long getLongValue(java.lang.String,int,long)>";
			break;
		case 6:
			funcString = "<invoker.sgx_invoker: byte getByteValue(java.lang.String,int,long)>";
			break;
		case 7:
			funcString = "<invoker.sgx_invoker: int[] getIntArray(java.lang.String,int,long)>";
			break;
		case 8:
			funcString = "<invoker.sgx_invoker: double[] getDoubleArray(java.lang.String,int,long)>";
			break;
		case 9:
			funcString = "<invoker.sgx_invoker: float[] getFloatArray(java.lang.String,int,long)>";
			break;
		case 10:
			funcString = "<invoker.sgx_invoker: char[] getCharArray(java.lang.String,int,long)>";
			break;
		case 11:
			funcString = "<invoker.sgx_invoker: long[] getLongArray(java.lang.String,int,long)>";
			break;
		case 12:
			funcString = "<invoker.sgx_invoker: byte[] getByteArray(java.lang.String,int,long)>";
			break;
		default:
			break;
		}
		return funcString;
	}

	// 转换branch语句
	@SuppressWarnings("unused")
	private void replaceBranchStmt(Body aBody, Local sgxObjLocal,
			Local branchResultLocal, PatchingChain<Unit> units,
			List<Local> localArray, Unit currProStmt, Local getUUIDLocal) {

		Value ifCondition = ((IfStmt) currProStmt).getCondition();
		G.v().out.println(" curr pro Unit: " + ifCondition + ";");
		ArrayList<Value> variable = new ArrayList<Value>();//
		ArrayList<Value> values = new ArrayList<Value>();
		ArrayList<Value> cons = new ArrayList<Value>();
		ArrayList<String> operator = new ArrayList<String>();

		analyzeExp(ifCondition, values, operator, cons, variable);

		int index = 0;
		String left_index = "-1";
		String right_index = "-1";
		String return_index = "-1";
		boolean setParam0 = false, setParam1 = false;
		String symbolString = null;
		int val_type = 0;
		int pos_index = 0;

		// for(Value local: values){
		// G.v().out.println("values:********"+local+"*************");
		// }
		// for(Value local: variable){
		// G.v().out.println("variable:********"+local+"*************");//parameter
		// non-constant
		// }
		// for(Value local: cons){
		// G.v().out.println("cons:********"+local+"*************");//constant
		// }
		for (String local : operator) {
			symbolString = local;
			G.v().out.println("operator:********" + local + "*************");
		}

		SootMethod toCall = Scene.v().getMethod(
				"<invoker.sgx_invoker: void clear()>");
		Stmt newInvokeStmt = Jimple.v().newInvokeStmt(
				Jimple.v().newVirtualInvokeExpr(sgxObjLocal, toCall.makeRef(),
						Arrays.asList()));
		G.v().out
				.println("newInvokeStmt to insert is: ++++++++++++++++++++++++++ "
						+ newInvokeStmt + "++++++++++++++++++++++");
		G.v().out
				.println("start insert before currStmt: ++++++++++++++++++++++++++ "
						+ currProStmt + "++++++++++++++++++++++");
		// 0527new solution for merging update function

		// units.insertBefore(newInvokeStmt, currProStmt);
		/*
		 * toCall = Scene.v().getMethod
		 * ("<invoker.sgx_invoker: void setCounter(long)>"); newInvokeStmt =
		 * Jimple.v().newInvokeStmt( Jimple.v().newVirtualInvokeExpr
		 * (sgxObjLocal, toCall.makeRef(),
		 * Arrays.asList(LongConstant.v(counter)))); //
		 * G.v().out.println("curr counter is: ++++++++++++++++++++++++++ "
		 * +counter+"++++++++++++++++++++++"); // G.v().out.println(
		 * "newInvokeStmt to insert is: ++++++++++++++++++++++++++ "
		 * +newInvokeStmt+"++++++++++++++++++++++"); // G.v().out.println(
		 * "start insert before currStmt: ++++++++++++++++++++++++++ "
		 * +currProStmt+"++++++++++++++++++++++"); //0527new solution for
		 * merging update function units.insertBefore(newInvokeStmt,
		 * currProStmt);
		 */
		int opTypeIndex = TypeIndex(values.get(0));// op value type index
		if (opTypeIndex == -1) {
			opTypeIndex = 1;
		}
		indexwriter(Integer.toString(opTypeIndex));// tuple-0
		G.v().out
				.println("<<<<<<>>>>>> tuple-0 branch: ++++++++++++++++++++++++++ "
						+ Integer.toString(opTypeIndex)
						+ "++++++++++++++++++++++");
		int list_size = 0;
		int MaxSize = (localArray.size() > N) ? N : localArray.size();
		Random rand = new Random();

		if (values.size() == 1) {
			G.v().out
					.println("there is only one para in condition values!!!++++++++++++++++++++++++++++++++");
		} else if (values.size() == 2) {
			if (condVals.contains(values.get(0))) {
				G.v().out.println("values0 is in condvals!");
				val_type = TypeIndex(values.get(0));// int or float
				G.v().out.println("val_type is:====" + val_type);
				pos_index = typeToList(val_type).indexOf(values.get(0));
				G.v().out.println("pos_index is:====" + pos_index);
				left_index = Integer.toString(val_type * 100 + pos_index);
				G.v().out.println("left_index is:====" + left_index);
				setParam0 = true;
			}
			if (condVals.contains(values.get(1))) {
				G.v().out.println("values1 is in condvals!");
				val_type = TypeIndex(values.get(1));// int or float
				G.v().out.println("val_type is:====" + val_type);
				pos_index = typeToList(val_type).indexOf(values.get(1));
				G.v().out.println("pos_index is:====" + pos_index);
				right_index = Integer.toString(val_type * 100 + pos_index);
				G.v().out.println("right_index is:====" + right_index);
				setParam1 = true;
			}
			if (!setParam0) {// maybe constant or Object
				if (TypeIndex(values.get(0)) == -1) {
					G.v().out.println("values0 is Object!");
					newInvokeStmt = prepareInsertStmt(values.get(0),
							sgxObjLocal, "invoker.sgx_invoker");// 只add类型相同的变量
					units.insertBefore(newInvokeStmt, currProStmt);
					left_index = "0";
				} else {
					G.v().out.println("values0 is constant!");
					left_index = ((Value) (values.get(0))).getType().toString()
							+ "_" + values.get(0);
				}
				setParam0 = true;
			}
			if (!setParam1) {
				if (TypeIndex(values.get(1)) == -1) {
					G.v().out.println("values1 is Object!");
					newInvokeStmt = prepareInsertStmt(values.get(1),
							sgxObjLocal, "invoker.sgx_invoker");// 只add类型相同的变量
					units.insertBefore(newInvokeStmt, currProStmt);
					right_index = "1";
				} else {
					G.v().out.println("values1 is constant!");
					right_index = ((Value) (values.get(1))).getType()
							.toString() + "_" + values.get(1);
				}
				if (((Value) (values.get(1))).getType().toString()
						.equals("null_type")) {
					right_index = "int_0";
				}
				setParam1 = true;
			}
		} else {
			G.v().out.println("********error: values size is not 1 nor 2!********");
		}
		if (!setParam0 || !setParam1)
			G.v().out.println("values are not in hidden list!!!!!********");

		indexwriter(left_index);// tuple-1
		indexwriter("-1");// tuple-1 is Array
		G.v().out.println("left_index：====b==:" + left_index);
		indexwriter(right_index);// tuple-2
		indexwriter("-1");// tuple-2 is Array
		G.v().out.println("right_index：===b===:" + right_index);
		G.v().out.println("operator：===b===:" + operator);
		if (!operator.isEmpty()) {
			if (symbolString.equals(" == "))
				indexwriter("6");
			else if (symbolString.equals(" != ")
					|| symbolString.equals(" cmp "))
				indexwriter("7");
			else if (symbolString.equals(" > "))
				indexwriter("8");
			else if (symbolString.equals(" < "))
				indexwriter("9");
			else if (symbolString.equals(" >= "))
				indexwriter("10");
			else if (symbolString.equals(" <= "))
				indexwriter("11");
			else
				indexwriter("-1");
		} else {
			indexwriter("-1");
		}
		indexwriter("-1");
		indexwriter("-1");// tuple-re is Array
		G.v().out.println("re：===b===:-1");
		G.v().out.println("counter：===b===:" + counter);
		if (left_index == "-1")
			G.v().out.println("stmt branch has no first operand:********"
					+ left_index + "*************");
		if (right_index == "-1")
			G.v().out.println("stmt branch has no second operand:********"
					+ right_index + "*************");

		toCall = Scene
				.v()
				.getMethod(
						"<invoker.sgx_invoker: boolean getBooleanValue(java.lang.String,long)>");
		// toCall = Scene.v().getMethod
		// (returnTypeIndexToCallFunc(1));//返回值为int类型
		DefinitionStmt assignStmt = Jimple.v().newAssignStmt(
				branchResultLocal,
				Jimple.v().newVirtualInvokeExpr(sgxObjLocal, toCall.makeRef(),
						Arrays.asList(getUUIDLocal, LongConstant.v(counter))));// IntConstant.v(1)));//返回值为int类型
		units.insertBefore(assignStmt, currProStmt);
		((IfStmt) currProStmt).setCondition(new JEqExpr(branchResultLocal,
				IntConstant.v(1)));

		G.v().out
				.println("assignStmt to insert is: ++++++++++++++++++++++++++ "
						+ assignStmt + "++++++++++++++++++++++");
		G.v().out
				.println("start insert before currStmt: ++++++++++++++++++++++++++ "
						+ currProStmt + "++++++++++++++++++++++");
		counter++;
	}

	// 转换update语句
	@SuppressWarnings("unused")
	private Unit replaceValueUpdateStmt(Body aBody, Local sgxObjLocal,
			PatchingChain<Unit> units, List<Local> localArray,
			Unit currProStmt, Local getUUIDLocal,
			Map<String, Map<String, Integer>> memberVariables,
			Map<String, List<String>> staticmemberVariables,
			Map<SootField, Value> OriginFieldCuuidArray) {
		// TODO Auto-generated method stub
		Value rightOp = null;
		Value leftOpValue = null;
		G.v().out.println("enter replaceValueUpdateStmt:"
				+ currProStmt.toString());
		boolean flag=false;//flag为true说明int a=arr[0]触发 为了使得涉及数组类型的操作的type都>=7 
		boolean isArrayLength=false;
		if (currProStmt instanceof AssignStmt) {
			rightOp = ((AssignStmt) currProStmt).getRightOp();
			leftOpValue = ((AssignStmt) currProStmt).getLeftOp();
			G.v().out.println("ass r curr pro Unit: " + rightOp + ";");
			G.v().out.println("ass r curr pro Unit type: "
					+ rightOp.getType().toString() + ";");
			G.v().out.println("ass l curr pro Unit: " + leftOpValue + ";");
			G.v().out.println("ass l curr pro Unit type: "
					+ leftOpValue.getType().toString() + ";");
		} else if (currProStmt instanceof IdentityStmt) {
			rightOp = ((IdentityStmt) currProStmt).getRightOp();
			leftOpValue = ((IdentityStmt) currProStmt).getLeftOp();
			G.v().out.println("ide r curr pro Unit: " + rightOp + ";");
			G.v().out.println("ide l curr pro Unit: " + leftOpValue + ";");
		} else {
			G.v().out.println("else currProStmt Type: "
					+ currProStmt.getClass() + ";");
		}
		G.v().out.println("=curr pro Unit: " + rightOp + ";");

		ArrayList<Value> variable = new ArrayList<Value>();//
		ArrayList<Value> cons = new ArrayList<Value>();//
		ArrayList<Value> values = new ArrayList<Value>();
		ArrayList<String> operator = new ArrayList<String>();
		boolean RightOpIsInvoke = false;
		boolean isRightOpInCondVal = false;
		boolean isRightOpStaticFiled = false;

		boolean rightCast = false;
		
		

		analyzeExp(rightOp, values, operator, cons, variable);//

		int index = 0;
		String left_index = "-1";
		String right_index = "-1";
		String return_index = "-1";

		String return_flag_index = "-1"; // add on 4.46 for new solution about
											// array&class
		String left_flag_index = "-1"; // add on 4.46 for new solution about
										// array&class
		String right_flag_index = "-1"; // add on 4.46 for new solution about
										// array&class
		boolean setParam0 = false, setParam1 = false;
		String symbolString = null;
		int val_type = 0;
		int pos_index = 0;
        if (values.get(0) instanceof JLengthExpr) {   //求数组长度时 type=7，right_index=5 方便处理
			isArrayLength = true;
			G.v().out.println("JLengthExpr:" + values.get(0));
		}
		for (Value local : values) {
			G.v().out.println("values:********" + local + "*************");
			G.v().out.println("values.type:********"
					+ local.getType().toString() + "*************");
			if (local instanceof CastExpr) {
				G.v().out.println("CastExpr");
				rightCast = true;
			}
		}
		G.v().out.println("********");
		boolean rOpArr = rightOp instanceof ArrayRef;

		G.v().out.println("rOpArr********" + rOpArr + "*************");
		if (rightCast) {
			G.v().out.println("This is CastExpr to be replaced is: ++++++++"
					+ currProStmt + "+++++update+++++++++");
			Value value = ((CastExpr) rightOp).getOp();
			Type type = ((CastExpr) rightOp).getType();
			G.v().out.println("value :" + value + " type:" + type);

			if (condVals.contains(value)) {
				G.v().out.println("indexWriter 1: "
						+ Integer.toString(TypeIndex(leftOpValue)));
				indexwriter(Integer.toString(TypeIndex(leftOpValue)));// tuple-0:
																		// opOne's
																		// type

				int leftTypeIndex = TypeIndex(value);// left value type index
				pos_index = typeToList(leftTypeIndex).indexOf(value);
				left_index = Integer.toString(leftTypeIndex * 100 + pos_index);
				G.v().out.println("20210626pos_index====" + pos_index);
				G.v().out.println("20210626left_index====" + left_index);
				indexwriter(left_index);// left
				indexwriter("-1");// l
				indexwriter("-1");// right
				indexwriter("-1");// r
				indexwriter("-1");// op

				val_type = TypeIndex(leftOpValue);// int or float
				pos_index = typeToList(val_type).indexOf(leftOpValue);
				return_index = Integer.toString(val_type * 100 + pos_index);//

				indexwriter(return_index);// re
				indexwriter("-1");// r

				SootMethod toCall = Scene.v().getMethod(
						"<invoker.sgx_invoker: void clear()>");
				Stmt newInvokeStmt = Jimple.v().newInvokeStmt(
						Jimple.v().newVirtualInvokeExpr(sgxObjLocal,
								toCall.makeRef(), Arrays.asList()));
				// 0527new solution for merging update function
				// units.insertBefore(newInvokeStmt, currProStmt);
				/*
				 * toCall = Scene.v().getMethod(
				 * "<invoker.sgx_invoker: void setCounter(long)>");
				 * newInvokeStmt = Jimple.v().newInvokeStmt(
				 * Jimple.v().newVirtualInvokeExpr (sgxObjLocal,
				 * toCall.makeRef(), Arrays.asList(LongConstant.v(counter))));
				 * G.v().out.println(
				 * "start insert before currStmt: ++++++++++++++++++++++++++ "
				 * +currProStmt+"++++++++++++++++++++++"); //0527new solution
				 * for merging update function units.insertBefore(newInvokeStmt,
				 * currProStmt);
				 */
				toCall = Scene
						.v()
						.getMethod(
								"<invoker.sgx_invoker: void updateValueInEnclave(java.lang.String,int,long)>");

				newInvokeStmt = Jimple.v().newInvokeStmt(
						Jimple.v().newVirtualInvokeExpr(
								sgxObjLocal,
								toCall.makeRef(),
								Arrays.asList(getUUIDLocal, IntConstant.v(0),
										LongConstant.v(counter)))); // IntConstant.v(returnTypeIndex)));
				// G.v().out.println("newInvokeStmt to insert is: ++++++++++++++++++++++++++ "+newInvokeStmt+"++++++++++++++++++++++");
				// G.v().out.println("start insert before currStmt: ++++++++++++++++++++++++++ "+currProStmt+"++++++++++++++++++++++");
				units.insertBefore(newInvokeStmt, currProStmt);
				units.remove(currProStmt);
				counter++;
			} else {
				Local tmpGetCast = Jimple.v().newLocal(
						"tmpGetCast" + Long.toString(counter), type);
				aBody.getLocals().add(tmpGetCast);
				localArray.add(tmpGetCast);
				G.v().out.println("tmpValue: " + tmpGetCast.toString());
				AssignStmt assignStmt = Jimple.v().newAssignStmt(tmpGetCast,
						rightOp);
				G.v().out.println("newAssignStmt is: " + assignStmt.toString());
				units.insertBefore(assignStmt, currProStmt);
				AssignStmt assignStmt1 = Jimple.v().newAssignStmt(leftOpValue,
						tmpGetCast);
				units.insertBefore(assignStmt, currProStmt);
				replaceValueUpdateStmt(aBody, sgxObjLocal, units, localArray,
						currProStmt, getUUIDLocal, memberVariables,
						staticmemberVariables, OriginFieldCuuidArray);
				units.remove(currProStmt);
			}
			return null;
		}
		// right op is array int[] arr1=arr2[0]
		//右边多维数组引用类型（数组+下标）
		else if (rightOp instanceof ArrayRef
				&& TypeIndex(((ArrayRef) ((AssignStmt) currProStmt)
						.getRightOp()).getBase()) >= 13) { // temp = a[0][];
			G.v().out.println("2x array. 0520");
			// arrny index is sensive
			Value Arrvalue = ((ArrayRef) ((AssignStmt) currProStmt)
					.getRightOp()).getBase();
			Value Indevalue = ((ArrayRef) ((AssignStmt) currProStmt)
					.getRightOp()).getIndex();

			G.v().out.println("Arrvalue :" + Arrvalue + "  Indevalue:"
					+ Indevalue);
			G.v().out.println(SenstiveFieldArray.containsKey(Arrvalue));
			if (SenstiveFieldArray.containsKey(Arrvalue)) {// arr1=arr2[0] arr2
															// is sensive
				val_type = TypeIndex(Indevalue);
				pos_index = typeToList(val_type).indexOf(Indevalue);
				int Index = val_type * 100 + pos_index;
				if (!SenstiveFieldArray.containsKey(leftOpValue)) {
					SenstiveFieldArray.put(leftOpValue,
							SenstiveFieldArray.get(Arrvalue));
					SenstiveFieldIndexArray.put(leftOpValue, Index);
					SenstiveFieldCuuidArray.put(leftOpValue,
							SenstiveFieldCuuidArray.get(Arrvalue));
				}
			}else {

				val_type = TypeIndex(Arrvalue);
				if (!typeToList(val_type).contains(Arrvalue)) {
					typeToList(val_type).add(Arrvalue);
				}
				pos_index = typeToList(val_type).indexOf(Arrvalue);
				int base = val_type * 10 + pos_index;// 右侧敏感数组的逻辑位置
				Value leftOp = ((AssignStmt) currProStmt).getLeftOp();
				G.v().out.println("leftOp: " + leftOp);
				G.v().out.println(": " + (leftOp instanceof ArrayRef));
				Value leftValue = ((AssignStmt) currProStmt).getLeftOp();

				int leftType = TypeIndex(leftOpValue);
				int leftPosIndex = typeToList(leftType).indexOf(leftOpValue);
				int leftBase = leftType * 10 + leftPosIndex;// 左侧敏感数组的逻辑位置

				String Index = "";
				if (Indevalue instanceof Constant) {
					Index = "int_" + Integer.parseInt(Indevalue.toString());
				} else {
					val_type = TypeIndex(Indevalue);
					pos_index = typeToList(val_type).indexOf(Indevalue);
					Index = "" + (val_type * 100 + pos_index);// 右侧敏感数组下标是变量
																// 该变量的逻辑位置
				}
				

			SootMethod	toCall = Scene
						.v()
						.getMethod(
								"<invoker.sgx_invoker: void updateValueInEnclave(java.lang.String,int,long)>");
				Stmt newInvokeStmt = Jimple.v().newInvokeStmt(
						Jimple.v().newVirtualInvokeExpr(
								sgxObjLocal,
								toCall.makeRef(),
								Arrays.asList(getUUIDLocal, IntConstant.v(0),
										LongConstant.v(counter)))); // IntConstant.v(returnTypeIndex)));
				units.insertBefore(newInvokeStmt, currProStmt);

				counter++;
				//a=b[0]形式的第一条update语句 复制b中的数据到a
				indexwriter("" + leftType);// l
				indexwriter("" + (-1));// l
				indexwriter("" + base);
				indexwriter("" + (3));// l
				indexwriter("" + (-1));
				indexwriter("" + (-1));
				indexwriter("" + (-1));
				indexwriter("" + leftBase);

				toCall = Scene
						.v()
						.getMethod(
								"<invoker.sgx_invoker: void updateValueInEnclave(java.lang.String,int,long)>");
				newInvokeStmt = Jimple.v().newInvokeStmt(
						Jimple.v().newVirtualInvokeExpr(
								sgxObjLocal,
								toCall.makeRef(),
								Arrays.asList(getUUIDLocal, IntConstant.v(0),
										LongConstant.v(counter)))); // IntConstant.v(returnTypeIndex)));
				units.insertBefore(newInvokeStmt, currProStmt);
				counter++;

				indexwriter("" + leftType);// l
				indexwriter("" + (Index));// l
				if(identityArray.containsKey(Arrvalue)){
					indexwriter("" + identityArray.get(Arrvalue).substring(5,6));// l
					identityArray.put(leftOpValue,identityArray.get(Arrvalue));
				}
				else {
					indexwriter("" + (-1));// l
				}
				indexwriter("" + (4));// l
				indexwriter("" + (-1));
				indexwriter("" + (-1));
				indexwriter("" + (-1));
				indexwriter("" + leftBase);

				G.v().out.println("add: 处理多维数组的赋值");

			}
			units.remove(currProStmt);
			return null;

		}
		// int a=arr[0] 右边是一维数组引用类型（数组+下标）
		else if (rightOp instanceof ArrayRef
				&& TypeIndex(((ArrayRef) ((AssignStmt) currProStmt)
						.getRightOp()).getBase()) <= 12) {
			G.v().out.println("right op: " + rightOp);
			G.v().out.println("getBase: "
					+ ((ArrayRef) ((AssignStmt) currProStmt).getRightOp())
							.getBase());
			G.v().out.println("getBase type:  "
					+ ((ArrayRef) ((AssignStmt) currProStmt).getRightOp())
							.getBase().getType());
			G.v().out.println("type int :"
					+ TypeIndex(((ArrayRef) ((AssignStmt) currProStmt)
							.getRightOp()).getBase()));
			Value Arrvalue = ((ArrayRef) ((AssignStmt) currProStmt)
					.getRightOp()).getBase();
			Value Indevalue = ((ArrayRef) ((AssignStmt) currProStmt)
					.getRightOp()).getIndex();
			String rightBase="";
		
			val_type=TypeIndex(Arrvalue);
			pos_index = typeToList(val_type).indexOf(Arrvalue);
			rightBase= val_type * 10 + pos_index+"";// 右侧敏感数组的逻辑位置
			
		
			String Index = "";
			flag=true;
			if (Indevalue instanceof Constant) {
				Index = "int_" + Integer.parseInt(Indevalue.toString());
			} else {
				val_type = TypeIndex(Indevalue);
				pos_index = typeToList(val_type).indexOf(Indevalue);
				Index = "" + (val_type * 100 + pos_index);// 右侧敏感数组下标是变量 该变量的逻辑位置
			}
			Value leftValue = ((AssignStmt) currProStmt).getLeftOp();

			int leftType = TypeIndex(leftOpValue);
			int leftPosIndex = typeToList(leftType).indexOf(leftOpValue);
			int leftBase = leftType * 100 + leftPosIndex;// 左侧变量的逻辑位置

			SootMethod toCall = Scene.v().getMethod(
					"<invoker.sgx_invoker: void setCuuid(java.lang.String)>");

			toCall = Scene
					.v()
					.getMethod(
							"<invoker.sgx_invoker: void updateValueInEnclave(java.lang.String,int,long)>");
			Stmt newInvokeStmt = Jimple.v().newInvokeStmt(
					Jimple.v().newVirtualInvokeExpr(
							sgxObjLocal,
							toCall.makeRef(),
							Arrays.asList(getUUIDLocal, IntConstant.v(0),
									LongConstant.v(counter)))); // IntConstant.v(returnTypeIndex)));
			units.insertBefore(newInvokeStmt, currProStmt);
			units.remove(currProStmt);
			counter++;
			indexwriter("" +TypeIndex(Arrvalue) );// 2022 08 19
			indexwriter("" + (Index));// l
			indexwriter("" + rightBase);
			indexwriter("2");// l
			indexwriter("" + (-1));
			indexwriter("" + (-1));
			indexwriter("" + leftBase);
			indexwriter("" + (-1));

			G.v().out.println("add: 处理一维数组的赋值");

			return null;

		} else if (condVals.contains(leftOpValue)
				&& rightOp instanceof ArrayRef
				&& !condVals.contains(((ArrayRef) ((AssignStmt) currProStmt)
						.getRightOp()).getBase())) { //

			Local tmpGetIndex = Jimple.v().newLocal(
					"tmpGetIndex" + Long.toString(counter),
					values.get(0).getType());
			aBody.getLocals().add(tmpGetIndex);
			localArray.add(tmpGetIndex);

			DefinitionStmt assignStmts = initAssignStmt(tmpGetIndex);
			units.insertAfter(assignStmts, lastIdentityStmt);

			AssignStmt assignStmt = Jimple.v().newAssignStmt(
					tmpGetIndex,
					((ArrayRef) ((AssignStmt) currProStmt).getRightOp())
							.getIndex());
			units.insertBefore(assignStmt, currProStmt);
			replaceValueGetStmt(aBody, sgxObjLocal, units, localArray,
					assignStmt, null, getUUIDLocal, memberVariables,
					staticmemberVariables);// tempget = get();
			((ArrayRef) ((AssignStmt) currProStmt).getRightOp())
					.setIndex(tmpGetIndex);// i7 = $r1[tempget];

			Local tmpGetValue = Jimple.v().newLocal(
					"tmpGetValue" + Long.toString(counter),
					values.get(0).getType());
			aBody.getLocals().add(tmpGetValue);
			localArray.add(tmpGetValue);

			assignStmts = initAssignStmt(tmpGetValue);
			units.insertAfter(assignStmts, lastIdentityStmt);

			assignStmt = Jimple.v().newAssignStmt(tmpGetValue,
					((AssignStmt) currProStmt).getRightOp()); // tmpGetValue =
																// $r1[tempget];
			units.insertBefore(assignStmt, currProStmt);
			((AssignStmt) currProStmt).setRightOp(tmpGetValue);// i7 =
																// tmpGetValue;
			values.set(0, tmpGetValue);
			G.v().out.println("+++++++" + currProStmt + "+++++update+++++++++");
		} else if (values.get(0) instanceof JLengthExpr
				&& !(condVals.contains(((JLengthExpr) values.get(0)).getOp()) || identityArray
						.containsKey(((JLengthExpr) values.get(0)).getOp()))) {

			Local tmpGetLength = Jimple.v().newLocal(
					"tmpGetLength" + Long.toString(counter),
					values.get(0).getType());
			aBody.getLocals().add(tmpGetLength);
			localArray.add(tmpGetLength);

			DefinitionStmt assignStmts = initAssignStmt(tmpGetLength);
			units.insertAfter(assignStmts, lastIdentityStmt);

			AssignStmt assignStmt = Jimple.v().newAssignStmt(tmpGetLength,
					values.get(0));
			units.insertBefore(assignStmt, currProStmt);
			((AssignStmt) currProStmt).setRightOp(tmpGetLength);
			values.set(0, tmpGetLength);
			G.v().out.println("+++++++" + currProStmt + "+++++update+++++++++");

		} else if (rightOp instanceof FieldRef) {
			G.v().out.println("=======G=======" + rightOp.toString());

			SootField sField = ((FieldRef) rightOp).getField();
			G.v().out.println("SootField sField：" + sField);
			G.v().out.println("SootField sField：" + sField.getName());
			G.v().out.println("curPr:" + currProStmt.toString());
			G.v().out.println(memberVariables.containsKey(aBody.getMethod()
					.getDeclaringClass().getName()));
			// G.v().out.println(memberVariables.get(aBody.getMethod().getDeclaringClass().getName()).containsKey(sField.getName()));
			if (memberVariables.containsKey(aBody.getMethod()
					.getDeclaringClass().getName())
					&& memberVariables.get(
							aBody.getMethod().getDeclaringClass().getName())
							.containsKey(sField.getName())) { // this sootfield
																// is sensitive
				G.v().out.println("SootField is sensitve20201029!");

				Value ssValue = null;
				G.v().out
						.println("=======o======="
								+ ((AssignStmt) currProStmt).getRightOp()
										.getUseBoxes());

				Iterator<ValueBox> ubIt = ((AssignStmt) currProStmt)
						.getRightOp().getUseBoxes().iterator();
				while (ubIt.hasNext()) {
					ValueBox vBox = ubIt.next();
					ssValue = vBox.getValue();
					break;
				}
				// G.v().out.println("ssValue: "+ssValue.toString()+";");
				SootFieldRef sootFieldRef = Scene.v().makeFieldRef(
						sField.getDeclaringClass(), "Cuuid",
						RefType.v("java.lang.String"), false);
				G.v().out.println("sootFieldRef: " + sootFieldRef.toString()
						+ ";");
				FieldRef fieldRef = Jimple.v().newInstanceFieldRef(ssValue,
						sootFieldRef);
				G.v().out.println("fieldRef: " + fieldRef.toString() + ";");
				Local tmpCuuid = Jimple.v().newLocal(
						"tmpCuuid" + Long.toString(counter),
						RefType.v("java.lang.String"));
				aBody.getLocals().add(tmpCuuid);
				localArray.add(tmpCuuid);
				AssignStmt asStmt = Jimple.v()
						.newAssignStmt(tmpCuuid, fieldRef);
				G.v().out.println("asStmt: " + asStmt.toString() + ";");
				units.insertBefore(asStmt, currProStmt);

				int symbol = memberVariables.get(
						aBody.getMethod().getDeclaringClass().getName()).get(
						sField.getName());
				if (TypeIndex(leftOpValue) >= 7) { // this field is array type
													// feild
					if (!SenstiveFieldArray.containsKey(leftOpValue)) {
						SenstiveFieldArray.put(leftOpValue, symbol);
						SenstiveFieldIndexArray.put(leftOpValue, -1);
						SenstiveFieldCuuidArray.put(leftOpValue, tmpCuuid);
						G.v().out
								.println("SenstiveFieldCuuidArray leftOpValue:"
										+ leftOpValue);
						G.v().out.println("SenstiveFieldCuuidArray value:"
								+ tmpCuuid);
					}
					units.remove(currProStmt);
					return null;
				} else {
					// this field is 1-6 type feild

					if (identityArray.containsKey(leftOpValue)) {
						return_index = identityArray.get(leftOpValue); // maybe
																		// "call+number"
					} else {
						val_type = TypeIndex(leftOpValue);// int or float
						pos_index = typeToList(val_type).indexOf(leftOpValue);
						return_index = Integer.toString(val_type * 100
								+ pos_index);
					}

					G.v().out.println("indexWriter 2: "
							+ Integer.toString(TypeIndex(leftOpValue)));

					if(!flag){
						indexwriter(Integer.toString(TypeIndex(leftOpValue)));
					}else{
						indexwriter(Integer.toString(7));
					}
					indexwriter(Integer.toString(symbol));// tuple-1
					indexwriter(left_flag_index);// tuple-1
					indexwriter(right_index);// tuple-2
					indexwriter(right_flag_index);// tuple-2
					indexwriter("-1");
					indexwriter(return_index);
					indexwriter(return_flag_index);

					SootMethod toCall = Scene.v().getMethod(
							"<invoker.sgx_invoker: void clear()>");
					Stmt newInvokeStmt = Jimple.v().newInvokeStmt(
							Jimple.v().newVirtualInvokeExpr(sgxObjLocal,
									toCall.makeRef(), Arrays.asList()));
					// 0527new solution for merging update function
					// units.insertBefore(newInvokeStmt, currProStmt);
					/*
					 * toCall = Scene.v().getMethod(
					 * "<invoker.sgx_invoker: void setCounter(long)>");
					 * newInvokeStmt = Jimple.v().newInvokeStmt(
					 * Jimple.v().newVirtualInvokeExpr (sgxObjLocal,
					 * toCall.makeRef(),
					 * Arrays.asList(LongConstant.v(counter)))); //0527new
					 * solution for merging update function
					 * units.insertBefore(newInvokeStmt, currProStmt);
					 */
					toCall = Scene
							.v()
							.getMethod(
									"<invoker.sgx_invoker: void setCuuid(java.lang.String)>");
					newInvokeStmt = Jimple.v().newInvokeStmt(
							Jimple.v().newVirtualInvokeExpr(sgxObjLocal,
									toCall.makeRef(), Arrays.asList(tmpCuuid)));
					units.insertBefore(newInvokeStmt, currProStmt);
					toCall = Scene
							.v()
							.getMethod(
									"<invoker.sgx_invoker: void updateValueInEnclave(java.lang.String,int,long)>");
					newInvokeStmt = Jimple.v().newInvokeStmt(
							Jimple.v().newVirtualInvokeExpr(
									sgxObjLocal,
									toCall.makeRef(),
									Arrays.asList(getUUIDLocal,
											IntConstant.v(1),
											LongConstant.v(counter)))); // IntConstant.v(returnTypeIndex)));
					units.insertBefore(newInvokeStmt, currProStmt);
					units.remove(currProStmt);
					counter++;
					return null;
				}

			} else { // this field is not sensitive
				G.v().out.println("this field is not sensitive !");
				Local tmpGetField = Jimple.v().newLocal(
						"tmpGetField" + Long.toString(counter),
						rightOp.getType());// leftOpValue
				aBody.getLocals().add(tmpGetField);
				localArray.add(tmpGetField);

				DefinitionStmt assignStmt = initAssignStmt(tmpGetField);
				units.insertAfter(assignStmt, lastIdentityStmt);

				assignStmt = Jimple.v().newAssignStmt(tmpGetField, rightOp);// temp
																			// =
																			// sootfield;
				units.insertBefore(assignStmt, currProStmt);

				((AssignStmt) currProStmt).setRightOp(tmpGetField);
				values.set(0, tmpGetField);
			}
		} else if (rightOp instanceof StaticFieldRef) {
			SootField sField = ((StaticFieldRef) rightOp).getField();
			if (staticmemberVariables.containsKey(currProStmt.getClass()
					.getName())
					&& staticmemberVariables.get(
							currProStmt.getClass().getName()).contains(
							sField.getName())) { // this sootfield is sensitive
				G.v().out.println("static SootField is sensitve!");
				// waiting for new step 0527
			} else { // this field is not sensitive
				Local tmpGetStaticField = Jimple.v().newLocal(
						"tmpGetStaticField" + Long.toString(counter),
						rightOp.getType());// leftOpValue
				aBody.getLocals().add(tmpGetStaticField);
				localArray.add(tmpGetStaticField);

				DefinitionStmt assignStmt = initAssignStmt(tmpGetStaticField);
				units.insertAfter(assignStmt, lastIdentityStmt);

				assignStmt = Jimple.v().newAssignStmt(tmpGetStaticField,
						rightOp);// temp = sootfield;
				units.insertBefore(assignStmt, currProStmt);

				((AssignStmt) currProStmt).setRightOp(tmpGetStaticField);
				values.set(0, tmpGetStaticField);
			}
		}

		ArrayList<Value> rightCondValue = new ArrayList<Value>();
		for (Value val : values) {
			if ((val instanceof InstanceInvokeExpr)
					|| (val instanceof JStaticInvokeExpr)
					|| (val instanceof StaticFieldRef)
					|| (val instanceof JInstanceFieldRef)) {
				RightOpIsInvoke = true;
				G.v().out.println("+++++++RightOpIsInvoke" + RightOpIsInvoke);
			}

			// || (val instanceof JStaticInvokeExpr) || (val instanceof
			// JInstanceFieldRef)
			// if(val instanceof StaticFieldRef){
			// G.v().out.println("isRightOpStaticFiled:********"+isRightOpStaticFiled+"*************");
			// isRightOpStaticFiled = true;
			// }
			Iterator<ValueBox> vbIterator = val.getUseBoxes().iterator();
			while (vbIterator.hasNext()) {
				Value tValue = vbIterator.next().getValue();
				G.v().out.println("+++++++tvalue" + currProStmt);
				rightCondValue.add(tValue);
			}
			// if(condVals.contains(val))
			// isRightOpInCondVal=true;
			// if(val instanceof ParameterRef){
			// G.v().out.println("the ParameterRef is: "+val);
			// localArray.add(val);
			// ((ParameterRef)val).
			// }

		}
		G.v().out.println("rightCondValue1: " + rightCondValue);
		G.v().out.println("condVals: " + condVals);
		rightCondValue.retainAll(condVals); // n
		G.v().out.println("rightCondValue2: " + rightCondValue);

		// to process stmt like x=invoke(temp1) or x=invoke(y)
		// if(RightOpIsInvoke){
		// // G.v().out.println("start insert an invoke tmp");
		// Local tmpValue =
		// Jimple.v().newLocal("tmpResult"+Long.toString(counter),
		// leftOpValue.getType());
		// aBody.getLocals().add(tmpValue);
		// localArray.add(tmpValue);
		// G.v().out.println("RightOpIsInvoke tmpValue: "+tmpValue.toString());
		//
		// //insert tmpValue init stmt after all IdentityStmts
		// DefinitionStmt assignStmt = initAssignStmt(tmpValue);
		// G.v().out.println("newAssignStmt is: "+assignStmt.toString());
		// G.v().out.println("lastIdentityStmt is: "+lastIdentityStmt.toString());
		// units.insertAfter(assignStmt, lastIdentityStmt);
		//
		// //insert tmp=a[x] or tmp=a[b]
		// assignStmt = Jimple.v().newAssignStmt(tmpValue,rightOp); //tem =
		// invoker
		// G.v().out.println("newAssignStmt is: "+assignStmt.toString());
		// //G.v().out.println("currProStmt is: "+currProStmt.toString());
		// units.insertBefore(assignStmt, currProStmt);
		// //G.v().out.println("currProStmt is: "+currProStmt.toString());
		// if(!rightCondValue.isEmpty()){//del with tmp=a[x]
		// // rightOp = assignStmt.getRightOp();
		// // leftOpValue = assignStmt.getLeftOp();
		// replaceValueGetStmt(aBody, sgxObjLocal, units, localArray,
		// (Unit)assignStmt, rightCondValue,getUUIDLocal);
		// }
		//
		// G.v().out.println("newInvokeStmt to insert is: ++++++++++++++++++++++++++ "+assignStmt+"++++++++++++++++++++++");
		// G.v().out.println("start insert before currStmt: ++++++++++++++++++++++++++ "+currProStmt+"++++++++++++++++++++++");
		// //
		// G.v().out.println("InvokeExpr class is: ++++++++++++++++++++++++++ "+rightOp.getClass()+"++++++++++++++++++++++");
		// ((AssignStmt)currProStmt).setRightOp(tmpValue);
		// //
		// G.v().out.println("InvokeExpr class is: ++++++++++++++++++++++++++ "+tmpValue.getClass()+"++++++++++++++++++++++");
		// G.v().out.println("currStmt: ++++++++++++++++++++++++++ "+currProStmt+"++++++++++++++++++++++");
		// values.clear();
		// operator.clear();
		// analyzeExp(tmpValue, values, operator, cons, variable);//
		// }

		for (String local : operator) {
			symbolString = local;
			G.v().out.println("operator:********" + local + "*************");
		}
		SootMethod toCall = Scene.v().getMethod(
				"<invoker.sgx_invoker: void clear()>");
		Stmt newInvokeStmt = Jimple.v().newInvokeStmt(
				Jimple.v().newVirtualInvokeExpr(sgxObjLocal, toCall.makeRef(),
						Arrays.asList()));
		// 0527new solution for merging update function
		// units.insertBefore(newInvokeStmt, currProStmt);
		/*
		 * toCall =
		 * Scene.v().getMethod("<invoker.sgx_invoker: void setCounter(long)>");
		 * newInvokeStmt = Jimple.v().newInvokeStmt(
		 * Jimple.v().newVirtualInvokeExpr (sgxObjLocal, toCall.makeRef(),
		 * Arrays.asList(LongConstant.v(counter)))); G.v().out.println(
		 * "start insert before currStmt0603: ++++++++++++++++++++++++++ "
		 * +currProStmt+"++++++++++++++++++++++");
		 * 
		 * //0527new solution for merging update function
		 * units.insertBefore(newInvokeStmt, currProStmt);
		 */

		G.v().out.println("=leftOpValue.type=="
				+ leftOpValue.getType().toString());
		G.v().out.println("=leftOpValue==" + leftOpValue);

		boolean isNeedCuuidFlag = false;
		Value tempCuuidValue = null;
		boolean arrayAssign=false;
		Value Arrvalue=null;
		/**
		 * edit on 4.26 for array re
		 */
		// a[i0] = 1;
		if (leftOpValue instanceof ArrayRef) {
			Arrvalue = ((ArrayRef) ((AssignStmt) currProStmt)
					.getLeftOpBox().getValue()).getBase();
			Value Indevalue = ((ArrayRef) ((AssignStmt) currProStmt)
					.getLeftOpBox().getValue()).getIndex();
			flag=true;
			arrayAssign=true;
			/**
			 * deal with Base Arrvalue
			 */
			G.v().out.println("A");
			if (SenstiveFieldArray.containsKey(Arrvalue)) {
				return_flag_index = SenstiveFieldArray.get(Arrvalue).toString();
				G.v().out.println("[SenstiveFieldArray]:" + return_flag_index);
				right_flag_index = SenstiveFieldIndexArray.get(Arrvalue)
						.toString();
				G.v().out.println("[SenstiveFieldArray]:" + right_flag_index);
				tempCuuidValue = SenstiveFieldCuuidArray.get(Arrvalue);
				isNeedCuuidFlag = true;
			} else if (identityArray.containsKey(Arrvalue)) {
				val_type = TypeIndex(Arrvalue);// int or float
				pos_index = typeToList(val_type).indexOf(Arrvalue);
				return_flag_index = Integer.toString(val_type * 10 + pos_index);
				right_index="2";//right标志为2表示数组元素更新
				
			}else if (condValsArrayInt.contains(Arrvalue)) {//a[0]=1 or a[i]=1 a是普通數組
				val_type = TypeIndex(Arrvalue);// int or float
				G.v().out.println(val_type);
				pos_index = typeToList(val_type).indexOf(Arrvalue);
				return_flag_index = Integer.toString(val_type * 10 + pos_index);
				right_index="2";//right标志为2表示数组元素更新
				G.v().out.println("20220706 arr[i]=0 or arr [0]=0");
				
			} else if (condValsArrayChar.contains(Arrvalue)) {//a[0]=1 or a[i]=1 a是普通數組
				val_type = TypeIndex(Arrvalue);// int or float
				G.v().out.println(val_type);
				pos_index = typeToList(val_type).indexOf(Arrvalue);
				return_flag_index = Integer.toString(val_type * 10 + pos_index);
				right_index="2";//right标志为2表示数组元素更新
				G.v().out.println("20220706 arr[i]=0 or arr [0]=0");
				
			}
			else {
				val_type = TypeIndex(Arrvalue);// int or float
				pos_index = typeToList(val_type).indexOf(Arrvalue);
				return_flag_index = Integer.toString(val_type * 10 + pos_index);
			}

			/**
			 * deal with Index Indevalue
			 */
			int list_size = 0;
			int MaxSize = (localArray.size() > N) ? N : localArray.size();
			Random rand = new Random();
			if(Indevalue instanceof Constant){
				return_index="int_"+Indevalue.toString();
			}
			else if (condVals.contains(Indevalue)) {
				val_type = TypeIndex(Indevalue);// int or float
				pos_index = typeToList(val_type).indexOf(Indevalue);
				return_index = Integer.toString(val_type * 100 + pos_index);//

				G.v().out.println("210628Indevalue:" + Indevalue);
				G.v().out.println("210628pos_index:" + pos_index);
				G.v().out.println("210628left_index" + left_index);
			} else {
				for (Local loc : localArray) {// 将variable随机插入localarray
					if ((loc.equals(Indevalue)) && (list_size >= MaxSize - 1)) {
						int index_random = rand.nextInt(MaxSize - 1);
						localArray.remove(loc);
						localArray.add(index_random, loc);
					}
					list_size++;
				}
				for (Local loc : localArray) {
					if (!isTypeCompatible(Indevalue.getType(), loc.getType()))
						continue;
					if ((loc.equals(Indevalue) || (rand.nextDouble() <= ratio))
							&& (index < N)) {
						if (loc.equals(Indevalue)) {
							return_index = Integer.toString(index);//
							setParam0 = true;
						}
						if (!condVals.contains(loc)) {
							newInvokeStmt = prepareInsertStmt(loc, sgxObjLocal,
									"invoker.sgx_invoker");// 只add类型相同的变量
							units.insertBefore(newInvokeStmt, currProStmt);
							index++;
						}
					}
				}
				if (!setParam0) {
					if (Indevalue instanceof ParameterRef) {
						G.v().out
								.println("the only @paraRef Indevalue Value is: "
										+ Indevalue);
						// new local = @paraRef1
					} else if (Indevalue instanceof Constant) {
						return_index = ((Value) (Indevalue)).getType()
								.toString() + "_" + Indevalue;
						setParam0 = true;
					}
				}
			}
		} else if (TypeIndex(leftOpValue) > 6) { // r1 = r2
			
			G.v().out.println("r1=r2");
			
			G.v().out.println("TypeIndex > 6:" + leftOpValue);
			G.v().out.println("TypeIndex > 6:identityArray = "
					+ identityArray.toString());
			G.v().out.println("double arrays"+condValsArrayDouble);
			G.v().out.println("left: "+leftOpValue+" right: "+rightOp);
			if (identityArray.containsKey(leftOpValue)) {

				return_flag_index = identityArray.get(leftOpValue); // maybe
																	// "call+number"
				G.v().out.println("if one d array left: "+val_type+" "+pos_index);
			} else {
				val_type = TypeIndex(leftOpValue);// int or float
				pos_index = typeToList(val_type).indexOf(leftOpValue);
				return_flag_index = Integer.toString(val_type * 10 + pos_index);
				G.v().out.println("else one d array left: "+val_type+" "+pos_index);
			}
			if (identityArray.containsKey(rightOp)) {
				G.v().out.println("if one d array right: "+val_type+" "+pos_index);
				return_flag_index = identityArray.get(rightOp); // maybe
																	// "call+number"
			} else {
				val_type = TypeIndex(rightOp);// int or float
				pos_index = typeToList(val_type).indexOf(rightOp);
				left_flag_index= Integer.toString(val_type * 10 + pos_index);
				G.v().out.println("else one d array right: "+val_type+" "+pos_index);
			}
			toCall = Scene
					.v()
					.getMethod(
							"<invoker.sgx_invoker: void updateValueInEnclave(java.lang.String,int,long)>");
			newInvokeStmt = Jimple.v().newInvokeStmt(
					Jimple.v().newVirtualInvokeExpr(
							sgxObjLocal,
							toCall.makeRef(),
							Arrays.asList(getUUIDLocal, IntConstant.v(0),
									LongConstant.v(counter)))); // IntConstant.v(returnTypeIndex)));
			units.insertBefore(newInvokeStmt, currProStmt);
			units.remove(currProStmt);
			counter++;
			
			G.v().out.println("left: "+leftOpValue+"  pos: "+return_flag_index);
			G.v().out.println("right: "+rightOp+"  pos:"+left_flag_index);
			//同维度数组之间相互赋值
			indexwriter(""+TypeIndex(leftOpValue));
			indexwriter("-1");
			indexwriter(left_flag_index);
			indexwriter("3");
			indexwriter("-1");
			indexwriter("-1");
			indexwriter("-1");
			indexwriter(return_flag_index);
			return null;
			
			
		} else { // i0 = i1
			int returnTypeIndex = TypeIndex(leftOpValue);// return value type
															// index
			G.v().out.println("returnTypeIndex=" + returnTypeIndex);
			pos_index = typeToList(returnTypeIndex).indexOf(leftOpValue);
			G.v().out.println("pos_index=" + pos_index);
			return_index = Integer.toString(returnTypeIndex * 100 + pos_index);
			G.v().out.println("return_index=" + return_index);
		}

		/**
		 * for type
		 */

		val_type = TypeIndex(values.get(0));
		G.v().out.println("values.get(0)=" + values.get(0));

		pos_index = typeToList(val_type).indexOf(values.get(0)); // 获取全局数组中
																	// 它的index
		G.v().out.println("pos_index=" + pos_index);
		G.v().out.println("123321");
		//当求数组长度时 type用7表示
		if(!isArrayLength&&!arrayAssign){
			G.v().out.println("not array length type: "+val_type);
			indexwriter(""+Integer.toString(val_type));// tuple-0: opOne's type
		}
		if(arrayAssign){
			indexwriter(""+TypeIndex(Arrvalue));
		}
		
		G.v().out
				.println("<<<<<<>>>>>> tuple-0 update: ++++++++++++++++++++++++++ "
						+ Integer.toString(val_type) + "++++++++++++++++++++++");

		/**
		 * for value1 value2
		 */
		boolean two_dim_flag = false; // add on 0529 2020
		int list_size = 0;
		int MaxSize = (localArray.size() > N) ? N : localArray.size();
		Random rand = new Random();
		G.v().out.println("values.size=" + values.size());
		if (values.size() == 1) {//这里处理int a=arr[0]  拿到前面处理了 // int a=arr[0] 右边是一维数组引用类型（数组+下标） 
			G.v().out.println("0515 :" + values.get(0) + " "
					+ values.get(0).getType());
			/**
			 * if value1 is arrayref
			 */
			if (values.get(0) instanceof ArrayRef
					&& TypeIndex(values.get(0)) <= 6
					&& TypeIndex(values.get(0)) != -1) {
				G.v().out
						.println("???--> values.get(0) instanceof ArrayRef && TypeIndex(values.get(0))<=6 && TypeIndex(values.get(0)) != -1 :");
				Arrvalue = ((ArrayRef) ((AssignStmt) currProStmt)
						.getRightOp()).getBase();
				Value Indevalue = ((ArrayRef) ((AssignStmt) currProStmt)
						.getRightOp()).getIndex();
				/**
				 * deal with Base Arrvalue
				 */
				// G.v().out.println(MultiBaseMap.containsKey(Arrvalue));
				if (SenstiveFieldArray.containsKey(Arrvalue)) {

					left_flag_index = SenstiveFieldArray.get(Arrvalue)
							.toString();
					right_index = SenstiveFieldIndexArray.get(Arrvalue)
							.toString();
					G.v().out.println("left_flag_index:" + left_flag_index
							+ " right_index:" + right_index);
					tempCuuidValue = SenstiveFieldCuuidArray.get(Arrvalue);
					G.v().out.println("tempCuuidValue :" + tempCuuidValue);
					isNeedCuuidFlag = true;
				} else if (MultiBaseMap.containsKey(Arrvalue)) {
					left_flag_index = MultiBaseMap.get(Arrvalue).toString();
					right_index = MultiIndexMap.get(Arrvalue).toString();
					G.v().out.println("left_flag_index:" + left_flag_index
							+ " right_index:" + right_index);

				} else if (identityArray.containsKey(Arrvalue)) {
					left_flag_index = identityArray.get(Arrvalue); // maybe
																	// "call+number"
				} else if (condVals.contains(Arrvalue)) {
					val_type = TypeIndex(Arrvalue);// int or float
					pos_index = typeToList(val_type).indexOf(Arrvalue);
					G.v().out.println(" 0427 pos_index:" + pos_index
							+ " Arrvalue:" + Arrvalue);
					left_flag_index = Integer.toString(val_type * 10
							+ pos_index);
				}
				/**
				 * deal with Index Indevalue
				 */
				if (condVals.contains(Indevalue)) {

					val_type = TypeIndex(Indevalue);// int or float
					pos_index = typeToList(val_type).indexOf(Indevalue);
					left_index = Integer.toString(val_type * 100 + pos_index);//

					G.v().out.println("2752Indevalue:" + Indevalue);
					G.v().out.println("2753pos_index:" + pos_index);
					G.v().out.println("2754left_index" + left_index);

				} else {
					G.v().out.println("2758  将variable随机插入localarray");
					for (Local loc : localArray) {// 将variable随机插入localarray
						if ((loc.equals(Indevalue))
								&& (list_size >= MaxSize - 1)) {
							int index_random = rand.nextInt(MaxSize - 1);
							localArray.remove(loc);
							localArray.add(index_random, loc);
						}
						list_size++;
					}
					for (Local loc : localArray) {
						if (!isTypeCompatible(Indevalue.getType(),
								loc.getType()))
							continue;
						if ((loc.equals(Indevalue) || (rand.nextDouble() <= ratio))
								&& (index < N)) {
							if (loc.equals(Indevalue)) {
								left_index = Integer.toString(index);//
								G.v().out.println("2773 set left_index:"
										+ left_index);
								setParam0 = true;
							}
							if (!condVals.contains(loc)) {
								newInvokeStmt = prepareInsertStmt(loc,
										sgxObjLocal, "invoker.sgx_invoker");// 只add类型相同的变量
								G.v().out.println("0515 newInvokeStmt:"
										+ newInvokeStmt);
								units.insertBefore(newInvokeStmt, currProStmt);
								index++;
							}
						}
					}
					if (!setParam0) {
						if (Indevalue instanceof ParameterRef) {
							G.v().out
									.println("the only @paraRef Indevalue Value is: "
											+ Indevalue);
							// new local = @paraRef1
						} else if (Indevalue instanceof Constant) {
							left_index = ((Value) (Indevalue)).getType()
									.toString() + "_" + Indevalue;
							setParam0 = true;
						}
					}
				}
			} else if (TypeIndex(values.get(0)) > 6
					&& !(values.get(0) instanceof ArrayRef)
					&& identityArray.containsKey(values.get(0))) { // r1 = r2
																	// flag

				left_flag_index = identityArray.get(values.get(0)); // maybe
																	// "call+number"
				G.v().out.println("r1 = r2 flag");
				// left_index = "1";//in enclave
			} else if (TypeIndex(values.get(0)) > 6
					&& !(values.get(0) instanceof ArrayRef)
					&& condVals.contains(values.get(0))) { // 60 or70 flag
				val_type = TypeIndex(values.get(0));
				pos_index = typeToList(val_type).indexOf(values.get(0));
				left_flag_index = Integer.toString(val_type * 10 + pos_index);
				G.v().out.println("60 or70	flag");
				// left_index = "1";//in enclave
			} else if (condVals.contains(values.get(0))) {
				if(values.get(0) instanceof Constant){
					left_index="int_"+values.get(0).toString();
				}else{
					val_type = TypeIndex(values.get(0));// int or float flag
					pos_index = typeToList(val_type).indexOf(values.get(0));
					left_index = Integer.toString(val_type * 100 + pos_index);//
				}
				
				G.v().out.println("int or float flag");

				G.v().out.println("values.get(0):" + values.get(0));
				G.v().out.println("pos_index:" + pos_index);
				G.v().out.println("2814 set left_index" + left_index);
			}
			/**
			 * deal with "i0 = lengthof r0;"
			 */
			else if (values.get(0) instanceof JLengthExpr
					&& (condVals
							.contains(((JLengthExpr) values.get(0)).getOp()) || identityArray
							.containsKey(((JLengthExpr) values.get(0)).getOp()))) {
				G.v().out.println("[0527]=curr method is==" + currProStmt
						+ " and rightop is:" + values.get(0) + " Array is"
						+ ((JLengthExpr) values.get(0)).getOp());
				Iterator<ValueBox> ubIt = currProStmt.getUseBoxes().iterator();
				while (ubIt.hasNext()) {
					ValueBox vBox = ubIt.next();
					Value value = vBox.getValue();
					G.v().out.println("use:" + value);
					if (identityArray.containsKey(value)) {
						
						//left_flag_index = identityArray.get(value); // maybe
						val_type = TypeIndex(value);// int or float
						indexwriter(""+val_type);// tuple-1
						pos_index = typeToList(val_type).indexOf(value);
						left_flag_index = Integer.toString(val_type * 10
								+ pos_index);										// "call+number"
						left_index = "10000"; // we define "10000" in left_index
						right_index="5";					// represent length
						G.v().out.println("3 :" + left_flag_index);
						break;
					} else if (condVals.contains(value)) {
						val_type = TypeIndex(value);// int or float
						indexwriter(""+val_type);// tuple-1
						pos_index = typeToList(val_type).indexOf(value);
						left_flag_index = Integer.toString(val_type * 10
								+ pos_index);
						left_index = "10000"; // we define "10000" in left_index
						right_index="5";				// represent length
						break;
					}
				}
			} else { // else
				G.v().out.println("add: else :" + values.get(0));

				if (TypeIndex(values.get(0)) > 6) { // array
					if (values.get(0) instanceof ArrayRef) {// 2x array
					   Arrvalue = ((ArrayRef) ((AssignStmt) currProStmt)
								.getRightOp()).getBase();
						Value Indevalue = ((ArrayRef) ((AssignStmt) currProStmt)
								.getRightOp()).getIndex();
						if (!typeToList(TypeIndex(Arrvalue)).contains(Arrvalue)) {
							typeToList(TypeIndex(Arrvalue)).add(Arrvalue);
						}
						return_flag_index = Integer
								.toString((TypeIndex(Arrvalue) - 6)
										* 10
										+ typeToList(TypeIndex(Arrvalue))
												.indexOf(Arrvalue));
						two_dim_flag = true;
					}
					Local tmpUpdateArray = Jimple.v().newLocal(
							"tmpUpdateArray" + Long.toString(counter),
							values.get(0).getType());
					aBody.getLocals().add(tmpUpdateArray);
					localArray.add(tmpUpdateArray);

					DefinitionStmt assignStmts = initAssignStmt(tmpUpdateArray);
					units.insertAfter(assignStmts, lastIdentityStmt);

					G.v().out.println("tmpUpdateArray: "
							+ tmpUpdateArray.toString());
					AssignStmt assignStmt = Jimple.v().newAssignStmt(
							tmpUpdateArray, values.get(0));
					G.v().out.println("newAssignStmt is: "
							+ assignStmt.toString());
					units.insertBefore(assignStmt, currProStmt);

					newInvokeStmt = prepareInsertStmt(tmpUpdateArray,
							sgxObjLocal, "invoker.sgx_invoker");// 只add类型相同的变量
					G.v().out.println("add: values.get(0) else array :"
							+ newInvokeStmt.toString() + "  index:" + index);
					units.insertBefore(newInvokeStmt, currProStmt);
					// left_index = "0";
					left_flag_index = "0";
				} else { // not array
					G.v().out.println("2869 not array");

					for (Local loc : localArray) {// 将variable随机插入localarray
						if ((loc.equals(values.get(0)))
								&& (list_size >= MaxSize - 1)) {
							int index_random = rand.nextInt(MaxSize - 1);
							localArray.remove(loc);
							localArray.add(index_random, loc);
						}
						list_size++;
					}
					for (Local loc : localArray) {
						if (!isTypeCompatible(values.get(0).getType(),
								loc.getType()))
							continue;
						G.v().out.println("loc=" + loc.toString());
						G.v().out
								.println("localArray=" + localArray.toString());
						if ((loc.equals(values.get(0)) || (rand.nextDouble() <= ratio))
								&& (index < N)) {
							if (loc.equals(values.get(0))) {
								// val_type = TypeIndex(values.get(0));//int or
								// float
								// left_index =
								// "1"+Integer.toString(val_type*10+index);//
								G.v().out.println("loc.equals: index:" + index);
								left_index = Integer.toString(index);//
								G.v().out.println("2891 set left_index::"
										+ left_index);
								setParam0 = true;
							}
							if (!condVals.contains(loc)) {
								newInvokeStmt = prepareInsertStmt(loc,
										sgxObjLocal, "invoker.sgx_invoker");// 只add类型相同的变量
								G.v().out.println("add: loc :"
										+ newInvokeStmt.toString() + "  index:"
										+ index);
								units.insertBefore(newInvokeStmt, currProStmt);
								index++;
							}
						}
					}
					G.v().out.println("setParam0=" + setParam0);
					if (!setParam0) {
						G.v().out.println("!setParam0");
						if (values.get(0) instanceof ParameterRef) {
							G.v().out.println("the only @paraRef Value is: "
									+ values.get(0));
							// new local = @paraRef1
						} else if (values.get(0) instanceof Constant) {
							left_index = ((Value) (values.get(0))).getType()
									.toString() + "_" + values.get(0);
							G.v().out.println("2911 set left_index:"
									+ left_index);
							setParam0 = true;
						}
					}
				}
			}
		} else if (values.size() == 2) {
			G.v().out.println("!!!!!enter values.size()==2!!!!!!!!!");
			if (condVals.contains(values.get(0))) {
				G.v().out.println("values0 is cond val" + "++++++++++++++"
						+ values.get(0));
				val_type = TypeIndex(values.get(0));// int or float
				pos_index = typeToList(val_type).indexOf(values.get(0));
				G.v().out
						.println(values.get(0) + " pos_index ==: " + pos_index);
				left_index = Integer.toString(val_type * 100 + pos_index);
				setParam0 = true;
			}
			if (condVals.contains(values.get(1))) {
				G.v().out.println("values1 is cond val" + "++++++++++++++"
						+ values.get(1));
				val_type = TypeIndex(values.get(1));// int or float
				pos_index = typeToList(val_type).indexOf(values.get(1));
				right_index = Integer.toString(val_type * 100 + pos_index);
				setParam1 = true;
			}
			if (!setParam0 && !setParam1) {
				for (Value val : values) {// variable-tobehidden;
					for (Local loc : localArray) {// 将variable随机插入localarray
						if ((loc.equals(val)) && (list_size >= MaxSize - 1)) {
							int index_random = rand.nextInt(MaxSize - 1);
							localArray.remove(loc);
							localArray.add(index_random, loc);
						}
						list_size++;
					}
				}
				for (Local loc : localArray) {
					if (!isTypeCompatible(values.get(0).getType(),
							loc.getType()))
						continue;
					if ((loc.equals(values.get(0))
							|| (loc.equals(values.get(1))) || (rand
							.nextDouble() <= ratio)) && (index < N)) {
						if (loc.equals(values.get(0))) {
							// val_type = TypeIndex(values.get(0));//int or
							// float
							left_index = Integer.toString(index);// val_type*10+index);//
							G.v().out.println("2951 set left_index"
									+ left_index);
							setParam0 = true;
						}
						if (loc.equals(values.get(1))) {
							// val_type = TypeIndex(values.get(1));//int or
							// float
							right_index = Integer.toString(index);//
							setParam1 = true;
						}
						if (!condVals.contains(loc)) {
							newInvokeStmt = prepareInsertStmt(loc, sgxObjLocal,
									"invoker.sgx_invoker");// 只add类型相同的变量
							units.insertBefore(newInvokeStmt, currProStmt);
							index++;
						}
					}
				}
			} else if (!setParam0) {
				for (Local loc : localArray) {// 将variable随机插入localarray
					if ((loc.equals(values.get(0)))
							&& (list_size >= MaxSize - 1)) {
						int index_random = rand.nextInt(MaxSize - 1);
						localArray.remove(loc);
						localArray.add(index_random, loc);
					}
					list_size++;
				}
				for (Local loc : localArray) {
					if (!isTypeCompatible(values.get(0).getType(),
							loc.getType()))
						continue;
					if ((loc.equals(values.get(0)) || (rand.nextDouble() <= ratio))
							&& (index < N)) {
						if (loc.equals(values.get(0))) {
							val_type = TypeIndex(values.get(0));// int or float
							left_index = Integer.toString(index);//
							G.v().out.println("2982 set left_index"
									+ left_index);
							setParam0 = true;
						}
						if (!condVals.contains(loc)) {
							newInvokeStmt = prepareInsertStmt(loc, sgxObjLocal,
									"invoker.sgx_invoker");// 只add类型相同的变量
							units.insertBefore(newInvokeStmt, currProStmt);
							index++;
						}
					}
				}
			} else if (!setParam1) {
				for (Local loc : localArray) {// 将variable随机插入localarray
					if ((loc.equals(values.get(1)))
							&& (list_size >= MaxSize - 1)) {
						int index_random = rand.nextInt(MaxSize - 1);
						localArray.remove(loc);
						localArray.add(index_random, loc);
					}
					list_size++;
				}
				for (Local loc : localArray) {
					if (!isTypeCompatible(values.get(1).getType(),
							loc.getType()))
						continue;
					if ((loc.equals(values.get(1)) || (rand.nextDouble() <= ratio))
							&& (index < N)) {
						if (loc.equals(values.get(1))) {
							val_type = TypeIndex(values.get(1));// int or float
							right_index = Integer.toString(index);//
							setParam1 = true;
						}
						// if(!condVals.contains(loc)){
						// newInvokeStmt = prepareInsertStmt(loc, sgxObjLocal,
						// "invoker.sgx_invoker");//只add类型相同的变量
						// G.v().out.println("after prepareInsertStmt ,newInvokeStmt="+newInvokeStmt);
						// units.insertBefore(newInvokeStmt, currProStmt);
						// index++;
						// }
					}
				}
			}

			G.v().out.println("-----------8.1------------");
			if (!setParam0) {
				left_index = ((Value) (values.get(0))).getType().toString()
						+ "_" + values.get(0);
				setParam0 = true;
			}
			if (!setParam1) {
				right_index = ((Value) (values.get(1))).getType().toString()
						+ "_" + values.get(1);
				setParam1 = true;
			}
		} else {
			G.v().out
					.println("********error: values size isnot 1 nor 2!********");
		}
		G.v().out.println("indexwriter 3:");
		G.v().out.println("left_index:" + left_index);
		G.v().out.println("left_flag_index:" + left_flag_index);
		G.v().out.println("right_index:" + right_index);
		G.v().out.println("right_flag_index:" + right_flag_index);
		indexwriter(left_index);// tuple-1
		indexwriter(left_flag_index);// tuple-1
		indexwriter(right_index);// tuple-2
		indexwriter(right_flag_index);// tuple-2
		if (!operator.isEmpty()) {
			if (symbolString.equals(" + "))
				indexwriter("1");
			else if (symbolString.equals(" - ") || symbolString.equals(" cmp ")
					|| symbolString.equals(" cmpl ")
					|| symbolString.equals(" cmpg "))
				indexwriter("2");
			else if (symbolString.equals(" * "))
				indexwriter("3");
			else if (symbolString.equals(" / "))
				indexwriter("4");
			else if (symbolString.equals(" % "))
				indexwriter("5");
			else if (symbolString.equals(" & ")) { 
				indexwriter("12");
			} else if (symbolString.equals(" | ")) { 
				indexwriter("13");
			} else if (symbolString.equals(" ^ ")) { 
				indexwriter("14");
			}  else if (symbolString.equals(" << ")) { 
				indexwriter("15");
			}  else if (symbolString.equals(" >>> ")) { 
				indexwriter("16");
			}  else if (symbolString.equals(" >>> ")) { 
				indexwriter("17");
			} 
			
			else {
				G.v().out.println("not normal operator:" + operator);
				indexwriter("-1");
			}
		} else {
			indexwriter("-1");
		}
		indexwriter(return_index);
		indexwriter(return_flag_index);
		G.v().out.println("return_index:" + return_index);
		G.v().out.println("return_flag_index:" + return_flag_index);
		G.v().out.println("counter:" + counter);
		if (left_index == "-1")
			G.v().out.println("stmt update has no first operand:********"
					+ left_index + "*************");
		if (right_index == "-1")
			G.v().out.println("stmt update has no second operand:********"
					+ right_index + "*************");

		G.v().out.println("1111222111");
		if (isNeedCuuidFlag) {
			G.v().out.println("1111333311");
			toCall = Scene.v().getMethod(
					"<invoker.sgx_invoker: void setCuuid(java.lang.String)>");
			newInvokeStmt = Jimple.v().newInvokeStmt(
					Jimple.v().newVirtualInvokeExpr(sgxObjLocal,
							toCall.makeRef(), Arrays.asList(tempCuuidValue)));
			G.v().out
					.println("start insert before currStmt: ++++++++++++++++++++++++++ "
							+ currProStmt + "++++++++++++++++++++++");
			units.insertBefore(newInvokeStmt, currProStmt);
			G.v().out.println("1111444111");
		}
		G.v().out.println("1111555111");

		toCall = Scene
				.v()
				.getMethod(
						"<invoker.sgx_invoker: void updateValueInEnclave(java.lang.String,int,long)>");

		int status = 0;
		if (return_index.equals("-1") && !return_flag_index.equals("-1")) {
			status = 1;
			// if (two_dim_flag) {
			// status = 2;
			// }
		}
		// uupdate0603
		newInvokeStmt = Jimple.v().newInvokeStmt(
				Jimple.v().newVirtualInvokeExpr(
						sgxObjLocal,
						toCall.makeRef(),
						Arrays.asList(getUUIDLocal, IntConstant.v(status),
								LongConstant.v(counter)))); // IntConstant.v(returnTypeIndex)));
		G.v().out
				.println("newInvokeStmt to insert is: ++++++++++++++++++++++++++ "
						+ newInvokeStmt + "++++++++++++++++++++++");
		G.v().out
				.println("start insert before currStmt: ++++++++++++++++++++++++++ "
						+ currProStmt + "++++++++++++++++++++++");
		units.insertBefore(newInvokeStmt, currProStmt);
		units.remove(currProStmt);
		counter++;

		return newInvokeStmt;
	}
//转化数组静态初始化语句
    @SuppressWarnings("unused")
    private void replaceArrayStaticInitStmt(
			Body aBody,
			Local sgxObjLocal,
			PatchingChain<Unit> units,
			List<Local> localArray,
			Unit currProStmt,
			Local getUUIDLocal,
			Iterator<Unit> scanIt,
			Map<String,Map<String, Integer>> memberVariables,
			Map<String, List<String>> staticmemberVariables,
			Map<SootField, Value> OriginFieldCuuidArray) {
		// TODO Auto-generated method stub
   
    	Unit currStmt = null;
    	int totalSize = 1;  //对应一维数组大小(总元素个数)
    	int d = 1;  //数组的维度
    	String[] dimensions = new String[4];
    	List<String> data = new ArrayList<>();
    	boolean flag = true;

    	Value right = ((AssignStmt) currProStmt).getRightOp();
    	NewArrayExpr n = (NewArrayExpr)right;
    	
    	dimensions[0] = "int_"+n.getSize().toString();
    	G.v().out.println("StaticNewArrayExpr :"+n+"  "+n.getSize());
    	
    	Value left = ((AssignStmt) currProStmt).getLeftOp();
    	
    	while (scanIt.hasNext()) {
			currStmt = scanIt.next();
			if (((AssignStmt) currStmt).getRightOp() == left) {
				break;
			}
			if (!(((AssignStmt) currStmt).getRightOp() instanceof NewArrayExpr)) {
				flag = false;
			}
			if(flag){
				d++;
				right = ((AssignStmt) currStmt).getRightOp();
		    	n = (NewArrayExpr)right;
		    	dimensions[d-1] = "int_"+n.getSize().toString();
			}
			if (TypeIndex(((AssignStmt) currStmt).getRightOp()) < 7) {
				data.add(((AssignStmt) currStmt).getRightOp().getType().toString()+"_"+((AssignStmt) currStmt).getRightOp().toString());
			}
			G.v().out.println(currStmt);
			units.remove(currStmt);
		}
    	totalSize = data.size();
    	G.v().out.println("arrayStaticInitInfo:" );
    	G.v().out.println("d: " + d);
    	G.v().out.println("totalSize: " + totalSize);
    	G.v().out.println("data: " + data.toString());
    	G.v().out.println("dimensions: " + Arrays.toString(dimensions));
		G.v().out.println("currStmt: " + currStmt);
		G.v().out.println("currProStmt: " + currProStmt);
    	
		units.remove(currProStmt);
		for(int i =0;i<4;i++){
			if(dimensions[i]==null){
				dimensions[i]="0";
			}
		}
		//更新d、dimension、为data申请空间
		int val_type = TypeIndex(((AssignStmt) currProStmt).getLeftOp());//int or float
		int pos_index = typeToList(val_type).indexOf(((AssignStmt) currProStmt).getLeftOp());
		int index = val_type*10+pos_index;
		G.v().out.println("leftOp: "+((AssignStmt) currProStmt).getLeftOp().toString()+"  val_type: "+val_type+"  pos_index: "+pos_index+"  index: "+index);
		SootMethod toCall = Scene.v().getMethod ("<invoker.sgx_invoker: void updateValueInEnclave(java.lang.String,int,long)>");
		Stmt newInvokeStmt1 = Jimple.v().newInvokeStmt(
				Jimple.v().newVirtualInvokeExpr
		           (sgxObjLocal, toCall.makeRef(), Arrays.asList(getUUIDLocal,IntConstant.v(0),LongConstant.v(counter))));
		units.insertBefore(newInvokeStmt1, currStmt);
		counter++;
		indexwriter(""+TypeIndex(((AssignStmt) currProStmt).getLeftOp()));
		indexwriter(""+dimensions[0]);
		indexwriter(""+dimensions[1]);
		indexwriter(""+0);
		indexwriter(""+dimensions[2]);
		indexwriter("-1");
		indexwriter(""+(-1));
		indexwriter(""+index);
		
		//更新location originloc
		Stmt newInvokeStmt2 = Jimple.v().newInvokeStmt(
				Jimple.v().newVirtualInvokeExpr
		           (sgxObjLocal, toCall.makeRef(), Arrays.asList(getUUIDLocal,IntConstant.v(0),LongConstant.v(counter))));
		units.insertBefore(newInvokeStmt2, currStmt);
		counter++;
		indexwriter(""+ TypeIndex(((AssignStmt) currProStmt).getLeftOp()));
		indexwriter(""+index);
		indexwriter(""+index);
		indexwriter(""+1);
		indexwriter(""+(-1));
		indexwriter("-1");
		indexwriter(""+(-1));
		indexwriter(""+index);
		
		//更新data
		for (int i = 0; i < totalSize; i++) {
			Stmt newInvokeStmt3 = Jimple.v().newInvokeStmt(
					Jimple.v().newVirtualInvokeExpr
			           (sgxObjLocal, toCall.makeRef(), Arrays.asList(getUUIDLocal,IntConstant.v(0),LongConstant.v(counter))));
			units.insertBefore(newInvokeStmt3, currStmt);
			counter++;
			indexwriter(""+ TypeIndex(((AssignStmt) currProStmt).getLeftOp()));
			indexwriter("" + data.get(i));
			indexwriter("" + (-1));
			indexwriter("2");
			indexwriter(""+(-1));
			indexwriter("-1");
			indexwriter(""+ "int_" + i);
			indexwriter(""+index);
		}
		
	    //处理最后一条语句 r1 = $r2
	    replaceValueUpdateStmt(aBody, sgxObjLocal, units, localArray, currStmt,getUUIDLocal,memberVariables,staticmemberVariables, OriginFieldCuuidArray);
    }
	private InvokeStmt prepareInsertStmt(Value loggedValue, Local loggerLocal,
			String className) {
		Type vType = loggedValue.getType();
		G.v().out.println("loggedValue type:"
				+ loggedValue.getType().toString());
		G.v().out.println("loggerLocal :" + loggerLocal.toString());

		SootMethod toCall = null;
		if (vType instanceof IntType || vType instanceof BooleanType
				|| vType instanceof ShortType) {
			toCall = Scene.v().getMethod("<" + className + ": void add(int)>");
		} else if (vType instanceof DoubleType) {
			toCall = Scene.v().getMethod(
					"<" + className + ": void add(double)>");
		} else if (vType instanceof FloatType) {
			toCall = Scene.v()
					.getMethod("<" + className + ": void add(float)>");
		} else if (vType instanceof soot.LongType) {
			toCall = Scene.v().getMethod("<" + className + ": void add(long)>");
		} else if (vType instanceof CharType) {
			toCall = Scene.v().getMethod("<" + className + ": void add(char)>");
		} else if (vType instanceof ByteType) {
			toCall = Scene.v().getMethod("<" + className + ": void add(byte)>");
		} else if (vType instanceof ArrayType) {
			switch (TypeIndex(loggedValue)) {
			case 7:
				toCall = Scene.v().getMethod(
						"<" + className + ": void add(int[])>");
				break;
			case 8:
				toCall = Scene.v().getMethod(
						"<" + className + ": void add(double[])>");
				break;
			case 9:
				toCall = Scene.v().getMethod(
						"<" + className + ": void add(float[])>");
				break;
			case 10:
				toCall = Scene.v().getMethod(
						"<" + className + ": void add(char[])>");
				break;
			case 11:
				toCall = Scene.v().getMethod(
						"<" + className + ": void add(long[])>");
				break;
			case 12:
				toCall = Scene.v().getMethod(
						"<" + className + ": void add(byte[])>");
				break;
			case 13:
				toCall = Scene.v().getMethod(
						"<" + className + ": void add(int[][])>");
				break;
			case 14:
				toCall = Scene.v().getMethod(
						"<" + className + ": void add(double[][])>");
				break;
			case 15:
				toCall = Scene.v().getMethod(
						"<" + className + ": void add(float[][])>");
				break;
			case 16:
				toCall = Scene.v().getMethod(
						"<" + className + ": void add(char[][])>");
				break;
			case 17:
				toCall = Scene.v().getMethod(
						"<" + className + ": void add(long[][])>");
				break;
			case 18:
				toCall = Scene.v().getMethod(
						"<" + className + ": void add(byte[][])>");
				break;
			case 19:
				toCall = Scene.v().getMethod(
						"<" + className + ": void add(int[][][])>");
				break;
			case 20:
				toCall = Scene.v().getMethod(
						"<" + className + ": void add(double[][][])>");
				break;
			case 21:
				toCall = Scene.v().getMethod(
						"<" + className + ": void add(float[][][])>");
				break;
			case 22:
				toCall = Scene.v().getMethod(
						"<" + className + ": void add(char[][][])>");
				break;
			case 23:
				toCall = Scene.v().getMethod(
						"<" + className + ": void add(long[][][])>");
				break;
			case 24:
				toCall = Scene.v().getMethod(
						"<" + className + ": void add(byte[][][])>");
				break;
			case -1:
				toCall = null;
				break;
			default:
				break;
			}
			// toCall = Scene.v().getMethod
			// ("<"+className+": void add(java.lang.Object)>");
		} else {
			G.v().out.println("else loggedValue:" + loggedValue);
			toCall = Scene.v().getMethod(
					"<" + className + ": void add(java.lang.Object)>");
		}

		InvokeStmt newInvokeStmt = Jimple.v().newInvokeStmt(
				Jimple.v().newVirtualInvokeExpr(loggerLocal, toCall.makeRef(),
						Arrays.asList(loggedValue)));
		G.v().out
				.println("newInvokeStmt to insert is: ++++++++++++++++++++++++++ "
						+ newInvokeStmt + "++++++++++++++++++++++");
		return newInvokeStmt;

	}

	@SuppressWarnings("unused")
	private boolean isTypeCompatible(Type typeValue, Type localType) {
		if ((localType.toString().equals(typeValue.toString()))
				|| (typeValue instanceof RefLikeType && localType instanceof RefLikeType))
			return true;
		else
			return false;
	}

	@SuppressWarnings("unused")
	private void analyzeExp(
			Value exp,// x>y //rightop
			// ArrayList<String> params,
			ArrayList<Value> values, ArrayList<String> operator,
			ArrayList<Value> cons, ArrayList<Value> variable) {
		G.v().out.println("exp:********" + exp.toString() + "*************");

		if (exp instanceof JLengthExpr) {
			G.v().out.println("JLengthExpr exp********" + exp.toString()
					+ "*************");
			values.add(exp);
			// isInvoke = true;
		} else if (exp instanceof InstanceInvokeExpr) {
			G.v().out.println("InvokeExpr:********" + exp.toString()
					+ "*************");
			values.add(exp);
			// isInvoke = true;
		} else if (exp instanceof JStaticInvokeExpr) {
			G.v().out.println("JStaticInvokeExpr:********" + exp.toString()
					+ "*************");
			values.add(exp);
			// isInvoke = true;
		} else if (exp instanceof BinopExpr) {// add add div mul or sub xor rem
												// shl shr
			// G.v().out.println("BinopExpr:********"+exp.toString()+"*************");
			analyzeExp(((BinopExpr) exp).getOp1(), values, operator, cons,
					variable);
			analyzeExp(((BinopExpr) exp).getOp2(), values, operator, cons,
					variable);
			operator.add(((BinopExpr) exp).getSymbol());
		} else if (exp instanceof InstanceOfExpr) {
			G.v().out.println("InstanceOfExpr exp********" + exp.toString()
					+ "*************");
		} else if (exp instanceof CastExpr) {
			/**
			 * G.v().out.println("CastExpr exp********"+exp.toString()+
			 * "*************"); analyzeExp(((BinopExpr)exp).getOp1(), values,
			 * operator, cons, variable);
			 * G.v().out.println("CastExpr exp********finish*************");
			 */
			values.add(exp);
			// operator.add(((CastExpr)exp).get);
		} else {
			if (exp instanceof Constant) {
				G.v().out.println("Constant exp********" + exp.toString()
						+ "*************");
				values.add(exp);
				cons.add(exp);
			} else if (exp instanceof Local) {
				G.v().out.println("Local exp********" + exp.toString()
						+ "*************");
				values.add(exp);
				// variable.add(((Local)exp));
			} else if (exp instanceof ArrayRef) {
				G.v().out.println("ArrayRef:********" + exp.toString()
						+ "*************");
				values.add(exp);
				// isInvoke = true;
			} else if (exp instanceof StaticFieldRef) {
				G.v().out.println("StaticFieldRef:********" + exp.getClass()
						+ "*************");
				values.add(exp);
			} else if (exp instanceof JInstanceFieldRef) {
				values.add(exp);
				G.v().out.println("JInstanceFieldRef:********" + exp.getClass()
						+ "*************");
			} else {
				G.v().out.println("other type:********" + exp.getClass()
						+ "*************");
				values.add(exp);
				// isInvoke = true;
			}
		}
	}

	// 自定义值返回变量类型
	@SuppressWarnings("unused")
	private int TypeIndex(Value tValue) {
		int typeIndex = -1;
		String typeStr = tValue.getType().toString();
		G.v().out
				.println("<<<<<<>>>>>> in Function TypeIndex typeStr:********"
						+ typeStr + "*************");

		if (typeStr.equals("int") || typeStr.equals("short")||typeStr.equals("byte")||typeStr.equals("char")
				|| typeStr.equals("java.lang.Integer")
				|| typeStr.equals("boolean")) {
			typeIndex = 1;
		} else if (typeStr.equals("double")) {
			typeIndex = 2;
		} else if (typeStr.equals("float")) {
			typeIndex = 3;
		} else if (typeStr.equals("long")) {
			typeIndex = 5;
		}else if (typeStr.equals("int[]")||typeStr.equals("char[]")
				||typeStr.equals("byte[]")) {
			typeIndex = 7;
		} else if (typeStr.equals("double[]")) {
			typeIndex = 8;
		} else if (typeStr.equals("float[]")) {
			typeIndex = 9;
		} else if (typeStr.equals("long[]")) {
			typeIndex = 11;
		}else if (typeStr.equals("int[][]")) {
			typeIndex = 13;
		} else if (typeStr.equals("double[][]")) {
			typeIndex = 14;
		} else if (typeStr.equals("float[][]")) {
			typeIndex = 15;
		} else if (typeStr.equals("char[][]")) {
			typeIndex = 16;
		} else if (typeStr.equals("long[][]")) {
			typeIndex = 17;
		} else if (typeStr.equals("byte[][]")) {
			typeIndex = 18;
		} else if (typeStr.equals("int[][][]")) {
			typeIndex = 13;
		} else if (typeStr.equals("double[][][]")) {
			typeIndex = 14;
		} else if (typeStr.equals("float[][][]")) {
			typeIndex = 15;
		} else if (typeStr.equals("char[][][]")) {
			typeIndex = 16;
		} else if (typeStr.equals("long[][][]")) {
			typeIndex = 17;
		} else if (typeStr.equals("byte[][][]")) {
			typeIndex = 18;
		} else { // TODO: contains type object , boolean , short
			G.v().out.println("<<<<<<>>>>>>other Value.getType():"
					+ tValue.getType());
			typeIndex = -1; // for hashcode
		}
		return typeIndex;
	}

	private ArrayList<Value> typeToList(int typeIndex) {
		if (typeIndex == 1)
			return condValsInt;
		else if (typeIndex == 2)
			return condValsDouble;
		else if (typeIndex == 3)
			return condValsFloat;
		else if (typeIndex == 4)
			return condValsChar;
		else if (typeIndex == 5)
			return condValsLong;
		else if (typeIndex == 6)
			return condValsByte;
		else if (typeIndex == 7)
			return condValsArrayInt;
		else if (typeIndex == 8)
			return condValsArrayDouble;
		else if (typeIndex == 9)
			return condValsArrayFloat;
		else if (typeIndex == 10)
			return condValsArrayChar;
		else if (typeIndex == 11)
			return condValsArrayLong;
		else if (typeIndex == 12)
			return condValsArrayByte;
		else if (typeIndex == 13)
			return condValsArrayInt;
		else if (typeIndex == 14)
			return condValsArrayDouble;
		else if (typeIndex == 15)
			return condValsArrayFloat;
		else if (typeIndex == 16)
			return condValsArrayChar;
		else if (typeIndex == 17)
			return condValsArrayLong;
		else if (typeIndex == 18)
			return condValsArrayByte;
		else
			// TODO: contains type object , boolean , short
			G.v().out.println("other condvalstype");
		return condValsOtherType;
	}

	// 插入删除语句
	private void insertDeletValueStmt(Body aBody, Local sgxObjLocal,
			PatchingChain<Unit> units, Unit currStmt, Local getUUIDLocal,
			Map<Value, SootClass> needToDestoryForMemberVari,
			List<Local> localArray) {

		long status = 0L;
		Local tmpCuuid = null;
		G.v().out.println("A");

		SootMethod toCall = Scene
				.v()
				.getMethod(
						"<invoker.sgx_invoker: boolean deleteValueInEnclave(java.lang.String,java.lang.String,long)>");
		VirtualInvokeExpr initValueExpr = null;

		if (!needToDestoryForMemberVari.isEmpty()) {
			status = 1;
			Value ssValue = null;
			SootClass sootClass = null;
			// [Default] Only one cuuid need to be destory. on 0601/2020
			for (Value key : needToDestoryForMemberVari.keySet()) {
				ssValue = key;
			}
			for (SootClass value : needToDestoryForMemberVari.values()) {
				sootClass = value;
			}
			G.v().out.println("[delete]ssValue: " + ssValue.toString() + ";");
			G.v().out.println("[delete]sootClass: " + sootClass.toString()
					+ ";");
			SootFieldRef sootFieldRef = Scene.v().makeFieldRef(sootClass,
					"Cuuid", RefType.v("java.lang.String"), false);
			G.v().out.println("[delete]sootFieldRef: "
					+ sootFieldRef.toString() + ";");
			FieldRef fieldRef = Jimple.v().newInstanceFieldRef(ssValue,
					sootFieldRef);
			G.v().out.println("[delete]fieldRef: " + fieldRef.toString() + ";");
			tmpCuuid = Jimple.v().newLocal("deleteCuuid",
					RefType.v("java.lang.String"));
			aBody.getLocals().add(tmpCuuid);
			localArray.add(tmpCuuid);
			AssignStmt asStmt = Jimple.v().newAssignStmt(tmpCuuid, fieldRef);
			G.v().out.println("a[delete]sStmt: " + asStmt.toString() + ";");
			units.insertBefore(asStmt, currStmt);

			initValueExpr = Jimple.v().newVirtualInvokeExpr(
					sgxObjLocal,
					toCall.makeRef(),
					Arrays.asList(getUUIDLocal, tmpCuuid,
							LongConstant.v(status)));
			G.v().out.println("C1");
		} else {
			initValueExpr = Jimple.v().newVirtualInvokeExpr(
					sgxObjLocal,
					toCall.makeRef(),
					Arrays.asList(getUUIDLocal, StringConstant.v(""),
							LongConstant.v(status)));
			G.v().out.println("C2");
		}
		G.v().out.println("B");
		Stmt newInitInvokeStmt = Jimple.v().newInvokeStmt(initValueExpr);
		G.v().out.println("ValueDeleteStmt is:#" + newInitInvokeStmt + "#--");
		units.insertBefore(newInitInvokeStmt, currStmt);
		// units.
	}

	@SuppressWarnings("unused")
	private void insertValueInitStmt(Body aBody, Local sgxObjLocal,
			PatchingChain<Unit> units, Unit currStmt, Local getUUID,
			Local invokeUUID, Local invokeLineNo) {

		G.v().out.println("condValsTypeArray:"
				+ condValsTypeArray.toString());

		G.v().out.println("8.31:");

		SootMethod toCall1 = Scene.v().getMethod(
				"<invoker.sgx_invoker: java.lang.String getUUID()>");

		VirtualInvokeExpr getuuidExpr = Jimple.v().newVirtualInvokeExpr(
				sgxObjLocal, toCall1.makeRef());
		AssignStmt asStmt = Jimple.v().newAssignStmt(getUUID, getuuidExpr);
		units.insertBefore(asStmt, currStmt);

		SootMethod toCall = Scene
				.v()
				.getMethod(
						"<invoker.sgx_invoker: boolean initValueInEnclave(java.lang.String,java.lang.String,long)>");

		VirtualInvokeExpr initValueExpr = Jimple.v().newVirtualInvokeExpr(
				sgxObjLocal, toCall.makeRef(),
				Arrays.asList(getUUID, invokeUUID, invokeLineNo));
		Stmt newInitInvokeStmt = Jimple.v().newInvokeStmt(initValueExpr);
		G.v().out.println("ValueInitStmt is:#" + newInitInvokeStmt + "#--");
		units.insertBefore(newInitInvokeStmt, currStmt);
		if (condValsArrayInt.size() != 0) {
			int sz = condValsArrayInt.size();
			int type = TypeIndex(condValsArrayInt.get(0));
			G.v().out.println("sz: " + sz + " type: " + type);
			G.v().out.println(condValsArrayInt);
			toCall = Scene
					.v()
					.getMethod(
							"<invoker.sgx_invoker: void initNodeInEnclave(java.lang.String,int,int)>");

			initValueExpr = Jimple.v().newVirtualInvokeExpr(
					sgxObjLocal,
					toCall.makeRef(),
					Arrays.asList(getUUID, IntConstant.v(type),
							IntConstant.v(sz)));
			newInitInvokeStmt = Jimple.v().newInvokeStmt(initValueExpr);
			G.v().out.println("newInitInvokeStmt is:#" + newInitInvokeStmt
					+ "#--");
			units.insertBefore(newInitInvokeStmt, currStmt);
		}
		if (condValsArrayChar.size() != 0) {
			int sz = condValsArrayChar.size();
			int type = TypeIndex(condValsArrayChar.get(0));
			G.v().out.println("sz: " + sz + " type: " + type);
			G.v().out.println(condValsArrayChar);
			toCall = Scene
					.v()
					.getMethod(
							"<invoker.sgx_invoker: void initNodeInEnclave(java.lang.String,int,int)>");

			initValueExpr = Jimple.v().newVirtualInvokeExpr(
					sgxObjLocal,
					toCall.makeRef(),
					Arrays.asList(getUUID, IntConstant.v(type),
							IntConstant.v(sz)));
			newInitInvokeStmt = Jimple.v().newInvokeStmt(initValueExpr);
			G.v().out.println("newInitInvokeStmt is:#" + newInitInvokeStmt
					+ "#--");
			units.insertBefore(newInitInvokeStmt, currStmt);
		}
		G.v().out.println("更新形参中的敏感数组的param值为1");
		G.v().out.println("identi array: "+identityArray);
		for(Value key:identityArray.keySet()){
			G.v().out.println("counter="+counter);
			int type=TypeIndex(key);
			int pos_index = typeToList(type).indexOf(key);
			String return_index = Integer.toString(type*10 + pos_index);
			G.v().out.println("arr: "+key+"  pos:"+return_index);
			toCall = Scene
					.v()
					.getMethod(
							"<invoker.sgx_invoker: void updateValueInEnclave(java.lang.String,int,long)>");

		Stmt newInvokeStmt = Jimple.v().newInvokeStmt(
					Jimple.v().newVirtualInvokeExpr(
							sgxObjLocal,
							toCall.makeRef(),
							Arrays.asList(getUUID, IntConstant.v(0),
									LongConstant.v(counter)))); 
		units.insertBefore(newInvokeStmt, currStmt);
	
		
		
		indexwriter(""+type);
		indexwriter("0");
		indexwriter(identityArray.get(key).substring(5,6));
		indexwriter("4");
		indexwriter("-1");
		indexwriter("-1");
		indexwriter("-1");
		indexwriter(return_index);
		counter++;
		
			
		}
	}

	@SuppressWarnings("unused")
	private void insertSgxInitStmt(Body aBody, Local sgxObjLocal, // sgxObjLocal
			PatchingChain<Unit> units, Unit currStmt, // first stmt
			String className) { // Object NewArrayExpr)
	// String funcNameString = aBody.getMethod().toString();
	// G.v().out.println("funcNameString: "+funcNameString+";");
	// int argsString = aBody.getMethod().equivHashCode();
	// G.v().out.println("argsString: "+argsString+";");
	// G.v().out.println("getNumberedSubSignature: "+aBody.getMethod().getNumberedSubSignature()+";");
	// G.v().out.println("getTags: "+aBody.getTags());
	// G.v().out.println("hashCode: "+aBody.hashCode());
	// G.v().out.println("getParameterCount: "+aBody.getMethod().getParameterCount());
	// StringBuilder methodID = new StringBuilder();
	// methodID.append(funcNameString);
	// for(int i=0; i<aBody.getMethod().getParameterCount(); i++){
	// G.v().out.println("ParameterLocal-"+i+": "+aBody.getParameterLocal(i).toString());
	// methodID.append("_");
	// methodID.append(aBody.getParameterLocal(i));
	// }
	// G.v().out.println("methodID: "+methodID);

		G.v().out.println("2199 currStmt: " + currStmt.toString());

		// /"sgxInvoker = new invoker.sgx_invoker;"
		soot.jimple.NewExpr sootNew = soot.jimple.Jimple.v().newNewExpr(
				RefType.v(className));
		soot.jimple.AssignStmt stmt = soot.jimple.Jimple.v().newAssignStmt(
				sgxObjLocal, sootNew);
		G.v().out.println("2204 stmt: " + stmt.toString());
		units.insertBefore(stmt, currStmt);

		G.v().out.println("2206 currStmt: " + currStmt.toString());
		// "specialinvoke sgxInvoker.<invoker.sgx_invoker: void <init>()>();"
		SpecialInvokeExpr newTrans = Jimple.v().newSpecialInvokeExpr(
				sgxObjLocal,
				Scene.v().getMethod("<invoker.sgx_invoker: void <init>()>")
						.makeRef(), Arrays.asList());
		soot.jimple.Stmt invokeStmt = soot.jimple.Jimple.v().newInvokeStmt(
				newTrans);
		units.insertBefore(invokeStmt, currStmt);

		// "virtualinvoke sgxInvoker.<invoker.sgx_invoker: boolean initenclave()>();"
		SootMethod toCall = Scene.v().getMethod(
				"<invoker.sgx_invoker: boolean initenclave()>");
		Stmt newInvokeStmt = Jimple.v().newInvokeStmt(
				Jimple.v().newVirtualInvokeExpr(sgxObjLocal, toCall.makeRef(),
						Arrays.asList()));// IntConstant.v(1)
		units.insertBefore(newInvokeStmt, currStmt);
	}

	// 赋值语句中的变量赋初始值
	private AssignStmt initAssignStmt(Local l) {
		Type t = l.getType();
		G.v().out.println("20210603=" + l.toString());
		G.v().out.println("20210603=" + t);
		soot.jimple.AssignStmt stmt = null;
		if (t instanceof RefLikeType) {
			stmt = soot.jimple.Jimple.v().newAssignStmt(l, NullConstant.v());
			G.v().out.println("20210603=" + stmt.toString());
		} else if (t instanceof IntType) {
			stmt = soot.jimple.Jimple.v().newAssignStmt(l, IntConstant.v(0));
			G.v().out.println("20210603=" + stmt.toString());
		} else if (t instanceof DoubleType) {
			stmt = soot.jimple.Jimple.v().newAssignStmt(l, DoubleConstant.v(0));
		} else if (t instanceof FloatType) {
			stmt = soot.jimple.Jimple.v().newAssignStmt(l, FloatConstant.v(0));
		} else if (t instanceof soot.LongType) {
			stmt = soot.jimple.Jimple.v().newAssignStmt(l, LongConstant.v(0));
		} else if (t instanceof BooleanType) {
			stmt = soot.jimple.Jimple.v().newAssignStmt(l, IntConstant.v(0));
		} else if (t instanceof ShortType) {
			stmt = soot.jimple.Jimple.v().newAssignStmt(l, IntConstant.v(0));
		} else if (t instanceof CharType) {
			stmt = soot.jimple.Jimple.v().newAssignStmt(l, IntConstant.v(0));
		} else if (t instanceof ByteType) {
			stmt = soot.jimple.Jimple.v().newAssignStmt(l, IntConstant.v(0));
		}
		return stmt;
	}

	// 初始化声明变量
	private void initidentyLocal(List<Local> localList,
			PatchingChain<Unit> units, Unit currStmt,
			HashSet<Value> identifiedLocal) {

		soot.jimple.AssignStmt stmt = null;
		for (Local l : localList) {
			// G.v().out.println("++++++Local is:++++++++++"+l.toString());
			if (identifiedLocal.contains(l)) {
				G.v().out.println(l.toString()
						+ ": has been inited in original javafile!");
				continue;
			}
			stmt = initAssignStmt(l);
			G.v().out.println(l.toString()
					+ ": init stmt will be inserted into jimplefile! :"
					+ stmt.toString());
			units.insertBefore(stmt, currStmt);
		}
	}

	@SuppressWarnings("unused")
	private void insertCloseEnclaveStmt(Local sgxObjLocal, // sgxObjLocal
			PatchingChain<Unit> units, Unit currStmt, // first stmt
			String className) {

		SootMethod toCall = Scene.v().getMethod(
				"<invoker.sgx_invoker: boolean closeenclave()>");
		Stmt newInvokeStmt = Jimple.v().newInvokeStmt(
				Jimple.v().newVirtualInvokeExpr(sgxObjLocal, toCall.makeRef(),
						Arrays.asList()));
		units.insertBefore(newInvokeStmt, currStmt);
	}

	private void preInitSensitiveVariables(Value tValue) {

		if (!condVals.contains(tValue)&&!(tValue instanceof Constant)) {
			condVals.add(tValue);
			// G.v().out.println("---==condVals:"+condVals.toString());
		}

		String tValueTypeStr = tValue.getType().toString();
		if (tValueTypeStr.equals("int")||tValueTypeStr.equals("char")||tValueTypeStr.equals("byte")
				|| tValueTypeStr.equals("java.lang.Integer")
				|| tValueTypeStr.equals("short")) {
			if (!condValsInt.contains(tValue)) {
				condValsInt.add(tValue);
			}
		} else if (tValueTypeStr.equals("boolean")) {
			if (!condValsInt.contains(tValue)) {
				condValsInt.add(tValue);
			}
		} else if (tValueTypeStr.equals("double")) {
			if (!condValsDouble.contains(tValue)) {
				condValsDouble.add(tValue);
			}
		} else if (tValueTypeStr.equals("float")) {
			if (!condValsFloat.contains(tValue)) {
				condValsFloat.add(tValue);
			}
		} 
//		else if (tValueTypeStr.equals("char")) {
//			if (!condValsChar.contains(tValue)) {
//				condValsChar.add(tValue);
//			}
//		} 
		else if (tValueTypeStr.equals("long")) {
			if (!condValsLong.contains(tValue)) {
				condValsLong.add(tValue);
			}
		} else if (tValueTypeStr.equals("int[]")
				||tValueTypeStr.equals("char[]")
				||tValueTypeStr.equals("byte[]")
				|| tValueTypeStr.equals("int[][]")
				|| tValueTypeStr.equals("int[][][]")) {
			if (!condValsArrayInt.contains(tValue)) {
				condValsArrayInt.add(tValue);
			}
		} else if (tValueTypeStr.equals("double[]")) {
			if (!condValsArrayDouble.contains(tValue)) {
				condValsArrayDouble.add(tValue);
			}
		} else if (tValueTypeStr.equals("float[]")) {
			if (!condValsArrayFloat.contains(tValue)) {
				condValsArrayFloat.add(tValue);
			}
		} 
//		else if (tValueTypeStr.equals("char[]")) {
//			if (!condValsArrayChar.contains(tValue)) {
//				condValsArrayChar.add(tValue);
//			}
//		} 
		else if (tValueTypeStr.equals("long[]")) {
			if (!condValsArrayLong.contains(tValue)) {
				condValsArrayLong.add(tValue);
			}
		} 
//		else if (tValueTypeStr.equals("byte")) {
//			if (!condValsByte.contains(tValue)) {
//				condValsByte.add(tValue);
//			}
//		}
		else if (tValueTypeStr.equals("byte[]")) {
			if (!condValsArrayByte.contains(tValue)) {
				condValsArrayByte.add(tValue);
			}
		} else {
			G.v().out.println("Other condValsOtherType" + tValueTypeStr);

		}
	}

	/**
	 * is ArrayRef judge 0424
	 * 
	 * @param value
	 * @return
	 */
	private boolean isArrayRef(Value value) {
		if (value instanceof ArrayRef) {
			return true;
		}
		return false;
	}

}
