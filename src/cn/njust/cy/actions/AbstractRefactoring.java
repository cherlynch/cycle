package cn.njust.cy.actions;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.PartInitException;

import cn.njust.cy.entity.DependencyDetail;
import depends.matrix.core.DependencyValue;
import depends.util.FileTraversal;
import depends.util.FileUtil;

public class AbstractRefactoring {
	private IFile fileA;
	private IFile fileB;
	private String newFileName;
	private DependencyDetail details;
	private IJavaProject project;
	
	public AbstractRefactoring(IFile fileA,IFile fileB,String newFileName,DependencyDetail details,IJavaProject project) {
		this.fileA = fileA;
		this.fileB = fileB;
		this.newFileName = newFileName;
		this.details = details;
		this.project = project;
		createNewInterface();
		modifySourceClassA();
		modifySourceClassB();	
	}
	
	public MethodDeclaration findMethodByName(MethodDeclaration[] methodDecls,String methodName) {
//	public Set<MethodDeclaration> findMethodByName(MethodDeclaration[] methodDecls,String methodName) {
		Set <MethodDeclaration> candidateMethodDecls = new HashSet<MethodDeclaration>();
		for(MethodDeclaration methodDecl:methodDecls) {
			if(methodDecl.getName().toString().equals(methodName)) {
				candidateMethodDecls.add(methodDecl);
				return methodDecl;
			}
		}
//		return candidateMethodDecls;
		return null;
	}
	
	public void createNewInterface() {//根据classA创建新的interface
		IFolder sourceFolder = (IFolder) fileA.getParent();
		IPackageFragment sourcePackage = (IPackageFragment)JavaCore.create(sourceFolder);
		ICompilationUnit sourceIUnit = (ICompilationUnit)JavaCore.create(fileA);
		CompilationUnit sourceUnit = parse(sourceIUnit);	
		TypeDeclaration typeDecl = (TypeDeclaration) sourceUnit.types().get(0);
		MethodDeclaration[] methodDecls = typeDecl.getMethods();
		System.out.println(methodDecls[0]);System.out.println(methodDecls.length);
		try {
			for(IJavaElement unit:sourcePackage.getChildren()) {
				if(unit.getElementName().equals(newFileName)) {
					newFileName = newFileName + "Copy"; //如果已经存在，则重新命名
				}
			}
			ICompilationUnit extractIUnit = sourcePackage.createCompilationUnit(newFileName+".java","",false, null);
			
			//设置package 
			if(sourceUnit.getPackage() != null) {
				extractIUnit.createPackageDeclaration(sourceUnit.getPackage().getName().toString(), null);
			}
//			String typeStr = "public interface " + interfaceName + " {"+ "\n" + "}";
			String typeStr = "public abstract class " + newFileName + " {"+ "\n" + "}";
			
			Set<String> requiredImportClass = new HashSet<String>(); //需要import的class
			if(typeDecl.getSuperclassType()!=null) {
				Type superClass = typeDecl.getSuperclassType();
				typeStr = "public abstract class " + newFileName +" extends " + superClass + "{"+ "\n" + "}";
				requiredImportClass.add(superClass.resolveBinding().getPackage().getName()+"."+superClass);
			}
			extractIUnit.createType(typeStr,null,true, null);
			IType type = extractIUnit.getType(newFileName);
			
			//创建 abstract method  不是所有的method都提取，只提取需要调用的
			String qualifiedNameA = typeDecl.resolveBinding().getQualifiedName();
			Set<MethodDeclaration> methodDeclsRequiredAbstract = new HashSet<MethodDeclaration>(); //需要用到的method
			for(DependencyValue value:details.getValues()) {
				if(value.getType().equals("Call")) {
					if(!value.getDetailTo().equals(qualifiedNameA)) {
						String methodName = value.getDetailTo().replaceAll(qualifiedNameA+".","");
						MethodDeclaration methodDecl = findMethodByName(methodDecls,methodName);
						methodDeclsRequiredAbstract.add(methodDecl);
					}
				}
			}
			
			for(MethodDeclaration methodDecl:methodDeclsRequiredAbstract) {
				String myMethod = "";
				List<Modifier> modifiers = methodDecl.modifiers();
				String parameterList = "";
				if(methodDecl.parameters()!=null) {
					List<SingleVariableDeclaration> sourceMethodParameters = methodDecl.parameters();
					for(SingleVariableDeclaration parameter : sourceMethodParameters) {
						if(parameter.getType().toString().equals("")) {
							//如果参数类型为本身，则要将参数也换成abstract
						}
						if(sourceMethodParameters.get(sourceMethodParameters.size()-1).equals(parameter)) {
							parameterList = parameterList + parameter.getType() +" "+ parameter.getName();
						}else {
							parameterList = parameterList + parameter.getType() +" "+ parameter.getName() + ", ";
						}
					}
				}
				boolean isPrivate = false;
				for(Modifier modifier:modifiers) {
					String keywordStr = modifier.getKeyword().toString();
					if(keywordStr.equals("private")||keywordStr.equals("protected")||keywordStr.equals("static")) isPrivate = true;
					myMethod = myMethod + modifier.getKeyword();
				}
				String throwns = "";
				List<Object> thrownList = methodDecl.thrownExceptions();
				if(thrownList.size()!=0) {
					throwns = " throws ";
					for(Object thrownStr:thrownList) {
						if(thrownList.get(thrownList.size()-1).equals(thrownStr)) {
							throwns = throwns + thrownStr.toString();
						}else {
							throwns = throwns + thrownStr.toString() + ",";
						}
					}
				}
				// 设置为abstract 或 interface
//				myMethod = myMethod + " " + methodDecl.getReturnType2()+ " " +methodDecl.getName()
//				+"("+parameterList+")" + throwns + ";";
				myMethod = myMethod +" abstract"+ " " + methodDecl.getReturnType2()+ " " +methodDecl.getName()
								+"("+parameterList+")" + throwns + ";";
				if(methodDecl.getReturnType2()!=null&&!isPrivate) {//不是构造函数；不是私有函数
					type.createMethod(myMethod, null, true, null);
					System.out.println(myMethod);
				}
			}
		
			//得到需要Import的class
			for(MethodDeclaration methodDecl:methodDeclsRequiredAbstract) {
				ITypeBinding returnType = methodDecl.resolveBinding().getReturnType();
				if(returnType.getPackage()!=null) {
					requiredImportClass.add(returnType.getPackage().getName()+"."+returnType.getName());
				}
				for(ITypeBinding parameterIBinding:methodDecl.resolveBinding().getParameterTypes()) {
					if(parameterIBinding.getPackage()!=null) {
						requiredImportClass.add(parameterIBinding.getPackage().getName()+"."+parameterIBinding.getName());
					}							
				}
				for(ITypeBinding thrownIBinding:methodDecl.resolveBinding().getExceptionTypes()) {
					if(thrownIBinding.getPackage()!=null) {
						requiredImportClass.add(thrownIBinding.getPackage().getName()+"."+thrownIBinding.getName());
					}							
				}
			}
			
			//创建需要Import的class
			for(String importStr:requiredImportClass) {
				String requiredImportpackage = importStr.substring(0, importStr.lastIndexOf("."));
				if(!requiredImportpackage.equals("java.lang")&&!requiredImportpackage.equals(sourcePackage.getElementName()))
//					requiredImportClass.remove(str);
					extractIUnit.createImport(importStr,null, null);
			}
			IJavaElement javaElement = extractIUnit;
			JavaUI.openInEditor(javaElement);
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedTreeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void modifySourceClassA() { //修改classA ：添加interface 
		ICompilationUnit sourceIUnit = (ICompilationUnit)JavaCore.create(fileA);
		CompilationUnit sourceUnit = parse(sourceIUnit);	
		TypeDeclaration typeDecl = (TypeDeclaration) sourceUnit.types().get(0);
		AST ast = sourceUnit.getAST();
		ASTRewrite sourceRewriter = ASTRewrite.create(ast);								
		//对源文件设置extends class
		sourceRewriter.set(typeDecl,TypeDeclaration.SUPERCLASS_TYPE_PROPERTY,ast.newName(newFileName),null);
		
//		ListRewrite superInterfaceRewrite = sourceRewriter.getListRewrite(typeDecl, TypeDeclaration.SUPER_INTERFACE_TYPES_PROPERTY);
//		superInterfaceRewrite.insertLast(ast.newName(interfaceName), null);
		try {
			Document document = new Document(sourceIUnit.getSource());
			TextEdit edits = sourceRewriter.rewriteAST();
			edits.apply(document);
			sourceIUnit.getBuffer().setContents(document.get());
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedTreeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	// 对 classB 重命名
	public void modifySourceClassB() {
		ICompilationUnit sourceIUnit = (ICompilationUnit)JavaCore.create(fileB);
		CompilationUnit sourceUnit = parse(sourceIUnit);	
		IPackageFragment sourcePackage = (IPackageFragment)JavaCore.create((IFolder) fileB.getParent());
		TypeDeclaration typeDecl = (TypeDeclaration) sourceUnit.types().get(0);
		MethodDeclaration[] methodDecls = typeDecl.getMethods();
		AST ast = sourceUnit.getAST();
		ASTRewrite sourceRewriter = ASTRewrite.create(ast);		
		String packageName = sourcePackage.getElementName();
		String sourceName = fileA.getName().replaceAll(".java", "");
		String className = packageName + "." + sourceIUnit.getElementName().replace(".java", "");
		//如果A和B不在一个package,那么需要移除import A;添加import abstractA;
		IPackageFragment sourcePackageA = (IPackageFragment)JavaCore.create((IFolder) fileA.getParent());
		if(!packageName.equals(sourcePackageA.getElementName())) {
			ListRewrite packageRewrite = sourceRewriter.getListRewrite(sourceUnit, CompilationUnit.IMPORTS_PROPERTY);
//			ImportDeclaration removeImport =  如果需要移除
			String importStr = sourcePackageA.getElementName()+"."+ newFileName;
			ImportDeclaration importDecl = ast.newImportDeclaration();
			importDecl.setName(ast.newName(importStr));
			packageRewrite.insertLast(importDecl, null);
		}
		//重命名
		typeDecl.accept(new ASTVisitor() {
			public boolean visit(SimpleType node) {
//				System.out.println(node.toString());
				if(node.toString().equals(sourceName)) {
					sourceRewriter.set(node,SimpleType.NAME_PROPERTY, ast.newName(newFileName), null);	
				}
				return true;
			}
		});
		try {
			Document document = new Document(sourceIUnit.getSource());
			TextEdit edits = sourceRewriter.rewriteAST();
			edits.apply(document);
			sourceIUnit.getBuffer().setContents(document.get());
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedTreeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public void modifyImplementsChange(ICompilationUnit IUnit) {
		CompilationUnit unit = parse(IUnit);
		TypeDeclaration typeDecl = (TypeDeclaration) unit.types().get(0);
		AST ast = unit.getAST();
		ASTRewrite sourceRewriter = ASTRewrite.create(ast);
		String sourceName = fileA.getName().replaceAll(".java", "");
		//待修改（重命名的只是接口中修改的地方，同时将将要调用的函数找到，以便提取abstract method）
		typeDecl.accept(new ASTVisitor() {
			public boolean visit(SimpleType node) {
//				System.out.println(node.toString());
				if(node.toString().equals(sourceName)) {
					sourceRewriter.set(node,SimpleType.NAME_PROPERTY, ast.newName(newFileName), null);	
				}
				return true;
			}
		});
		try {
			Document document = new Document(IUnit.getSource());
			TextEdit edits = sourceRewriter.rewriteAST();
			edits.apply(document);
			IUnit.getBuffer().setContents(document.get());
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedTreeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		Collection<DependencyValue> values = details.getValues();
//		for(DependencyValue value:values) {
//			if(value.getType().equals("Call")||value.getType().equals("Parameter")) {
//				String method = 
//			}
//		}
	}
	
	private static CompilationUnit parse(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null); // parse
	}
}
