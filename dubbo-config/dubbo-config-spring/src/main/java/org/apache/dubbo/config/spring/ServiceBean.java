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
package org.apache.dubbo.config.spring;

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.annotation.Service;
import org.apache.dubbo.config.spring.context.event.ServiceBeanExportedEvent;
import org.apache.dubbo.config.spring.extension.SpringExtensionFactory;
import org.apache.dubbo.config.support.Parameter;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

/**
 * [dubbo服务]的工厂bean
 *
 * 其实现了InitializingBean、DisposableBean、ApplicationContextAware、BeanNameAware、ApplicationEventPublisherAware等一些列
 * spring框架的扩展接口
 *
 * ServiceFactoryBean
 *
 * @export
 */
public class ServiceBean<T> extends ServiceConfig<T> implements InitializingBean, DisposableBean,
        ApplicationContextAware, BeanNameAware, ApplicationEventPublisherAware {

    // 序列化id
    private static final long serialVersionUID = 213195494150089726L;

    // Dubbo的服务注解
    // transient修饰符表示此变量不进行序列化
    private final transient Service service;

    // 应用上下文
    // 实现了ApplicationContextAware接口，当该类bean注入完依赖初始化之前，
    // spring会将当前上下文通过接口调用设置进来
    private transient ApplicationContext applicationContext;

    // bean（服务）名称
    // 实现了BeanNameAware接口，在spring填充完bean的属性（依赖注入）之后，
    // 会调用接口将bean的名字设置进来
    private transient String beanName;

    // 应用事件发布器
    // spring容器的所有事件将会广播给实现ApplicationEventPublisherAware接口的对象，
    // 当事件发生时，spring会调用接口通知
    private ApplicationEventPublisher applicationEventPublisher;

    public ServiceBean() {
        super();
        this.service = null;
    }

    public ServiceBean(Service service) {
        super(service);
        this.service = service;
    }

    /**
     * 当spring创建当前bean对象，并填充完属性（依赖注入之后，初始化之前）调用此方法，
     * 将spring的上下文即容器设置进来
     *
     * @param applicationContext
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        // 保存spring容器到spring扩展工厂中，这样，通过Spring扩展也可以获取到Spring加载的bean
        SpringExtensionFactory.addApplicationContext(applicationContext);
    }

    /**
     * 当spring创建当前bean对象，并填充完属性（依赖注入之后，初始化之前）调用此方法，
     * 将bean的名字设置进来
     *
     * @param name
     */
    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }

    /**
     * Gets associated {@link Service}
     *
     * @return associated {@link Service}
     */
    public Service getService() {
        return service;
    }

    /**
     * 当前bean被spring初始化后，将会调用此方法
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // 检查服务路径（即interface属性）是否为空
        if (StringUtils.isEmpty(getPath())) {
            if (StringUtils.isNotEmpty(beanName)
                    && StringUtils.isNotEmpty(getInterface())
                    && beanName.startsWith(getInterface())) {
                // 如果为空，将使用beanName作为path
                setPath(beanName);
            }
        }
    }

    /**
     * Get the name of {@link ServiceBean}
     *
     * @return {@link ServiceBean}'s name
     * @since 2.6.5
     */
    @Parameter(excluded = true)
    public String getBeanName() {
        return this.beanName;
    }

    /**
     * 触发服务已曝露事件
     * @since 2.6.5
     */
    @Override
    public void exported() {
        super.exported();
        // Publish ServiceBeanExportedEvent
        publishExportEvent();
    }

    /**
     * 推送服务曝露事件
     *
     * @since 2.6.5
     */
    private void publishExportEvent() {
        ServiceBeanExportedEvent exportEvent = new ServiceBeanExportedEvent(this);
        applicationEventPublisher.publishEvent(exportEvent);
    }

    /**
     * 注销当前bean对象
     *
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        // no need to call unexport() here, see
        // org.apache.dubbo.config.spring.extension.SpringExtensionFactory.ShutdownHookListener
    }

    /**
     * 获取引用的bean/服务的类型
     *
     * @param ref
     * @return
     */
    // merged from dubbox
    @Override
    protected Class getServiceClass(T ref) {
        if (AopUtils.isAopProxy(ref)) {
            return AopUtils.getTargetClass(ref);
        }
        return super.getServiceClass(ref);
    }

    /**
     * 当spring创建当前bean对象，并填充完属性后调用此方法，
     * 将事件推送对象设置进来
     *
     * @param applicationEventPublisher
     * @since 2.6.5
     */
    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
}
