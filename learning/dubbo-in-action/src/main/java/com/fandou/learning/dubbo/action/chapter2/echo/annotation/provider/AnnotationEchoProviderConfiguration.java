package com.fandou.learning.dubbo.action.chapter2.echo.annotation.provider;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableDubbo(scanBasePackages = "com.fandou.learning.dubbo.action.chapter2.echo.annotation.provider")
public class AnnotationEchoProviderConfiguration {
    // 注册中心协议地址
    private static final String registryUri = "zookeeper://192.168.8.100:2181";

    @Bean
    public ProviderConfig providerConfig(){
        return new ProviderConfig();
    }

    @Bean
    public ApplicationConfig applicationConfig(){
        ApplicationConfig application = new ApplicationConfig("annotation-echo-provider");
        // 指定日志类型
        application.setLogger("slf4j");
        return application;
    }

    @Bean
    public RegistryConfig registryConfig(){
        return new RegistryConfig(registryUri);
    }

    @Bean
    public ProtocolConfig protocolConfig(){
        return new ProtocolConfig("dubbo",20880);
    }
}

