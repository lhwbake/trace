package com.bake.trace.plugins.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.MDC;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.bake.trace.thread.ThreadContext;
import com.bake.trace.thread.TraceInfo;

public class RestServerInterceptor extends HandlerInterceptorAdapter {
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		ThreadContext.init();
		TraceInfo traceInfo = new TraceInfo();
		String traceId = request.getHeader("traceId");
		String rpcId = request.getHeader("rpcId");
		if(StringUtils.isNotBlank(traceId) && StringUtils.isNotBlank(rpcId)) {
			traceInfo = new TraceInfo(traceId, rpcId);
			traceInfo.addHierarchy();
		}
		//当调用的服务有下一级时，将traceInfo存起来
		ThreadContext.putTraceInfo(traceInfo);
		MDC.put("traceId", traceId);
		MDC.put("rpcId", rpcId);
		return super.preHandle(request, response, handler);
	}


	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		super.afterCompletion(request, response, handler, ex);
		ThreadContext.removeTraceInfo();
		MDC.remove("traceId");
		MDC.remove("rpcId");
	}

}
