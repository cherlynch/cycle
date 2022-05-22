package cn.njust.cy.views;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.*;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.text.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.*;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;

import cn.njust.cy.detect.Cycle;
import cn.njust.cy.detect.judgeTypes;
import cn.njust.cy.entity.Dependency;
import cn.njust.cy.entity.DependencyDetail;
import cn.njust.cy.actions.AbstractRefactoring;
import cn.njust.cy.actions.MoveRefactoring;
import depends.matrix.core.DependencyValue;

public class cycleView extends ViewPart {
	private static final String MESSAGE_DIALOG_TITLE = "MyViewer";
	
	private TreeViewer treeViewer;
	private Action identifyBadSmellsAction;
	private Action applyRefactoringAction;
	private Action applyRefactoringAction2;
	private Action doubleClickAction;
    Dependency[] dependencies;
    private IJavaProject selectedProject;
	private IJavaProject activeProject;
	private Set<IJavaElement> javaElementsToOpenInEditor;
	private Cycle cycle;

	class ViewContentProvider implements ITreeContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			if(dependencies!=null) {
				return dependencies;
			}
			else {
				return new Dependency[] {};
			}
		}
		public Object[] getChildren(Object parentElement) {
          if(parentElement instanceof Dependency) {
        	return ((Dependency)parentElement).getDetail();
          }else if(parentElement instanceof DependencyDetail){
        	return ((DependencyDetail)parentElement).getValues().toArray();
          }else {
        	return new DependencyValue[] {};
          }
		}
		public Object getParent(Object element) {
        	return null;
		}
		public boolean hasChildren(Object element) {
			 return getChildren(element).length > 0;
		}
	}
	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object element, int index) {
        	if(element instanceof Dependency) {
        		Dependency entry = (Dependency)element;
        		switch (index) {
        		case 1:
        			return entry.getDetail()[0].getName();
        		case 2:
        			return entry.getDetail()[1].getName();
        		case 3:
        			return entry.getDetail()[0].getValues().size()+"/"+entry.getDetail()[1].getValues().size();
        		default:
        			return "";
        		}
        	} 
        	else if(element instanceof DependencyDetail) {
        		DependencyDetail entry = (DependencyDetail)element;
        		switch (index) {
        		case 0:
        			return entry.getStr();
//        		case 1:{
//        			return entry.getName();
//        		}
        		default:
        			return "";
        		}
        	}
        	else if(element instanceof DependencyValue) {
        		DependencyValue entry = (DependencyValue)element;
        		switch (index) {
        		case 0:
        			return "["+entry.getType()+"]";
        		case 1:
        			return entry.getDetailFrom();
        		case 2:
        			return entry.getDetailTo();
        		default:
        			return "";
        		}
        	} 
        	else return "";
		}
		public Image getColumnImage(Object obj, int index) {
			return null;
		}
	}

	/**
	 * The constructor.
	 */
	public cycleView() {
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		treeViewer = new TreeViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		treeViewer.setContentProvider(new ViewContentProvider());
		treeViewer.setLabelProvider(new ViewLabelProvider());
//		treeViewer.setSorter(new NameSorter());
		treeViewer.setInput(getViewSite());
		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(20, true));
		layout.addColumnData(new ColumnWeightData(40, true));
		layout.addColumnData(new ColumnWeightData(40, true));
		layout.addColumnData(new ColumnWeightData(20, true));
		treeViewer.getTree().setLayout(layout);
		treeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		new TreeColumn(treeViewer.getTree(), SWT.LEFT).setText("Type");
		new TreeColumn(treeViewer.getTree(), SWT.LEFT).setText("ClassA");
		new TreeColumn(treeViewer.getTree(), SWT.LEFT).setText("ClassB");
		new TreeColumn(treeViewer.getTree(), SWT.LEFT).setText("nums");
		treeViewer.expandAll();

		for (int i = 0, n = treeViewer.getTree().getColumnCount(); i < n; i++) {
			treeViewer.getTree().getColumn(i).pack();
		}

		treeViewer.getTree().setLinesVisible(true);
		treeViewer.getTree().setHeaderVisible(true);
		makeActions();
		hookDoubleClickAction();
		contributeToActionBars();
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(selectionListener);
//		JavaCore.addElementChangedListener(ElementChangedListener.getInstance());
	}
	
	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
		
	}
	
	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(identifyBadSmellsAction);
		manager.add(applyRefactoringAction);
		manager.add(applyRefactoringAction2);
	}
	
	private void makeActions() {
		identifyBadSmellsAction = new Action() {
			public void run() {
				activeProject = selectedProject;
				String projectPath= activeProject.getProject().getLocation().toString();//获取项目路径
				cycle = new Cycle(projectPath);
				ArrayList <Dependency> dep = cycle.getCycle();
				dependencies = dep.toArray(new Dependency[dep.size()]);//得到循环依赖
				treeViewer.setContentProvider(new ViewContentProvider());
			}
		};
		identifyBadSmellsAction.setToolTipText("Identify Bad Smells");
		identifyBadSmellsAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
//		identifyBadSmellsAction.setEnabled(false);
		
		applyRefactoringAction = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
				if(selection.getFirstElement() instanceof Dependency) {
					Dependency dep = (Dependency)selection.getFirstElement();
					String relativePathA = getRelativePath(dep.getDetail()[0].getName());
					String relativePathB = getRelativePath(dep.getDetail()[1].getName());
					IPath newpathA = new Path(relativePathA);
					IPath newpathB = new Path(relativePathB); 
					IFile fileA = ResourcesPlugin.getWorkspace().getRoot().getFile(newpathA);
					IFile fileB = ResourcesPlugin.getWorkspace().getRoot().getFile(newpathB);
					DependencyDetail detailA = dep.getDetail()[0];
					DependencyDetail detailB = dep.getDetail()[1];
					String interfaceName = "Abstract" + fileA.getName().replaceAll(".java","");
					
					IJavaElement sourceJavaElementA = JavaCore.create(fileA);
					IJavaElement sourceJavaElementB = JavaCore.create(fileB);
					try {
						JavaUI.openInEditor(sourceJavaElementA);
						JavaUI.openInEditor(sourceJavaElementB);
					} catch (PartInitException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (JavaModelException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}						
					int mytype = new judgeTypes(dep).getTypes();
					System.out.println(mytype);
					if(mytype==1||mytype==2||mytype==-1) {
						AbstractRefactoring refactoring;
						if(mytype==1||mytype==-1) refactoring = new AbstractRefactoring(fileA,fileB,interfaceName,detailB,activeProject);
						else  refactoring = new AbstractRefactoring(fileB,fileA,interfaceName,detailA,activeProject);
						ICompilationUnit sourceIUnit = (ICompilationUnit)JavaCore.create(fileB);
						CompilationUnit sourceUnit = parse(sourceIUnit);	
						IPackageFragment sourcePackage = (IPackageFragment)JavaCore.create((IFolder) fileB.getParent());
						TypeDeclaration typeDecl = (TypeDeclaration) sourceUnit.types().get(0);
						if(typeDecl.isInterface()) {
							String interfaceName1 = typeDecl.resolveBinding().getBinaryName();
							System.out.println(interfaceName1);
							Collection<String> strs = cycle.getImplementsRelation(interfaceName1);
							for(String str:strs) {
								try {
									IType type = activeProject.findType(str);
									ICompilationUnit IUnit = type.getCompilationUnit();
									System.out.println("unit: "+IUnit.getElementName());
									IJavaElement javaElement = IUnit;
									JavaUI.openInEditor(javaElement);
									refactoring.modifyImplementsChange(IUnit);
								} catch (JavaModelException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (PartInitException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
					}
//					if(mytype==3||mytype==4) new MoveRefactoring(fileA,fileB,"");
//					
					
				}
			}
		};
		applyRefactoringAction.setToolTipText("Apply Refactoring");
		applyRefactoringAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_DEF_VIEW));
//		applyRefactoringAction.setEnabled(false);
		
		applyRefactoringAction2 = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
			}
		};
		applyRefactoringAction2.setToolTipText("Apply Refactoring");
		applyRefactoringAction2.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_DEF_VIEW));
		
		doubleClickAction = new Action() {
			public void run() {				
				IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
				if(selection.getFirstElement() instanceof DependencyDetail) {
					DependencyDetail detail = (DependencyDetail)selection.getFirstElement();
					String relativePath = getRelativePath(detail.getName());
					IPath newpath = new Path(relativePath);
					IFile sourceFile = ResourcesPlugin.getWorkspace().getRoot().getFile(newpath);
					System.out.println(sourceFile);
					IJavaElement sourceJavaElement = JavaCore.create(sourceFile);
					try {
						ITextEditor sourceEditor = (ITextEditor)JavaUI.openInEditor(sourceJavaElement);
						AnnotationModel annotationModel = (AnnotationModel)sourceEditor.getDocumentProvider().getAnnotationModel(sourceEditor.getEditorInput());
						Iterator<Annotation> annotationIterator = annotationModel.getAnnotationIterator();
						while(annotationIterator.hasNext()) {
							Annotation currentAnnotation = annotationIterator.next();
							if(currentAnnotation.getType().equals("myAnnotation")) {
								annotationModel.removeAnnotation(currentAnnotation);
							}
						}
						ICompilationUnit iu = (ICompilationUnit)JavaCore.create(sourceFile);
						CompilationUnit  u = parse(iu);	
						TypeDeclaration typeDecl = (TypeDeclaration) u.types().get(0);
						typeDecl.accept(new ASTVisitor() {
							public boolean visit(SimpleType node) {
								if(node.toString().equals("TextFigure")||node.toString().equals("textOwner.getFont()")) {
									Position position = new Position(node.getStartPosition(),node.getLength()+1);
									Annotation annotation = new Annotation("myAnnotation",false,"");
									annotationModel.addAnnotation(annotation, position);
									sourceEditor.setHighlightRange(node.getStartPosition(),node.getLength()+1, true);
								}
								return true;
							}
						});
						
					} catch (PartInitException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (JavaModelException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
	}
    
	private void hookDoubleClickAction() {
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	private ISelectionListener selectionListener = new ISelectionListener() {
		public void selectionChanged(IWorkbenchPart sourcepart, ISelection selection) {
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection structuredSelection = (IStructuredSelection)selection;
				Object element = structuredSelection.getFirstElement();
				IJavaProject javaProject = null;
				if(element instanceof IJavaProject) {
					javaProject = (IJavaProject)element;
				}
				else if(element instanceof IPackageFragmentRoot) {
					IPackageFragmentRoot packageFragmentRoot = (IPackageFragmentRoot)element;
					javaProject = packageFragmentRoot.getJavaProject();
				}
				else if(element instanceof IPackageFragment) {
					IPackageFragment packageFragment = (IPackageFragment)element;
					javaProject = packageFragment.getJavaProject();
				}
				else if(element instanceof ICompilationUnit) {
					ICompilationUnit compilationUnit = (ICompilationUnit)element;
					javaProject = compilationUnit.getJavaProject();
				}
				else if(element instanceof IType) {
					IType type = (IType)element;
					javaProject = type.getJavaProject();
				}
				if(javaProject != null && !javaProject.equals(selectedProject)) {
					selectedProject = javaProject;
					/*if(candidateRefactoringTable != null)
						tableViewer.remove(candidateRefactoringTable);*/
					identifyBadSmellsAction.setEnabled(true);
				}
			}
		}
	};
	
	public String getRelativePath(String absolutePath) {
		String projectPath = activeProject.getProject().getLocation().toString();
		String filePath = absolutePath.replaceAll("\\\\", "/");
		String relativePath = "/"+activeProject.getElementName()+filePath.replaceAll(projectPath, "");
		return relativePath;
	}
	private static CompilationUnit parse(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null); // parse
	}
}



