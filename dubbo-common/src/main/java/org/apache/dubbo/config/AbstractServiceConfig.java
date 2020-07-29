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

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.support.Parameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.EXPORTER_LISTENER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SERVICE_FILTER_KEY;

/**
 * 服务配置抽象类，定义了许多有管服务的属性及其getter/setter方法
 *
 * AbstractServiceConfig
 *
 * @export
 */
public abstract class AbstractServiceConfig extends AbstractInterfaceConfig {

    private static final long serialVersionUID = 1L;

    /**
     * 服务版本号
     *
     * The service version
     */
    protected String version;

    /**
     * 服务分组
     *
     * The service group
     */
    protected String group;

    /**
     * 是否服务已经不建议使用
     *
     * whether the service is deprecated
     */
    protected Boolean deprecated = false;

    /**
     * 注册服务延迟时间（单位毫秒）
     * The time delay register service (milliseconds)
     */
    protected Integer delay;

    /**
     * 是否发布服务
     *
     * Whether to export the service
     */
    protected Boolean export;

    /**
     * 服务权重/重量
     *
     * The service weight
     */
    protected Integer weight;

    /**
     * 文档中心
     *
     * Document center
     */
    protected String document;

    /**
     * 是否在注册中心注册为一个动态服务，其值为true，该状态在服务被注册后开启，并且它需要手动关闭。如果你要关闭/禁用该服务，你也还需要手动处理。
     *
     * Whether to register as a dynamic service or not on register center, the value is true, the status will be enabled
     * after the service registered,and it needs to be disabled manually; if you want to disable the service, you also need
     * manual processing
     */
    protected Boolean dynamic = true;

    /**
     * 是否使用token
     *
     * Whether to use token
     */
    protected String token;

    /**
     * 是否导出访问日志到日志中
     *
     * Whether to export access logs to logs
     */
    protected String accesslog;

    /**
     * 协议配置列表，服务将导出的协议列表
     * 与protocolIds参数类似，只有1个会生效
     *
     * The protocol list the service will export with
     * Also see {@link #protocolIds}, only one of them will work.
     */
    protected List<ProtocolConfig> protocols;

    /**
     * 协议id列表，服务将导出的协议id列表
     * 与protocols参数类似，只有1个会生效
     *
     * The id list of protocols the service will export with
     * Also see {@link #protocols}, only one of them will work.
     */
    protected String protocolIds;

    // 允许的最大执行次数
    // max allowed execute times
    private Integer executes;

    /**
     * 是否已经注册
     *
     * Whether to register
     */
    private Boolean register;

    /**
     * 预热期
     * Warm up period
     */
    private Integer warmup;

    /**
     * 序列化类型
     *
     * The serialization type
     */
    private String serialization;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Integer getDelay() {
        return delay;
    }

    public void setDelay(Integer delay) {
        this.delay = delay;
    }

    public Boolean getExport() {
        return export;
    }

    public void setExport(Boolean export) {
        this.export = export;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    @Parameter(escaped = true)
    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    public String getToken() {
        return token;
    }

    public void setToken(Boolean token) {
        if (token == null) {
            setToken((String) null);
        } else {
            setToken(String.valueOf(token));
        }
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
    }

    public Boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(Boolean dynamic) {
        this.dynamic = dynamic;
    }

    public List<ProtocolConfig> getProtocols() {
        return protocols;
    }

    @SuppressWarnings({"unchecked"})
    public void setProtocols(List<? extends ProtocolConfig> protocols) {
        this.protocols = (List<ProtocolConfig>) protocols;
    }

    public ProtocolConfig getProtocol() {
        return CollectionUtils.isEmpty(protocols) ? null : protocols.get(0);
    }

    public void setProtocol(ProtocolConfig protocol) {
        setProtocols(new ArrayList<>(Arrays.asList(protocol)));
    }

    @Parameter(excluded = true)
    public String getProtocolIds() {
        return protocolIds;
    }

    public void setProtocolIds(String protocolIds) {
        this.protocolIds = protocolIds;
    }

    public String getAccesslog() {
        return accesslog;
    }

    public void setAccesslog(Boolean accesslog) {
        if (accesslog == null) {
            setAccesslog((String) null);
        } else {
            setAccesslog(String.valueOf(accesslog));
        }
    }

    public void setAccesslog(String accesslog) {
        this.accesslog = accesslog;
    }

    public Integer getExecutes() {
        return executes;
    }

    public void setExecutes(Integer executes) {
        this.executes = executes;
    }

    @Override
    @Parameter(key = SERVICE_FILTER_KEY, append = true)
    public String getFilter() {
        return super.getFilter();
    }

    @Override
    @Parameter(key = EXPORTER_LISTENER_KEY, append = true)
    public String getListener() {
        return listener;
    }

    @Override
    public void setListener(String listener) {
        this.listener = listener;
    }

    public Boolean isRegister() {
        return register;
    }

    public void setRegister(Boolean register) {
        this.register = register;
    }

    public Integer getWarmup() {
        return warmup;
    }

    public void setWarmup(Integer warmup) {
        this.warmup = warmup;
    }

    public String getSerialization() {
        return serialization;
    }

    public void setSerialization(String serialization) {
        this.serialization = serialization;
    }
}
