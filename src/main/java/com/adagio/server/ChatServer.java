package com.adagio.server;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adagio.protocol.IMDecoder;
import com.adagio.protocol.IMEncoder;
import com.adagio.server.handler.HttpHandler;
import com.adagio.server.handler.SocketHandler;
import com.adagio.server.handler.WebSocktHandler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public class ChatServer{
	
	private static Logger LOG = LoggerFactory.getLogger(ChatServer.class);
	
	private int port = 80;
	
    public ChatServer(int port) {
		this.port = port;
	}


	public void start() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 1024)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
	                
                		ChannelPipeline pipeline = ch.pipeline();
	                	
	                	/** 解析自定义协议 */
	                	pipeline.addLast(new IMDecoder());
	                	pipeline.addLast(new IMEncoder());
	                	pipeline.addLast(new SocketHandler());
	                
	                	/** 解析Http请求 */
	            		pipeline.addLast(new HttpServerCodec());
	            		//主要是将同一个http请求或响应的多个消息对象变成一个 fullHttpRequest完整的消息对象
	            		pipeline.addLast(new HttpObjectAggregator(64 * 1024));
	            		//主要用于处理大数据流,比如一个1G大小的文件如果你直接传输肯定会撑暴jvm内存的 ,加上这个handler我们就不用考虑这个问题了
	            		pipeline.addLast(new ChunkedWriteHandler());
//	            		pipeline.addLast(new HttpHandler());
	            		
	            		/** 解析WebSocket请求 */
	            		pipeline.addLast(new WebSocketServerProtocolHandler("/im"));
	            		pipeline.addLast(new WebSocktHandler());
            		
                }
            }); 
            
            // 发起同步连接操作
            ChannelFuture f = b.bind(this.port).sync();
            LOG.info("服务已启动,监听端口" + this.port);
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
        	// 关闭 是否线程资源
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
    
    
//    public static void main(String[] args) throws IOException{
//        new ChatServer().start();
//    }
    
}