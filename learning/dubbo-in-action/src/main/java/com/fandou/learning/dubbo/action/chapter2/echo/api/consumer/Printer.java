package com.fandou.learning.dubbo.action.chapter2.echo.api.consumer;

import org.apache.dubbo.rpc.service.GenericService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Printer {
    private static final Logger logger = LoggerFactory.getLogger(Printer.class);

    // 应用上下文/容器
    private final ApiEchoConsumer echoConsumer;

    public Printer(ApiEchoConsumer echoConsumer) {
        this.echoConsumer = echoConsumer;
    }

    public void println(String msg){
       String result = echoConsumer.getEchoService().echo(msg);
       logger.info("result -> {}",result);
    }

    /**
     * 智能方式调用接口：自行指定方法名称、参数等
     *
     * @param msg
     */
    public void genericPrintln(String msg){
        GenericService genericService = echoConsumer.getGenericEchoService();
        Object genericResult = null;
        if(null != genericService){
            // 自行指定方法名称、参数等
            genericResult = genericService.$invoke("echo", new String[] { String.class.getName() },
                    new String[] { msg });
        }
        logger.info("genericResult -> {}",genericResult);
    }
}
