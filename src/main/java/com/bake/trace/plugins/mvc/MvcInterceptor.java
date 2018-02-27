package com.bake.trace.plugins.mvc;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.FastDateFormat;
import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.alibaba.fastjson.JSON;
import com.bake.trace.client.TraceClient;
import com.bake.trace.constants.RpcTypeEnum;
import com.bake.trace.thread.ThreadContext;
import com.bake.trace.thread.TraceInfo;
import com.bake.trace.util.HostUtil;
import com.bake.trace.vo.RpcTraceInfoVo;

public class MvcInterceptor extends HandlerInterceptorAdapter {
	
	private static final Logger logger = LoggerFactory.getLogger(MvcInterceptor.class);
	private static final FastDateFormat ISO_DATETIMES_TIME_ZONE_FORMAT_WITH_MILLIS = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		this.initTraceData(request, handler);
		return super.preHandle(request, response, handler);
	}
	
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		super.afterCompletion(request, response, handler, ex);
		finishTraceData(request, ex);
	}
	
	private void initTraceData(HttpServletRequest request, Object handler) {
		request.setAttribute("beginTime", System.currentTimeMillis() + "");
		ThreadContext.init();
		TraceInfo traceInfo = new TraceInfo();
		
		if(handler != null && (handler instanceof HandlerMethod)){
			HandlerMethod handlerMethod = (HandlerMethod)handler;
			
			RpcTraceInfoVo rpcTraceInfoVo = new RpcTraceInfoVo();
			rpcTraceInfoVo.setRequestDateTime(ISO_DATETIMES_TIME_ZONE_FORMAT_WITH_MILLIS.format(new Date()));
			rpcTraceInfoVo.setTraceId(traceInfo.getTraceId());
			rpcTraceInfoVo.setRpcId(traceInfo.getRpcId());
			rpcTraceInfoVo.setRpcType(RpcTypeEnum.HTTP.getRpcName());
			rpcTraceInfoVo.setServiceCategory("mvc");
			rpcTraceInfoVo.setServiceName(handlerMethod.getBean().getClass().getSimpleName());
			rpcTraceInfoVo.setMethodName(handlerMethod.getMethod().getName());
			
			rpcTraceInfoVo.setRequestJson(JSON.toJSONString(request.getParameterMap()));
			
			rpcTraceInfoVo.setServerHost(HostUtil.getIp(request)+":"+request.getLocalPort()+request.getServletPath());
			rpcTraceInfoVo.setClientHost(request.getRemoteAddr());
			
			request.setAttribute("rpcTraceInfoVo", rpcTraceInfoVo);
			logger.info("rpcTraceInfoVo", rpcTraceInfoVo);
		}
		
		//增加一个层级
		traceInfo.addHierarchy();
		
		ThreadContext.putTraceInfo(traceInfo);
		MDC.put("traceId", traceInfo.getTraceId());
		MDC.put("rpcId", traceInfo.getRpcId());
		
	}
	
	
	private void finishTraceData(HttpServletRequest request, Exception ex) {
		try{
			RpcTraceInfoVo rpcTraceInfoVo = (RpcTraceInfoVo)request.getAttribute(("rpcTraceInfoVo"));
			if(rpcTraceInfoVo != null) {
				long beginTime = Long.parseLong((String) request.getAttribute("beginTime"));
				rpcTraceInfoVo.setRunTime(System.currentTimeMillis() - beginTime);
				if(ex == null) {
					rpcTraceInfoVo.setResult("OK");
					logger.info("finishTraceData  OK!");
				}else{
					rpcTraceInfoVo.setResult("ERROR");
					rpcTraceInfoVo.setResponseJson(ex.getMessage());
				}
				TraceClient.sendTraceInfo(rpcTraceInfoVo);
			}
		}catch (Exception e){
			logger.error(e.getMessage(),e);
		}finally{
			request.removeAttribute("rpcTraceInfoVo");
			ThreadContext.removeTraceInfo();
			MDC.remove("traceId");
			MDC.remove("rpcId");
		}
	}
	

}
