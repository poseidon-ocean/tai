package com.adagio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.adagio.server.ChatServer;

@SpringBootApplication
public class TaiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaiApplication.class, args);
	}
	
	/**
	 * 启动chat服务
	 * @return
	 */
	@Bean
	public ChatServer startChat() {
		ChatServer chatServer = new ChatServer(9527);
		chatServer.start();
		return chatServer;
	}

}
