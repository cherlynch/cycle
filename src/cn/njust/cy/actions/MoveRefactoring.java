package cn.njust.cy.actions;

import org.eclipse.core.resources.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class MoveRefactoring {
	private IFile fileA;
	private IFile fileB;
	private String newFileName;
	
	public MoveRefactoring(IFile fileA,IFile fileB,String newFileName) {
		this.fileA = fileA;
		this.fileB = fileB;
		this.newFileName = newFileName;
		removeOldMethod();
		createNewClass();
	}
	
	public void removeOldMethod() {
		
	}
	
	public void createNewClass() {
		IFolder sourceFolder = (IFolder) fileA.getParent();
		IPackageFragment sourcePackage = (IPackageFragment)JavaCore.create(sourceFolder);
		ICompilationUnit sourceIUnit = (ICompilationUnit)JavaCore.create(fileA);
		
		try {
			ICompilationUnit extractIUnit = sourcePackage.createCompilationUnit(newFileName+".java","",false, null);
			
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
