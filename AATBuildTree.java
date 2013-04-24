/* Author:  Edric Orense
 * File:    AATBuilTree.java
 * Purpose: Creates Abstract Assembly Trees
 */

import java.util.Vector;

public class AATBuildTree {
    private MachineDependent mSize;
    private Register reg;
    private final int wordSize = mSize.WORDSIZE; 
    private AATRegister fpReg = new AATRegister(Register.FP());
    private AATRegister spReg = new AATRegister(Register.SP());
    private AATRegister retReg = new AATRegister(Register.ReturnAddr());
	
	
    //AAT Tree for function definitions
    public AATStatement functionDefinition(AATStatement body, int framesize, Label start,  
					   Label end) {

	AATLabel startLab = new AATLabel(start);
        AATConstant word = new AATConstant(wordSize);
        AATConstant frameSize = new AATConstant(framesize);
        AATOperator spOffset = new AATOperator(word, frameSize, AATOperator.MULTIPLY);
        
        spOffset = new AATOperator(spReg, spOffset, AATOperator.MINUS);
        AATMemory saveRegs = new AATMemory(spOffset);

        AATMove storeFP = new AATMove(saveRegs, fpReg);

        spOffset = new AATOperator(spReg, new AATOperator(new AATOperator(frameSize, new AATConstant(1), AATOperator.PLUS), word, AATOperator.MULTIPLY), AATOperator.MINUS);
        saveRegs = new AATMemory(spOffset);
        AATMove storeRet = new AATMove(saveRegs, retReg);    

        AATMove moveFP = new AATMove(fpReg, spReg);

        spOffset = new AATOperator(spReg, new AATOperator(new AATOperator(frameSize, new AATConstant(2), AATOperator.PLUS), word, AATOperator.MULTIPLY), AATOperator.MINUS);
        AATMove moveSP = new AATMove(spReg, new AATOperator(spReg, new AATConstant(wordSize*(framesize + 2)), AATOperator.MINUS));

        AATLabel endLab = new AATLabel(end);

        AATMove returnSp = new AATMove(spReg, fpReg);

        AATMove returnRet = new AATMove(retReg, saveRegs);

        spOffset = new AATOperator(word, frameSize, AATOperator.MULTIPLY);
        
        spOffset = new AATOperator(spReg, spOffset, AATOperator.MINUS);
        saveRegs = new AATMemory(spOffset);
        AATMove returnFp = new AATMove(fpReg, saveRegs);

        AATStatement seq = sequentialStatement(returnFp, new AATReturn());
        seq = sequentialStatement(returnRet, seq);
        seq = sequentialStatement(returnSp, seq);
        seq = sequentialStatement(endLab, seq);
        seq = sequentialStatement(body, seq);
        seq = sequentialStatement(moveSP, seq);
        seq = sequentialStatement(moveFP, seq);
        seq = sequentialStatement(storeRet, seq);
        seq = sequentialStatement(storeFP, seq);
        seq = sequentialStatement(startLab, seq);

	    return seq;
    }
    
    public AATStatement ifStatement(AATExpression test, AATStatement ifbody, AATStatement elsebody) {
        Label ifTrueLabel = new Label();
        AATConditionalJump ifTrueJump = new AATConditionalJump(test, ifTrueLabel);
        //elsebody

        Label ifEndLabel = new Label();
        AATJump ifEndJump = new AATJump(ifEndLabel);

        AATLabel ifTrue = new AATLabel(ifTrueLabel);
        //ifbody
        AATLabel ifEnd = new AATLabel(ifEndLabel);

        AATStatement seq = sequentialStatement(ifbody, ifEnd);
        seq = sequentialStatement(ifTrue, seq);
        seq = sequentialStatement(ifEndJump, seq);
        seq = sequentialStatement(elsebody, seq);
        seq = sequentialStatement(ifTrueJump, seq);

	    return seq;
    }
    
    public AATExpression allocate(AATExpression size) {
        Vector v = new Vector();
        v.addElement(size);
	    return new AATCallExpression(Label.AbsLabel("allocate"), v);
    }

    public AATStatement whileStatement(AATExpression test, AATStatement whilebody) { 
        Label whileTestLabel = new Label("WHILETEST");
        AATJump whileTestJump = new AATJump(whileTestLabel);
       
        Label whileStartLabel = new Label("WHILESTART");
        AATLabel whileStart = new AATLabel(whileStartLabel);

        AATLabel whileTest = new AATLabel(whileTestLabel);
        AATConditionalJump whileStartJump = new AATConditionalJump(test, whileStartLabel);

        AATStatement seq = sequentialStatement(whileTest, whileStartJump);
        seq = sequentialStatement(whilebody, seq);
        seq = sequentialStatement(whileStart, seq);
        seq = sequentialStatement(whileTestJump, seq);

	    return seq;
    }

    public AATStatement dowhileStatement(AATExpression test, AATStatement dowhilebody) {
        Label doWhileTestLabel = new Label("DOWHILETEST");
        AATJump doWhileTestJump = new AATJump(doWhileTestLabel);
       
        Label doWhileStartLabel = new Label("DOWHILESTART");
        AATLabel doWhileStart = new AATLabel(doWhileStartLabel);

        AATLabel doWhileTest = new AATLabel(doWhileTestLabel);
        AATConditionalJump doWhileStartJump = new AATConditionalJump(test, doWhileStartLabel);

        AATStatement seq = sequentialStatement(doWhileTest, doWhileStartJump);
        seq = sequentialStatement(dowhilebody, seq);
        seq = sequentialStatement(doWhileStart, seq);
        seq = sequentialStatement(doWhileTestJump, seq);
        seq = sequentialStatement(dowhilebody, seq);

        return seq;
    }
  
    public AATStatement forStatement(AATStatement init, AATExpression test, 
				     AATStatement increment, AATStatement body) {   
        Label forTestLabel = new Label("FORTEST");
        AATJump forTestJump = new AATJump(forTestLabel);

        Label forStartLabel = new Label("FORSTART");
        AATLabel forStart = new AATLabel(forStartLabel);

        AATLabel forTest = new AATLabel(forTestLabel);
        AATConditionalJump forStartJump = new AATConditionalJump(test, forStartLabel);

        AATStatement seq = sequentialStatement(forTest, forStartJump);
        seq = sequentialStatement(increment, seq);
        seq = sequentialStatement(body, seq);
        seq = sequentialStatement(forStart, seq);
        seq = sequentialStatement(forTestJump, seq);
        seq = sequentialStatement(init, seq);

        return seq;
    }
    
    public AATStatement emptyStatement() {
	    return new AATEmpty();
    }
  
    public AATStatement callStatement(Vector actuals, Label name) {
        Vector v = new Vector();
        v.addElement(new AATConstant(3));
	    return new AATCallStatement(name, actuals);
    }
    
    public AATStatement assignmentStatement(AATExpression lhs,
					    AATExpression rhs) {
	    return new AATMove(lhs, rhs);
    }
    
    public AATStatement sequentialStatement(AATStatement first,
					    AATStatement second) {
	    return new AATSequential(first, second);
    }
        
    public AATExpression baseVariable(int offset) {
	    return new AATMemory(new AATOperator(fpReg,
					       new AATConstant(offset),
					       AATOperator.MINUS)) ;
    }

    public AATExpression arrayVariable(AATExpression base,
				       AATExpression index,
				       int elementSize) {
        return new AATMemory(new AATOperator(base, new AATOperator(new AATConstant(elementSize), index, 3), AATOperator.MINUS));
    }
    
    public AATExpression classVariable(AATExpression base, int offset) {
	    return new AATMemory(new AATOperator(base, new AATConstant(offset), AATOperator.MINUS));
    }
  
    public AATExpression constantExpression(int value) {
	    return new AATConstant(value);
    }
  
    public AATExpression operatorExpression(AATExpression left,
					    AATExpression right,
					    int operator) {
	    return new AATOperator(left, right, operator);
    } 
  
    public AATExpression callExpression(Vector actuals, Label name) {
	    return new AATCallExpression(name, actuals);
    }
    
    public AATStatement returnStatement(AATExpression value, Label functionend) {
        AATStatement seq;
        AATReturn returnStat = new AATReturn();
        AATRegister resultReg = new AATRegister(Register.Result());
        AATMove saveResult = new AATMove(resultReg, value);
        AATJump endJump = new AATJump(functionend);

        seq = sequentialStatement(saveResult, endJump);
        //seq = sequentialStatement(returnStat, seq);

	    return seq;
    }
}
