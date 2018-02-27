package com.bake.trace.thread;

import java.util.HashMap;
import java.util.Map;

public class ThreadContext {
	/**
	 *线程上下文变量的持有者 
	 */
	private final static ThreadLocal<Map<String, Object>> CTX_HOLDER = new ThreadLocal<Map<String, Object>>();
	
	static {
		CTX_HOLDER.set(new HashMap<String, Object>());
	}
	
	/**
	 * 添加内容到线程上下文中
	 */
	public final static void putContext(String key, Object value) {
		Map<String, Object> ctx = CTX_HOLDER.get();
		if(ctx == null) {
			return;
		}
		ctx.put(key, value);
	}
	
	/**
	 * 从线程上下文中获取内容
	 */
	@SuppressWarnings("unchecked")
	public final static <T extends Object> T getContext(String key) {
		Map<String, Object> ctx = CTX_HOLDER.get();
		if(	ctx == null) {
			return null;
		}
		return (T) ctx.get(key);
	}
	
	/**
	 * 获取线程上下文
	 */
	public final static Map<String, Object> getContext() {
		Map<String, Object> ctx = CTX_HOLDER.get();
		if(ctx == null) {
			return null;
		}
		return ctx;
	}
	
	/**
	 * 删除上下午的key
	 */
	public final static  void remove(String key) {
		Map<String, Object> ctx = CTX_HOLDER.get();
		if(ctx != null) {
			ctx.remove(key);
		}
	}
	
	/**
	 * 上下文中是否包含key
	 */
	public final static boolean containa(String key) {
		Map<String, Object> ctx = CTX_HOLDER.get();
		if(ctx != null ) {
			return ctx.containsKey(key);
		}
		return false;
	}
	
	/**
	 * 清空线程上下文
	 */
	public final static void clean() {
		CTX_HOLDER.set(null);
	}
	
	/**
	 * 初始化线程上下文
	 */
	public final static void init() {
		CTX_HOLDER.set(new HashMap<String, Object>());
	}
	
	/**
	 * 设置调用信息
	 */
	public final static void putTraceInfo(TraceInfo traceInfo) {
		putContext(TRACE_INFO_KEY, traceInfo);
	}
	
	/**
	 * 获取调用栈信息
	 */
	public final static TraceInfo getTraceInfo() {
		return getContext(TRACE_INFO_KEY);
	}
	
	/**
	 * 删除调用栈信息
	 */
	public final static void removeTraceInfo() {
		remove(TRACE_INFO_KEY);
	}
	
	/**
	 * 调用栈信息
	 */
	private final static String TRACE_INFO_KEY = "traceInfo"; 
	
	
	
	
}

