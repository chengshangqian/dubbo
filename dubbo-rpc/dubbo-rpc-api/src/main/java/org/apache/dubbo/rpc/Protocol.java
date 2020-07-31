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
package org.apache.dubbo.rpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;

import java.util.Collections;
import java.util.List;

/**
 * 协议，默认值为dubbo（API/SPI，单例，线程安全）
 *
 * Protocol. (API/SPI, Singleton, ThreadSafe)
 */
@SPI("dubbo")
public interface Protocol {

    /**
     * 获取缺省端口号，如果用户没有配置端口
     *
     * Get default port when user doesn't config the port.
     *
     * @return default port
     */
    int getDefaultPort();

    /**
     * 发布为远程调用的服务：
     * 1.在接受到一个请求时，Protocol将记录请求源地址：RpcContext.getContext().setRemoteAddress()
     * 2.export()方法必须时等幂的，这样，当发布/导出相同URL时，调用一次和调用两次（其结果或影响）是没有任何区别的
     * 3.调用者实例invoker参数由框架传入，协议Protocol本身不需要关心
     *
     * 接口中的Adaptive注解没有指定key,相当于默认使用类名或类名单词用.连接后作为key，比如Protocol -> protocol，SimpleExt -> simple.ext作为key
     * export和refer的Adaptive注解相当于@Adaptive("protocol")
     *
     * Export service for remote invocation: <br>
     * 1. Protocol should record request source address after receive a request:
     * RpcContext.getContext().setRemoteAddress();<br>
     * 2. export() must be idempotent, that is, there's no difference between invoking once and invoking twice when
     * export the same URL<br>
     * 3. Invoker instance is passed in by the framework, protocol needs not to care <br>
     *
     * @param <T>     Service type 服务类型
     * @param invoker Service invoker 服务调用者
     * @return exporter reference for exported service, useful for unexport the service later 返回发布者（发布器）
     * @throws RpcException thrown when error occurs during export the service, for example: port is occupied
     */
    @Adaptive
    <T> Exporter<T> export(Invoker<T> invoker) throws RpcException;

    /**
     * 提供/获取一个远程服务（调用者）:
     * 1.当用户调用由refer()返回的调用者Invoker对象的invoke()方法，协议需要相应地执行Invoker对象的invoke()方法
     * 2.协议的责任是实现由refer()返回的Invoker。一般来说，协议在Invoker实现类中发送远程请求。
     * 3.当URL中有check=false参数时，调用Invoker的实现不能抛出异常而是在连接失败时尝试恢复
     *
     * Refer a remote service: <br>
     * 1. When user calls `invoke()` method of `Invoker` object which's returned from `refer()` call, the protocol
     * needs to correspondingly execute `invoke()` method of `Invoker` object <br>
     * 2. It's protocol's responsibility to implement `Invoker` which's returned from `refer()`. Generally speaking,
     * protocol sends remote request in the `Invoker` implementation. <br>
     * 3. When there's check=false set in URL, the implementation must not throw exception but try to recover when
     * connection fails.
     *
     * @param <T>  Service type 服务类型
     * @param type Service class 服务类型
     * @param url  URL address for the remote service 远程服务的URL地址
     * @return invoker service's local proxy 调用者服务的本地代理，可以理解调用者的本地代理或服务的本地代理
     * @throws RpcException when there's any error while connecting to the service provider
     */
    @Adaptive
    <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException;

    /**
     * 销毁协议：
     * 1.取消这个协议所发布或引用的所有的服务
     * 2.释放所有占用的资源，比如连接、端口等等
     * 3.协议Protocol可以继续导出和提供新的服务，即时它已经被销毁
     *
     * Destroy protocol: <br>
     * 1. Cancel all services this protocol exports and refers <br>
     * 2. Release all occupied resources, for example: connection, port, etc. <br>
     * 3. Protocol can continue to export and refer new service even after it's destroyed.
     */
    void destroy();

    /**
     * 获取所有提供此协议的协议服务器
     *
     * Get all servers serving this protocol
     *
     * @return
     */
    default List<ProtocolServer> getServers() {
        return Collections.emptyList();
    }

}