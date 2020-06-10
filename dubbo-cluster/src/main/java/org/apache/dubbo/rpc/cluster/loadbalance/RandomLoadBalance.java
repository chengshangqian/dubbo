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
package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This class select one provider from multiple providers randomly.
 * You can define weights for each provider:
 * If the weights are all the same then it will use random.nextInt(number of invokers).
 * If the weights are different then it will use random.nextInt(w1 + w2 + ... + wn)
 * Note that if the performance of the machine is better than others, you can set a larger weight.
 * If the performance is not so good, you can set a smaller weight.
 */
public class RandomLoadBalance extends AbstractLoadBalance {

    /**
     * 随机负载均衡策略名称
     */
    public static final String NAME = "random";

    /**
     * Select one invoker between a list using a random criteria
     * @param invokers List of possible invokers
     * @param url URL
     * @param invocation Invocation
     * @param <T>
     * @return The selected invoker
     */
    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        // 调用者数量/提供者数量
        // Number of invokers
        int length = invokers.size();

        // 相同权重：用于表示所有调用者/提供者的权重是否相同
        // Every invoker has the same weight?
        boolean sameWeight = true;

        // 每个调用者的权重
        // the weight of every invokers
        int[] weights = new int[length];

        // 获取首个调用者的权重
        // the first invoker's weight
        int firstWeight = getWeight(invokers.get(0), invocation);
        weights[0] = firstWeight;

        // 权重之和
        // The sum of weights
        int totalWeight = firstWeight;

        // 获取所有调用者的权重，并相加
        for (int i = 1; i < length; i++) {
            int weight = getWeight(invokers.get(i), invocation);
            // 保存每个调用者的权重，后续使用
            // save for later use
            weights[i] = weight;

            // 权重相加
            // Sum
            totalWeight += weight;

            // 如果之前的权重都相同（或没有出现不相同的调用者权限），比较当前调用者的权重和首个调用者权重不相同时，则表示所有的调用者权重不再相同
            if (sameWeight && weight != firstWeight) {
                sameWeight = false;
            }
        }

        // 总的权重大于0且调用者权重不均衡（即权重有大有小）：加权随机算法
        if (totalWeight > 0 && !sameWeight) {
            // 生成一个 [0，totalWeight)的随机数，然后基于权重之后范围随机选择一个调用者返回
            // If (not every invoker has the same weight & at least one invoker's weight>0), select randomly based on totalWeight.
            int offset = ThreadLocalRandom.current().nextInt(totalWeight);

            // 遍历所有的调用者，通过权重和获取的随机偏移量的递减算法，返回权重最大的那个调用者？
            // 比如 有3个调用者，其权重之和为10，每个调用者权重分别为253
            // 一、假设此次从0~10之间产生的随机偏移量为offset=4，则计算结果为
            // 第1次遍历：offset = offset - weights[0] = 4 - 2 = 2，将继续下一次循环
            // 第2次遍历：offset = offset - weights[1] = 2 - 5 = -3 < 0, 跳出循环并返回第2个调用者invokers.get(1)
            // 二、如果随机偏移量是9，则计算结果为
            // 第1次遍历：offset = offset - weights[0] = 9 - 2 = 7，将继续下一次循环
            // 第2次遍历：offset = offset - weights[0] = 7 - 5 = 2，将继续下一次循环
            // 第3次遍历：offset = offset - weights[0] = 2 - 3 = -1 < 0，跳出循环并返回第2个调用者invokers.get(2)
            // 以此类推，取决于随机产生的偏移量offset的值：如果产生的偏移量/遍历后递减的偏移量小于当前权重，则返回次调用者，所以如果产生的偏移量< 2，第1个调用将返回
            // Return a invoker based on the random value.
            for (int i = 0; i < length; i++) {
                offset -= weights[i];
                if (offset < 0) {
                    return invokers.get(i);
                }
            }
        }

        // 如果所有调用者的权重都相同或权重之和为0，则普通随机选择一个
        // If all invokers have the same weight value or totalWeight=0, return evenly.
        return invokers.get(ThreadLocalRandom.current().nextInt(length));
    }

}
