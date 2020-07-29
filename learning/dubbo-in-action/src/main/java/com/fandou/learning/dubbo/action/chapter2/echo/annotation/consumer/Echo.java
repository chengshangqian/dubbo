package com.fandou.learning.dubbo.action.chapter2.echo.annotation.consumer;

import com.fandou.learning.dubbo.action.chapter2.echo.service.EchoService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

/**
 * echo终端，调用echo服务
 */
@Component("echo")
public class Echo {
    // 注入依赖的远程服务
    @DubboReference
    private EchoService echoService;

    /**
     * 调用服务
     *
     * @param name
     * @return
     */
    public String echo(String name){
        String result = echoService.echo(name);
        return result;
    }
}
