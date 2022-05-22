package depends;

import java.util.ArrayList;
import java.util.Collection;

import cn.njust.cy.entity.Dependency;
import cn.njust.cy.entity.DependencyDetail;
import depends.extractor.JavaProcessor;
import depends.generator.DependencyGenerator;
import depends.generator.FileDependencyGenerator;
import depends.generator.FunctionDependencyGenerator;
import depends.matrix.core.DependencyMatrix;
import depends.matrix.core.DependencyPair;
import depends.matrix.core.DependencyValue;
import depends.util.FileUtil;

public class Depends {
	public DependencyMatrix getMatrix(String inputDir) {
		//String inputDir = "D:/JHotDraw";
		inputDir = FileUtil.uniqFilePath(inputDir);
		JavaProcessor javaProcessor = new JavaProcessor(); 
		DependencyGenerator dependencyGenerator = new FileDependencyGenerator();
//		DependencyGenerator dependencyGenerator = new FunctionDependencyGenerator();
		javaProcessor.setDependencyGenerator(dependencyGenerator);
		javaProcessor.buildDependencies(inputDir, null,null,false,false,true);	
		DependencyMatrix matrix = javaProcessor.getDependencies();
		return matrix;
	}
	
	
	
	
	
//	public ArrayList<Dependency> getCycleDep(String inputDir){
//		DependencyMatrix matrix = getMatrix(inputDir);
//		ArrayList<Dependency> cycleDep = new ArrayList<Dependency>();
//		Collection<DependencyPair> dependencyPairs = matrix.getDependencyPairs();
//		ArrayList<DependencyPair> dependencyList = new ArrayList<DependencyPair>(dependencyPairs);
//		int len = dependencyPairs.size();
////		for(DependencyPair pair:dependencyList) {
////			DependencyDetail detail = new DependencyDetail(matrix.getNodeName(pair.getFrom()),
////					matrix.getNodeName(pair.getTo()),null,null);
////			cycleDep.add(detail);
////		}
//		for(int i=0;i<len;i++) {
//			DependencyPair depA = dependencyList.get(i);
//			for(int j=i+1;j<len;j++) {
//				DependencyPair depB = dependencyList.get(j);
//				if(depA.getFrom()==depB.getTo()&&depA.getTo()==depB.getFrom()) {
////					String sourceDir = inputDir + "/src/";
//					String classA = matrix.getNodeName(depA.getFrom()).replaceAll("\\\\","/");//.replaceAll(inputDir, "");
//					String classB = matrix.getNodeName(depB.getFrom()).replaceAll("\\\\","/");//.replaceAll(inputDir, "");
//					DependencyDetail[] detail = new  DependencyDetail[2];
//					detail[0] = new DependencyDetail("AtoB",classA,depA.getDependencies());
//					detail[1] = new DependencyDetail("BtoA",classB,depB.getDependencies());
//					Dependency dependency = new Dependency(new DependencyDetail[2]);
//					cycleDep.add(dependency);
//				}
//			}
//		}
//		return cycleDep;
//	}
	
//	public static void main(String[] args) {
////		System.out.println(Main.class.getResource(""));
////		String str1="D:/ab",str2="D:/";
////		System.out.println(str1.replaceAll(str2,""));
//		ArrayList<Dependency> res = new Depends().getCycleDep("D:/JHotDraw");
//		for(Dependency detail:res) {
//			for(DependencyValue val:detail.getDetail()[0].getValues()) {
//				System.out.println("depA---"+val.getType()+" "+val.getDetailFrom()+" "+val.getDetailTo());
//			}
//			for(DependencyValue val:detail.getDetail()[1].getValues()) {
//				System.out.println("depB---"+val.getType()+" "+val.getDetailFrom()+" "+val.getDetailTo());
//			}
//		}
//	}
}
