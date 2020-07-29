package com.fandou.learning.dubbo.action.chapter4.spi.service;

import com.fandou.learning.dubbo.action.chapter4.spi.api.PrintService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FixedLengthPrintService implements PrintService {

    // 日志
    private static final Logger logger = LoggerFactory.getLogger(FixedLengthPrintService.class);

    public FixedLengthPrintService(){
        this(10);
    }

    private int length;

    public FixedLengthPrintService(int length){
        this.length = length;
    }

    @Override
    public void print(String msg) {

        if(msg.length() <= length){
            logger.info("打印消息[{}] => {}",length,msg);
            return;
        }

        logger.info("打印消息[{}] => {}",length,msg.substring(0,length));
    }
}
