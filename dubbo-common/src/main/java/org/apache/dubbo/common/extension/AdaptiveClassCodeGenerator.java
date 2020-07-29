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
package org.apache.dubbo.common.extension;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Code generator for Adaptive class
 */
public class AdaptiveClassCodeGenerator {

    private static final Logger logger = LoggerFactory.getLogger(AdaptiveClassCodeGenerator.class);

    private static final String CLASSNAME_INVOCATION = "org.apache.dubbo.rpc.Invocation";

    private static final String CODE_PACKAGE = "package %s;\n";

    private static final String CODE_IMPORTS = "import %s;\n";

    private static final String CODE_CLASS_DECLARATION = "public class %s$Adaptive implements %s {\n";

    private static final String CODE_METHOD_DECLARATION = "public %s %s(%s) %s {\n%s}\n";

    private static final String CODE_METHOD_ARGUMENT = "%s arg%d";

    private static final String CODE_METHOD_THROWS = "throws %s";

    private static final String CODE_UNSUPPORTED = "throw new UnsupportedOperationException(\"The method %s of interface %s is not adaptive method!\");\n";

    private static final String CODE_URL_NULL_CHECK = "if (arg%d == null) throw new IllegalArgumentException(\"url == null\");\n%s url = arg%d;\n";

    private static final String CODE_EXT_NAME_ASSIGNMENT = "String extName = %s;\n";

    private static final String CODE_EXT_NAME_NULL_CHECK = "if(extName == null) "
                    + "throw new IllegalStateException(\"Failed to get extension (%s) name from url (\" + url.toString() + \") use keys(%s)\");\n";

    private static final String CODE_INVOCATION_ARGUMENT_NULL_CHECK = "if (arg%d == null) throw new IllegalArgumentException(\"invocation == null\"); "
                    + "String methodName = arg%d.getMethodName();\n";


    private static final String CODE_EXTENSION_ASSIGNMENT = "%s extension = (%<s)%s.getExtensionLoader(%s.class).getExtension(extName);\n";

    private static final String CODE_EXTENSION_METHOD_INVOKE_ARGUMENT = "arg%d";

    private final Class<?> type;

    private String defaultExtName;

    public AdaptiveClassCodeGenerator(Class<?> type, String defaultExtName) {
        this.type = type;
        this.defaultExtName = defaultExtName;
    }

    /**
     * 至少有一个方法带有Adaptive注解
     *
     * test if given type has at least one method annotated with <code>Adaptive</code>
     */
    private boolean hasAdaptiveMethod() {
        return Arrays.stream(type.getMethods()).anyMatch(m -> m.isAnnotationPresent(Adaptive.class));
    }

    /**
     * 动态生成自适应类的代码：扫描指定扩展类型type，如果有方法中有使用Adaptive注解，将生成自适应类
     *
     * generate and return class code
     */
    public String generate() {
        // 如果指定的扩展类型不存在使用Adaptive注解的方法，不符合自适应类，不会生成自适应类代码，抛出异常
        // no need to generate adaptive class since there's no adaptive method found.
        if (!hasAdaptiveMethod()) {
            throw new IllegalStateException("No adaptive method exist on extension " + type.getName() + ", refuse to create the adaptive class!");
        }

        // 完整自适应类代码
        StringBuilder code = new StringBuilder();

        // 生成包、import、类型声明含实现的接口
        code.append(generatePackageInfo());
        code.append(generateImports());
        code.append(generateClassDeclaration());

        // 对Adaptive注解的方法进行生成
        Method[] methods = type.getMethods();
        for (Method method : methods) {
            code.append(generateMethod(method));
        }
        code.append("}");

        if (logger.isDebugEnabled()) {
            logger.debug(code.toString());
        }
        return code.toString();
    }

    /**
     * generate package info
     */
    private String generatePackageInfo() {
        return String.format(CODE_PACKAGE, type.getPackage().getName());
    }

    /**
     * generate imports
     */
    private String generateImports() {
        return String.format(CODE_IMPORTS, ExtensionLoader.class.getName());
    }

    /**
     * generate class declaration
     */
    private String generateClassDeclaration() {
        return String.format(CODE_CLASS_DECLARATION, type.getSimpleName(), type.getCanonicalName());
    }

    /**
     * generate method not annotated with Adaptive with throwing unsupported exception
     */
    private String generateUnsupported(Method method) {
        return String.format(CODE_UNSUPPORTED, method, type.getName());
    }

    /**
     * get index of parameter with type URL
     */
    private int getUrlTypeIndex(Method method) {
        int urlTypeIndex = -1;
        Class<?>[] pts = method.getParameterTypes();
        for (int i = 0; i < pts.length; ++i) {
            if (pts[i].equals(URL.class)) {
                urlTypeIndex = i;
                break;
            }
        }
        return urlTypeIndex;
    }

    /**
     * 自适应方法生成
     *
     * generate method declaration
     */
    private String generateMethod(Method method) {
        // 方法返回值类型和方法名
        String methodReturnType = method.getReturnType().getCanonicalName();
        String methodName = method.getName();

        // 方法体：核心分析
        String methodContent = generateMethodContent(method);

        // 方法参数
        String methodArgs = generateMethodArguments(method);

        // 方法异常
        String methodThrows = generateMethodThrows(method);

        return String.format(CODE_METHOD_DECLARATION, methodReturnType, methodName, methodArgs, methodThrows, methodContent);
    }

    /**
     * generate method arguments
     */
    private String generateMethodArguments(Method method) {
        Class<?>[] pts = method.getParameterTypes();
        return IntStream.range(0, pts.length)
                        .mapToObj(i -> String.format(CODE_METHOD_ARGUMENT, pts[i].getCanonicalName(), i))
                        .collect(Collectors.joining(", "));
    }

    /**
     * generate method throws
     */
    private String generateMethodThrows(Method method) {
        Class<?>[] ets = method.getExceptionTypes();
        if (ets.length > 0) {
            String list = Arrays.stream(ets).map(Class::getCanonicalName).collect(Collectors.joining(", "));
            return String.format(CODE_METHOD_THROWS, list);
        } else {
            return "";
        }
    }

    /**
     * generate method URL argument null check
     */
    private String generateUrlNullCheck(int index) {
        return String.format(CODE_URL_NULL_CHECK, index, URL.class.getName(), index);
    }

    /**
     * 生成方法内容
     *
     * generate method content
     */
    private String generateMethodContent(Method method) {
        // 判断方法是否含有Adaptive注解
        Adaptive adaptiveAnnotation = method.getAnnotation(Adaptive.class);

        StringBuilder code = new StringBuilder(512);

        // 如果不是Adaptive注解的方法
        if (adaptiveAnnotation == null) {
            // 自动生成的自适应类不支持的非Adaptive注解的方法：返回一个会抛出异常的方法体，如果调用的话
            return generateUnsupported(method);
        }

        // Adaptive注解的方法
        else {
            // 获取类型为URL的参数位置索引，如果有的话：
            // 生成的代码中，要求必须要有URL作判断，下面会通过直接或间接的方式生成获取URL的代码，如果没有将抛出异常
            int urlTypeIndex = getUrlTypeIndex(method);

            // found parameter in URL type
            // 如果方法中有URL类型的参数
            if (urlTypeIndex != -1) {
                // Null Point check
                // 添加对URL参数的空指针检查的代码
                code.append(generateUrlNullCheck(urlTypeIndex));
            }
            else {
                // did not find parameter in URL type
                // 如果方法不含有URL类型的参数，尝试检测（对象类型）参数中是否含有返回URL类型的方法：
                // 如果有，将生成通过调用方法参数的内部方法返回URL的代码
                // 即生成间接获取URL的代码，同时也会进行空指针检查
                code.append(generateUrlAssignmentIndirectly(method));
            }

            // 自适应参数值：即要匹配的扩展实现类的bean名称
            String[] value = getMethodAdaptiveValue(adaptiveAnnotation);

            // 是否含有Invocation类型的参数
            boolean hasInvocation = hasInvocationArgument(method);

            // Invocation参数控制键检查代码
            code.append(generateInvocationArgumentNullCheck(method));

            // 扩展实现类最终的类名extName
            code.append(generateExtNameAssignment(value, hasInvocation));

            // check extName == null?
            // 检查extName是否为空
            code.append(generateExtNameNullCheck(value));

            // 加载/缓存扩展实现类类型并获取扩展类实例
            code.append(generateExtensionAssignment());

            // return statement
            // 方法返回值语句，如果有的话
            code.append(generateReturnAndInvocation(method));
        }

        return code.toString();
    }

    /**
     * generate code for variable extName null check
     */
    private String generateExtNameNullCheck(String[] value) {
        return String.format(CODE_EXT_NAME_NULL_CHECK, type.getName(), Arrays.toString(value));
    }

    /**
     * generate extName assigment code
     */
    private String generateExtNameAssignment(String[] value, boolean hasInvocation) {
        // TODO: refactor it
        String getNameCode = null;
        for (int i = value.length - 1; i >= 0; --i) {
            if (i == value.length - 1) {
                if (null != defaultExtName) {
                    if (!"protocol".equals(value[i])) {
                        if (hasInvocation) {
                            getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName);
                        } else {
                            getNameCode = String.format("url.getParameter(\"%s\", \"%s\")", value[i], defaultExtName);
                        }
                    } else {
                        getNameCode = String.format("( url.getProtocol() == null ? \"%s\" : url.getProtocol() )", defaultExtName);
                    }
                } else {
                    if (!"protocol".equals(value[i])) {
                        if (hasInvocation) {
                            getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName);
                        } else {
                            getNameCode = String.format("url.getParameter(\"%s\")", value[i]);
                        }
                    } else {
                        getNameCode = "url.getProtocol()";
                    }
                }
            } else {
                if (!"protocol".equals(value[i])) {
                    if (hasInvocation) {
                        getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName);
                    } else {
                        getNameCode = String.format("url.getParameter(\"%s\", %s)", value[i], getNameCode);
                    }
                } else {
                    getNameCode = String.format("url.getProtocol() == null ? (%s) : url.getProtocol()", getNameCode);
                }
            }
        }

        return String.format(CODE_EXT_NAME_ASSIGNMENT, getNameCode);
    }

    /**
     * @return
     */
    private String generateExtensionAssignment() {
        return String.format(CODE_EXTENSION_ASSIGNMENT, type.getName(), ExtensionLoader.class.getSimpleName(), type.getName());
    }

    /**
     * generate method invocation statement and return it if necessary
     */
    private String generateReturnAndInvocation(Method method) {
        String returnStatement = method.getReturnType().equals(void.class) ? "" : "return ";

        String args = IntStream.range(0, method.getParameters().length)
                .mapToObj(i -> String.format(CODE_EXTENSION_METHOD_INVOKE_ARGUMENT, i))
                .collect(Collectors.joining(", "));

        return returnStatement + String.format("extension.%s(%s);\n", method.getName(), args);
    }

    /**
     * test if method has argument of type <code>Invocation</code>
     */
    private boolean hasInvocationArgument(Method method) {
        Class<?>[] pts = method.getParameterTypes();
        return Arrays.stream(pts).anyMatch(p -> CLASSNAME_INVOCATION.equals(p.getName()));
    }

    /**
     * generate code to test argument of type <code>Invocation</code> is null
     */
    private String generateInvocationArgumentNullCheck(Method method) {
        Class<?>[] pts = method.getParameterTypes();
        return IntStream.range(0, pts.length).filter(i -> CLASSNAME_INVOCATION.equals(pts[i].getName()))
                        .mapToObj(i -> String.format(CODE_INVOCATION_ARGUMENT_NULL_CHECK, i, i))
                        .findFirst().orElse("");
    }

    /**
     * get value of adaptive annotation or if empty return splitted simple name
     */
    private String[] getMethodAdaptiveValue(Adaptive adaptiveAnnotation) {
        String[] value = adaptiveAnnotation.value();
        // value is not set, use the value generated from class name as the key
        if (value.length == 0) {
            String splitName = StringUtils.camelToSplitName(type.getSimpleName(), ".");
            value = new String[]{splitName};
        }
        return value;
    }

    /**
     * 如果方法的（对象类型）参数中含有返回URL类型的方法：生成间接获取URL的代码
     *
     * get parameter with type <code>URL</code> from method parameter:
     * <p>
     * test if parameter has method which returns type <code>URL</code>
     * <p>
     * if not found, throws IllegalStateException
     */
    private String generateUrlAssignmentIndirectly(Method method) {
        // 方法参数（类型）对象
        Class<?>[] pts = method.getParameterTypes();

        // 查找内部含有返回URL的无参方法的对象类型参数
        Map<String, Integer> getterReturnUrl = new HashMap<>();
        // find URL getter method
        for (int i = 0; i < pts.length; ++i) {
            for (Method m : pts[i].getMethods()) {
                String name = m.getName();
                if ((name.startsWith("get") || name.length() > 3)
                        && Modifier.isPublic(m.getModifiers())
                        && !Modifier.isStatic(m.getModifiers())
                        && m.getParameterTypes().length == 0
                        && m.getReturnType() == URL.class) {
                    getterReturnUrl.put(name, i);
                }
            }
        }

        // 如果找不到，将抛出异常
        if (getterReturnUrl.size() <= 0) {
            // getter method not found, throw
            throw new IllegalStateException("Failed to create adaptive class for interface " + type.getName()
                    + ": not found url parameter or url attribute in parameters of method " + method.getName());
        }

        // 首先匹配getUrl方法
        Integer index = getterReturnUrl.get("getUrl");
        if (index != null) {
            return generateGetUrlNullCheck(index, pts[index], "getUrl");
        }
        // 匹配其它返回URL类型的无法方法：其中一个
        else {
            Map.Entry<String, Integer> entry = getterReturnUrl.entrySet().iterator().next();
            return generateGetUrlNullCheck(entry.getValue(), pts[entry.getValue()], entry.getKey());
        }
    }

    /**
     * 生成 通过方法中对象参数的方法获取URL的代码
     *
     * 1, test if argi is null
     * 2, test if argi.getXX() returns null
     * 3, assign url with argi.getXX()
     */
    private String generateGetUrlNullCheck(int index, Class<?> type, String method) {
        // Null point check
        StringBuilder code = new StringBuilder();

        // 对象参数空指针检查
        code.append(String.format("if (arg%d == null) throw new IllegalArgumentException(\"%s argument == null\");\n",
                index, type.getName()));

        // 对象参数返回URL的方法返回值空指针检查
        code.append(String.format("if (arg%d.%s() == null) throw new IllegalArgumentException(\"%s argument %s() == null\");\n",
                index, method, type.getName(), method));

        // 调用对象参数的方法获取URL
        code.append(String.format("%s url = arg%d.%s();\n", URL.class.getName(), index, method));

        return code.toString();
    }

}
