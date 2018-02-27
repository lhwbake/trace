package com.bake.trace.constants;

public enum RpcTypeEnum {
	
	ICE("ice"),
	WEBSERVICE("ws"),
	HTTP("http"),
	DB("db"),
	REDIS("redis"),
	MQ("mq"),
	TASK("task"),
	DUBBOO("dubbo");
	
	private String rpcName;

	private RpcTypeEnum(String rpcName){
		this.rpcName =  rpcName;
	}

	public String getRpcName() {
		return rpcName;
	}

	public void setRpcName(String rpcName) {
		this.rpcName = rpcName;
	}		
	
}
