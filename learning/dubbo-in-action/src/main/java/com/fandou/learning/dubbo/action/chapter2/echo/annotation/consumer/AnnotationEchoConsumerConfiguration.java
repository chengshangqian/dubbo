package com.fandou.learning.dubbo.action.chapter2.echo.annotation.consumer;

import org.apache.dubbo.config.*;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableDubbo(scanBasePackages = "com.fandou.learning.dubbo.action.chapter2.echo.annotation.consumer")
@ComponentScan(value = {"com.fandou.learning.dubbo.action.chapter2.echo.annotation.consumer"})
public class AnnotationEchoConsumerConfiguration {
    // 注册中心协议地址
    private static final String registryUri = "zookeeper://192.168.8.100:2181";

    @Bean
    public ConsumerConfig consumerConfig(){
        return new ConsumerConfig();
    }

    @Bean
    public ApplicationConfig applicationConfig(){
        ApplicationConfig applicationConfig = new ApplicationConfig("annotation-echo-consumer");
        // 指定日志类型
        applicationConfig.setLogger("slf4j");
        // 配置QOS
        applicationConfig.setQosEnable(true);
        applicationConfig.setQosAcceptForeignIp(false);
        applicationConfig.setQosPort(23456);
        return applicationConfig;
    }

    @Bean
    public RegistryConfig registryConfig(){
        return new RegistryConfig(registryUri);
    }

    /*
    @Bean
    public ProtocolConfig protocolConfig(){
        return new ProtocolConfig("dubbo",20880);
    }
    */
}
