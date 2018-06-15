package com.adagio.protocol;

/**
 * 自定义IM协议， Instant Messaging Protocol 即时通讯协议
 *
 */
public enum IMP {
	
	/** 系统消息 */
	SYSTEM("SYSTEM"),
	/** 登录指令 */
	LOGIN("LOGIN"),
	/** 登出指令 */
	LOGOUT("LOGOUT"),
	/** 聊天消息 */
	CHAT("CHAT"),
	/** 送鲜花 */
	FLOWER("FLOWER");

	private String name;
	
	IMP(String name){
		this.name = name;
	}
	
	public static boolean isIMP(String content) {
		return content.matches("^\\[(SYSTEM|LOGIN|LOGIN|CHAT)\\]");
	}
	
	public String getName() {
		return this.name;
	}
	
	public String toString() {
		return this.name;
	}
}
