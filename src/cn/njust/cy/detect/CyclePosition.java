package cn.njust.cy.detect;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;

public class CyclePosition {
	public void getPositions(IFile file,String name) {
		ICompilationUnit iu = (ICompilationUnit)JavaCore.create(file);
	}
}
