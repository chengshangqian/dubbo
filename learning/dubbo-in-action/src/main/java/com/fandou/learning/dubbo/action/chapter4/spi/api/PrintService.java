package com.fandou.learning.dubbo.action.chapter4.spi.api;

import org.apache.dubbo.common.extension.SPI;

@SPI("simple")
public interface PrintService {
    void print(String msg);
}
