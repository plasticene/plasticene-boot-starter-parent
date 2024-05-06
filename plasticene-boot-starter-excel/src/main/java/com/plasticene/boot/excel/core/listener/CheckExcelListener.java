package com.plasticene.boot.excel.core.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelAnalysisException;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.excel.util.StringUtils;
import com.plasticene.boot.common.exception.BizException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2024/4/22 16:40
 * 只读取表头
 */
@Getter
@Slf4j
public class CheckExcelListener<T> extends AnalysisEventListener<T> {
    // 外部传入的模板表头，用于对比
    private final List<String> headList;
    // 表头匹配检验错误信息，为null说明完全匹配
    private String errMsg;
    // 记录是否有数据，解析到一条就算有
    private final List<T> dataList = ListUtils.newArrayListWithExpectedSize(1);

    public CheckExcelListener(List<String> headList) {
        this.headList = headList;
    }

    @Override
    public void invoke(T data, AnalysisContext context) {
        log.info("解析到一条数据:{}", data);
        dataList.add(data);
        throw new ExcelAnalysisException("检测到excel有数据");
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {

    }

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        // 传空说明不需要检验
        if (CollectionUtils.isEmpty(headList)) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        int size = headList.size();
        for (int i = 0; i < size; i++) {
            String excelHead = headMap.get(i);
            String templateHead = headList.get(i);
            if (!Objects.equals(excelHead, templateHead)) {
                if (StringUtils.isBlank(sb)) {
                    sb.append("文件与模版不匹配，");
                }
                sb.append("第" + (i + 1) + "列名应为:" + templateHead + "; ");
            }
        }
        // 完全匹配，这是表头检验结束，不需要继续解析表数据，抛出异常终止解析
        throw new ExcelAnalysisException("表头解析完全匹配，结束解析");

    }
}
