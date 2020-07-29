package com.fandou.learning.dubbo.action.chapter2.echo.annotation.provider;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;


public class AnnotationEchoProvider {

    /**
     * 启动服务提供者应用
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        // 也可以用这种方式指定日志类型
        System.setProperty("dubbo.application.logger","slf4j");

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AnnotationEchoProviderConfiguration.class);
        context.start();
        System.in.read();
    }
}
