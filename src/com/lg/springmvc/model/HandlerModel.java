package com.lg.springmvc.model;

import java.lang.reflect.Method;
import java.util.Map;

@Deprecated
public class HandlerModel {

	// url对应的请求的controller对象
	private Object handler;

	// url对应的请求的方法
	private Method method;

	// 方法的参数名和索引值
	private Map<String, Integer> paramMap;

	public HandlerModel() {
	}

	public HandlerModel(Object handler, Method method, Map<String, Integer> paramMap) {
		this.handler = handler;
		this.method = method;
		this.paramMap = paramMap;
	}

	public Object getHandler() {
		return handler;
	}

	public void setHandler(Object handler) {
		this.handler = handler;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public Map<String, Integer> getParamMap() {
		return paramMap;
	}

	public void setParamMap(Map<String, Integer> paramMap) {
		this.paramMap = paramMap;
	}

	@Override
	public String toString() {
		return "HandlerModel [handler=" + handler + ", method=" + method + ", paramMap=" + paramMap + "]";
	}

}
