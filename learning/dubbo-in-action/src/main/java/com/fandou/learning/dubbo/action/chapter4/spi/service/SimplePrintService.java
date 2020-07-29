package com.fandou.learning.dubbo.action.chapter4.spi.service;

import com.fandou.learning.dubbo.action.chapter4.spi.api.PrintService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimplePrintService implements PrintService {
    // 日志
    private static final Logger logger = LoggerFactory.getLogger(SimplePrintService.class);

    @Override
    public void print(String msg) {
        logger.info("打印消息 => {}",msg);
    }
}
