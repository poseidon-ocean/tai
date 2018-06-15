package com.adagio.processor;

import com.adagio.protocol.IMDecoder;
import com.adagio.protocol.IMEncoder;
import com.adagio.protocol.IMMessage;
import com.adagio.protocol.IMP;
import com.alibaba.fastjson.JSONObject;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * 自定义协议内容的逻辑处理
 *
 */
public class MsgProcessor {

	//记录在线用户
	private static ChannelGroup onlineUsers = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	
	//定义一些扩展属性
	private final AttributeKey<String> NICK_NAME = AttributeKey.valueOf("nickName");
	private final AttributeKey<String> IP_ADDR = AttributeKey.valueOf("ipAddr");
	private final AttributeKey<JSONObject> ATTRS = AttributeKey.valueOf("attrs");
	
	//自定义解码器
	private IMDecoder decoder = new IMDecoder();
	//自定义编码器
	private IMEncoder encoder = new IMEncoder();
	
	/**
	 * 获取用户昵称
	 * @param client
	 * @return
	 */
	public String getNickName(Channel client) {
		return client.attr(NICK_NAME).get();
	}
	
	/**
	 * 获取用户远程ip地址
	 * @param client
	 * @return
	 */
	public String getAddress(Channel client) {
		return client.remoteAddress().toString().replaceFirst("/", "");
	}
	
	/**
	 * 获取扩展属性
	 * @param client
	 * @return
	 */
	public JSONObject getAttrs(Channel client) {
		
		try {
			
			return client.attr(ATTRS).get();
			
		} catch (Exception e) {
			return null;
		}
		
	}
	
	/**
	 * 设置扩展属性
	 * @param client
	 * @param key
	 * @param value
	 */
	private void setAttrs(Channel client, String key, Object value) {
		try {
			
			JSONObject json = client.attr(ATTRS).get();
			json.put(key, value);
			client.attr(ATTRS).set(json);
			
		} catch (Exception e) {
			JSONObject json = new JSONObject();
			json.put(key, value);
			client.attr(ATTRS).set(json);
		}
	}
	
	/**
	 * 退出通知
	 * @param client
	 */
	public void logout(Channel client) {
		//如果nickName为null，没有遵从聊天协议的链接，表示非法登录
		if(null == getNickName(client)) return ;
		
		for(Channel channel : onlineUsers) {
			IMMessage request = new IMMessage(IMP.SYSTEM.getName(), sysTime(), onlineUsers.size(), getNickName(client) + "离开");
			String content = encoder.encode(request);
			channel.writeAndFlush(new TextWebSocketFrame(content));
		}
		onlineUsers.remove(client);
	}
	
	/**
	 * 发送消息
	 * @param client
	 * @param msg
	 */
	public void sendMsg(Channel client, IMMessage msg) {
		sendMsg(client, encoder.encode(msg));
	}
	
	/**
	 * 发送消息
	 * @param client
	 * @param msg
	 */
	public void sendMsg(Channel client, String msg) {
		IMMessage request = decoder.decode(msg);
		if(null == request) return ;
		
		String addr = getAddress(client);
		
		String cmd = request.getCmd();
		if(cmd.equals(IMP.LOGIN.getName())) {
			client.attr(NICK_NAME).getAndSet(request.getSender());
			client.attr(IP_ADDR).getAndSet(addr);
			onlineUsers.add(client);
			
			for(Channel channel : onlineUsers) {
				if(channel != client) {
					request = new IMMessage(IMP.SYSTEM.getName(), sysTime(), onlineUsers.size(), getNickName(client) + "加入");
				} else {
					request = new IMMessage(IMP.SYSTEM.getName(), sysTime(), onlineUsers.size(), "已与服务器建立连接");
				}
				request.setTime(sysTime());
				String content = encoder.encode(request);
				channel.writeAndFlush(new TextWebSocketFrame(content));
			}
		} else if(cmd.equals(IMP.CHAT.getName())) {
			for(Channel channel : onlineUsers) {
				if(channel == client) {
					request.setSender("you");
				} else {
					request.setSender(getNickName(client));
				}
				request.setTime(sysTime());
				String content = encoder.encode(request);
				channel.writeAndFlush(new TextWebSocketFrame(content));
			}
		} else if(cmd.equals(IMP.FLOWER.getName())) {
			JSONObject attrs = getAttrs(client);
			long currTime = sysTime();
			if(null != attrs){
				long lastTime = attrs.getLongValue("lastFlowerTime");
				//60秒之内不允许重复刷鲜花
				int secends = 10;
				long sub = currTime - lastTime;
				if(sub < 1000 * secends){
					request.setSender("you");
					request.setCmd(IMP.SYSTEM.getName());
					request.setContent("您送鲜花太频繁," + (secends - Math.round(sub / 1000)) + "秒后再试");
					String content = encoder.encode(request);
					client.writeAndFlush(new TextWebSocketFrame(content));
					return;
				}
			}
			
			//正常送花
			for (Channel channel : onlineUsers) {
				if (channel == client) {
					request.setSender("you");
					request.setContent("你给大家送了一波鲜花雨");
					setAttrs(client, "lastFlowerTime", currTime);
				}else{
					request.setSender(getNickName(client));
					request.setContent(getNickName(client) + "送来一波鲜花雨");
				}
				request.setTime(sysTime());
				
				String content = encoder.encode(request);
				channel.writeAndFlush(new TextWebSocketFrame(content));
			}


		}
		
	}
	
	/**
	 * 获取系统时间
	 * @return
	 */
	private Long sysTime() {
		return System.currentTimeMillis();
	}
	
}
