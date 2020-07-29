/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.demo.consumer;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.utils.ReferenceConfigCache;
import org.apache.dubbo.demo.DemoService;
import org.apache.dubbo.rpc.service.GenericService;

public class Application {
    public static void main(String[] args) {
        if (isClassic(args)) {
            runWithRefer();
        } else {
            runWithBootstrap();
        }
    }

    // 注册中心协议地址
    private static final String zkRegistry = "zookeeper://127.0.0.1:2181";

    /**
     * 是否是经典调用方式
     *
     * @param args
     * @return
     */
    private static boolean isClassic(String[] args) {
        return args.length > 0 && "classic".equalsIgnoreCase(args[0]);
    }

    /**
     * 使用引导方式运行
     */
    private static void runWithBootstrap() {
        // 定义要消费的服务
        ReferenceConfig<DemoService> reference = new ReferenceConfig<>();
        reference.setInterface(DemoService.class);
        // 支持智能服务方式
        reference.setGeneric("true");
        reference.setCache("");

        // 引导
        DubboBootstrap bootstrap = DubboBootstrap.getInstance();

        // 配置消费者应用信息
        bootstrap.application(new ApplicationConfig("dubbo-demo-api-consumer"))
                // 配置注册中心
                .registry(new RegistryConfig(zkRegistry))
                // 配置要消费的服务
                .reference(reference)
                // 启动
                .start();

        // 获取服务
        DemoService demoService = ReferenceConfigCache.getCache().get(reference);

        // 调用接口,消费服务
        String message = demoService.sayHello("dubbo");
        // 打印结果
        System.out.println(message);

        // generic invoke
        // 智能调用方式
        GenericService genericService = (GenericService) demoService;
        // 配置要调用的接口名称、参数等更多信息，然后调用
        Object genericInvokeResult = genericService.$invoke("sayHello", new String[] { String.class.getName() },
                new Object[] { "dubbo generic invoke" });
        // 打印调用结果
        System.out.println(genericInvokeResult);
    }

    /**
     * 使用经典方式运行
     */
    private static void runWithRefer() {
        // 定义要消费的服务
        ReferenceConfig<DemoService> reference = new ReferenceConfig<>();
        // 配置服务消费者应用信息
        reference.setApplication(new ApplicationConfig("dubbo-demo-api-consumer"));
        // 配置注册中心
        reference.setRegistry(new RegistryConfig(zkRegistry));
        // 配置服务接口
        reference.setInterface(DemoService.class);
        // 获取真实服务对象实例
        DemoService service = reference.get();
        // 调用服务
        String message = service.sayHello("dubbo");
        // 打印结果
        System.out.println(message);
    }
}
