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
package org.apache.dubbo.demo.provider;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.demo.DemoService;

import java.util.concurrent.CountDownLatch;

/**
 * dubbo应用示例
 * 服务提供者
 */
public class Application {
    /**
     * 注册中心URI：协议://主机地址:端口号
     * 协议为zookeeper，表示使用zookeeper作为注册中心
     */
    private static final String registryUri = "zookeeper://127.0.0.1:2181";

    /**
     * 启动服务提供者
     *
     * @param args 参数数组，main方法的args数组不会为null，但可能长度为0
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // 典型方式启动
        if (isClassic(args)) {
            // 调用export方法启动服务提供者
            // 调用setter方法配置启动参数
            startWithExport();
        }
        // 引导方式启动
        else {
            // 调用引导对象DubboBootstrap的start方法启动服务提供者
            // 链式调用的方式配置启动参数
            startWithBootstrap();
        }
    }

    /**
     * 是否典型启动方式
     *
     * @param args 从main方法传递的args数组不会为null，但可能长度为0
     * @return
     */
    private static boolean isClassic(String[] args) {
        return args.length > 0 && "classic".equalsIgnoreCase(args[0]);
    }

    /**
     * 引导方式启动
     */
    private static void startWithBootstrap() {
        // 服务配置对象
        ServiceConfig<DemoServiceImpl> service = new ServiceConfig<>();

        // 设置服务接口类型参数，如果此时没有设置id，会将接口完整类型名做为id
        service.setInterface(DemoService.class);
        // 设置服务实现类实例（引用）
        service.setRef(new DemoServiceImpl());

        // Dubbo引导，单例
        DubboBootstrap bootstrap = DubboBootstrap.getInstance();

        /**
         * 下面开始配置dubbo引导参数
         * DubboBootstrap内部使用一个配置管理者ConfigManager实例管理所有的配置对象xxxConfig实例
         */
        // 配置dubbo的应用信息
        bootstrap.application(new ApplicationConfig("dubbo-demo-api-provider"))
                // 配置注册中心
                .registry(new RegistryConfig(registryUri))
                // 配置（一个将要发布的）服务
                .service(service)
                // 启动提供者服务器
                .start()
                // 等待请求
                .await();
    }

    /**
     * 典型方式启动
     *
     * @throws InterruptedException
     */
    private static void startWithExport() throws InterruptedException {
        // 定义要发布的服务
        ServiceConfig<DemoServiceImpl> service = new ServiceConfig<>();
        // 服务接口
        service.setInterface(DemoService.class);
        // 真正提供服务的对象
        service.setRef(new DemoServiceImpl());

        // 配置服务提供者应用信息
        service.setApplication(new ApplicationConfig("dubbo-demo-api-provider"));
        // 配置注册中心
        service.setRegistry(new RegistryConfig(registryUri));
        // 发布/曝露/导出服务
        service.export();

        System.out.println("dubbo service started");
        // 等待请求
        new CountDownLatch(1).await();
    }
}
