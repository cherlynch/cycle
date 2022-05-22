/*
MIT License

Copyright (c) 2018-2019 Gang ZHANG

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package depends.extractor;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.codehaus.plexus.util.FileUtils;

import depends.entity.Entity;
import depends.entity.FileEntity;
import depends.entity.repo.BuiltInType;
import depends.entity.repo.EntityRepo;
import depends.entity.repo.InMemoryEntityRepo;
import depends.extractor.UnsolvedBindings;
import depends.generator.DependencyGenerator;
import depends.matrix.core.DependencyMatrix;
import depends.matrix.transform.OrderedMatrixGenerator;
import depends.relations.ImportLookupStrategy;
import depends.relations.Inferer;
import depends.util.FileTraversal;
import depends.util.FileUtil;

import static depends.deptypes.DependencyType.*;
public class JavaProcessor {
    private static final String JAVA_LANG = "java";
    private static final String JAVA_SUFFIX = ".java"; //后缀
    
	public Inferer inferer;
	protected EntityRepo entityRepo;
	DependencyMatrix dependencyMatrix;
	protected String inputSrcPath;
	public String[] includeDirs;
	
	private DependencyGenerator dependencyGenerator;
	private Set<UnsolvedBindings> potentialExternalDependencies;
	private List<String> typeFilter;
	private List<String> includePaths;
	
    public JavaProcessor() {
    	entityRepo = new InMemoryEntityRepo();
		inferer = new Inferer(entityRepo, getImportLookupStrategy(), getBuiltInType(), false);
    }
    
	public String supportedLanguage() {
		return JAVA_LANG;
	}
	
	public String[] fileSuffixes() {
		return new String[] {JAVA_SUFFIX};
	}
	
	protected JavaFileParser createFileParser(String fileFullPath) {
		return new JavaFileParser(fileFullPath,entityRepo, inferer);//语法分析
	}
	
	public ImportLookupStrategy getImportLookupStrategy() {
		return new JavaImportLookupStrategy();
	}
	
	public BuiltInType getBuiltInType() {
		return new JavaBuiltInType();
	}

	public void buildDependencies(String inputDir, String[] includeDir, List<String> typeFilter, boolean callAsImpl, boolean isCollectUnsolvedBindings, boolean isDuckTypingDeduce) {
		this.inputSrcPath = inputDir;
		this.includeDirs = includeDir;
		this.typeFilter = typeFilter;
		this.inferer.setCollectUnsolvedBindings(isCollectUnsolvedBindings);
		this.inferer.setDuckTypingDeduce(isDuckTypingDeduce);
		parseAllFiles(); //遍历所有的文件
		markAllEntitiesScope();
		resolveBindings(callAsImpl);
		identifyDependencies();
	}

	private void markAllEntitiesScope() {
		entityRepo.getFileEntities().stream().forEach(entity -> {
			Entity file = entity.getAncestorOfType(FileEntity.class);
			try {
				if (!file.getQualifiedName().startsWith(this.inputSrcPath)) {
					entity.setInScope(false);
				}
			} catch (Exception e) {

			}
		});
	}

	/**
	 * 
	 * @param callAsImpl
	 * @return unsolved bindings
	 */
	public void resolveBindings(boolean callAsImpl) {
		System.out.println("Resolve types and bindings of variables, methods and expressions....");
		this.potentialExternalDependencies = inferer.resolveAllBindings(callAsImpl,this);
		if (getExternalDependencies().size() > 0) {
			System.out.println("There are " + getExternalDependencies().size() + " items are potential external dependencies.");
		}
		System.out.println("types and bindings resolved successfully...");
	}

	private void identifyDependencies() {
		System.out.println("dependencie data generating...");
		dependencyMatrix = dependencyGenerator.build(entityRepo, typeFilter);
		entityRepo = null;
		System.out.println("reorder dependency matrix...");
		dependencyMatrix = new OrderedMatrixGenerator(dependencyMatrix).build();
		System.out.println("Dependencie data generating done successfully...");
	}

	private final void parseAllFiles() {
		System.out.println("Start parsing files...");
		Set<String> phase2Files = new HashSet<>(); 
		FileTraversal fileTransversal = new FileTraversal(new FileTraversal.IFileVisitor() {
			@Override
			public void visit(File file) {
				String fileFullPath = file.getAbsolutePath();
				fileFullPath = FileUtil.uniqFilePath(fileFullPath);
				if (!fileFullPath.startsWith(inputSrcPath)) {
					return;
				}
				if (isPhase2Files(fileFullPath)) {

				} else {
					parseFile(fileFullPath);
				}
			}

		});
		fileTransversal.extensionFilter(this.fileSuffixes());
		fileTransversal.travers(this.inputSrcPath);
		for (String f : phase2Files) {
			parseFile(f);
		}
		System.out.println("all files procceed successfully...");

	}

	protected void parseFile(String fileFullPath) {
		JavaFileParser fileParser = createFileParser(fileFullPath);
		try {
			System.out.println("parsing " + fileFullPath + "...");
			fileParser.parse();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("error occoured during parse file " + fileFullPath);
			e.printStackTrace();
		}
	}

	protected boolean isPhase2Files(String fileFullPath) {
		return false;
	}

	public List<String> includePaths() {
		if (this.includePaths ==null) {
			this.includePaths = buildIncludePath();
		}
		return includePaths;
	}

	private List<String> buildIncludePath() {
		includePaths = new ArrayList<String>();
		for (String path : includeDirs) {
			if (FileUtils.fileExists(path)) {
				path = FileUtil.uniqFilePath(path);
				if (!includePaths.contains(path))
					includePaths.add(path);
			}
			path = this.inputSrcPath + File.separator + path;
			if (FileUtils.fileExists(path)) {
				path = FileUtil.uniqFilePath(path);
				if (!includePaths.contains(path))
					includePaths.add(path);
			}
		}
		return includePaths;
	}

	public DependencyMatrix getDependencies() {
		return dependencyMatrix;
	}

	public EntityRepo getEntityRepo() {
		return this.entityRepo;
	}

	public void setDependencyGenerator(DependencyGenerator dependencyGenerator) {
		this.dependencyGenerator = dependencyGenerator;
	}

	public Set<UnsolvedBindings> getExternalDependencies() {
		return potentialExternalDependencies;
	}

	public String getRelationMapping(String relation) {
		return relation;
	}
}
