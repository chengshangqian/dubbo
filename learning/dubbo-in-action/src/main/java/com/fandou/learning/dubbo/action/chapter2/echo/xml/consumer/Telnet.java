package com.fandou.learning.dubbo.action.chapter2.echo.xml.consumer;

import com.fandou.learning.dubbo.action.chapter2.echo.service.EchoService;

/**
 * Telnet客户端
 */
public class Telnet {

    // 注入远程的echo服务
    private EchoService echoService;

    public EchoService getEchoService() {
        return echoService;
    }

    public void setEchoService(EchoService echoService) {
        this.echoService = echoService;
    }

    public String telnet(String cmd){
        return echoService.echo(cmd);
    }
}
