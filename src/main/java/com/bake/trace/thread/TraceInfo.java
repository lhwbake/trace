package com.bake.trace.thread;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang.StringUtils;

public class TraceInfo {

	/**
	 * 跟踪ID
	 */
	private String traceId;
	
	/**
	 * rpc调用层级，从1.1开始，每新一集结尾增加.1
	 */
	private String hierarchy;
	
	/**
	 * 当前级别序号
	 */
	private AtomicInteger sequenceNo;
	
	public TraceInfo addSequenceNo() {
		this.sequenceNo.incrementAndGet();
		return this;
	}
	
	public TraceInfo addHierarchy(){
		this.hierarchy = getRpcId();
		this.sequenceNo = new AtomicInteger(0);
		return this;
	}
	
	public TraceInfo(String traceId, String rpcId) {
		this.traceId = traceId;
		setRpcId(rpcId);
	}
	
	public TraceInfo(String traceId, String rpcId, Map<String, String> userInfo) {
		this.traceId = traceId;
		setRpcId(rpcId);
	}
	
	public TraceInfo() {
		this.traceId = UUID.randomUUID().toString().replace("-", "");
		this.hierarchy = "";
		this.sequenceNo = new AtomicInteger(1);
	}
	
	/**
	 * 获取跟踪ID
	 * @return
	 */
	public String getTraceId() {
		return traceId;
	}
	
	/**
	 * 设置跟踪 ID
	 * @param traceId
	 */
	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}
	
	/**
	 * 获取当前rpcId
	 */
	public String  getRpcId() {
		if(StringUtils.isBlank(hierarchy)) {
			return sequenceNo.get() + "";
		}
		return hierarchy + "." + sequenceNo.get();		
	}
	
	/**
	 * 设置 rpc调用层级 ，从1.1开始，每级递增0.1*
	 */
	public void setRpcId(String rpcId) {
		int lastDotIndex = StringUtils.lastIndexOf(rpcId, '.');
		if(lastDotIndex > -1) {
			this.hierarchy = rpcId.substring(0, lastDotIndex);
			this.sequenceNo = new AtomicInteger(Integer.parseInt(rpcId.substring(lastDotIndex + 1)));
		}else{
			this.hierarchy = "";
			this.sequenceNo = new AtomicInteger(Integer.parseInt(rpcId));
		}
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("TraceInfo(");
		sb.append("traceId='").append(traceId).append('\'');
		sb.append(", rpcId=").append(getRpcId());
		sb.append(')');
		return sb.toString();
	}
	
	
}
