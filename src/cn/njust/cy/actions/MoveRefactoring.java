package cn.njust.cy.actions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import cn.njust.cy.entity.DependencyDetail;
import depends.matrix.core.DependencyValue;

public class MoveRefactoring extends Refactoring{
	private IFile fileA;
	private IFile fileB;
	private String newFileName;
	private DependencyDetail detail;
	private HashSet<MethodDeclaration> methodsToRemove ;
	private HashSet<String> changeFiles;
	public MoveRefactoring(IFile fileA,IFile fileB,String newFileName,DependencyDetail detail,HashSet<String> changeFiles) {
		this.fileA = fileA;
		this.fileB = fileB;
		this.newFileName = "Another" + fileA.getName().replace(".java", "");
		this.detail = detail;
		this.methodsToRemove = new HashSet<MethodDeclaration>();
		this.changeFiles = changeFiles;
		removeOldMethod();
		createNewClass();
		modifyChanges();
	}
	
	public void removeOldMethod() {
		IFolder folder = (IFolder) fileA.getParent();
		IPackageFragment mypackage = (IPackageFragment)JavaCore.create(folder);
		ICompilationUnit sourceIUnit = (ICompilationUnit)JavaCore.create(fileA);
		CompilationUnit sourceUnit = parse(sourceIUnit);	
		TypeDeclaration sourceTypeDecl = (TypeDeclaration) sourceUnit.types().get(0);
		MethodDeclaration[] sourceMethodDecls = sourceTypeDecl.getMethods();
		AST ast = sourceUnit.getAST();
		ASTRewrite sourceRewriter = ASTRewrite.create(ast);	
		ListRewrite bodyListRewrite = sourceRewriter.getListRewrite(sourceTypeDecl, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		HashSet<String> methodsToRemoveName = new HashSet<String>();
		for(DependencyValue val:detail.getValues()) {
			System.out.println(val.getDetailFrom());
			String []splits = val.getDetailFrom().split("\\.");
			methodsToRemoveName.add(splits[splits.length-1]);
		}
		for(MethodDeclaration methodDecl:sourceMethodDecls) {
			for(String methodName:methodsToRemoveName) {
				if(methodDecl.getName().toString().equals(methodName)) {
					methodsToRemove.add(methodDecl);
					bodyListRewrite.remove(methodDecl, null);
				}
			}
		}
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
	
	public void createNewClass() {
		IFolder sourceFolder = (IFolder) fileA.getParent();
		IPackageFragment sourcePackage = (IPackageFragment)JavaCore.create(sourceFolder);
		ICompilationUnit sourceIUnit = (ICompilationUnit)JavaCore.create(fileA);
		CompilationUnit sourceUnit = parse(sourceIUnit);
		try {
			ICompilationUnit extractIUnit = sourcePackage.createCompilationUnit(newFileName+".java","",false, null);
			if(sourceUnit.getPackage() != null) {
				extractIUnit.createPackageDeclaration(sourceUnit.getPackage().getName().toString(), null);
			}
			String typeStr = "public class " + newFileName + " {"+ "\n" + "}";
			extractIUnit.createType(typeStr,null,true, null);
			IType type = extractIUnit.getType(newFileName);
			
			Set<String> requiredImportClass = new HashSet<String>(); //需要import的class
			for(MethodDeclaration methodDecl:methodsToRemove) {
				type.createMethod(methodDecl.toString(), null, true, null);
				methodDecl.accept(new ASTVisitor() {
					public boolean visit(SimpleType node) {
						System.out.println("name "+node.resolveBinding().getBinaryName());
						requiredImportClass.add(node.resolveBinding().getBinaryName());
						return true;
					}
				});
			}
			for(String importStr:requiredImportClass) {
				String requiredImportpackage = importStr.substring(0, importStr.lastIndexOf("."));
				if(!requiredImportpackage.equals("java.lang")&&!requiredImportpackage.equals(sourcePackage.getElementName()))
					extractIUnit.createImport(importStr,null, null);
			}
			IMethod[] methods = type.getMethods();		 
			System.out.println("methods "+methods.length);
			for (IMethod method : methods) {
				HashSet<IMethod> callers = getCallersOf(method);
				System.out.println("callers: "+callers.size());
				for(IMethod c:callers) {
					System.out.println("callers: "+c.getElementName());
				}
			}
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void modifyChanges() {
		if(changeFiles!=null) { 
			String []arr = new String[2];
			for(String str:changeFiles) {
				arr[0] = str.substring(0,str.lastIndexOf("."));
				arr[1] = str.substring(str.lastIndexOf(".")+1);
				System.out.println("class  "+arr[0]);
				System.out.println("method  "+arr[1]);
				modifyCallChange(arr);
			}
		}
	}
	
	public void modifyCallChange(String[] arr) {
		IJavaProject myProject = ((ICompilationUnit)JavaCore.create(fileA)).getJavaProject();//获取当前项目
		try {
			IType type = myProject.findType(arr[0]);
			ICompilationUnit IUnit = type.getCompilationUnit();
			CompilationUnit unit = parse(IUnit);
			TypeDeclaration typeDecl = (TypeDeclaration) unit.types().get(0);
			MethodDeclaration[] methodDecls = typeDecl.getMethods();
			MethodDeclaration methodDecl = findMethodByName(methodDecls,arr[1]);
			AST ast = unit.getAST();
			ASTRewrite sourceRewriter = ASTRewrite.create(ast);
			String sourceName = fileA.getName().replaceAll(".java", "");
			System.out.println("methodDecl  "+ methodDecl.getName());
			
			methodDecl.accept(new ASTVisitor() {
				public boolean visit(SimpleName node) {
					System.out.println("node : "+node);
					if(node.toString().equals(sourceName)) {
						sourceRewriter.set(node,SimpleType.NAME_PROPERTY, ast.newName(newFileName), null);	
//						sourceRewriter.set(node,SimpleName.VAR_PROPERTY, ast.newName(newFileName), null);	
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
			
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	public String getNewFileName() {
		return newFileName;
	}

	public void setNewFileName(String newFileName) {
		this.newFileName = newFileName;
	}

	private static CompilationUnit parse(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null); // parse
	}
	
	 
	public HashSet<IMethod> getCallersOf(IMethod m) {
		CallHierarchy callHierarchy = CallHierarchy.getDefault();
		IMember[] members = {m};
		MethodWrapper[] methodWrappers = callHierarchy.getCallerRoots(members);
		HashSet<IMethod> callers = new HashSet<IMethod>();
		for (MethodWrapper mw : methodWrappers) {
			MethodWrapper[] mw2 = mw.getCalls(new NullProgressMonitor());
			HashSet<IMethod> temp = getIMethods(mw2);
			callers.addAll(temp);    
		}
		return callers;
	}

	public HashSet<IMethod> getIMethods(MethodWrapper[] methodWrappers) {
		HashSet<IMethod> c = new HashSet<IMethod>(); 
		for (MethodWrapper m : methodWrappers) {
			IMethod im = getIMethodFromMethodWrapper(m);
			if (im != null) {
				c.add(im);
			}
		}
		return c;
	}

	public IMethod getIMethodFromMethodWrapper(MethodWrapper m) {
		try {
			IMember im = m.getMember();
			if (im.getElementType() == IJavaElement.METHOD) {
				return (IMethod)m.getMember();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	public MethodDeclaration findMethodByName(MethodDeclaration[] methodDecls,String methodName) {
//		public Set<MethodDeclaration> findMethodByName(MethodDeclaration[] methodDecls,String methodName) {
			Set <MethodDeclaration> candidateMethodDecls = new HashSet<MethodDeclaration>();
			for(MethodDeclaration methodDecl:methodDecls) {
				if(methodDecl.getName().toString().equals(methodName)) {
					candidateMethodDecls.add(methodDecl);
					return methodDecl;
				}
			}
//			return candidateMethodDecls;
			return null;
		}
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		// TODO Auto-generated method stub
		final RefactoringStatus status= new RefactoringStatus();
		try {
			pm.beginTask("Checking preconditions...", 2);
			removeOldMethod();
		} finally {
			pm.done();
		}
		return status;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		// TODO Auto-generated method stub
		RefactoringStatus status= new RefactoringStatus();
		try {
			pm.beginTask("Checking preconditions...", 1);
		} finally {
			pm.done();
		}
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		// TODO Auto-generated method stub
				try {
					pm.beginTask("Creating change...", 1);
					final Collection<Change> changes = new ArrayList<Change>();
//					changes.addAll(compilationUnitChanges.values());
//					changes.addAll(createCompilationUnitChanges.values());
					CompositeChange change = new CompositeChange(getName(), changes.toArray(new Change[changes.size()])) {
						@Override
						public ChangeDescriptor getDescriptor() {
							ICompilationUnit IUnit = (ICompilationUnit)JavaCore.create(fileA);
							CompilationUnit unit = parse(IUnit);
							TypeDeclaration sourceTypeDecl = (TypeDeclaration) unit.types().get(0);
							String projectName = IUnit.getJavaProject().getElementName();
							String description = MessageFormat.format("Refactor from ''{0}''", new Object[] { sourceTypeDecl.getName().getIdentifier()});
							String comment = null;
							return new RefactoringChangeDescriptor(new MoveRefactoringDescriptor(projectName,description,
									comment,fileA,fileB,newFileName,detail,changeFiles));
						}
					};
					return change;
				} finally {
					pm.done();
				}
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "MoveRefactoring";
	}
}
