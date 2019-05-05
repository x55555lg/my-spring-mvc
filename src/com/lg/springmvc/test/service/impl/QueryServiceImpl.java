package com.lg.springmvc.test.service.impl;

import com.lg.springmvc.annotation.Service;
import com.lg.springmvc.test.service.QueryService;

@Service("myQueryService")
public class QueryServiceImpl implements QueryService {

	@Override
	public String search(String name) {
		return "invoke search name = " + name;
	}

}