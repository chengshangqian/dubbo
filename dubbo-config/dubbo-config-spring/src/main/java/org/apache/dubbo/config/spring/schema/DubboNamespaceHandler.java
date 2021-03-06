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
package org.apache.dubbo.config.spring.schema;

import org.apache.dubbo.common.Version;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.SslConfig;
import org.apache.dubbo.config.spring.ConfigCenterBean;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.config.spring.beans.factory.config.ConfigurableSourceBeanMetadataElement;
import org.apache.dubbo.config.spring.context.DubboBootstrapApplicationListener;
import org.apache.dubbo.config.spring.context.DubboLifecycleComponentApplicationListener;

import com.alibaba.spring.util.AnnotatedBeanDefinitionRegistryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.w3c.dom.Element;

import static com.alibaba.spring.util.AnnotatedBeanDefinitionRegistryUtils.registerBeans;

/**
 * Dubbo的命名空间处理器，用于处理dubbo命名空间的xml元素。
 *
 * 通过实现spring框架的NamespaceHandler接口及其xml配置文件扩展规范，实现与spring框架的整合（bean解析）：
 * 当项目使用spring启动，解析xml配置文件，遇到dubbo命名空间的元素时，会调用此DubboNamespaceHandler解析dubbo命名空间的xml元素
 *
 * DubboNamespaceHandler
 *
 * @export
 */
public class DubboNamespaceHandler extends NamespaceHandlerSupport implements ConfigurableSourceBeanMetadataElement {

    static {
        Version.checkDuplicate(DubboNamespaceHandler.class);
    }

    /**
     * 注册bean的解析器
     */
    @Override
    public void init() {
        registerBeanDefinitionParser("application", new DubboBeanDefinitionParser(ApplicationConfig.class, true));
        registerBeanDefinitionParser("module", new DubboBeanDefinitionParser(ModuleConfig.class, true));
        registerBeanDefinitionParser("registry", new DubboBeanDefinitionParser(RegistryConfig.class, true));
        registerBeanDefinitionParser("config-center", new DubboBeanDefinitionParser(ConfigCenterBean.class, true));
        registerBeanDefinitionParser("metadata-report", new DubboBeanDefinitionParser(MetadataReportConfig.class, true));
        registerBeanDefinitionParser("monitor", new DubboBeanDefinitionParser(MonitorConfig.class, true));
        registerBeanDefinitionParser("metrics", new DubboBeanDefinitionParser(MetricsConfig.class, true));
        registerBeanDefinitionParser("ssl", new DubboBeanDefinitionParser(SslConfig.class, true));
        registerBeanDefinitionParser("provider", new DubboBeanDefinitionParser(ProviderConfig.class, true));
        registerBeanDefinitionParser("consumer", new DubboBeanDefinitionParser(ConsumerConfig.class, true));
        registerBeanDefinitionParser("protocol", new DubboBeanDefinitionParser(ProtocolConfig.class, true));
        registerBeanDefinitionParser("service", new DubboBeanDefinitionParser(ServiceBean.class, true));
        // 服务引用reference标签计解析器，可以不需要id
        registerBeanDefinitionParser("reference", new DubboBeanDefinitionParser(ReferenceBean.class, false));
        // 注解annotation标签解析器，其bean的id也是必须的，由父类完成id的设置逻辑
        registerBeanDefinitionParser("annotation", new AnnotationBeanDefinitionParser());
    }

    /**
     * Override {@link NamespaceHandlerSupport#parse(Element, ParserContext)} method
     *
     * @param element       {@link Element}
     * @param parserContext {@link ParserContext}
     * @return
     * @since 2.7.5
     */
    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        // registry一般是DefaultListableBeanFactory实例：spring的IOC容器
        BeanDefinitionRegistry registry = parserContext.getRegistry();

        // ApplicationListener是Spring事件机制的一部分，与抽象类ApplicationEvent类配合来完成ApplicationContext的事件机制。
        // 如果容器中存在ApplicationListener的Bean，当ApplicationContext调用publishEvent方法时，对应的Bean会被触发。这一过程是典型的观察者模式的实现。
        //
        // 将监听器作为bean注册到spring容器中：将监听spring容器的所有事件
        // 主要是容器刷新和关闭事件：刷新时即容器初始化完毕，启动dubbo框架，如连接注册中心、发布服务等；容器关闭时，停止dubbo框架，如取消注册中心、取消服务发布等
        registerAnnotationConfigProcessors(registry);
        registerApplicationListeners(registry);

        // init方法注册解析器，最终调用spring框架进行解析
        // 父类parse方法会根据element元素名称，从init方法中解析器注册表中查找对应的解析器进行解析
        BeanDefinition beanDefinition = super.parse(element, parserContext);

        setSource(beanDefinition);
        return beanDefinition;
    }

    /**
     *  ApplicationListener是Spring事件机制的一部分，与抽象类ApplicationEvent类配合来完成ApplicationContext的事件机制。
     *  如果容器中存在ApplicationListener的Bean，当ApplicationContext调用publishEvent方法时，对应的Bean会被触发。这一过程是典型的观察者模式的实现。
     *
     * Register {@link ApplicationListener ApplicationListeners} as a Spring Bean
     *
     * @param registry {@link BeanDefinitionRegistry}
     * @see ApplicationListener
     * @see AnnotatedBeanDefinitionRegistryUtils#registerBeans(BeanDefinitionRegistry, Class[])
     * @since 2.7.5
     */
    private void registerApplicationListeners(BeanDefinitionRegistry registry) {
        registerBeans(registry, DubboLifecycleComponentApplicationListener.class);
        // 将监听容器的刷新和关闭事件
        registerBeans(registry, DubboBootstrapApplicationListener.class);
    }

    /**
     * Register the processors for the Spring Annotation-Driven features
     *
     * @param registry {@link BeanDefinitionRegistry}
     * @see AnnotationConfigUtils
     * @since 2.7.5
     */
    private void registerAnnotationConfigProcessors(BeanDefinitionRegistry registry) {
        AnnotationConfigUtils.registerAnnotationConfigProcessors(registry);
    }
}
