package com.adagio.protocol;

import org.msgpack.MessagePack;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 自定义IM协议的编码器
 *
 */
public class IMEncoder extends MessageToByteEncoder<IMMessage> {

	@Override
	protected void encode(ChannelHandlerContext ctx, IMMessage msg, ByteBuf out) throws Exception {
		out.writeBytes(new MessagePack().write(msg));
	}
	
	public String encode(IMMessage msg) {
		
		if(null == msg) return "";
		
		String prex = "[" + msg.getCmd() + "]" + "[" + msg.getTime() + "]";
		
		String cmd = msg.getCmd();
		
		if(IMP.LOGIN.getName().equals(cmd) || IMP.CHAT.getName().equals(cmd) || IMP.FLOWER.getName().equals(cmd)) {
			prex += ("[" + msg.getSender() + "]");
		} else if(IMP.SYSTEM.getName().equals(cmd)) {
			prex += ("[" + msg.getOnline() + "]");
		}
		
		String content = msg.getContent();
		if(!(null == content || "".equals(content))) {
			prex += (" - " + msg.getContent());
		}
		
		return prex;
	}

}
