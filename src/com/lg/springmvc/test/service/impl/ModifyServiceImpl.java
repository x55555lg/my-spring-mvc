package com.lg.springmvc.test.service.impl;

import com.lg.springmvc.annotation.Service;

import com.lg.springmvc.test.service.ModifyService;

@Service
public class ModifyServiceImpl implements ModifyService {

	@Override
	public String add(String name, String addr) {
		return "invoke add name = " + name + " addr = " + addr;
	}

	@Override
	public String remove(Integer id) {
		return "remove id = " + id;
	}

}
