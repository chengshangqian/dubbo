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
package org.apache.dubbo.config;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.CompositeConfiguration;
import org.apache.dubbo.common.config.Environment;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.MethodUtils;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.AsyncMethodInfo;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.utils.ReflectUtils.findMethodByMethodSignature;

/**
 * Utility methods and public methods for parsing configuration
 *
 * Utility 实用
 *
 * 配置抽象类，主要包括一些初始化配置的实用和公共方法
 *
 * @export
 */
public abstract class AbstractConfig implements Serializable {

    // 日志
    protected static final Logger logger = LoggerFactory.getLogger(AbstractConfig.class);

    // 序列化版本唯一id
    private static final long serialVersionUID = 4267533505537413570L;

    /**
     * The legacy properties container
     *
     * legacy 遗产，遗留
     *
     * 遗留属性集，旧的属性集，key是新属性，value为对应的旧属性
     *
     */
    private static final Map<String, String> LEGACY_PROPERTIES = new HashMap<String, String>();

    /**
     * The suffix container
     *
     * suffix （类名）后缀
     *
     * 后缀容器
     */
    private static final String[] SUFFIXES = new String[]{"Config", "Bean", "ConfigBase"};

    static {
        // 协议名称
        LEGACY_PROPERTIES.put("dubbo.protocol.name", "dubbo.service.protocol");
        // 协议主机地址
        LEGACY_PROPERTIES.put("dubbo.protocol.host", "dubbo.service.server.host");
        // 协议主机端口
        LEGACY_PROPERTIES.put("dubbo.protocol.port", "dubbo.service.server.port");
        // 协议最大线程数
        LEGACY_PROPERTIES.put("dubbo.protocol.threads", "dubbo.service.max.thread.pool.size");
        // 消费者调用超时时间
        LEGACY_PROPERTIES.put("dubbo.consumer.timeout", "dubbo.service.invoke.timeout");
        // 消费者重试次数
        LEGACY_PROPERTIES.put("dubbo.consumer.retries", "dubbo.service.max.retry.providers");
        // 消费者是否允许无提供者
        LEGACY_PROPERTIES.put("dubbo.consumer.check", "dubbo.service.allow.no.provider");
        // 服务URL地址
        LEGACY_PROPERTIES.put("dubbo.service.url", "dubbo.service.address");
    }

    /**
     * The config id
     *
     * 配置id
     */
    protected String id;

    // （参数名称）前缀
    protected String prefix;

    // 配置是否已刷新
    protected final AtomicBoolean refreshed = new AtomicBoolean(false);

    /**
     * 转换遗留属性值
     *
     * @param key 遗留属性键
     * @param value 遗留属性原始值
     * @return 遗留属性转换后的值
     */
    private static String convertLegacyValue(String key, String value) {
        if (value != null && value.length() > 0) {
            if ("dubbo.service.max.retry.providers".equals(key)) {
                return String.valueOf(Integer.parseInt(value) - 1);
            } else if ("dubbo.service.allow.no.provider".equals(key)) {
                return String.valueOf(!Boolean.parseBoolean(value));
            }
        }
        return value;
    }

    /**
     * 获取类型的标签名
     *
     * @param cls 类型
     * @return
     */
    public static String getTagName(Class<?> cls) {
        // 获取类型标签：简单类名
        String tag = cls.getSimpleName();

        // 检查标签名后缀
        for (String suffix : SUFFIXES) {
            // 如果是SUFFIXES中的一种
            if (tag.endsWith(suffix)) {
                // 去掉后缀
                tag = tag.substring(0, tag.length() - suffix.length());
                break;
            }
        }

        // 将驼峰式命名转换为分隔符-分割的小写命名，比如 MyName -> my-name，myName -> my-name,...
        return StringUtils.camelToSplitName(tag, "-");
    }

    /**
     * 拼接参数
     *
     * @param parameters 参数集
     * @param config 配置
     */
    public static void appendParameters(Map<String, String> parameters, Object config) {
        appendParameters(parameters, config, null);
    }

    /**
     * 通过反射，用指定的前缀拼接参数（参数名key和参数值）
     *
     * @param parameters 参数集
     * @param config 配置对象
     * @param prefix 前缀
     */
    @SuppressWarnings("unchecked")
    public static void appendParameters(Map<String, String> parameters, Object config, String prefix) {
        if (config == null) {
            return;
        }

        // 获取配置的方法集合
        Method[] methods = config.getClass().getMethods();

        // 遍历方法集合
        for (Method method : methods) {
            try {
                // 方法名称
                String name = method.getName();

                // 基本类型或简单类型的getter方法
                if (MethodUtils.isGetter(method)) {

                    // 方法上有@Parameter注解
                    Parameter parameter = method.getAnnotation(Parameter.class);

                    // 如果方法返回值为Object类型或Parameter注解且其excluded属性为true则忽略
                    if (method.getReturnType() == Object.class || parameter != null && parameter.excluded()) {
                        continue;
                    }

                    // 参数名
                    String key;

                    // 如果方法上有@Parameter注解且指定了参数名key
                    if (parameter != null && parameter.key().length() > 0) {
                        // 使用注解指定的参数名
                        key = parameter.key();
                    }
                    // 从方法命名计算参数名：了解参数的命名规则
                    else {
                        key = calculatePropertyFromGetter(name);
                    }
                    
                    // 调用方法取得参数值
                    Object value = method.invoke(config);

                    // 转为字符串并去除两头空格
                    String str = String.valueOf(value).trim();

                    /* 保存参数 */
                    // 如果有值
                    if (value != null && str.length() > 0) {
                        // 如果方法上有@Parameter注解其escaped为true，对值进行编码
                        if (parameter != null && parameter.escaped()) {
                            str = URL.encode(str);
                        }

                        // 如果方法上有@Parameter注解其append为true，对现有参数的（前）值进行拼接（方法重载？)
                        if (parameter != null && parameter.append()) {
                            // 从参数集合获取前值（可能是有多个相同名称的方法，获得前一个放入参数集合中的值？)
                            String pre = parameters.get(key);
                            // 如果有，使用,拼接值
                            if (pre != null && pre.length() > 0) {
                                str = pre + "," + str;
                            }
                        }

                        // 参数拼接前缀
                        if (prefix != null && prefix.length() > 0) {
                            key = prefix + "." + key;
                        }

                        // 将参数名和值放入参数集合中
                        parameters.put(key, str);
                    }
                    // 如果值为空，但方法上有@Parameter注解且其required为true，则抛出异常
                    // 即被注解为参数必须有值的参数却获取不到值，则抛出异常
                    else if (parameter != null && parameter.required()) {
                        throw new IllegalStateException(config.getClass().getSimpleName() + "." + key + " == null");
                    }
                }
                // 集合类型参数getter方法
                else if (isParametersGetter(method)) {
                    Map<String, String> map = (Map<String, String>) method.invoke(config, new Object[0]);
                    parameters.putAll(convert(map, prefix));
                }
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
    }

    /**
     * 拼接属性
     *
     * @param parameters
     * @param config
     */
    @Deprecated
    protected static void appendAttributes(Map<String, Object> parameters, Object config) {
        appendAttributes(parameters, config, null);
    }

    /**
     * 拼接属性
     *
     * @param parameters
     * @param config
     * @param prefix
     */
    @Deprecated
    protected static void appendAttributes(Map<String, Object> parameters, Object config, String prefix) {
        if (config == null) {
            return;
        }
        Method[] methods = config.getClass().getMethods();
        for (Method method : methods) {
            try {
                Parameter parameter = method.getAnnotation(Parameter.class);
                if (parameter == null || !parameter.attribute()) {
                    continue;
                }
                String name = method.getName();
                if (MethodUtils.isGetter(method)) {
                    String key;
                    if (parameter.key().length() > 0) {
                        key = parameter.key();
                    } else {
                        key = calculateAttributeFromGetter(name);
                    }
                    Object value = method.invoke(config);
                    if (value != null) {
                        if (prefix != null && prefix.length() > 0) {
                            key = prefix + "." + key;
                        }
                        parameters.put(key, value);
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
    }

    /**
     * 转换方法配置为异步方法信息
     *
     * @param methodConfig
     * @return
     */
    protected static AsyncMethodInfo convertMethodConfig2AsyncInfo(MethodConfig methodConfig) {
        if (methodConfig == null || (methodConfig.getOninvoke() == null && methodConfig.getOnreturn() == null && methodConfig.getOnthrow() == null)) {
            return null;
        }

        //check config conflict
        if (Boolean.FALSE.equals(methodConfig.isReturn()) && (methodConfig.getOnreturn() != null || methodConfig.getOnthrow() != null)) {
            throw new IllegalStateException("method config error : return attribute must be set true when onreturn or onthrow has been set.");
        }

        AsyncMethodInfo asyncMethodInfo = new AsyncMethodInfo();

        asyncMethodInfo.setOninvokeInstance(methodConfig.getOninvoke());
        asyncMethodInfo.setOnreturnInstance(methodConfig.getOnreturn());
        asyncMethodInfo.setOnthrowInstance(methodConfig.getOnthrow());

        try {
            String oninvokeMethod = methodConfig.getOninvokeMethod();
            if (StringUtils.isNotEmpty(oninvokeMethod)) {
                asyncMethodInfo.setOninvokeMethod(getMethodByName(methodConfig.getOninvoke().getClass(), oninvokeMethod));
            }

            String onreturnMethod = methodConfig.getOnreturnMethod();
            if (StringUtils.isNotEmpty(onreturnMethod)) {
                asyncMethodInfo.setOnreturnMethod(getMethodByName(methodConfig.getOnreturn().getClass(), onreturnMethod));
            }

            String onthrowMethod = methodConfig.getOnthrowMethod();
            if (StringUtils.isNotEmpty(onthrowMethod)) {
                asyncMethodInfo.setOnthrowMethod(getMethodByName(methodConfig.getOnthrow().getClass(), onthrowMethod));
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

        return asyncMethodInfo;
    }

    /**
     * 通过名字获取方法
     *
     * @param clazz
     * @param methodName
     * @return
     */
    private static Method getMethodByName(Class<?> clazz, String methodName) {
        try {
            return ReflectUtils.findMethodByMethodName(clazz, methodName);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 获取子属性集
     *
     * @param properties
     * @param prefix
     * @return
     */
    protected static Set<String> getSubProperties(Map<String, String> properties, String prefix) {
        return properties.keySet().stream().filter(k -> k.contains(prefix)).map(k -> {
            k = k.substring(prefix.length());
            return k.substring(0, k.indexOf("."));
        }).collect(Collectors.toSet());
    }

    /**
     * 通过名字提取属性
     *
     * @param clazz
     * @param setter
     * @return
     * @throws Exception
     */
    private static String extractPropertyName(Class<?> clazz, Method setter) throws Exception {
        String propertyName = setter.getName().substring("set".length());
        Method getter = null;
        try {
            getter = clazz.getMethod("get" + propertyName);
        } catch (NoSuchMethodException e) {
            getter = clazz.getMethod("is" + propertyName);
        }
        Parameter parameter = getter.getAnnotation(Parameter.class);
        if (parameter != null && StringUtils.isNotEmpty(parameter.key()) && parameter.useKeyAsProperty()) {
            propertyName = parameter.key();
        } else {
            propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
        }
        return propertyName;
    }

    /**
     * 从getter方法的方法名name中计算属性Property
     *
     * @param name
     * @return
     */
    private static String calculatePropertyFromGetter(String name) {
        // 去掉方法名中的get或is
        int i = name.startsWith("get") ? 3 : 2;

        // 将驼峰式名称转为使用.分割的属性名
        return StringUtils.camelToSplitName(name.substring(i, i + 1).toLowerCase() + name.substring(i + 1), ".");
    }

    /**
     * 从getter方法中计算属性Attribute
     *
     * @param getter
     * @return
     */
    private static String calculateAttributeFromGetter(String getter) {
        int i = getter.startsWith("get") ? 3 : 2;
        return getter.substring(i, i + 1).toLowerCase() + getter.substring(i + 1);
    }

    /**
     * 调用SetParameters方法
     *
     * @param c
     * @param o
     * @param map
     */
    private static void invokeSetParameters(Class c, Object o, Map map) {
        try {
            Method method = findMethodByMethodSignature(c, "setParameters", new String[]{Map.class.getName()});
            if (method != null && isParametersSetter(method)) {
                method.invoke(o, map);
            }
        } catch (Throwable t) {
            // ignore
        }
    }

    /**
     * 调用GetParameters方法
     *
     * @param c
     * @param o
     * @return
     */
    private static Map<String, String> invokeGetParameters(Class c, Object o) {
        try {
            Method method = findMethodByMethodSignature(c, "getParameters", null);
            if (method != null && isParametersGetter(method)) {
                return (Map<String, String>) method.invoke(o);
            }
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }

    /**
     * 参数集合类型的getter方法：getParameters
     *
     * @param method
     * @return
     */
    private static boolean isParametersGetter(Method method) {
        String name = method.getName();
        return ("getParameters".equals(name)
                && Modifier.isPublic(method.getModifiers())
                && method.getParameterTypes().length == 0
                && method.getReturnType() == Map.class);
    }

    /**
     * 是否是setParameters方法
     *
     * @param method
     * @return
     */
    private static boolean isParametersSetter(Method method) {
        return ("setParameters".equals(method.getName())
                && Modifier.isPublic(method.getModifiers())
                && method.getParameterCount() == 1
                && Map.class == method.getParameterTypes()[0]
                && method.getReturnType() == void.class);
    }

    /**
     * 转换参数集合
     *
     * @param parameters
     * @param prefix
     * @return
     */
    private static Map<String, String> convert(Map<String, String> parameters, String prefix) {
        if (parameters == null || parameters.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new HashMap<>();

        // pre = prefix + "." | "";
        String pre = (prefix != null && prefix.length() > 0 ? prefix + "." : "");
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            result.put(pre + key, value);
            // For compatibility, key like "registry-type" will has a duplicate key "registry.type"
            if (key.contains("-")) {
                result.put(pre + key.replace('-', '.'), value);
            }
        }
        return result;
    }

    /**
     * 获取配置的id
     *
     * @return
     */
    @Parameter(excluded = true)
    public String getId() {
        return id;
    }

    /**
     * 设置配置的id
     *
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 更新配置id
     *
     * absent 缺席、不存在
     * @param value
     */
    public void updateIdIfAbsent(String value) {
        // 如果value有值，并且id为空
        if (StringUtils.isNotEmpty(value) && StringUtils.isEmpty(id)) {
            this.id = value;
        }
    }

    /**
     *
     * @param annotationClass
     * @param annotation
     */
    protected void appendAnnotation(Class<?> annotationClass, Object annotation) {
        Method[] methods = annotationClass.getMethods();
        for (Method method : methods) {
            if (method.getDeclaringClass() != Object.class
                    && method.getReturnType() != void.class
                    && method.getParameterTypes().length == 0
                    && Modifier.isPublic(method.getModifiers())
                    && !Modifier.isStatic(method.getModifiers())) {
                try {
                    String property = method.getName();
                    if ("interfaceClass".equals(property) || "interfaceName".equals(property)) {
                        property = "interface";
                    }
                    String setter = "set" + property.substring(0, 1).toUpperCase() + property.substring(1);
                    Object value = method.invoke(annotation);
                    if (value != null && !value.equals(method.getDefaultValue())) {
                        Class<?> parameterType = ReflectUtils.getBoxedClass(method.getReturnType());
                        if ("filter".equals(property) || "listener".equals(property)) {
                            parameterType = String.class;
                            value = StringUtils.join((String[]) value, ",");
                        } else if ("parameters".equals(property)) {
                            parameterType = Map.class;
                            value = CollectionUtils.toStringMap((String[]) value);
                        }
                        try {
                            Method setterMethod = getClass().getMethod(setter, parameterType);
                            setterMethod.invoke(this, value);
                        } catch (NoSuchMethodException e) {
                            // ignore
                        }
                    }
                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 获取【配置】元数据，其功能可参考{@link AbstractConfig#appendParameters(Map, Object, String)}方法
     *
     * Should be called after Config was fully initialized.
     *
     * 此方法应在配置完全初始化后调用
     *
     * // FIXME: this method should be completely replaced by appendParameters
     *
     * // 此方法应完全替换为appendParameters方法，见{@link AbstractConfig#appendParameters(Map, Object, String)}
     *
     * @return
     * @see AbstractConfig#appendParameters(Map, Object, String)
     * <p>
     * Notice! This method should include all properties in the returning map, treat @Parameter differently compared to appendParameters.
     *
     * 注意！此方法应在返回的映射中包含所有属性，将@Parameter与appendParameters区别对待。
     */
    public Map<String, String> getMetaData() {
        Map<String, String> metaData = new HashMap<>();
        Method[] methods = this.getClass().getMethods();
        for (Method method : methods) {
            try {
                String name = method.getName();
                if (MethodUtils.isMetaMethod(method)) {
                    String key;
                    Parameter parameter = method.getAnnotation(Parameter.class);
                    if (parameter != null && parameter.key().length() > 0 && parameter.useKeyAsProperty()) {
                        key = parameter.key();
                    } else {
                        key = calculateAttributeFromGetter(name);
                    }
                    // treat url and configuration differently, the value should always present in configuration though it may not need to present in url.
                    //if (method.getReturnType() == Object.class || parameter != null && parameter.excluded()) {
                    if (method.getReturnType() == Object.class) {
                        metaData.put(key, null);
                        continue;
                    }

                    /**
                     * Attributes annotated as deprecated should not override newly added replacement.
                     */
                    if (MethodUtils.isDeprecated(method) && metaData.get(key) != null) {
                        continue;
                    }

                    Object value = method.invoke(this);
                    String str = String.valueOf(value).trim();
                    if (value != null && str.length() > 0) {
                        metaData.put(key, str);
                    } else {
                        metaData.put(key, null);
                    }
                } else if (isParametersGetter(method)) {
                    Map<String, String> map = (Map<String, String>) method.invoke(this, new Object[0]);
                    metaData.putAll(convert(map, ""));
                }
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
        return metaData;
    }

    /**
     * 获取配置的参数前缀：默认是dubbo.配置类具体实例标签名
     *
     * @return
     */
    @Parameter(excluded = true)
    public String getPrefix() {
        return StringUtils.isNotEmpty(prefix) ? prefix : (CommonConstants.DUBBO + "." + getTagName(this.getClass()));
    }

    /**
     * 设置前缀
     *
     * @param prefix
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * 刷新配置
     */
    public void refresh() {
        // 环境变量
        Environment env = ApplicationModel.getEnvironment();
        try {
            // 组合配置
            CompositeConfiguration compositeConfiguration = env.getPrefixedConfiguration(this);

            // loop methods, get override value and set the new value back to method
            // 遍历所有方法，通过setter方法设置配置的所有参数值以完成配置更新
            Method[] methods = getClass().getMethods();
            for (Method method : methods) {

                // 基本类型或简单类型的setter方法
                if (MethodUtils.isSetter(method)) {
                    try {
                        String value = StringUtils.trim(compositeConfiguration.getString(extractPropertyName(getClass(), method)));
                        // isTypeMatch() is called to avoid duplicate and incorrect update, for example, we have two 'setGeneric' methods in ReferenceConfig.
                        if (StringUtils.isNotEmpty(value) && ClassUtils.isTypeMatch(method.getParameterTypes()[0], value)) {
                            method.invoke(this, ClassUtils.convertPrimitive(method.getParameterTypes()[0], value));
                        }
                    } catch (NoSuchMethodException e) {
                        logger.info("Failed to override the property " + method.getName() + " in " +
                                this.getClass().getSimpleName() +
                                ", please make sure every property has getter/setter method provided.");
                    }
                }

                // 参数集合类型的setter方法
                else if (isParametersSetter(method)) {
                    String value = StringUtils.trim(compositeConfiguration.getString(extractPropertyName(getClass(), method)));
                    if (StringUtils.isNotEmpty(value)) {
                        Map<String, String> map = invokeGetParameters(getClass(), this);
                        map = map == null ? new HashMap<>() : map;
                        map.putAll(convert(StringUtils.parseParameters(value), ""));
                        invokeSetParameters(getClass(), this, map);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to override ", e);
        }
    }

    /**
     * 重新toString方法
     * @return
     */
    @Override
    public String toString() {
        try {
            StringBuilder buf = new StringBuilder();
            buf.append("<dubbo:");
            buf.append(getTagName(getClass()));
            Method[] methods = getClass().getMethods();
            for (Method method : methods) {
                try {
                    if (MethodUtils.isGetter(method)) {
                        String name = method.getName();
                        String key = calculateAttributeFromGetter(name);

                        try {
                            getClass().getDeclaredField(key);
                        } catch (NoSuchFieldException e) {
                            // ignore
                            continue;
                        }

                        Object value = method.invoke(this);
                        if (value != null) {
                            buf.append(" ");
                            buf.append(key);
                            buf.append("=\"");
                            buf.append(value);
                            buf.append("\"");
                        }
                    }
                } catch (Exception e) {
                    logger.warn(e.getMessage(), e);
                }
            }
            buf.append(" />");
            return buf.toString();
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
            return super.toString();
        }
    }

    /**
     * 是否有效
     *
     * FIXME check @Parameter(required=true) and any conditions that need to match.
     */
    @Parameter(excluded = true)
    public boolean isValid() {
        return true;
    }

    /**
     * 重写equals方法
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj.getClass().getName().equals(this.getClass().getName()))) {
            return false;
        }

        Method[] methods = this.getClass().getMethods();
        for (Method method1 : methods) {
            if (MethodUtils.isGetter(method1)) {
                Parameter parameter = method1.getAnnotation(Parameter.class);
                if (parameter != null && parameter.excluded()) {
                    continue;
                }
                try {
                    Method method2 = obj.getClass().getMethod(method1.getName(), method1.getParameterTypes());
                    Object value1 = method1.invoke(this, new Object[]{});
                    Object value2 = method2.invoke(obj, new Object[]{});
                    if (!Objects.equals(value1, value2)) {
                        return false;
                    }
                } catch (Exception e) {
                    return true;
                }
            }
        }
        return true;
    }

    /**
     * 将一个配置实例（当前类的具体子类实例）添加到配置管理器中
     *
     * Add {@link AbstractConfig instance} into {@link ConfigManager}
     * <p>
     * Current method will invoked by Spring or Java EE container automatically, or should be triggered manually.
     *
     * @see ConfigManager#addConfig(AbstractConfig)
     * @since 2.7.5
     */
    @PostConstruct
    public void addIntoConfigManager() {
        ApplicationModel.getConfigManager().addConfig(this);
    }

    /**
     * 重写hashCode方法
     *
     * @return
     */
    @Override
    public int hashCode() {
        int hashCode = 1;

        Method[] methods = this.getClass().getMethods();
        for (Method method : methods) {
            if (MethodUtils.isGetter(method)) {
                Parameter parameter = method.getAnnotation(Parameter.class);
                if (parameter != null && parameter.excluded()) {
                    continue;
                }
                try {
                    Object value = method.invoke(this, new Object[]{});
                    hashCode = 31 * hashCode + value.hashCode();
                } catch (Exception ignored) {
                    //ignored
                }
            }
        }

        if (hashCode == 0) {
            hashCode = 1;
        }

        return hashCode;
    }
}
