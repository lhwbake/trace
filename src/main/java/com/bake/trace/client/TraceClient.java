package com.bake.trace.client;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.bake.jest.util.JestClientMgr;
import com.bake.trace.vo.RpcTraceInfoVo;

import io.searchbox.client.JestClient;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Index;

public class TraceClient {
	
	private static final Logger logger = LoggerFactory.getLogger(TraceClient.class);
	
	private static JestClient jestClient;
	private static ExecutorService pool = Executors.newFixedThreadPool(10);
	
	static{
		Properties properties = new Properties();
		properties.put("es.hosts", "192.168.0.101:9200,192.168.0.102:9200,192.168.0.103:9200");
		properties.put("es.username", "elastic");
		properties.put("es.password", "changeme");
		JestClientMgr jestClientMgr = new JestClientMgr(properties);
		jestClient = jestClientMgr.getJestClient();
	}
	
	public TraceClient(){
		
	}

	public static void sendTraceInfo(final RpcTraceInfoVo rpcTraceInfoVo) {
		pool.submit(new Runnable() {

			@Override
			public void run() {
				Index index = new Index.Builder(JSON.toJSONString(rpcTraceInfoVo, SerializerFeature.WriteNullStringAsEmpty)).index("trace_info").type("trace").build();
				try{
					DocumentResult result = jestClient.execute(index);
					logger.info(result.getJsonString());
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		});
	}
}
