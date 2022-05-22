package cn.njust.cy.entity;

import java.util.Collection;

import depends.matrix.core.DependencyValue;

public class DependencyDetail {
	private String str;
	private String name;
	private Collection<DependencyValue> values;
	
	public DependencyDetail(String str,String name, Collection<DependencyValue> values) {
		this.str = str;
		this.name = name;
		this.values = values;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Collection<DependencyValue> getValues() {
		return values;
	}
	public void setValues(Collection<DependencyValue> values) {
		this.values = values;
	}
	public String getStr() {
		return str;
	}
	public void setStr(String str) {
		this.str = str;
	}
	
}
