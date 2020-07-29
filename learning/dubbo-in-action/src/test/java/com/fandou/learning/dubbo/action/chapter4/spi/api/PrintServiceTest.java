package com.fandou.learning.dubbo.action.chapter4.spi.api;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

class PrintServiceTest {

    @Test
    void testPrint(){
        ServiceLoader<PrintService> serviceLoader = ServiceLoader.load(PrintService.class);
        int i = 0;
        for (PrintService p : serviceLoader) {
            p.print("消息" + (i++));
        }
    }

    @Test
    void testDubboSPIPrint(){
        PrintService simplePrintService = ExtensionLoader.getExtensionLoader(PrintService.class).getDefaultExtension();
        simplePrintService.print("abcdefghijklmn");

        PrintService fixedLengthPrintService = ExtensionLoader.getExtensionLoader(PrintService.class).getExtension("fixed");
        fixedLengthPrintService.print("abcdefghijklmn");

        PrintService doublePrintService = ExtensionLoader.getExtensionLoader(PrintService.class).getExtension("double");
        doublePrintService.print("abcdefghijklmn");

        PrintService mutilPrintService = ExtensionLoader.getExtensionLoader(PrintService.class).getExtension("mutil");
        mutilPrintService.print("abcdefghijklmn");
    }
}