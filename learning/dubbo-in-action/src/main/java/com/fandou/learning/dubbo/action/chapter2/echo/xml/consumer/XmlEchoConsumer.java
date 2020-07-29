package com.fandou.learning.dubbo.action.chapter2.echo.xml.consumer;

import com.fandou.learning.dubbo.action.chapter2.echo.service.EchoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;

public class XmlEchoConsumer {

    private static final Logger logger = LoggerFactory.getLogger(XmlEchoConsumer.class);

    public static void main(String[] args) throws IOException {
        // 也可以用这种方式指定日志类型
        System.setProperty("dubbo.application.logger","slf4j");

        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"spring/echo-consumer.xml"});
        context.start();
        logger.info("Echo服务消费方已启动...");

        // 直接调用
        EchoService echoService = (EchoService)context.getBean("echoService");
        String result = echoService.echo("张三");
        logger.info("result -> {}",result);

        // 注入其它的bean
        Telnet telnet = (Telnet)context.getBean("telnet");
        String telnetResult = telnet.telnet("telnet命令登录中...");
        logger.info("telnet -> {}",telnetResult);

        System.in.read();
    }
}
