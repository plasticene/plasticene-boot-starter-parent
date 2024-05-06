package com.plasticene.boot.excel.core.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.util.ListUtils;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2024/4/22 19:01
 */
public class DataExcelListener<T> extends AnalysisEventListener<T> {
    /**
     * 缓存数量
     */
    private static final int BATCH_COUNT = 1000;

    /**
     * 缓存的数据，不能一次性加载大批量数据到内存，容易OOM
     */
    private List<T> cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);


    /**
     * 数据处理器
     */
    private final Consumer<List<T>> dataHandler;

    public DataExcelListener(Consumer<List<T>> dataHandler) {
        this.dataHandler = dataHandler;
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception {

    }

    @Override
    public void invoke(T data, AnalysisContext context) {
        
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {

    }
}
