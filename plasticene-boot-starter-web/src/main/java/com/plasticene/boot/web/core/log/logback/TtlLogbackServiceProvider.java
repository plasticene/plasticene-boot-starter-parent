package com.plasticene.boot.web.core.log.logback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LogbackServiceProvider;
import org.slf4j.spi.MDCAdapter;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/5/29
 */
public class TtlLogbackServiceProvider extends LogbackServiceProvider {

    private MDCAdapter ttlMdcAdapter;

    @Override
    public void initialize() {
        super.initialize();
        this.ttlMdcAdapter = TtlLogbackMDCAdapter.getInstance();
        ((LoggerContext)super.getLoggerFactory()).setMDCAdapter(ttlMdcAdapter);
    }

    @Override
    public MDCAdapter getMDCAdapter() {
        return this.ttlMdcAdapter;
    }
}
