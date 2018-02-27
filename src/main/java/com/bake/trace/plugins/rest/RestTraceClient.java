package com.bake.trace.plugins.rest;

import java.util.Date;

import org.apache.commons.lang3.time.FastDateFormat;
import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSON;
import com.bake.trace.client.TraceClient;
import com.bake.trace.constants.RpcTypeEnum;
import com.bake.trace.thread.ThreadContext;
import com.bake.trace.thread.TraceInfo;
import com.bake.trace.vo.RpcTraceInfoVo;

import ch.qos.logback.core.helpers.ThrowableToStringArray;

public class RestTraceClient {

	private static final Logger logger = LoggerFactory.getLogger(RestTraceClient.class);
	private static RestTemplate restTemplate = new RestTemplate();
	
	private static final FastDateFormat ISO_DATETIME_TIME_LONE_FORMAT_WITH_MILLIS = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
	
	public static <T> T request(String url, HttpMethod httpMethod, Object requestBody, Class<T> responseType, String serviceName, String methodName) throws RestClientException {
		T responseEntityBody = null;
		TraceInfo traceInfo = ThreadContext.getTraceInfo();
		if(traceInfo == null) {
			traceInfo = new TraceInfo();
		}else{
			traceInfo.addSequenceNo();
		}
		MDC.put("traceId", traceInfo.getTraceId());
		MDC.put("rpcId", traceInfo.getRpcId());
		
		long startTime = System.currentTimeMillis();
		
		RpcTraceInfoVo rpcTraceInfoVo = new RpcTraceInfoVo();
		rpcTraceInfoVo.setRequestDateTime(ISO_DATETIME_TIME_LONE_FORMAT_WITH_MILLIS.format(new Date(startTime)));
		rpcTraceInfoVo.setTraceId(traceInfo.getTraceId());
		rpcTraceInfoVo.setRpcId(traceInfo.getRpcId());
		rpcTraceInfoVo.setRpcType(RpcTypeEnum.HTTP.getRpcName());
		rpcTraceInfoVo.setServiceCategory("rest");
		rpcTraceInfoVo.setServiceName(serviceName);
		rpcTraceInfoVo.setMethodName(methodName);
		rpcTraceInfoVo.setRequestJson(JSON.toJSONString(requestBody));
		rpcTraceInfoVo.setServerHost(url);
		//rpcTraceInfoVo.setClientHost(HostUtil.getIp(request));
		
		try{
			HttpHeaders headers = new HttpHeaders();
			headers.add("traceId", traceInfo.getTraceId());
			headers.add("rpcId", traceInfo.getRpcId());
			HttpEntity<Object> requestEntity = new HttpEntity<>(requestBody, headers);
			ResponseEntity<T> responseEntity = restTemplate.exchange(url, httpMethod, requestEntity, responseType);
			responseEntityBody = responseEntity.getBody();
			long runTime = System.currentTimeMillis()-startTime;
			rpcTraceInfoVo.setRequestJson(JSON.toJSONString(responseEntityBody));
			rpcTraceInfoVo.setRunTime(runTime);
			rpcTraceInfoVo.setResult("OK");
			
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			long runTime = System.currentTimeMillis()-startTime;
			//异常的结果
			String traceStr [] = ThrowableToStringArray.convert(e);
			StringBuilder builder = new StringBuilder();
			for(String trace : traceStr) {
				builder.append(trace);
				builder.append("\n");
			}
			rpcTraceInfoVo.setResponseJson(builder.toString());
			rpcTraceInfoVo.setResult("ERROR");
			rpcTraceInfoVo.setRunTime(runTime);
		}finally{
			//发送ES
			TraceClient.sendTraceInfo(rpcTraceInfoVo);
		}
		
		return responseEntityBody;
	}	
}
