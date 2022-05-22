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

package depends.matrix.core;

import java.util.ArrayList;
import java.util.List;

public class DependencyValue{
	private String type;
	private String detailFrom;
	private String detailTo;
	
	public DependencyValue(String type, String detailFrom, String detailTo) {
		this.type = type;
		this.detailFrom = detailFrom;
		this.detailTo = detailTo;
	}
	
	public void addDependency(String type,String fromDetail,String toDetail) {
		
	}
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getDetailFrom() {
		return detailFrom;
	}
	public void setDetailFrom(String detailFrom) {
		this.detailFrom = detailFrom;
	}
	public String getDetailTo() {
		return detailTo;
	}
	public void setDetailTo(String detailTo) {
		this.detailTo = detailTo;
	}
	
	
//	private int weight;
//	private String type;
//	private List<Info> detailList;
////	private List<DependencyDetail> dependencyDetail;
//	public DependencyValue(String type) {
//		this.type = type;
//		this.weight=0;
//		this.detailList = new ArrayList<>();
////		this.dependencyDetail = new ArrayList<>();
//	}
//
//	public void addDependency(int weight,Info detail) {
//        this.weight += weight;
//        if (detail!=null)
//            detailList.add(detail);
//	}
//	
//	public int getWeight() {
//		return weight;
//	}
//
//	public String getType() {
//		return type;
//	}
//
//	public List<Info> getDetails() {
//		return detailList;
//	}
//
//	public void addDependency(int weight,List<Info> details) {
//        this.weight += weight;
//        if (details!=null)
//        	detailList.addAll(details);
//	}

}