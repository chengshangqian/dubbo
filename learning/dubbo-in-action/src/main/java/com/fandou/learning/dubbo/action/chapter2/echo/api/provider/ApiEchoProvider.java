package com.fandou.learning.dubbo.action.chapter2.echo.api.provider;

import com.fandou.learning.dubbo.action.chapter2.echo.service.EchoService;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;

/**
 * API方式编写Echo服务提供者
 */
public class ApiEchoProvider {
    // 注册中心协议地址
    private static final String registryUri = "zookeeper://127.0.0.1:2181";

    /**
     * 启动服务提供者
     */
    public void start(){
        // 也可以用这种方式指定日志类型
        System.setProperty("dubbo.application.logger","slf4j");

        // 服务提供者应用信息
        ApplicationConfig application = new ApplicationConfig("api-echo-provider");
        // 指定日志类型
        application.setLogger("slf4j");

        // 注册中心
        RegistryConfig registry = new RegistryConfig(registryUri);

        // 服务配置
        ServiceConfig<EchoServiceImpl> service = new ServiceConfig<>();
        service.setInterface(EchoService.class);
        service.setRef(new EchoServiceImpl());

        // 引导启动
        DubboBootstrap providerServer = DubboBootstrap.getInstance();

        // 配置服务提供者应用信息
        providerServer.application(application)
                // 配置注册中心
                .registry(registry)
                // 配置提供的服务
                .service(service)
                // 启动
                .start()
                // 等待启动完成
                .await();
    }

    public static void main(String[] args) {
        // 也可以用这种方式指定日志类型
        //System.setProperty("dubbo.application.logger","slf4j");
        new ApiEchoProvider().start();
    }

}
