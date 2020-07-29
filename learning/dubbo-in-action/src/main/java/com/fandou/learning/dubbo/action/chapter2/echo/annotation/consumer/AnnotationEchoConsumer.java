package com.fandou.learning.dubbo.action.chapter2.echo.annotation.consumer;

import com.fandou.learning.dubbo.action.chapter2.echo.service.EchoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class AnnotationEchoConsumer {
    // 日志
    private static final Logger logger = LoggerFactory.getLogger(AnnotationEchoConsumer.class);

    public static void main(String[] args) {
        // 也可以用这种方式指定日志类型
        System.setProperty("dubbo.application.logger","slf4j");

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AnnotationEchoConsumerConfiguration.class);
        context.start();

        EchoService echoService = context.getBean(EchoService.class);
        String echoResult =  echoService.echo("echoResult成九五");
        logger.info("echoResult -> {}",echoResult);

        Echo echo = context.getBean(Echo.class);
        String result =  echo.echo("成九五");
        logger.info("result -> {}",result);

        echo = context.getBean("echo",Echo.class);
        result =  echo.echo("九五学堂");
        logger.info("result -> {}",result);
    }
}
