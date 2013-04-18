public class TypeClass{

	public TypeClass(Type type, AATExpression tree){
		type_ = type;
		tree_ = tree;
	}

	public Type type(){
		return type_;
	}

	public AATExpression tree(){
		return tree_;
	}

	public void setType(Type type){
		type_ = type;
	}

	public void setTree(AATExpression tree){
		tree_ = tree;
	}

	private Type type_;
	private AATExpression tree_;
}