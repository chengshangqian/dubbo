package com.fandou.learning.dubbo.action.chapter2.echo.xml.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;

public class XmlEchoProvider {

    private static final Logger logger = LoggerFactory.getLogger(XmlEchoProvider.class);

    public static void main(String[] args) throws IOException {
        // 也可以用这种方式指定日志类型
        System.setProperty("dubbo.application.logger","slf4j");

        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"spring/echo-provider.xml"});
        context.start();
        logger.info("Echo服务提供方已启动...");
        System.in.read();
    }
}
