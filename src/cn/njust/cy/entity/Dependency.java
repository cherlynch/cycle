package cn.njust.cy.entity;

import java.util.Collection;
import java.util.HashMap;

public class Dependency {
	private DependencyDetail[] detail;

	public Dependency(DependencyDetail[] detail) {
		this.detail = detail;
	}

	public DependencyDetail[] getDetail() {
		return detail;
	}

	public void setDetail(DependencyDetail[] detail) {
		this.detail = detail;
	}
	
//	private String from;
//	private String to;
//	Collection<DependencyValue> dependenciesFrom;
//	Collection<DependencyValue> dependenciesTo;
//	
//	public Dependency(String from, String to,Collection<DependencyValue> dependenciesFrom,Collection<DependencyValue> dependenciesTo) {
//		this.from = from;
//		this.to= to;
//		this.dependenciesFrom = dependenciesFrom;
//		this.dependenciesTo = dependenciesTo;
//	}
//	
//	public String getFrom() {
//		return from;
//	}
//
//	public void setFrom(String from) {
//		this.from = from;
//	}
//
//	public String getTo() {
//		return to;
//	}
//
//	public void setTo(String to) {
//		this.to = to;
//	}
//
//	public Collection<DependencyValue> getDependenciesFrom() {
//		return dependenciesFrom;
//	}
//
//	public void setDependenciesFrom(Collection<DependencyValue> dependenciesFrom) {
//		this.dependenciesFrom = dependenciesFrom;
//	}
//
//	public Collection<DependencyValue> getDependenciesTo() {
//		return dependenciesTo;
//	}
//
//	public void setDependenciesTo(Collection<DependencyValue> dependenciesTo) {
//		this.dependenciesTo = dependenciesTo;
//	}

	
}
