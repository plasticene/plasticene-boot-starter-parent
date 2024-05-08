package com.plasticene.boot.excel.core.aop;

import com.alibaba.excel.EasyExcel;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.excel.core.anno.ExcelExport;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.List;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2024/5/8 15:00
 */
@Aspect
@Slf4j
public class ExcelExportAspect {

    @Pointcut("@annotation(com.plasticene.boot.excel.core.anno.ExcelExport)")
    public void excelExportPointcut(){}

    @Around("excelExportPointcut()")
    public void aroundExcelExport(ProceedingJoinPoint joinPoint) throws Throwable {
        // 先执行业务逻辑拿到数据
        Object obj = joinPoint.proceed();
        if (!(obj instanceof List)) {
            throw new BizException("接口返回类型必须是List");
        }
        List dataList = (List) obj;
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        // 获取方法返回类型
        ParameterizedType returnType = (ParameterizedType) method.getGenericReturnType();
        Type dataType = returnType.getActualTypeArguments()[0];
        ExcelExport excelExport = method.getAnnotation(ExcelExport.class);        // 文件名
        String fileName = excelExport.name() + excelExport.suffix().getValue();
        String sheetName = excelExport.sheet();
        HttpServletResponse response = getResponse();
        fileName = URLEncoder.encode(fileName, "UTF-8");
        // 根据实际的文件类型找到对应的 contentType
        String contentType = MediaTypeFactory.getMediaType(fileName).map(MediaType::toString)
                .orElse("application/vnd.ms-excel");
        response.setContentType(contentType);
        response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);
        response.setCharacterEncoding("utf-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename*=utf-8''" + fileName);
        if (dataType instanceof Class) {
            EasyExcel.write(response.getOutputStream(), (Class) dataType).sheet(sheetName).doWrite(dataList);
        } else {
            // List<List<String>
            EasyExcel.write(response.getOutputStream()).sheet(sheetName).doWrite(dataList);
        }
    }

    private HttpServletResponse getResponse() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletResponse response = requestAttributes.getResponse();
        return response;
    }


}
