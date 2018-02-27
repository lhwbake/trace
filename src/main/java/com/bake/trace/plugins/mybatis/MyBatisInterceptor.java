package com.bake.trace.plugins.mybatis;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSON;
import com.bake.trace.client.TraceClient;
import com.bake.trace.constants.RpcTypeEnum;
import com.bake.trace.thread.ThreadContext;
import com.bake.trace.thread.TraceInfo;
import com.bake.trace.vo.RpcTraceInfoVo;

@Intercepts({
	@Signature(type=Executor.class, method ="update", args = {MappedStatement.class, Object.class}),
	@Signature(type=Executor.class, method ="query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})})
public class MyBatisInterceptor implements Interceptor {
	
	private static final Logger logger = LoggerFactory.getLogger(MyBatisInterceptor.class);
	private Properties properties;
	private static final FastDateFormat ISO_DATETIME_TIME_LONE_FORMAT_WITH_MILLIS = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
	
	public Object intercept(Invocation invocation) throws Throwable {
		MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
		Object parameter = null;
		if(invocation.getArgs().length > 1) {
			parameter = invocation.getArgs()[1];
		}
		String sqlId = mappedStatement.getId();
		BoundSql boundSql = mappedStatement.getBoundSql(parameter);
		Configuration configuration  = mappedStatement.getConfiguration();
		Object returnValue = null;
		long startTime = System.currentTimeMillis();
		returnValue = invocation.proceed();
		long endTime = System.currentTimeMillis();
		long runTime = (endTime - startTime);
		String jdbcUrl = "";
		DataSource dataSource = configuration.getEnvironment().getDataSource();
		if(dataSource instanceof DruidDataSource) {
			jdbcUrl = ((DruidDataSource) dataSource).getUrl();
		}
		//发送到trace
		send2Trace(jdbcUrl, configuration.getDatabaseId(), sqlId, showSql(configuration, boundSql), returnValue, runTime);
		return returnValue;
	}
	
	private void send2Trace(String jdbcUrl, String databaseType, String sqlId, String sql, Object returnValue, long runTime) {
		TraceInfo traceInfo = ThreadContext.getTraceInfo();
		//当调用连不为空的时候，再记录trace,防止无用的数据
		if(traceInfo != null) {
			traceInfo.addSequenceNo();
			//long beginTime = System.currentTimeMillis();
			//Object resultObject = null;
			RpcTraceInfoVo rpcTraceInfoVo = new RpcTraceInfoVo();
			try {
				rpcTraceInfoVo.setRequestDateTime(ISO_DATETIME_TIME_LONE_FORMAT_WITH_MILLIS.format(new Date()));
				rpcTraceInfoVo.setTraceId(traceInfo.getRpcId());
				rpcTraceInfoVo.setRpcId(traceInfo.getRpcId());
				rpcTraceInfoVo.setRpcType(RpcTypeEnum.DB.getRpcName());
				rpcTraceInfoVo.setServiceCategory("Mybatis");
				String sqlIdStr = StringUtils.substringAfter(sqlId, "mapper.");
				rpcTraceInfoVo.setServiceName(StringUtils.substringBefore(sqlIdStr, "."));
				rpcTraceInfoVo.setMethodName(StringUtils.substringAfter(sqlIdStr, "."));
				rpcTraceInfoVo.setRequestJson(sql);
				rpcTraceInfoVo.setServerHost(StringUtils.substringBetween(jdbcUrl, "jdbc:", "?"));
				rpcTraceInfoVo.setRunTime(runTime);
				rpcTraceInfoVo.setResult("OK");
				rpcTraceInfoVo.setResponseJson(JSON.toJSONString(returnValue));
			}catch (Exception e) {
				rpcTraceInfoVo.setResult("ERROR");//ERROR
				e.printStackTrace();
			}finally {
				try {
					TraceClient.sendTraceInfo(rpcTraceInfoVo);
				}catch(Exception e) {
					logger.error("mybatis send2EagleEye errorL:"+e.getMessage());
				}
			}
		}
	}
	
	public static String getSql(Configuration configuration, BoundSql boundSql, String sqlId, long time) {
		String sql = showSql(configuration, boundSql);
		StringBuilder str = new StringBuilder(100);
		str.append(sqlId);
		str.append(":");
		str.append(sql);
		str.append(":");
		str.append("time");
		str.append("ms");
		return str.toString();
	}
	
	private static String getParameterValue(Object obj) {
		String value = null;
		if(obj instanceof String) {
			value = "'" + obj.toString() + "'";
		}else if(obj instanceof Date) {
			DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT);
			value = "'" + formatter.format(new Date()) + "'";
		}else {
			if(obj != null) {
				value  = obj.toString();
			}else{
				value = "";
			}
		}
		return value;
	}
	
	public static String showSql(Configuration configuration, BoundSql boundSql) {
		Object parameterObject = boundSql.getParameterObject();
		List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
		String sql = boundSql.getSql().replaceAll("[\\s]", " ");
		try{
			if(parameterMappings.size() > 0 && parameterObject != null) {
				TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
				if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
					sql = sql.replaceFirst("\\?", getParameterValue(parameterObject));
				}else {
					MetaObject metaObject = configuration.newMetaObject(parameterObject);
					for(ParameterMapping parameterMapping : parameterMappings) {
						String propertyName = parameterMapping.getProperty();
						if(metaObject.hasGetter(propertyName)) {
							Object obj = metaObject.getValue(propertyName);
							sql = sql.replaceFirst("\\?", getParameterValue( obj));
						}else if(boundSql.hasAdditionalParameter(propertyName)){
							Object obj = boundSql.getAdditionalParameter(propertyName);
							sql = sql.replaceAll("//?", getParameterValue(obj));
						}
					}
				}
			}
		}catch (Exception e) {
			
		}
		return sql;
	}
	@Override
	public Object plugin(Object target) {
		return Plugin.wrap(target, this);
	}

	@Override
	public void setProperties(Properties properties) {
		this.properties = getProperties();
		
	}

	public Properties getProperties() {
		return properties;
	}
	
	
}
