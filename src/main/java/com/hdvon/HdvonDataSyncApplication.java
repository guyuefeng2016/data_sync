package com.hdvon;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@MapperScan(basePackages = {"com.hdvon.mapper"})
public class HdvonDataSyncApplication {

	public static void main(String[] args) throws Exception{
		SpringApplication.run(HdvonDataSyncApplication.class, args);
	}

}
