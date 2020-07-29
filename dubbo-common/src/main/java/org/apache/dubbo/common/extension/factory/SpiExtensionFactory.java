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
package org.apache.dubbo.common.extension.factory;

import org.apache.dubbo.common.extension.ExtensionFactory;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.extension.SPI;

/**
 * SPI扩展类型实例工厂
 *
 * SpiExtensionFactory
 */
public class SpiExtensionFactory implements ExtensionFactory {

    @Override
    public <T> T getExtension(Class<T> type, String name) {
        // 只负责type为SPI注解的接口类型实例加载和查找
        if (type.isInterface() && type.isAnnotationPresent(SPI.class)) {
            ExtensionLoader<T> loader = ExtensionLoader.getExtensionLoader(type);
            // 如果该SPI扩展类存在普通扩展类或自动激活扩展类，
            // 则获取其自适应类（因为自适应类的规范，内部必须调用真实的扩展类即普通扩展类或自动激活扩展类来实现业务逻辑）
            if (!loader.getSupportedExtensions().isEmpty()) {
                // 注意，如果该SPI类没有声明Adaptive注解的实现类或方法或SPI没有缺省值或URL中没有指定，也会抛出异常
                return loader.getAdaptiveExtension();
            }
        }
        return null;
    }

}
