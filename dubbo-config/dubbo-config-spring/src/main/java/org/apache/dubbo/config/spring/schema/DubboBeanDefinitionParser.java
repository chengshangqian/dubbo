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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.AbstractServiceConfig;
import org.apache.dubbo.config.ArgumentConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.config.spring.beans.factory.annotation.DubboConfigAliasPostProcessor;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.env.Environment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static com.alibaba.spring.util.BeanRegistrar.registerInfrastructureBean;
import static org.apache.dubbo.common.constants.CommonConstants.HIDE_KEY_PREFIX;

/**
 * dubbo的bean定义解析器：实现spring的bean定义解析器
 *
 * AbstractBeanDefinitionParser
 *
 * @export
 */
public class DubboBeanDefinitionParser implements BeanDefinitionParser {

    private static final Logger logger = LoggerFactory.getLogger(DubboBeanDefinitionParser.class);
    private static final Pattern GROUP_AND_VERSION = Pattern.compile("^[\\-.0-9_a-zA-Z]+(\\:[\\-.0-9_a-zA-Z]+)?$");
    private static final String ONRETURN = "onreturn";
    private static final String ONTHROW = "onthrow";
    private static final String ONINVOKE = "oninvoke";
    private static final String METHOD = "Method";

    // bean的类型
    private final Class<?> beanClass;
    // 当前标签（bean）是否必须有id属性
    private final boolean required;

    /**
     * 构造dubbo的bean定义解析器
     *
     * @param beanClass
     * @param required
     */
    public DubboBeanDefinitionParser(Class<?> beanClass, boolean required) {
        this.beanClass = beanClass;
        this.required = required;
    }

    /**
     * 解析dubbo的bean定义
     *
     * @param element xml元素对象
     * @param parserContext 解析上下文：所有dubbo标签以及相关信息
     * @param beanClass bean的类型
     * @param required 当前标签（bean）是否必须有id属性
     * @return
     */
    @SuppressWarnings("unchecked")
    private static BeanDefinition parse(Element element, ParserContext parserContext, Class<?> beanClass, boolean required) {
        // spring中的beanDefinition对象
        // ----------- 1. bean的基本信息：初始化解析对象 -----------
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(beanClass);
        beanDefinition.setLazyInit(false);

        // ----------- 2. 解决id属性问题：要求有id但为空 -----------
        // 获取属性id：与系统属性合并，如果有的话?
        String id = resolveAttribute(element, "id", parserContext);
        // 处理要求有id但id为空的情况
        if (StringUtils.isEmpty(id) && required) {
            // 获取bean的name属性
            String generatedBeanName = resolveAttribute(element, "name", parserContext);
            // 如果name也为空
            if (StringUtils.isEmpty(generatedBeanName)) {
                // 如果当前解析的是protocol标签，使用默认值dubbo
                if (ProtocolConfig.class.equals(beanClass)) {
                    generatedBeanName = "dubbo";
                }
                // 其它标签，则尝试获取interface属性值作为bean名称
                else {
                    generatedBeanName = resolveAttribute(element, "interface", parserContext);
                }
            }

            // 如果bean名称还是空
            if (StringUtils.isEmpty(generatedBeanName)) {
                // 尝试使用bean类的全限定名
                generatedBeanName = beanClass.getName();
            }

            // id使用bean名称
            id = generatedBeanName;

            // 再检查是否有重复：id需要保证唯一，如果上下文中已经存在该id，则在id中添加数字后缀再检查
            int counter = 2;
            while (parserContext.getRegistry().containsBeanDefinition(id)) {
                id = generatedBeanName + (counter++);
            }
        }

        // ----------- 3. 将id属性写入bean定义对象即解析对象：id不为空则写入，id允许为空且确实没有配置的情况则不会写入 -----------
        // 除了reference元素外，其它的元素目前都要求id属性
        if (StringUtils.isNotEmpty(id)) {
            // 如果自行指定的id重复，抛出异常
            if (parserContext.getRegistry().containsBeanDefinition(id)) {
                throw new IllegalStateException("Duplicate spring bean id " + id);
            }
            // 将bean定义对象注册到上下文的bean工厂中（org.springframework.beans.factory.support.DefaultListableBeanFactory的实例），
            parserContext.getRegistry().registerBeanDefinition(id, beanDefinition);
            // 同时将id属性保存到bean定义对象中
            beanDefinition.getPropertyValues().addPropertyValue("id", id);
        }

        // ----------- 4.部分标签的特殊处理 -----------
        // 处理protocol标签
        if (ProtocolConfig.class.equals(beanClass)) {
            for (String name : parserContext.getRegistry().getBeanDefinitionNames()) {
                BeanDefinition definition = parserContext.getRegistry().getBeanDefinition(name);
                PropertyValue property = definition.getPropertyValues().getPropertyValue("protocol");
                if (property != null) {
                    Object value = property.getValue();
                    if (value instanceof ProtocolConfig && id.equals(((ProtocolConfig) value).getName())) {
                        definition.getPropertyValues().addPropertyValue("protocol", new RuntimeBeanReference(id));
                    }
                }
            }
        }
        // 处理service标签
        else if (ServiceBean.class.equals(beanClass)) {
            String className = resolveAttribute(element, "class", parserContext);
            if (StringUtils.isNotEmpty(className)) {
                RootBeanDefinition classDefinition = new RootBeanDefinition();
                classDefinition.setBeanClass(ReflectUtils.forName(className));
                classDefinition.setLazyInit(false);
                parseProperties(element.getChildNodes(), classDefinition, parserContext);
                beanDefinition.getPropertyValues().addPropertyValue("ref", new BeanDefinitionHolder(classDefinition, id + "Impl"));
            }
        }
        // 处理provider标签：嵌套
        else if (ProviderConfig.class.equals(beanClass)) {
            parseNested(element, parserContext, ServiceBean.class, true, "service", "provider", id, beanDefinition);
        }
        // 处理consumer标签：嵌套
        else if (ConsumerConfig.class.equals(beanClass)) {
            parseNested(element, parserContext, ReferenceBean.class, false, "reference", "consumer", id, beanDefinition);
        }

        // ----------- 5.通用标签的处理 -----------
        // 所有属性名集合
        Set<String> props = new HashSet<>();
        // 属性参数集合（键值对）
        ManagedMap parameters = null;

        // 解析具有规范的getter/setter方法的属性，除了parameters属性，其它单独的属性保存到bean对象中
        for (Method setter : beanClass.getMethods()) {
            // 方法名称
            String name = setter.getName();
            // 如果是setter方法：名字大于3个字符，以set开头，且参数只有1个的公共方法
            if (name.length() > 3 && name.startsWith("set")
                    && Modifier.isPublic(setter.getModifiers())
                    && setter.getParameterTypes().length == 1) {

                // 获取参数类型
                Class<?> type = setter.getParameterTypes()[0];

                // 属性名
                String beanProperty = name.substring(3, 4).toLowerCase() + name.substring(4);

                // 使用-格式化属性名：例如，MyName 将被解析为 my-name，其中-是分隔符
                String property = StringUtils.camelToSplitName(beanProperty, "-");

                // 加入属性名字集合中
                props.add(property);

                // 检查是否存在匹配的getter方法
                // check the setter/getter whether match
                Method getter = null;
                try {
                    getter = beanClass.getMethod("get" + name.substring(3), new Class<?>[0]);
                } catch (NoSuchMethodException e) {
                    try {
                        getter = beanClass.getMethod("is" + name.substring(3), new Class<?>[0]);
                    } catch (NoSuchMethodException e2) {
                        // ignore, there is no need any log here since some class implement the interface: EnvironmentAware,
                        // ApplicationAware, etc. They only have setter method, otherwise will cause the error log during application start up.
                    }
                }

                // 如果没有符合的getter方法，则遍历查找下一个getter方法
                if (getter == null
                        || !Modifier.isPublic(getter.getModifiers())
                        || !type.equals(getter.getReturnType())) {
                    continue;
                }

                // 解析parameters属性：保存到parameters中
                if ("parameters".equals(property)) {
                    parameters = parseParameters(element.getChildNodes(), beanDefinition, parserContext);
                }
                // 解析methods属性
                else if ("methods".equals(property)) {
                    parseMethods(id, element.getChildNodes(), beanDefinition, parserContext);
                }
                // 解析arguments属性
                else if ("arguments".equals(property)) {
                    parseArguments(id, element.getChildNodes(), beanDefinition, parserContext);
                }
                // 解析其它特殊的属性
                else {
                    // 获取属性的值，如果有配置对应的属性值，再进行处理
                    String value = resolveAttribute(element, property, parserContext);
                    if (value != null) {
                        value = value.trim();
                        if (value.length() > 0) {
                            // 解析registry属性
                            if ("registry".equals(property) && RegistryConfig.NO_AVAILABLE.equalsIgnoreCase(value)) {
                                RegistryConfig registryConfig = new RegistryConfig();
                                registryConfig.setAddress(RegistryConfig.NO_AVAILABLE);
                                beanDefinition.getPropertyValues().addPropertyValue(beanProperty, registryConfig);
                            }
                            // 解析provider、registry、protocol属性
                            else if ("provider".equals(property) || "registry".equals(property) || ("protocol".equals(property) && AbstractServiceConfig.class.isAssignableFrom(beanClass))) {
                                /**
                                 * For 'provider' 'protocol' 'registry', keep literal value (should be id/name) and set the value to 'registryIds' 'providerIds' protocolIds'
                                 * The following process should make sure each id refers to the corresponding instance, here's how to find the instance for different use cases:
                                 * 1. Spring, check existing bean by id, see{@link ServiceBean#afterPropertiesSet()}; then try to use id to find configs defined in remote Config Center
                                 * 2. API, directly use id to find configs defined in remote Config Center; if all config instances are defined locally, please use {@link ServiceConfig#setRegistries(List)}
                                 */
                                beanDefinition.getPropertyValues().addPropertyValue(beanProperty + "Ids", value);
                            }
                            // 解析其它特殊书属性如async、timeout、delay、version、stat、reliable、
                            // onreturn、onthrow、oninvoke等
                            else {
                                Object reference;
                                // 如果基本类型
                                if (isPrimitive(type)) {
                                    // 兼容处理async、timeout、delay、version、stat、reliable属性的缺省值
                                    if ("async".equals(property) && "false".equals(value)
                                            || "timeout".equals(property) && "0".equals(value)
                                            || "delay".equals(property) && "0".equals(value)
                                            || "version".equals(property) && "0.0.0".equals(value)
                                            || "stat".equals(property) && "-1".equals(value)
                                            || "reliable".equals(property) && "false".equals(value)) {
                                        // backward compatibility for the default value in old version's xsd
                                        value = null;
                                    }
                                    reference = value;
                                }
                                // 解析onreturn、onthrow、oninvoke属性
                                else if (ONRETURN.equals(property) || ONTHROW.equals(property) || ONINVOKE.equals(property)) {
                                    int index = value.lastIndexOf(".");
                                    String ref = value.substring(0, index);
                                    String method = value.substring(index + 1);
                                    reference = new RuntimeBeanReference(ref);
                                    beanDefinition.getPropertyValues().addPropertyValue(property + METHOD, method);
                                }
                                // 解析其它具有getter/setter的属性
                                else {
                                    if ("ref".equals(property) && parserContext.getRegistry().containsBeanDefinition(value)) {
                                        BeanDefinition refBean = parserContext.getRegistry().getBeanDefinition(value);
                                        // 服务的bean必须是单例
                                        if (!refBean.isSingleton()) {
                                            throw new IllegalStateException("The exported service ref " + value + " must be singleton! Please set the " + value + " bean scope to singleton, eg: <bean id=\"" + value + "\" scope=\"singleton\" ...>");
                                        }
                                    }
                                    reference = new RuntimeBeanReference(value);
                                }
                                // 保存到bean定义对象中
                                beanDefinition.getPropertyValues().addPropertyValue(beanProperty, reference);
                            }
                        }
                    }
                }
            }
        }

        // 解析剩余的属性，保存到bean定义对象中的parameters属性中
        NamedNodeMap attributes = element.getAttributes();
        int len = attributes.getLength();
        // 遍历解析所有属性
        for (int i = 0; i < len; i++) {
            Node node = attributes.item(i);
            String name = node.getLocalName();
            // 如果属性还未解析过，则解析并保存到parameters属性参数集合中
            if (!props.contains(name)) {
                if (parameters == null) {
                    parameters = new ManagedMap();
                }
                String value = node.getNodeValue();
                parameters.put(name, new TypedStringValue(value, String.class));
            }
        }

        // 最后将parameters保存到bean对象中的parameters属性中
        if (parameters != null) {
            beanDefinition.getPropertyValues().addPropertyValue("parameters", parameters);
        }

        return beanDefinition;
    }

    private static boolean isPrimitive(Class<?> cls) {
        return cls.isPrimitive() || cls == Boolean.class || cls == Byte.class
                || cls == Character.class || cls == Short.class || cls == Integer.class
                || cls == Long.class || cls == Float.class || cls == Double.class
                || cls == String.class || cls == Date.class || cls == Class.class;
    }

    private static void parseNested(Element element, ParserContext parserContext, Class<?> beanClass, boolean required, String tag, String property, String ref, BeanDefinition beanDefinition) {
        NodeList nodeList = element.getChildNodes();
        if (nodeList == null) {
            return;
        }
        boolean first = true;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (!(node instanceof Element)) {
                continue;
            }
            if (tag.equals(node.getNodeName())
                    || tag.equals(node.getLocalName())) {
                if (first) {
                    first = false;
                    String isDefault = resolveAttribute(element, "default", parserContext);
                    if (StringUtils.isEmpty(isDefault)) {
                        beanDefinition.getPropertyValues().addPropertyValue("default", "false");
                    }
                }
                BeanDefinition subDefinition = parse((Element) node, parserContext, beanClass, required);
                if (subDefinition != null && StringUtils.isNotEmpty(ref)) {
                    subDefinition.getPropertyValues().addPropertyValue(property, new RuntimeBeanReference(ref));
                }
            }
        }
    }

    private static void parseProperties(NodeList nodeList, RootBeanDefinition beanDefinition, ParserContext parserContext) {
        if (nodeList == null) {
            return;
        }
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (!(nodeList.item(i) instanceof Element)) {
                continue;
            }
            Element element = (Element) nodeList.item(i);
            if ("property".equals(element.getNodeName())
                    || "property".equals(element.getLocalName())) {
                String name = resolveAttribute(element, "name", parserContext);
                if (StringUtils.isNotEmpty(name)) {
                    String value = resolveAttribute(element, "value", parserContext);
                    String ref = resolveAttribute(element, "ref", parserContext);
                    if (StringUtils.isNotEmpty(value)) {
                        beanDefinition.getPropertyValues().addPropertyValue(name, value);
                    } else if (StringUtils.isNotEmpty(ref)) {
                        beanDefinition.getPropertyValues().addPropertyValue(name, new RuntimeBeanReference(ref));
                    } else {
                        throw new UnsupportedOperationException("Unsupported <property name=\"" + name + "\"> sub tag, Only supported <property name=\"" + name + "\" ref=\"...\" /> or <property name=\"" + name + "\" value=\"...\" />");
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static ManagedMap parseParameters(NodeList nodeList, RootBeanDefinition beanDefinition, ParserContext parserContext) {
        if (nodeList == null) {
            return null;
        }
        ManagedMap parameters = null;
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (!(nodeList.item(i) instanceof Element)) {
                continue;
            }
            Element element = (Element) nodeList.item(i);
            if ("parameter".equals(element.getNodeName())
                    || "parameter".equals(element.getLocalName())) {
                if (parameters == null) {
                    parameters = new ManagedMap();
                }
                String key = resolveAttribute(element, "key", parserContext);
                String value = resolveAttribute(element, "value", parserContext);
                boolean hide = "true".equals(resolveAttribute(element, "hide", parserContext));
                if (hide) {
                    key = HIDE_KEY_PREFIX + key;
                }
                parameters.put(key, new TypedStringValue(value, String.class));
            }
        }
        return parameters;
    }

    @SuppressWarnings("unchecked")
    private static void parseMethods(String id, NodeList nodeList, RootBeanDefinition beanDefinition,
                                     ParserContext parserContext) {
        if (nodeList == null) {
            return;
        }
        ManagedList methods = null;
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (!(nodeList.item(i) instanceof Element)) {
                continue;
            }
            Element element = (Element) nodeList.item(i);
            if ("method".equals(element.getNodeName()) || "method".equals(element.getLocalName())) {
                String methodName = resolveAttribute(element, "name", parserContext);
                if (StringUtils.isEmpty(methodName)) {
                    throw new IllegalStateException("<dubbo:method> name attribute == null");
                }
                if (methods == null) {
                    methods = new ManagedList();
                }
                BeanDefinition methodBeanDefinition = parse(element,
                        parserContext, MethodConfig.class, false);
                String name = id + "." + methodName;
                BeanDefinitionHolder methodBeanDefinitionHolder = new BeanDefinitionHolder(
                        methodBeanDefinition, name);
                methods.add(methodBeanDefinitionHolder);
            }
        }
        if (methods != null) {
            beanDefinition.getPropertyValues().addPropertyValue("methods", methods);
        }
    }

    @SuppressWarnings("unchecked")
    private static void parseArguments(String id, NodeList nodeList, RootBeanDefinition beanDefinition,
                                       ParserContext parserContext) {
        if (nodeList == null) {
            return;
        }
        ManagedList arguments = null;
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (!(nodeList.item(i) instanceof Element)) {
                continue;
            }
            Element element = (Element) nodeList.item(i);
            if ("argument".equals(element.getNodeName()) || "argument".equals(element.getLocalName())) {
                String argumentIndex = resolveAttribute(element, "index", parserContext);
                if (arguments == null) {
                    arguments = new ManagedList();
                }
                BeanDefinition argumentBeanDefinition = parse(element,
                        parserContext, ArgumentConfig.class, false);
                String name = id + "." + argumentIndex;
                BeanDefinitionHolder argumentBeanDefinitionHolder = new BeanDefinitionHolder(
                        argumentBeanDefinition, name);
                arguments.add(argumentBeanDefinitionHolder);
            }
        }
        if (arguments != null) {
            beanDefinition.getPropertyValues().addPropertyValue("arguments", arguments);
        }
    }

    /**
     * 解析dubbo的bean定义入口：实现spring的bean定义解析器的parse方法
     *
     * @param element xml元素对象
     * @param parserContext 解析上下文：所有dubbo标签以及相关信息
     * @return
     */
    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        // Register DubboConfigAliasPostProcessor
        registerDubboConfigAliasPostProcessor(parserContext.getRegistry());

        return parse(element, parserContext, beanClass, required);
    }

    /**
     * Register {@link DubboConfigAliasPostProcessor}
     *
     * @param registry {@link BeanDefinitionRegistry}
     * @since 2.7.5 [Feature] https://github.com/apache/dubbo/issues/5093
     */
    private void registerDubboConfigAliasPostProcessor(BeanDefinitionRegistry registry) {
        registerInfrastructureBean(registry, DubboConfigAliasPostProcessor.BEAN_NAME, DubboConfigAliasPostProcessor.class);
    }

    private static String resolveAttribute(Element element, String attributeName, ParserContext parserContext) {
        String attributeValue = element.getAttribute(attributeName);
        Environment environment = parserContext.getReaderContext().getEnvironment();
        return environment.resolvePlaceholders(attributeValue);
    }
}
