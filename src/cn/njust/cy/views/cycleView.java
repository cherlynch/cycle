package cn.njust.cy.views;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.*;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.*;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;

import cn.njust.cy.detect.Cycle;
import cn.njust.cy.detect.judgeTypes;
import cn.njust.cy.entity.Dependency;
import cn.njust.cy.entity.DependencyDetail;
import cn.njust.cy.actions.InversionRefactoring;
import cn.njust.cy.actions.MoveRefactoring;
import cn.njust.cy.actions.RemoveImport;
import depends.matrix.core.DependencyValue;

public class cycleView extends ViewPart {
	private static final String MESSAGE_DIALOG_TITLE = "MyViewer";
	
	private TreeViewer treeViewer;
	
	private Action identifyBadSmellsAction;
	private Action applyRefactoringAction;
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

	class NameSorter extends ViewerSorter {
		public int compare(Viewer viewer, Object obj1, Object obj2) {
			return ((Dependency)obj1).getId()-((Dependency)obj2).getId();
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
		layout.addColumnData(new ColumnWeightData(50, true));
		layout.addColumnData(new ColumnWeightData(50, true));
		layout.addColumnData(new ColumnWeightData(10, true));
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
		getSite().getWorkbenchWindow().getWorkbench().getOperationSupport().getOperationHistory().addOperationHistoryListener(new IOperationHistoryListener() {
			public void historyNotification(OperationHistoryEvent event) {
				int eventType = event.getEventType();
				if(eventType == OperationHistoryEvent.UNDONE  || eventType == OperationHistoryEvent.REDONE ||
						eventType == OperationHistoryEvent.OPERATION_ADDED || eventType == OperationHistoryEvent.OPERATION_REMOVED) {
					if(activeProject != null) {
//						applyRefactoringAction.setEnabled(false);
					}
				}
			}
		});
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
	}
	
	private void makeActions() {
		identifyBadSmellsAction = new Action() { //识别循环依赖
			public void run() {
				activeProject = selectedProject;
				String projectPath= activeProject.getProject().getLocation().toString();//获取项目路径
				IWorkbench wb = PlatformUI.getWorkbench();
				IProgressService ps = wb.getProgressService();
				try {
					ps.busyCursorWhile(new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							monitor.beginTask("Identification of circular dependency refactoring opportunities",0);
							if(monitor.isCanceled())
								throw new OperationCanceledException();
							cycle = new Cycle(projectPath);
							Collection <Dependency> dep = cycle.getCycle();
							dependencies = dep.toArray(new Dependency[dep.size()]);//得到循环依赖	
							monitor.worked(1);
							HashSet<String> callers = cycle.getCallers("org.jfree.experimental.chart.swt.editor.SWTAxisEditor.getInstance");
							for(String c:callers) {
								System.out.println("callers: "+c);
							}
							
						}
					});
					
				} catch (InvocationTargetException | InterruptedException e) {
					// TODO Auto-generated catch block
					MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), MESSAGE_DIALOG_TITLE,
							"Errors were detected in the project. Fix the errors before refactoring.");
					e.printStackTrace();
				}
				treeViewer.setContentProvider(new ViewContentProvider());
			}
		};
		identifyBadSmellsAction.setToolTipText("Identify Bad Smells");
		identifyBadSmellsAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
//		identifyBadSmellsAction.setEnabled(false);
		
		applyRefactoringAction = new Action() {//重构
			public void run() {
				IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
				if(selection.getFirstElement() instanceof Dependency) {
					Dependency selectDependency = (Dependency)selection.getFirstElement();//获取选中的依赖
					String relativePathA = getRelativePath(selectDependency.getDetail()[0].getName());
					String relativePathB = getRelativePath(selectDependency.getDetail()[1].getName());
					IPath newpathA = new Path(relativePathA);
					IPath newpathB = new Path(relativePathB); 
					IFile fileA = ResourcesPlugin.getWorkspace().getRoot().getFile(newpathA);
					IFile fileB = ResourcesPlugin.getWorkspace().getRoot().getFile(newpathB);
					DependencyDetail detailA = selectDependency.getDetail()[0];
					DependencyDetail detailB = selectDependency.getDetail()[1];
					String newFileName = "";
					
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
					CompilationUnit sourceUnitA = parse((ICompilationUnit)JavaCore.create(fileA));	
					TypeDeclaration typeDeclA = (TypeDeclaration) sourceUnitA.types().get(0);
					CompilationUnit sourceUnitB = parse((ICompilationUnit)JavaCore.create(fileB));	
					TypeDeclaration typeDeclB = (TypeDeclaration) sourceUnitB.types().get(0);					
					
					int mytype = new judgeTypes(selectDependency).getTypes();
					Refactoring refactoring = null;
					System.out.println("mytype "+mytype);
					if(mytype==5)  refactoring = new RemoveImport(fileA,fileB);
					else if(mytype==6)  refactoring = new RemoveImport(fileB,fileA);
					else {
						if(mytype==1||typeDeclB.isInterface()||mytype==-1) {
							HashSet<String>  strs = new HashSet<String>();							
							if(typeDeclB.isInterface()) {
								strs = openChangeFiles(typeDeclB);
							}
							refactoring = new InversionRefactoring(fileA,fileB,newFileName,detailB,strs);
						}
						if(mytype==2||typeDeclA.isInterface()) {
							Collection<String>  strs = new HashSet<String>();							
							if(typeDeclA.isInterface()) {
								strs = openChangeFiles(typeDeclA);
							}
							refactoring = new InversionRefactoring(fileB,fileA,newFileName,detailA,strs);
						}
						if(mytype==4) { //B extends/implements A
							HashSet<String> str = new HashSet<String>();
							for(DependencyValue val:detailA.getValues()) {
								str.addAll(cycle.getCallers(val.getDetailFrom()));
								System.out.println("from " + val.getDetailFrom());
							}
							for(String s:str) {
								System.out.println("callers: "+s);
								String pro = s.substring(0,s.lastIndexOf("."));
								try {
									IType type = activeProject.findType(pro);
									IJavaElement javaElement = type.getCompilationUnit();
									JavaUI.openInEditor(javaElement);
								} catch (JavaModelException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (PartInitException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							refactoring = new MoveRefactoring(fileA,fileB,newFileName,detailA,str);
						}
						if(mytype==3) {
							HashSet<String> str = new HashSet<String>();
							for(DependencyValue val:detailA.getValues()) {
								str.addAll(cycle.getCallers(val.getDetailFrom()));
							}
							for(String s:str) {
								System.out.println("callers: "+s);
								String pro = s.substring(0,s.lastIndexOf("."));
								try {
									IType type = activeProject.findType(pro);
									IJavaElement javaElement = type.getCompilationUnit();
									JavaUI.openInEditor(javaElement);
								} catch (JavaModelException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (PartInitException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}	
							}
							refactoring = new MoveRefactoring(fileB,fileA,newFileName,detailA,str);
						}
						
						MyRefactoringWizard wizard = new MyRefactoringWizard(refactoring, applyRefactoringAction);
						RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard); 
						try { 
							String titleForFailedChecks = ""; //$NON-NLS-1$ 
							op.run(getSite().getShell(), titleForFailedChecks); 
						} catch(InterruptedException e) {
							e.printStackTrace();
						}
					}
					
				}
			}
		};
		applyRefactoringAction.setToolTipText("Apply Refactoring");
		applyRefactoringAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_DEF_VIEW));
//		applyRefactoringAction.setEnabled(false);
		
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
						Collection<DependencyValue> values = detail.getValues();
						Collection<String> strList = new HashSet<String>();
						for(DependencyValue val:values) {
							String []detailToSplit = val.getDetailTo().split("\\.");
							String str = detailToSplit[detailToSplit.length-1];
							strList.add(str);
						}
						ICompilationUnit iu = (ICompilationUnit)JavaCore.create(sourceFile);
						CompilationUnit  u = parse(iu);	
						TypeDeclaration typeDecl = (TypeDeclaration) u.types().get(0);
						ArrayList<Position> positions = new ArrayList<Position>();
						for(String s:strList) {
							System.out.println(s);
							typeDecl.accept(new ASTVisitor() {
								public boolean visit(SimpleType node) {
									if(node.toString().equals(s)) {
										int startPosition = node.getStartPosition();
										int nodelength = node.getLength();
										Position position = new Position(startPosition-1,nodelength+2);
										positions.add(position);
//										sourceEditor.setHighlightRange(startPosition-1,length+2, true);
									}
									return true;
								}
							});
						}
						Position firstPosition = null;Position lastPosition = null;
						int minOffset = Integer.MAX_VALUE;int maxOffset = -1;
						for(Position position:positions) {
							Annotation annotation = new Annotation("myAnnotation",false,"");
							annotationModel.addAnnotation(annotation, position);
							if(position.getOffset() < minOffset) {
								minOffset = position.getOffset();
								firstPosition = position;
							}
							if(position.getOffset() > maxOffset) {
								maxOffset = position.getOffset();
								lastPosition = position;
							}
						}
						int offset = firstPosition.getOffset();
						int length = lastPosition.getOffset() + lastPosition.getLength() - firstPosition.getOffset();
							sourceEditor.setHighlightRange(offset, length, true);
						
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
	
	public Dependency getParentDependency(DependencyDetail detail) {
		for(Dependency dependency:dependencies) {
			if(dependency.getId()==detail.getId()) {
				return dependency;
			}
		}
		return null;
	}
	public HashSet<String> openChangeFiles(TypeDeclaration typeDeclB) {
		String interfaceName = typeDeclB.resolveBinding().getBinaryName();
		HashSet<String>  strs = cycle.getImplementsRelation(interfaceName);
		for(String str:strs) {
			try {
				IType type = activeProject.findType(str);
				IJavaElement javaElement = type.getCompilationUnit();
				JavaUI.openInEditor(javaElement);
			} catch (JavaModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (PartInitException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return strs;
	}
}



