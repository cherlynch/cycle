/*
zMIT License

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

package depends.matrix.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class DependencyPair {
	private Integer from;
	private Integer to;
	private HashSet<DependencyValue> dependencies;
//	private String from;
//	private String to;
	public DependencyPair(Integer from, Integer to) {
//	public DependencyPair(String from, String to) {
		this.from = from;
		this.to= to;
		dependencies = new HashSet<>();
	}

	public static String key(Integer from, Integer to) {
		return ""+from+"-->"+to;
	}
	
	public void addDependency(String depType, String detailFrom,String detailTo) {
//		if (dependencies.get(depType)==null)
//			dependencies.put(depType, new DependencyValue(depType));
//		DependencyValue value = dependencies.get(depType);
		DependencyValue value = new DependencyValue(depType,detailFrom,detailTo);
		dependencies.add(value);
	}
	
//	public void addDependency(String depType, int weight,List<Info> details) {
//		if (dependencies.get(depType)==null)
//			dependencies.put(depType, new DependencyValue(depType));
//		DependencyValue value = dependencies.get(depType);		
//		value.addDependency(weight,details);
//	}
	
	public Integer getFrom() {
//	public String getFrom() {
		return from;
	}
	public Integer getTo() {
//	public String getTo() {
		return to;
	}

	public Collection<DependencyValue> getDependencies() {
		return dependencies;
	}
	public void reMap(Integer from, Integer to) {
//	public void reMap(String from, String to) {
		this.from = from;
		this.to = to;
	}


}
