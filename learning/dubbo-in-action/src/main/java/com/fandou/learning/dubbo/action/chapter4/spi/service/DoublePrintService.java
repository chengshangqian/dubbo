package com.fandou.learning.dubbo.action.chapter4.spi.service;

import com.fandou.learning.dubbo.action.chapter4.spi.api.PrintService;
import org.apache.dubbo.common.extension.SPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DoublePrintService implements PrintService {
    // 日志
    private static final Logger logger = LoggerFactory.getLogger(DoublePrintService.class);

    @Override
    public void print(String msg) {
        logger.info("打印消息 => {},{}",msg,msg);
    }
}
