package com.fandou.learning.dubbo.action.chapter2.echo.api.consumer;

import com.fandou.learning.dubbo.action.chapter2.echo.service.EchoService;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.utils.ReferenceConfigCache;
import org.apache.dubbo.rpc.service.GenericService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * API编写EchoService消费者端
 */
public final class ApiEchoConsumer {
    // 注册中心协议地址
    private static final String registryUri = "zookeeper://127.0.0.1:2181";

    // 要消费的服务（引用）
    private ReferenceConfig<EchoService> reference;

    // 获取的服务实例
    private EchoService echoService;

    // 日志
    private static final Logger logger = LoggerFactory.getLogger(ApiEchoConsumer.class);

    /**
     * 启动消费者应用：使用引导的方式启动
     *
     * @return
     */
    public ApiEchoConsumer start(){
        // 也可以用这种方式指定日志类型
        //System.setProperty("dubbo.application.logger","slf4j");

        // 消费者应用信息
        ApplicationConfig application = new ApplicationConfig("api-echo-consumer");
        // 指定日志类型
        application.setLogger("slf4j");
        // 默认开启了QOS，但同一台机同一个应用下运行提供者和消费者，QOS的端口会冲突，所以需要分别指定
        application.setQosEnable(true);
        application.setQosAcceptForeignIp(false);
        application.setQosPort(44444);

        // 注册中心
        RegistryConfig registry = new RegistryConfig(registryUri);

        // 定义要消费的服务（引用）
        reference = new ReferenceConfig<>();
        // 指定接口类型
        reference.setInterface(EchoService.class);
        // 支持智能调用接口
        reference.setGeneric("true");

        // 引导方式启动
        DubboBootstrap consumerServer = DubboBootstrap.getInstance();

        // 配置消费者应用
        consumerServer.application(application)
                // 配置注册中心
                .registry(registry)
                // 配置要消费应服务
                .reference(reference)
                // 启动
                .start();

        // 先获取一次
        // getEchoService();

        return this;
    }

    /**
     * 经典启动方式
     *
     * @return
     */
    public ApiEchoConsumer startWithRefer(){
        logger.info("startWithRefer方法被调用...");
        // 消费者应用信息
        ApplicationConfig application = new ApplicationConfig("api-echo-consumer");
        // 指定日志类型
        application.setLogger("slf4j");
        // 默认开启了QOS，但同一台机同一个应用下运行提供者和消费者，QOS的端口会冲突，所以需要分别指定
        application.setQosEnable(true);
        application.setQosAcceptForeignIp(false);
        application.setQosPort(44444);

        // 注册中心
        RegistryConfig registry = new RegistryConfig(registryUri);


        // 定义要消费的服务（引用）
        reference = new ReferenceConfig<>();
        // 配置应用信息
        reference.setApplication(application);
        // 配置注册中心
        reference.setRegistry(registry);
        // 指定接口类型
        reference.setInterface(EchoService.class);
        // 支持智能调用接口
        reference.setGeneric("true");

        // 先获取一次
        // getEchoService();

        return this;
    }

    public EchoService getEchoService() {
        if(null == echoService){
            // 获取服务，两种方式任意一种都可以
            // echoService = reference.get();
            echoService = ReferenceConfigCache.getCache().get(reference);
        }

        return echoService;
    }

    public GenericService getGenericEchoService() {
        if(null != reference.getGeneric() && "true".equals(reference.getGeneric())){
            final EchoService echoService = getEchoService();
            if(null != echoService){
                return (GenericService) echoService;
            }
        }

       return null;
    }

    public static void main(String[] args) {
        // 也可以用这种方式指定日志类型
        System.setProperty("dubbo.application.logger","slf4j");

        // 启动服务消费者
        ApiEchoConsumer consumer = new ApiEchoConsumer();
        // 两种方式都一样
        // consumer.startWithRefer()
        consumer.start();

        // 本地的打印服务调用远程的echo服务
        Printer printer = new Printer(consumer);
        printer.println("打印...");
        printer.genericPrintln("dubbo智能调用...");
    }
}
