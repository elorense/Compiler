import java.io.*;

class CodeGenerator implements AATVisitor { 

    
    public CodeGenerator(String output_filename) {
        try {
            output = new PrintWriter(new FileOutputStream(output_filename));
        } catch (IOException e) {
            System.out.println("Could not open file "+output_filename+" for writing.");
        }
        /*  Feel free to add code here, if you want to */
        EmitSetupCode();
    }
  
    public Object VisitCallExpression(AATCallExpression expression) { 
        int actuals = expression.actuals().size();

        if(actuals == 0){
            emit("jal " + expression.label());        
            emit("addi " + Register.ACC() + "," + Register.Result() + "," + 0);
        }else{
            for(int i = actuals - 1; i >= 0; i--){
                ((AATExpression) expression.actuals().elementAt(i)).Accept(this);           
                emit("sw " + Register.ACC() + ", 0(" + Register.SP() + ")");    
                emit("addi " + Register.SP() + "," + Register.SP() + "," + (-wordSize));
            }
            emit("jal " + expression.label());
            emit("addi " + Register.SP() + "," + Register.SP() + "," + ((wordSize)*actuals));  
            emit("addi " + Register.ACC() + "," + Register.Result() + "," + 0); 
        }
        return null;
    }
  
    public Object VisitMemory(AATMemory expression) { 
        AATConstant constNode;
        AATOperator opNode;
        AATRegister regNode;
        AATExpression expNode = expression.mem();

        if(expNode instanceof AATOperator){
            opNode = (AATOperator) expNode; 

            if(opNode.operator() == AATOperator.PLUS){
                if(opNode.left() instanceof AATRegister){
                    regNode = (AATRegister) opNode.left(); 
                    if(opNode.right() instanceof AATConstant){
                        constNode = (AATConstant) opNode.right();
                        emit("lw " + Register.ACC() + "," + (constNode.value()) + "(" + regNode.register() + ")");
                    }else{                      
                        expression.mem().Accept(this);
                        emit("lw " + Register.ACC() + ",0(" + Register.ACC() + ")");
                    }
                }else{
                    if(opNode.right() instanceof AATConstant){
                        constNode = (AATConstant) opNode.right();
                        opNode.left().Accept(this);
                        emit("lw " + Register.ACC() + "," + (constNode.value()) + "(" + Register.ACC() + ")");
                    }else{                  
                        expression.mem().Accept(this);
                        emit("lw " + Register.ACC() + ",0(" + Register.ACC() + ")");
                    }
                }
            }else if(opNode.operator() == AATOperator.MINUS){
                if(opNode.left() instanceof AATRegister){
                    regNode = (AATRegister) opNode.left();
                    if(opNode.right() instanceof AATConstant){
                        constNode = (AATConstant) opNode.right();
                        emit("lw " + Register.ACC() + "," + (-constNode.value()) + "(" + regNode.register() + ")");
                    }else{
                        expression.mem().Accept(this);
                        emit("lw " + Register.ACC() + ",0(" + Register.ACC() + ")");
                    }
                }else{
                    if(opNode.right() instanceof AATConstant){
                        constNode = (AATConstant) opNode.right();
                        opNode.left().Accept(this);
                        emit("lw " + Register.ACC() + "," + (-constNode.value()) + "(" + Register.ACC() + ")");

                    }else{
                        expression.mem().Accept(this);
                        emit("lw "+Register.ACC()+",0(" +Register.ACC()+")");
                    }
                }
            }
        }else{
            expression.mem().Accept(this);
            emit("lw " + Register.ACC() + ",0(" + Register.ACC() + ")");   
        }
        return null;
    }
    
    
    public Object VisitOperator(AATOperator expression) { 
        Label labelEq, labelNotEq, endLabelEq, endLabelNotEq;
        AATConstant constRight;

        if (expression.operator()==AATOperator.PLUS){
            expression.left().Accept(this);
                if(expression.right() instanceof AATConstant){
                    constRight = (AATConstant) expression.right();
                    emit("add "+Register.ACC()+","+Register.ACC()+","+constRight.value());
                }else{         
                    emit("sw "+Register.ACC()+", 0("+Register.ESP()+")");
                    emit("addi "+Register.ESP()+","+Register.ESP()+","+(0-wordSize));
                        
                    expression.right().Accept(this);
                        
                    emit("addi "+Register.ESP()+","+Register.ESP()+","+(wordSize));
                    emit("lw "+Register.Tmp1()+",0("+Register.ESP()+")");
                    emit("add "+Register.ACC()+","+Register.Tmp1()+","+Register.ACC());
                          
            }
        }else if (expression.operator()==AATOperator.MINUS){
            expression.left().Accept(this);
            
            if(expression.right() instanceof AATConstant){
                constRight =(AATConstant) expression.right();
                emit("sub "+Register.ACC()+","+Register.ACC()+","+constRight.value());
            }else{                  
                emit("sw "+Register.ACC()+", 0("+Register.ESP()+")");
                emit("addi "+Register.ESP()+","+Register.ESP()+","+(0-wordSize));
                    
                expression.right().Accept(this);
                    
                emit("addi "+Register.ESP()+","+Register.ESP()+","+(wordSize));
                emit("lw "+Register.Tmp1()+",0("+Register.ESP()+")");
                emit("sub "+Register.ACC()+","+Register.Tmp1()+","+Register.ACC());
                               
            }
        }else if (expression.operator()==AATOperator.MULTIPLY){
            expression.left().Accept(this);
            
            emit("sw "+Register.ACC()+", 0("+Register.ESP()+")");
            emit("addi "+Register.ESP()+","+Register.ESP()+","+(0-wordSize));
            
            expression.right().Accept(this);
            
            emit("addi "+Register.ESP()+","+Register.ESP()+","+(wordSize));
            emit("lw "+Register.Tmp1()+",0("+Register.ESP()+")");
            emit("mult "+Register.Tmp1()+","+Register.ACC());
            emit("mflo "+Register.ACC());       
        }else if (expression.operator()==AATOperator.DIVIDE){
            expression.left().Accept(this);
            
            emit("sw "+Register.ACC()+", 0("+Register.ESP()+")");
            emit("addi "+Register.ESP()+","+Register.ESP()+","+(0-wordSize));
            
            expression.right().Accept(this);
            
            emit("addi "+Register.ESP()+","+Register.ESP()+","+(wordSize));
            emit("lw "+Register.Tmp1()+",0("+Register.ESP()+")");
            emit("div "+Register.Tmp1()+","+Register.ACC());
            emit("mflo "+Register.ACC());
        }else if (expression.operator()==AATOperator.AND){
            expression.left().Accept(this);
            
            emit("sw "+Register.ACC()+", 0("+Register.ESP()+")");
            emit("addi "+Register.ESP()+","+Register.ESP()+","+(0-wordSize));
            
            expression.right().Accept(this);
            
            emit("addi "+Register.ESP()+","+Register.ESP()+","+(wordSize));
            emit("lw "+Register.Tmp1()+",0("+Register.ESP()+")");
            emit("mult "+Register.Tmp1()+","+Register.ACC());
            emit("mflo "+Register.ACC());
        }else if (expression.operator()==AATOperator.OR){
            expression.left().Accept(this);
            
            emit("sw "+Register.ACC()+", 0("+Register.ESP()+")");
            emit("addi "+Register.ESP()+","+Register.ESP()+","+(0-wordSize));
            expression.right().Accept(this);
            
            emit("addi "+Register.ESP()+","+Register.ESP()+","+(wordSize));
            emit("lw "+Register.Tmp1()+",0("+Register.ESP()+")");
            emit("add "+Register.ACC()+","+Register.Tmp1()+","+Register.ACC());
        }else if (expression.operator()==AATOperator.EQUAL){
            labelEq = new Label("labelEq");
            endLabelEq = new Label("endLabelEq"); 
            
            expression.left().Accept(this);
            
            emit("sw "+Register.ACC()+", 0("+Register.ESP()+")");
            emit("addi "+Register.ESP()+","+Register.ESP()+","+(0-wordSize));
            
            expression.right().Accept(this);
            
            emit("addi "+Register.ESP()+","+Register.ESP()+","+(wordSize));
            emit("lw "+Register.Tmp1()+",0("+Register.ESP()+")");
            emit("beq "+Register.Tmp1()+","+Register.ACC()+", "+labelEq);
            emit("addi "+Register.ACC()+","+Register.Zero()+","+0);
            emit("j "+endLabelEq);
            emit(labelEq+" :");    
            emit("addi "+Register.ACC()+","+Register.Zero()+","+1);  
            emit(endLabelEq+" :"); 
        }else if (expression.operator()==AATOperator.NOT_EQUAL){
            labelNotEq = new Label("labelNotEq");
            endLabelNotEq = new Label("endLabelNotEq");
            
            expression.left().Accept(this);
            
            emit("sw "+Register.ACC()+", 0("+Register.ESP()+")");
            emit("addi "+Register.ESP()+","+Register.ESP()+","+(0-wordSize));
            
            expression.right().Accept(this);
            
            emit("addi "+Register.ESP()+","+Register.ESP()+","+(wordSize));
            emit("lw "+Register.Tmp1()+",0("+Register.ESP()+")");
            emit("bne "+Register.Tmp1()+","+Register.ACC()+", "+labelNotEq);
            emit("addi "+Register.ACC()+","+Register.Zero()+","+0); 
            emit("j "+endLabelNotEq);
            emit(labelNotEq+" :");
            emit("addi "+Register.ACC()+","+Register.Zero()+","+1);  
            emit(endLabelNotEq+" :"); 
        }else if (expression.operator()==AATOperator.GREATER_THAN){
            expression.left().Accept(this);
            
            emit("sw "+Register.ACC()+", 0("+Register.ESP()+")");
            emit("addi "+Register.ESP()+","+Register.ESP()+","+(0-wordSize));
            
            expression.right().Accept(this);
            
            emit("addi "+Register.ESP()+","+Register.ESP()+","+(wordSize));
            emit("lw "+Register.Tmp1()+",0("+Register.ESP()+")");
            emit("slt "+Register.ACC()+","+Register.ACC()+","+Register.Tmp1());
        }else if (expression.operator()==AATOperator.LESS_THAN){
            expression.left().Accept(this);
            
            emit("sw "+Register.ACC()+", 0("+Register.ESP()+")");
            emit("addi "+Register.ESP()+","+Register.ESP()+","+(0-wordSize));
            
            expression.right().Accept(this);
            
            emit("addi "+Register.ESP()+","+Register.ESP()+","+(wordSize));
            emit("lw "+Register.Tmp1()+",0("+Register.ESP()+")");
            emit("slt "+Register.ACC()+","+Register.Tmp1()+","+Register.ACC());
        } else if (expression.operator()==AATOperator.LESS_THAN_EQUAL){
            expression.left().Accept(this);
            
            emit("sw "+Register.ACC()+", 0("+Register.ESP()+")");
            emit("addi "+Register.ESP()+","+Register.ESP()+","+(0-wordSize));
            
            expression.right().Accept(this);
            
            emit("addi "+Register.ESP()+","+Register.ESP()+","+(wordSize));
            emit("lw "+Register.Tmp1()+",0("+Register.ESP()+")");   
            emit("addi "+Register.Tmp1()+","+Register.Tmp1()+","+-1);       
            emit("slt "+Register.ACC()+","+Register.Tmp1()+","+Register.ACC());
        } else if (expression.operator()==AATOperator.GREATER_THAN_EQUAL){
            expression.left().Accept(this);
            
            emit("sw "+Register.ACC()+", 0("+Register.ESP()+")");
            emit("addi "+Register.ESP()+","+Register.ESP()+","+(0-wordSize));
            
            expression.right().Accept(this);
            
            emit("addi "+Register.ESP()+","+Register.ESP()+","+(wordSize));
            emit("lw "+Register.Tmp1()+",0("+Register.ESP()+")");       
            emit("addi "+Register.ACC()+","+Register.ACC()+","+-1); 
            emit("slt "+Register.ACC()+","+Register.ACC()+","+Register.Tmp1());
        } else if (expression.operator()==AATOperator.NOT){
            expression.left().Accept(this); 
            
            emit("sw "+Register.ACC()+", 0("+Register.ESP()+")");
            emit("lw "+Register.Tmp1()+",0("+Register.ESP()+")");
            emit("addi "+Register.ACC()+","+Register.Zero()+","+1);
            emit("sub "+Register.ACC()+","+Register.ACC()+","+Register.Tmp1());         
        }           

        return null;
    }

    public Object VisitRegister(AATRegister expression) { 
        emit("addi " + Register.ACC() + ", " + expression.register() + ", " + 0);
        return null;
    }
    
    public Object VisitCallStatement(AATCallStatement statement) {
        int actuals = statement.actuals().size();

        if(actuals == 0){
            emit("jal " + statement.label());        
        }else{
            for(int i = actuals - 1; i >= 0; i--){
                ((AATExpression) statement.actuals().elementAt(i)).Accept(this);           
                emit("sw " + Register.ACC() + ", 0(" + Register.SP() + ")");    
                emit("addi " + Register.SP() + "," + Register.SP() + "," + (-wordSize));
            }
     
            emit("jal " + statement.label());
            emit("addi " + Register.SP() + "," + Register.SP() + "," + ((wordSize)*actuals));  
        }
        return null;
    }
    
    public Object VisitConditionalJump(AATConditionalJump statement) {
        AATExpression statTest = statement.test();
        AATOperator opNode;
        AATConstant constLeft, constRight;

        if(statTest instanceof AATOperator){
            opNode = (AATOperator) statTest;
            if(opNode.operator() == AATOperator.EQUAL){
                if(opNode.left() instanceof AATConstant){
                    constLeft = (AATConstant) opNode.left();
                    
                    if(opNode.right() instanceof AATConstant){
                        constRight = (AATConstant) opNode.right();
                        emit("beq " + constLeft.value() + "," + constRight.value() + ", " + statement.label());
                    }else{
                        opNode.right().Accept(this);
                        emit("beq " + constLeft.value() + "," + Register.ACC() + ", " + statement.label());
                    }
                }else{
                    opNode.left().Accept(this);
                    
                    if (opNode.right() instanceof AATConstant){
                        constRight = (AATConstant) opNode.right(); 
                        emit("beq " + Register.ACC() + "," + constRight.value() + ", " + statement.label());
                    }else{
                        emit("sw " + Register.ACC() + ", 0(" + Register.ESP() + ")"); 
                        emit("addi " + Register.ESP() + "," + Register.ESP() + "," + (-wordSize));   
                        
                        opNode.right().Accept(this); 
                        
                        emit("lw " + Register.Tmp1() + "," + wordSize + "(" + Register.ESP() + ")");
                        emit("addi " + Register.ESP() + "," + Register.ESP() + "," + (wordSize));        
                        emit("beq " + Register.Tmp1() + "," + Register.ACC() + ", " + statement.label());                 
                    }
                }
            }else if(opNode.operator() == AATOperator.NOT_EQUAL){
                if(opNode.left() instanceof AATConstant){
                    constLeft = (AATConstant) opNode.left(); 
                    
                    if (opNode.right() instanceof AATConstant){
                        constRight = (AATConstant) opNode.right(); 
                        emit("bne " + constLeft.value() + "," + constRight.value() + ", " + statement.label());
                    }else {
                        opNode.right().Accept(this);
                        emit("bne " + constLeft.value() + "," + Register.ACC() + ", " + statement.label());
                    }
                }else{
                    opNode.left().Accept(this);
                    
                    if (opNode.right() instanceof AATConstant){
                        constRight = (AATConstant) opNode.right(); 
                        emit("bne " + Register.ACC() + "," + constRight.value() + ", " + statement.label());
                    }else{
                        emit("sw " + Register.ACC() + ", 0(" + Register.ESP() + ")"); 
                        emit("addi " + Register.ESP() + "," + Register.ESP() + "," + (-wordSize));   
                        
                        opNode.right().Accept(this);
                        
                        emit("lw " + Register.Tmp1() + "," + wordSize + "(" + Register.ESP() + ")");
                        emit("addi " + Register.ESP() + "," + Register.ESP() + "," + (wordSize));        
                        emit("bne " + Register.Tmp1() + "," + Register.ACC() + ", " + statement.label());                 
                    }
                }
            }else if(opNode.operator() == AATOperator.GREATER_THAN){ 
                opNode.left().Accept(this);                 
                
                if(opNode.right() instanceof AATConstant){                  
                    constRight = (AATConstant) opNode.right();
                    
                    if(constRight.value()==0){
                        emit("bgtz " + Register.ACC() + ", " + statement.label());    
                    }else{
                        emit("addi " + Register.ACC() + "," + Register.ACC() + "," + (-(constRight.value())));                        
                        emit("bgtz " + Register.ACC() + ", " + statement.label());
                    }
                }else{
                    emit("sw " + Register.ACC() + ", 0(" + Register.ESP() + ")"); 
                    emit("addi " + Register.ESP() + "," + Register.ESP() + "," + (-wordSize));   
                    
                    opNode.right().Accept(this);
                    
                    emit("lw " + Register.Tmp1() + "," + wordSize + "("  + Register.ESP() + ")");
                    emit("addi " + Register.ESP() + "," + Register.ESP() + "," + (wordSize));    
                    emit("slt " + Register.ACC() + "," + Register.ACC() + "," + Register.Tmp1());
                    emit("bgtz " + Register.ACC() + ", " + statement.label());  
                }
            }else if (opNode.operator() == AATOperator.LESS_THAN){ 
                opNode.left().Accept(this);                 
                
                if(opNode.right() instanceof AATConstant){
                    constRight = (AATConstant) opNode.right();
                    
                    if(constRight.value()==0){
                        emit("bltz " + Register.ACC() + ", " + statement.label());    
                    }else{
                        emit("addi " + Register.ACC() + "," + Register.ACC() + "," + (-(constRight.value())));                        
                        emit("bltz " + Register.ACC() + ", " + statement.label());
                    }
                }else{
                    emit("sw " + Register.ACC() + ", 0(" + Register.ESP() + ")"); 
                    emit("addi " + Register.ESP() + "," + Register.ESP() + "," + (-wordSize));   
                    
                    opNode.right().Accept(this);
                    
                    emit("lw " + Register.Tmp1() + "," + wordSize + "(" + Register.ESP() + ")");
                    emit("addi " + Register.ESP() + "," + Register.ESP()+"," + (wordSize));    
                    emit("slt " + Register.ACC() + "," + Register.Tmp1() + "," + Register.ACC());
                    emit("bgtz " + Register.ACC() + ", " + statement.label());
                }
            }else if(opNode.operator() == AATOperator.GREATER_THAN_EQUAL){                               
                opNode.left().Accept(this);                 
                
                if(opNode.right() instanceof AATConstant){                     
                    constRight = (AATConstant) opNode.right();
                    
                    if(constRight.value() == 0){
                        emit("bgez " + Register.ACC() + ", " + statement.label());    
                    }else{
                        emit("addi " + Register.ACC() + "," + Register.ACC() + "," + (-(constRight.value()-1)));                      
                        emit("bgtz " + Register.ACC() + ", " + statement.label());
                    }
                }else{                  
                    emit("sw " + Register.ACC() + ", 0(" + Register.ESP() + ")"); 
                    emit("addi " + Register.ESP() + "," + Register.ESP() + "," + (-wordSize));   
                    
                    opNode.right().Accept(this);
                    
                    emit("lw " + Register.Tmp1() + "," + wordSize + "(" + Register.ESP() + ")");
                    emit("addi " + Register.ESP() + "," + Register.ESP() + "," + (wordSize));                
                    emit("addi " + Register.ACC() + "," + Register.ACC() + "," +-1);                     
                    emit("slt " + Register.ACC() + "," + Register.ACC() + "," + Register.Tmp1());
                    emit("bgtz " + Register.ACC() + ", " + statement.label());                
                }
            }else if(opNode.operator() == AATOperator.LESS_THAN_EQUAL){                  
                opNode.left().Accept(this);                 
                
                if(opNode.right() instanceof AATConstant){
                    constRight = (AATConstant) opNode.right(); 
                    
                    if(constRight.value() == 0){
                        emit("blez " + Register.ACC() + ", " + statement.label());    
                    }else{
                        emit("addi " + Register.ACC() + "," + Register.ACC() + "," + (-(constRight.value()-1)));                      
                        emit("bltz " + Register.ACC() + ", " + statement.label());
                    }
                }else{
                    emit("sw " + Register.ACC() + ", 0(" + Register.ESP() + ")"); 
                    emit("addi " + Register.ESP() + "," + Register.ESP() + "," + (-wordSize));   
                    
                    opNode.right().Accept(this);
                    
                    emit("lw " + Register.Tmp1() + "," + wordSize + " (" + Register.ESP() + ")");
                    emit("addi " + Register.ESP() + "," + Register.ESP() + "," + (wordSize));                
                    emit("addi " + Register.Tmp1() + "," + Register.Tmp1() + "," + -1);                       
                    emit("slt " + Register.ACC() + "," + Register.Tmp1() + "," + Register.ACC());
                    emit("bgtz " + Register.ACC() + ", " + statement.label());      
                }
            }else{         
                statement.test().Accept(this);
                emit("bgtz " + Register.ACC() + ", " + statement.label());
            }
        }else{         
            statement.test().Accept(this);
            emit("bgtz " + Register.ACC() + ", " + statement.label());
        }

        return null;
    }
    
    public Object VisitEmpty(AATEmpty statement) {
        return null;
    }
    
    public Object VisitJump(AATJump statement) {
        emit("j " + statement.label());
        return null;
    }

    public Object VisitLabel(AATLabel statement) {
        emit(statement.label() + ":");
        return null;
    }

    public Object VisitMove(AATMove statement) {
        AATRegister regNode, regNodeRight, regNodeLeft;
        AATOperator opNode;
        AATConstant constNode;
        AATMemory memNode;
        AATExpression expNode;

        if(statement.lhs() instanceof AATRegister){         
            regNode =(AATRegister) statement.lhs();       
            
            if (statement.rhs() instanceof AATOperator){
                opNode = (AATOperator) statement.rhs();
                
                if (opNode.operator()==AATOperator.PLUS){
                    if(opNode.left() instanceof AATRegister){
                        regNodeLeft = (AATRegister) opNode.left();
                        
                        if (opNode.right() instanceof AATConstant){
                            constNode = (AATConstant) opNode.right();  
                            emit("addi "+regNode.register()+","+regNodeLeft.register()+","+(constNode.value()));
                        }
                    }else{
                        statement.rhs().Accept(this);
                        emit("addi "+regNode.register()+","+Register.ACC()+",0");
                    }               
                }else if(opNode.operator()==AATOperator.MINUS){
                    if(opNode.left() instanceof AATRegister){
                        regNodeLeft = (AATRegister) opNode.left();
                        
                        if (opNode.right() instanceof AATConstant){
                            constNode = (AATConstant) opNode.right();  
                            emit("addi "+regNode.register()+","+regNodeLeft.register()+","+(-constNode.value()));
                        }
                    }else{
                        statement.rhs().Accept(this);
                        emit("addi "+regNode.register()+","+Register.ACC()+",0");
                    }
                    
                }else{
                    statement.rhs().Accept(this);
                    emit("addi "+regNode.register()+","+Register.ACC()+",0");
                }
            }else{ 
                if(statement.rhs() instanceof AATRegister){
                    regNodeRight = (AATRegister) statement.rhs();
                    emit("addi "+regNode.register()+","+regNodeRight.register()+",0");
                }else{           
                    statement.rhs().Accept(this);
                    emit("addi "+regNode.register()+","+Register.ACC()+",0");
                }
            }
        }else{      
            if(statement.lhs() instanceof AATMemory){            
                memNode = (AATMemory) statement.lhs();
                expNode = memNode.mem();            
                
                if (expNode instanceof AATOperator){ 
                    opNode = (AATOperator) expNode;        

                    if (opNode.operator()==AATOperator.PLUS){
                        if(opNode.left() instanceof AATRegister){
                            regNodeLeft = (AATRegister) opNode.left();                       
                            if(opNode.right() instanceof AATConstant){
                                constNode = (AATConstant) opNode.right();   
                                if(statement.rhs() instanceof AATRegister){
                                    regNode = (AATRegister) statement.rhs();
                                    emit("sw "+regNode.register()+","+ ((constNode.value()))+"("+regNode.register()+")");
                                }else{
                                    statement.rhs().Accept(this);           
                                    emit("sw "+Register.ACC()+","+ ((constNode.value()))+"("+regNodeLeft.register()+")");
                                }
                            }else {
                                expNode.Accept(this);
                                if (statement.rhs() instanceof AATRegister){
                                    regNode = (AATRegister) statement.rhs();
                                    emit("sw "+regNode.register()+", 0("+Register.ACC()+")");
                                }else{                                  
                                    emit("sw "+Register.ACC()+", 0("+Register.ESP()+")");
                                    emit("addi "+Register.ESP()+","+Register.ESP()+","+ -wordSize);                                                                
                                    
                                    statement.rhs().Accept(this);
                                    
                                    emit("lw "+Register.Tmp1()+","+wordSize+"("+Register.ESP()+")");
                                    emit("addi "+Register.ESP()+","+Register.ESP()+","+ wordSize);
                                    emit("sw "+Register.ACC()+", 0("+Register.Tmp1()+")");                                                                  
                                }
                            }                           
                        }else{                                                            
                            if(opNode.right() instanceof AATConstant){
                                constNode = (AATConstant) opNode.right();                                  
                                opNode.left().Accept(this);                      
                                
                                emit("sw "+Register.ACC()+", 0("+Register.ESP()+")");
                                emit("addi "+Register.ESP()+","+Register.ESP()+","+ -wordSize);                        
                                
                                statement.rhs().Accept(this);
                                
                                emit("lw "+Register.Tmp1()+","+wordSize+"("+Register.ESP()+")");
                                emit("addi "+Register.ESP()+","+Register.ESP()+","+ wordSize);
                                emit("sw "+Register.ACC()+","+ ((constNode.value()))+"("+Register.Tmp1()+")");
                            }else{
                                expNode.Accept(this);
                                if(statement.rhs() instanceof AATRegister){
                                    regNode = (AATRegister) statement.rhs();
                                    emit("sw "+regNode.register()+", 0("+Register.ACC()+")");
                                }else{                                   
                                    emit("sw "+Register.ACC()+", 0("+Register.ESP()+")");
                                    emit("addi "+Register.ESP()+","+Register.ESP()+","+ -wordSize);                    
                                    
                                    statement.rhs().Accept(this);
                                    
                                    emit("lw "+Register.Tmp1()+","+wordSize+"("+Register.ESP()+")");
                                    emit("addi "+Register.ESP()+","+Register.ESP()+","+ wordSize);
                                    emit("sw "+Register.ACC()+", 0("+Register.Tmp1()+")");                                                                  
                                }
                            }                       
                        }
                    }else if (opNode.operator()==AATOperator.MINUS){
                        if(opNode.left() instanceof AATRegister){
                            regNodeLeft = (AATRegister) opNode.left();                   
                            if(opNode.right() instanceof AATConstant){
                                constNode = (AATConstant) opNode.right();   
                                if(statement.rhs() instanceof AATRegister){
                                    regNode = (AATRegister) statement.rhs();
                                    emit("sw "+regNode.register()+","+ (-(constNode.value()))+"("+regNode.register()+")");
                                }else{
                                    statement.rhs().Accept(this);
                                    emit("sw "+Register.ACC()+","+ (-(constNode.value()))+"("+regNodeLeft.register()+")");
                                }
                            }                       
                            else{
                                expNode.Accept(this);
                                if (statement.rhs() instanceof AATRegister){
                                    regNode=(AATRegister) statement.rhs();
                                    emit("sw "+regNode.register()+", 0("+Register.ACC()+")");
                                }
                                else{                               
                                    emit("sw "+Register.ACC()+", 0("+Register.ESP()+")");
                                    emit("addi "+Register.ESP()+","+Register.ESP()+","+ -wordSize);                                
                                   
                                    statement.rhs().Accept(this);
                                   
                                    emit("lw "+Register.Tmp1()+","+wordSize+"("+Register.ESP()+")");
                                    emit("addi "+Register.ESP()+","+Register.ESP()+","+ wordSize);
                                    emit("sw "+Register.ACC()+", 0("+Register.Tmp1()+")");                                                                  
                                }
                            }                   
                        }
                        else{                      
                            if(opNode.right() instanceof AATConstant){                
                                constNode = (AATConstant) opNode.right();                                                  
                                opNode.left().Accept(this);                          
                                
                                emit("sw "+Register.ACC()+", 0("+Register.ESP()+")");
                                emit("addi "+Register.ESP()+","+Register.ESP()+","+ -wordSize);                        
                                
                                statement.rhs().Accept(this);
                                
                                emit("lw "+Register.Tmp1()+","+wordSize+"("+Register.ESP()+")");
                                emit("addi "+Register.ESP()+","+Register.ESP()+","+ wordSize);                         
                                emit("sw "+Register.ACC()+","+ (-(constNode.value()))+"("+Register.Tmp1()+")");
                             }else {
                                expNode.Accept(this);
                                if (statement.rhs() instanceof AATRegister){
                                    regNode = (AATRegister) statement.rhs();
                                    emit("sw "+regNode.register()+", 0("+Register.ACC()+")");
                                }else{
                                    emit("sw "+Register.ACC()+", 0("+Register.ESP()+")");
                                    emit("addi "+Register.ESP()+","+Register.ESP()+","+ -wordSize);                                
                                    
                                    statement.rhs().Accept(this);
                                    
                                    emit("lw "+Register.Tmp1()+","+wordSize+"("+Register.ESP()+")");
                                    emit("addi "+Register.ESP()+","+Register.ESP()+","+ wordSize);
                                    emit("sw "+Register.ACC()+", 0("+Register.Tmp1()+")");                                                      
                                }
                              }                         
                         }
                    }          
                }else {
                    expNode.Accept(this);
                    if (statement.rhs() instanceof AATRegister){
                        regNode = (AATRegister) statement.rhs();
                        emit("sw "+regNode.register()+", 0("+Register.ACC()+")");
                    }else{
                        emit("sw "+Register.ACC()+", 0("+Register.ESP()+")");
                        emit("addi "+Register.ESP()+","+Register.ESP()+","+ -wordSize);                    
                        
                        statement.rhs().Accept(this);
                        
                        emit("lw "+Register.Tmp1()+","+wordSize+"("+Register.ESP()+")");
                        emit("addi "+Register.ESP()+","+Register.ESP()+","+ wordSize);
                        emit("sw "+Register.ACC()+", 0("+Register.Tmp1()+")");              
                    }
                }               
            }
        }           
        return null;
    }

    public Object VisitReturn(AATReturn statement) {
        emit("jr " + Register.ReturnAddr());
        return null;
    }

    public Object VisitHalt(AATHalt halt) {
    /* Don't need to implement halt -- you can leave 
       this as it is, if you like */
        return null;
    }
    public Object VisitSequential(AATSequential statement) {
        statement.left().Accept(this);
        statement.right().Accept(this);
        return null;
    }
    
    public Object VisitConstant(AATConstant expression) {
        emit("addi " + Register.ACC() + "," + Register.Zero() + "," + expression.value());
        return null;
    }
    
    private void emit(String assem) {
        assem = assem.trim();
        if (assem.charAt(assem.length()-1) == ':') 
            output.println(assem);
        else
            output.println("\t" + assem);
        }
    
    public void GenerateLibrary() {
        emit("Print:");
        emit("lw $a0, 4(" + Register.SP() + ")");
        emit("li $v0, 1");
        emit("syscall");
        emit("li $v0,4");
        emit("la $a0, sp");
        emit("syscall");
        emit("jr $ra");
        emit("Println:");
        emit("li $v0,4");
        emit("la $a0, cr");
        emit("syscall");
        emit("jr $ra");
        emit("Read:");
        emit("li $v0,5");
        emit("syscall");
        emit("jr $ra");
        emit("allocate:");
        emit("la " + Register.Tmp1() + ", HEAPPTR");
        emit("lw " + Register.Result() + ",0(" + Register.Tmp1() + ")");
        emit("lw " + Register.Tmp2() + ", 4(" + Register.SP() + ")");
        emit("sub " + Register.Tmp2() + "," + Register.Result() + "," + Register.Tmp2());
        emit("sw " + Register.Tmp2() + ",0(" + Register.Tmp1() + ")");
        emit("jr $ra");
        emit(".data");
        emit("cr:");
        emit(".asciiz \"\\n\"");
        emit("sp:");
        emit(".asciiz \" \"");
        emit("HEAPPTR:");
        emit(".word 0");
        output.flush();
    }
    
    private void EmitSetupCode() {
        emit(".globl main");
        emit("main:");
        emit("addi " + Register.ESP() + "," + Register.SP() + ",0");
        emit("addi " + Register.SP() + "," + Register.SP() + "," + 
             - wordSize * STACKSIZE);
        emit("addi " + Register.Tmp1() + "," + Register.SP() + ",0");
        emit("addi " + Register.Tmp1() + "," + Register.Tmp1() + "," + 
             - wordSize * STACKSIZE);
        emit("la " + Register.Tmp2() + ", HEAPPTR");
        emit("sw " + Register.Tmp1() + ",0(" + Register.Tmp2() + ")");
        emit("sw " + Register.ReturnAddr() + "," + wordSize  + "("+ Register.SP() + ")"); 
        emit("jal main1");
        emit("li $v0, 10");
        emit("syscall");
    }
    
    private final int STACKSIZE = 1000;
    private PrintWriter output;
    private int wordSize = MachineDependent.WORDSIZE;

    /* Feel Free to add more instance variables, if you like */
}

