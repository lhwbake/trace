package com.bake.trace.vo;

import lombok.Data;

@Data
public class RpcTraceInfoVo {
	private String traceId;
	private String rpcId;
	private String rpcType;
	private String clientName;
	private String serviceCategory;
	private String serviceName;
	private String methodName;
	private String serverHost;
	private String clientHost;
	private String requestDateTime;
	private String result;
	private String responseJson;
	private String env;
	private String requestJson;
	private long runTime;
}
