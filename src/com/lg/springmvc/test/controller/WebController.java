package com.lg.springmvc.test.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lg.springmvc.annotation.Autowired;
import com.lg.springmvc.annotation.Controller;
import com.lg.springmvc.annotation.RequestMapping;
import com.lg.springmvc.annotation.RequestParam;
import com.lg.springmvc.test.service.ModifyService;
import com.lg.springmvc.test.service.QueryService;

/**
 * 
 * @author xulg 2017年8月21日
 */
@Controller
@RequestMapping("/web")
public class WebController {

	@Autowired("myQueryService")
	private QueryService queryService;

	@Autowired
	private ModifyService modifyService;

	@RequestMapping("/search")
	public void search(@RequestParam("name") String name, HttpServletRequest request, HttpServletResponse response) {
		String result = queryService.search(name);
		out(response, result);
	}

	@RequestMapping("/add")
	public void add(@RequestParam("name") String name, @RequestParam("addr") String addr, HttpServletRequest request,
			HttpServletResponse response) {
		String result = modifyService.add(name, addr);
		out(response, result);
	}

	@RequestMapping("/remove")
	public void remove(@RequestParam("name") Integer id, HttpServletRequest request, HttpServletResponse response) {
		String result = modifyService.remove(id);
		out(response, result);
	}

	private void out(HttpServletResponse response, String str) {
		try {
			response.setContentType("application/json;charset=utf-8");
			response.getWriter().print(str);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}