package com.fandou.learning.dubbo.action.chapter2.echo.annotation.provider;

import com.fandou.learning.dubbo.action.chapter2.echo.service.EchoService;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.rpc.RpcContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

@DubboService
public class EchoServiceImpl implements EchoService {
    private static final Logger logger = LoggerFactory.getLogger(EchoServiceImpl.class);

    @Override
    public String echo(String message) {
        String now = new SimpleDateFormat("HH:mm:ss").format(new Date());
        logger.info("[{}] Hello {},request from consumer:{}",now,message, RpcContext.getContext().getRemoteAddress());
        return message;
    }
}
