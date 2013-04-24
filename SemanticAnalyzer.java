/* Author:  Edric Orense
 * File:    SemanticAnalyzer.java
 * Purpose: Goes through a SimpleJava file and checks for semantic errors
 */

import java.util.Vector;

public class SemanticAnalyzer implements ASTVisitor {

	private TypeEnvironment typeEnv;
	private VariableEnvironment varEnv;
	private FunctionEnvironment funcEnv;
	private String returnTypeStat;
	private Label startLabel, endLabel;
	private MachineDependent mSize;
	private final int wordSize = mSize.WORDSIZE; 
	private int offset;
	private AATBuildTree buildTree;

	public SemanticAnalyzer(){
		typeEnv = new TypeEnvironment();
		varEnv = new VariableEnvironment();
		funcEnv = new FunctionEnvironment();
		funcEnv.addBuiltinFunctions();
		buildTree = new AATBuildTree();
	}

	
	public Object VisitArrayVariable(ASTArrayVariable array) {
		Type base, index;
		TypeClass typeClass = null, baseTypeClass, indexTypeClass;
		ArrayType arrayType;

		baseTypeClass = (TypeClass) array.base().Accept(this);
		indexTypeClass = (TypeClass) array.index().Accept(this);

		base = baseTypeClass.type();
		index = indexTypeClass.type();

		if(base instanceof ArrayType){
			arrayType = (ArrayType) base;
			base = (Type) arrayType.type();
		}else{
			CompError.message(array.line(),
								"Base variable must be of type array.");
		}

		if(!index.equals(IntegerType.instance())){
			CompError.message(array.index().line(),
								"Index of array must be of type int.");
		}

		AATExpression aatArray = buildTree.arrayVariable(baseTypeClass.tree(), indexTypeClass.tree(), wordSize);
		typeClass = new TypeClass(base, aatArray);

		return typeClass;
	}

	
	public Object VisitAssignmentStatement(ASTAssignmentStatement assign) {
		TypeClass leftClass, rightClass;
		Type left, right;
		leftClass = (TypeClass) assign.variable().Accept(this);
		rightClass = (TypeClass) assign.value().Accept(this);

		left = leftClass.type();
		right = rightClass.type();

		if(!left.equals(right)){
			CompError.message(assign.line(),
								"Type mismatch error.");
		}

		return buildTree.assignmentStatement(leftClass.tree(), rightClass.tree());
	}

	
	public Object VisitBaseVariable(ASTBaseVariable base) {
		VariableEntry varEnt;
		Type type;
		TypeClass typeClass = null;

		varEnt = varEnv.find(base.name());
		
		if(varEnt == null){
			CompError.message(base.line()
								, "Variable \"" + base.name() + "\" is not yet defined.");
			type = IntegerType.instance();
		}else{
			type = varEnt.type();
			typeClass = new TypeClass(type, buildTree.baseVariable(varEnt.offset()));
		}

		return typeClass;
	}

	
	public Object VisitBooleanLiteral(ASTBooleanLiteral boolliteral) {
		int boolVal = 0;
		if(boolliteral.value()){
			boolVal = 1;
		}

		return new TypeClass(BooleanType.instance(), buildTree.constantExpression(boolVal));
	}

	
	public Object VisitClass(ASTClass classs) {
		Type t;
		VariableEnvironment instanceVars;

		instanceVars = (VariableEnvironment) classs.variabledefs().Accept(this);		
		t = new ClassType(instanceVars);
		typeEnv.insert(classs.name(), t);
		return null;
	}

	
	public Object VisitClasses(ASTClasses classes) {
		for(int i = 0; i < classes.size(); i++){
			classes.elementAt(i).Accept(this);
		}
		return null;
	}

	
	public Object VisitClassVariable(ASTClassVariable classvariable) {
		Type base, returnType;
		ClassType classType;
		TypeClass typeClass = null, baseTypeClass;

		returnType = BooleanType.instance();
		baseTypeClass = (TypeClass)classvariable.base().Accept(this);
		base = baseTypeClass.type();

		if(base instanceof ClassType){
			classType = (ClassType) base;
			if(classType.variables().find(classvariable.variable()) == null){
				CompError.message(classvariable.line(),
									classvariable.variable() + " is not a class member");
			}else{
				returnType = classType.variables().find(classvariable.variable()).type();
				AATExpression classVarTree = buildTree.classVariable(baseTypeClass.tree(), classType.variables().find(classvariable.variable()).offset());
				typeClass = new TypeClass(returnType, classVarTree);
			}
		}else{
			CompError.message(classvariable.line(),
								"Base variable must be a class type.");
		}

		return typeClass;
	}

	
	public Object VisitDoWhileStatement(ASTDoWhileStatement dowhile) {
		Type test;
		TypeClass testClass;

		testClass = (TypeClass) dowhile.test().Accept(this);
		test = testClass.type();

		if(!test.equals(BooleanType.instance())){
			CompError.message(dowhile.line()
								, "Do while test must be of type boolean.");
		}

		AATStatement body = (AATStatement) dowhile.body().Accept(this);

		return buildTree.dowhileStatement(testClass.tree(), body);
	}

	
	public Object VisitEmptyStatement(ASTEmptyStatement empty) {
		
		return buildTree.emptyStatement();
	}

	
	public Object VisitForStatement(ASTForStatement forstmt) {
		Type test;
		AATStatement init = buildTree.emptyStatement(), inc = buildTree.emptyStatement(), body = buildTree.emptyStatement();
		TypeClass testClass;

		if(forstmt.initialize() != null){
			init = (AATStatement) forstmt.initialize().Accept(this);
		}

		if(forstmt.increment() != null){
			inc = (AATStatement) forstmt.increment().Accept(this);
		}

		testClass = (TypeClass) forstmt.test().Accept(this);
		test = testClass.type();

		if(!test.equals(BooleanType.instance())){
			CompError.message(forstmt.line()
								, "For test must be of type boolean.");
		}

		body = (AATStatement) forstmt.body().Accept(this);

		return buildTree.forStatement(init, testClass.tree(), inc, body);
	}

	
	public Object VisitFormal(ASTFormal formal) {
		
		return null;
	}

	
	public Object VisitFormals(ASTFormals formals) {
		Vector v = new Vector(formals.size());
		Vector v1 = new Vector(formals.size()); //keep type data
		Vector v2 = new Vector(formals.size()); //keep ASTFormal
		Type t;
		int j = 0;
		String key, keyPref;

		for(int i = 0; i < formals.size(); i++){
			t = typeEnv.find(formals.elementAt(i).type());
			v2.addElement(formals.elementAt(i));
			
			if(t == null){
				CompError.message(formals.elementAt(i).line(), 
								"Type \"" + formals.elementAt(i).type() + "\" is not yet defined.");
				t = IntegerType.instance();
			}
			if(formals.elementAt(i).arraydimension() != 0){
				keyPref = t.toString();
				for(j = 1; j <= formals.elementAt(i).arraydimension(); j++){
					key = keyPref + Integer.toString(j) + "dim";

					if (typeEnv.find(key) == null) {
						t = new ArrayType(t);
						typeEnv.insert(key, t);
					}else{
						t = typeEnv.find(key);
					}
				}
			}

			v1.addElement(t);
			
		}
		v.addElement(v1);
		v.addElement(v2);
		return v;
	}

	
	public Object VisitFunction(ASTFunction function) {
		Vector v= null, v1 = null, v2 = null;
		FunctionEntry funcEnt = funcEnv.find(function.name());
		Type t;

		varEnv.beginScope();

		returnTypeStat = function.type();
		t = typeEnv.find(function.type());

		if(function.formals() != null){
			v = (Vector) function.formals().Accept(this);
			v1 = (Vector) v.elementAt(0);
			v = (Vector) v.elementAt(1);

		}

		if(t == null){
			CompError.message(function.line(),
								"Type: " + function.type() + " is not yet defined.");
			t = IntegerType.instance();
		}

		if(funcEnt == null){
			startLabel = new Label(function.name());
			endLabel = new Label(function.name());
			funcEnt = new FunctionEntry(t, v1, startLabel, endLabel);
			funcEnv.insert(function.name(), funcEnt);

		}else{
			startLabel = funcEnt.startlabel();
			endLabel = funcEnt.endlabel();

			if(!funcEnt.result().equals(t)){
				CompError.message(function.line(), "Return type mismatch with function prototype.");
			}
			v2 = (Vector) funcEnt.formals().elementAt(1);
			if(!compareFormals(v, v2)){
				CompError.message(function.line(), "Function \"" + function.name() + "\" has arguments that do not match its prototype.");
			}

		}

		addFormals(v);
		AATStatement funcBody = (AATStatement) function.body().Accept(this);
		int frameSize = varEnv.size();
		varEnv.endScope();

		return buildTree.functionDefinition(funcBody, frameSize, startLabel, endLabel);
	}

	private void addFormals(Vector formals){
		varEnv.beginScope();
		offset = -4;
		ASTFormal currVar;
		VariableEntry varEnt;
		Type t;
		String key, keyPref;

		if (formals == null) {
			return;
		}

		for(int i = 0; i < formals.size(); i++){
			currVar = (ASTFormal) formals.elementAt(i);
			t = typeEnv.find(currVar.type());

			if(currVar.arraydimension() != 0){
				keyPref = t.toString();
				key = keyPref + Integer.toString(currVar.arraydimension()) + "dim";
				t = typeEnv.find(key);
			}

			varEnt = new VariableEntry(t, offset);
			varEnv.insert(currVar.name(), varEnt);
			offset -= wordSize;
		}	
	}


	private boolean compareFormals(Vector a, Vector b){
		ASTFormal elemA, elemB;

		if(a == b){
			return true;
		}else if(a == null || b == null){
			return false;
		}

		
		if(a.size() != b.size()){
			return false;
		}

		if(a.size() < 1 && a.elementAt(1) instanceof Vector){
			a = (Vector) a.elementAt(1);

		}	
		

		if(b.size() < 1 && b.elementAt(1) instanceof Vector){
			b = (Vector) b.elementAt(1);

		}

		for(int i = 0; i < a.size(); i++) {
			elemA = (ASTFormal) a.elementAt(i);
			elemB = (ASTFormal) b.elementAt(i);
			if(!elemB.type().equals(elemA.type()) 
				|| elemB.arraydimension() != elemA.arraydimension()){
				return false;
			}
			
		}

		return true;
	}


	public Object VisitFunctionCallExpression(
			ASTFunctionCallExpression functioncall) {
		FunctionEntry funcCall;
		Vector v1, v2 = null, v3 = new Vector();
		Type returnType;

		if(functioncall.size() == 0){
			v1 = null;
		}else{
			v1 = new Vector(functioncall.size());
			v3 = new Vector(functioncall.size());
			for (int i = 0; i < functioncall.size(); i++) {
				TypeClass tc = (TypeClass) functioncall.elementAt(i).Accept(this);
				v1.addElement(tc.type());
				v3.addElement(tc.tree());
			}
		}

		funcCall = funcEnv.find(functioncall.name());
		if(funcCall == null){
			CompError.message(functioncall.line(),
								"Function " + functioncall.name() +" is not yet defined.");
			returnType = IntegerType.instance();
		}else{

			if(funcCall.formals() != null){
				if(funcCall.formals().size() == 0){
					v2 = null;
				}else{
					if(funcCall.formals().elementAt(0) instanceof Vector){
						v2 = (Vector) funcCall.formals().elementAt(0);

					}else{
						v2 = funcCall.formals();
					}
				}
				
			}
			returnType = funcCall.result();

			if(v1 != v2 && v2 != null && v1 != null){

				if (v1.size() != v2.size()) {
					CompError.message(functioncall.line(),
										"Argument count does not match");
				}else{
					for(int i = 0; i < functioncall.size(); i++){
						if(!v1.elementAt(i).equals(v2.elementAt(i))){
							CompError.message(functioncall.line(),
										"Argument number " + (i + 1) + " does not match");
						}
					}
				}
			}

			if((v1 == null || v2 == null) && v2 != v1){
				CompError.message(functioncall.line(),
										"Argument count does not match");
			}
				
		}
		TypeClass typeClass = new TypeClass(returnType, buildTree.callExpression(v3, funcEnv.find(functioncall.name()).startlabel()));
		return typeClass;
	}


	public Object VisitFunctionCallStatement(
			ASTFunctionCallStatement functioncall) {
		FunctionEntry funcCall;
		Vector v1, v2 = null, v3 = null;

		if(functioncall.size() == 0){
			v1 = null;
			v3 = new Vector();
		}else{
			v1 = new Vector(functioncall.size());
			v3 = new Vector(functioncall.size());
			for (int i = 0; i < functioncall.size(); i++) {
				TypeClass tc = (TypeClass) functioncall.elementAt(i).Accept(this);
				v1.addElement(tc.type());
				v3.addElement(tc.tree());
			}
		}

		funcCall = funcEnv.find(functioncall.name());
		if(funcCall == null){
			CompError.message(functioncall.line(),
								"Function " + functioncall.name() +" is not yet defined.");
		}else{

			if(funcCall.result() != VoidType.instance()){
				CompError.message(functioncall.line(), 
									"Function \"" + functioncall.name() + "\" is not a procedure.");
			}

			if(funcCall.formals() != null){
				if(funcCall.formals().size() == 0){
					v2 = null;
				}else{
					if(funcCall.formals().elementAt(0) instanceof Vector){
						v2 = (Vector) funcCall.formals().elementAt(0);

					}else{
						v2 = funcCall.formals();
					}
				}

			}

			if(v1 != v2 && v2 != null && v1 != null){

				if(v2.elementAt(0) instanceof Vector){
					v2 = (Vector) v2.elementAt(0);
				}

				if(v1.elementAt(0) instanceof Vector){
					v1 = (Vector) v1.elementAt(0);
				}

				if (v1.size() != v2.size()) {
					CompError.message(functioncall.line(),
										"Argument count does not match");
				}else{
					for(int i = 0; i < functioncall.size(); i++){
						if(!v1.elementAt(i).equals(v2.elementAt(i))){
							CompError.message(functioncall.line(),
										"Argument number " + (i + 1) + " does not match");
						}
					}
				}
			}

			if((v1 == null || v2 == null) && v2 != v1){
				CompError.message(functioncall.line(),
										"Argument count does not match");
			}
				
		}

		return buildTree.callStatement(v3, funcEnv.find(functioncall.name()).startlabel());
	}

	
	public Object VisitIfStatement(ASTIfStatement ifsmt) {
		Type test;
		TypeClass testClass;

		testClass = (TypeClass) ifsmt.test().Accept(this);
		test = testClass.type();

		if(!test.equals(BooleanType.instance())){
			CompError.message(ifsmt.line()
								, "If test must be of type boolean.");
		}

		AATStatement thenBody = (AATStatement) ifsmt.thenstatement().Accept(this);
		AATStatement elseBody = buildTree.emptyStatement();

		if(ifsmt.elsestatement() != null){
			elseBody = (AATStatement) ifsmt.elsestatement().Accept(this);
		}

		return buildTree.ifStatement(testClass.tree(), thenBody, elseBody);
	}

	
	public Object VisitIntegerLiteral(ASTIntegerLiteral literal) {
		TypeClass typeClass = new TypeClass(IntegerType.instance(), buildTree.constantExpression(literal.value()));
		return typeClass;
	}

	
	public Object VisitInstanceVariableDef(ASTInstanceVariableDef variabledef) {
		VariableEntry varEnt;
		Type t = typeEnv.find(variabledef.type());
		String key, keyPref;
		int i;

		if(t != null){
			if(variabledef.arraydimension() != 0){
				keyPref = t.toString();
				for(i = 1; i <= variabledef.arraydimension(); i++){

					key = keyPref + Integer.toString(i) + "dim";
					if (typeEnv.find(key) == null) {
						t = new ArrayType(t);
						typeEnv.insert(key, t);
					}else{
						t = typeEnv.find(key);
					}
				}
			}
			varEnt = new VariableEntry(t, offset);
		}else{
			varEnt = new VariableEntry(IntegerType.instance());
			CompError.message(variabledef.line(), "Type is not yet defined");
		}

		offset += wordSize;
		return varEnt;
	}

	
	public Object VisitInstanceVariableDefs(ASTInstanceVariableDefs variabledefs) {
		VariableEnvironment instanceVars = new VariableEnvironment();
		VariableEntry varEnt;
		String key;
		offset = 0;


		for(int i = 0; i < variabledefs.size(); i++){
			varEnt = (VariableEntry) variabledefs.elementAt(i).Accept(this);
			key = variabledefs.elementAt(i).name();
			instanceVars.insert(key, varEnt);
		}

		return instanceVars;
	}

	
	public Object VisitNewArrayExpression(ASTNewArrayExpression newarray) {
		Type elements, t;
		TypeClass elementsClass;
		String key, keyPref;

		t = typeEnv.find(newarray.type());
		elementsClass = (TypeClass) newarray.elements().Accept(this);
		elements = elementsClass.type();

		if(!elements.equals(IntegerType.instance())){
			CompError.message(newarray.line(),
								"Number of elements in array must be of type int.");
		}

		if(t != null){
			if(newarray.arraydimension() != 0){
				keyPref = t.toString();
				key = keyPref + Integer.toString(newarray.arraydimension()) + "dim";
				t = typeEnv.find(key);

				if(t == null){
					CompError.message(newarray.line(),
										newarray.type() + " with dimensions " + newarray.arraydimension() + " is not yet defined.");
				}
			}
		}else{
			CompError.message(newarray.line(), "Type: " + newarray.type() + " is not yet defined");
		}

		AATExpression allocateSize = buildTree.operatorExpression(elementsClass.tree(), buildTree.constantExpression(wordSize), AATOperator.MULTIPLY);

		return new TypeClass(t, buildTree.allocate(allocateSize));
	}

	
	public Object VisitNewClassExpression(ASTNewClassExpression newclass) {
		Type type;
		ClassType classType;
		TypeClass typeClass;
		int classSize;
		AATExpression allocateSize;

		type = typeEnv.find(newclass.type());
		if(type == null){
			CompError.message(newclass.line(),
								"Class " + newclass.type() + " is not yet defined.");
			type = IntegerType.instance();
			classSize = wordSize;
			allocateSize = buildTree.constantExpression(classSize);
		}else{
			classType = (ClassType) type;
			classSize = classType.variables().size() * wordSize;
			allocateSize = buildTree.constantExpression(classSize);

		}

		return new TypeClass(type, buildTree.allocate(allocateSize));
	}

	
	public Object VisitOperatorExpression(ASTOperatorExpression opexpr) {
		Type left, right, returnType;
		TypeClass leftClass, rightClass, typeClass;

		leftClass = (TypeClass) opexpr.left().Accept(this);
		rightClass = (TypeClass) opexpr.right().Accept(this);
		left = leftClass.type();
		right = rightClass.type();

		if(opexpr.operator() == 5 || 
			opexpr.operator() == 6){
			if(!left.equals(BooleanType.instance())){
				CompError.message(opexpr.left().line(),
									"Left side of expression using \"&& or ||\" must be a boolean.");
			}
			if(!right.equals(BooleanType.instance())){
				CompError.message(opexpr.right().line(),
									"Right side of expression using \"&& or ||\" must be a boolean.");	
			}

			returnType = BooleanType.instance();
		}else{
			if(!left.equals(IntegerType.instance())){
				CompError.message(opexpr.left().line(),
									"Left side of expression using \"+, -, *, /, <, >, >=, <= , != or ==\" must be an int.");
			}
			if(!right.equals(IntegerType.instance())){
				CompError.message(opexpr.right().line(),
									"Right side of expression using \"+, -, *, /, <, >, >=, <= , != or ==\" must be an int");	
			}

			if(opexpr.operator() >= 1 && opexpr.operator() <= 4){
				returnType = IntegerType.instance();
			}else{
				returnType = BooleanType.instance();
			}
		}

		return new TypeClass(returnType, buildTree.operatorExpression(leftClass.tree(), rightClass.tree(), opexpr.operator()));
	}

	
	public Object VisitProgram(ASTProgram program) {
		AATStatement prog = buildTree.emptyStatement();

		if(program.classes() != null)
			program.classes().Accept(this);
		if(program.functiondefinitions() != null){
			prog = (AATStatement) program.functiondefinitions().Accept(this);
			
		}
		return prog;
	}

	
	public Object VisitFunctionDefinitions(
			ASTFunctionDefinitions functiondefinitions) {
		AATStatement aatStat = buildTree.emptyStatement();
		Vector v = new Vector();
		for (int i = 0; i < functiondefinitions.size() ; i++) {
			v.addElement(functiondefinitions.elementAt(i).Accept(this));
		} 

		AATStatement seq = (AATStatement) v.elementAt(v.size() - 1);

		for(int j = v.size() - 2; j >= 0; j--){
			seq = buildTree.sequentialStatement((AATStatement)v.elementAt(j), seq);
		}

		return seq;
	}

	
	public Object VisitPrototype(ASTPrototype prototype) {
		Type result = typeEnv.find(prototype.type());
		FunctionEntry funcEnt;
		Vector formals = null; 
		Label startLabel, endLabel;

		if(prototype.formals() != null){
			formals = (Vector)prototype.formals().Accept(this);

		}
		
		if(result == null){
			CompError.message(prototype.line(), 
					"Type: " + prototype.type() + " is not yet defined.");
			result = IntegerType.instance();
		}

		startLabel = new Label(prototype.name() + "START");
		endLabel = new Label(prototype.name() + "END");
		funcEnt = new FunctionEntry(result, formals, startLabel, endLabel);
		
		if(funcEnv.find(prototype.name()) == null){
			funcEnv.insert(prototype.name(), funcEnt);
		}else{
			CompError.message(prototype.line(), "Function prototype name: " + prototype.name() + " is already in use.");
		}
		
		return buildTree.emptyStatement();
	}

	
	public Object VisitReturnStatement(ASTReturnStatement ret) {
		Type t, returnType;
		TypeClass typeClass = null;

		if(ret.value() == null){
			t = VoidType.instance();
			typeClass = new TypeClass(t, buildTree.constantExpression(0));

		}else{
			typeClass = (TypeClass) ret.value().Accept(this);
			t = typeClass.type();
		}

		returnType = typeEnv.find(returnTypeStat);

		if(returnType == null){
			returnType = VoidType.instance();
		}

		if(!returnType.equals(t)){
			CompError.message(ret.line(),
								"Return must be of type " + returnTypeStat);
		}

		
		return buildTree.returnStatement(typeClass.tree(), endLabel);
	}

	
	public Object VisitStatements(ASTStatements statements) {
		//varEnv.beginScope();
		offset = 0;
		Vector v = new Vector();

		for(int i = 0; i < statements.size(); i++){
			v.addElement(statements.elementAt(i).Accept(this));
		}

		AATStatement seq = (AATStatement) v.elementAt(v.size() - 1);

		for(int j = v.size() - 2; j >= 0; j--){
			seq = buildTree.sequentialStatement((AATStatement)v.elementAt(j), seq);
		}

		//varEnv.endScope();
		return seq;
	}

	
	public Object VisitUnaryOperatorExpression(
			ASTUnaryOperatorExpression operator) {
		Type type;
		TypeClass typeClass;
		typeClass = (TypeClass) operator.operand().Accept(this);
		type = typeClass.type();

		if(!type.equals(BooleanType.instance())){
			CompError.message(operator.line(),
								"Operand for \"!\" must be a boolean.");
			type = BooleanType.instance();
		}


		AATExpression unaryOp = buildTree.operatorExpression(typeClass.tree(), buildTree.constantExpression(1), AATOperator.MINUS);

		return new TypeClass(type, unaryOp);
	}

	
	public Object VisitVariableDefStatement(ASTVariableDefStatement vardef) {
		Type type, init;
		String key, keyPref;
		VariableEntry varEnt;
		TypeClass initClass = null;
		AATExpression base;
		AATStatement varInit;

		type = typeEnv.find(vardef.type());

		if(type == null){
			CompError.message(vardef.line(),
								"Type \"" + vardef.type() + "\" is not yet defined.");
			type = IntegerType.instance();
		}

		keyPref = type.toString();

		if(vardef.arraydimension() != 0){
			for(int j = 1; j <= vardef.arraydimension(); j++){
				key = keyPref + Integer.toString(j) + "dim";
				if (typeEnv.find(key) == null) {
					type = new ArrayType(type);
					typeEnv.insert(key, type);
				}else{
					type = typeEnv.find(key);
				}
			}
			
		}		
		base = buildTree.baseVariable(offset);
		

		if(vardef.init() != null){

			initClass = (TypeClass) vardef.init().Accept(this);
			init = initClass.type();
			if(!init.equals(type)){
				CompError.message(vardef.line(),
									"Type mismatch error.");
			}

			varInit = buildTree.assignmentStatement(base, initClass.tree());
		}else{
			varInit = buildTree.emptyStatement();
		}
		

		varEnt = new VariableEntry(type, offset);
		offset += wordSize;


		varEnv.insert(vardef.name(), varEnt);

		return varInit;
	}

	
	public Object VisitVariableExpression(
			ASTVariableExpression variableexpression) {
		TypeClass type;
		type = (TypeClass) variableexpression.variable().Accept(this);



		return type;
	}

	
	public Object VisitWhileStatement(ASTWhileStatement whilestatement) {
		TypeClass testClass;
		Type test;

		testClass = (TypeClass) whilestatement.test().Accept(this);
		test = testClass.type();


		if(!test.equals(BooleanType.instance())){
			CompError.message(whilestatement.line()
								, "While test must be of type boolean.");
		}

		AATStatement body = (AATStatement) whilestatement.body().Accept(this);

		return buildTree.whileStatement(testClass.tree(), body);
	}


}
