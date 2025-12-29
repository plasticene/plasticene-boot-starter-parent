<#assign serviceNameLower = table.serviceName?uncap_first>
package ${package.Controller};

import ${package.Service}.${table.serviceName};
import ${package.Param}.${table.entityName}Param;
import ${package.Query}.${table.entityName}Query;
import ${package.VO}.${table.entityName}VO;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.pojo.ResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;


/**
 * <p>
 * ${table.comment!}
 * </p>
 *
 * @author ${author}
 * @since ${date}
 */
@RestController
@RequestMapping("<#if package.ModuleName?? && package.ModuleName != "">/${package.ModuleName}</#if>/<#if controllerMappingHyphenStyle>${controllerMappingHyphen}<#else>${table.entityPath}</#if>")
@Tag(name = "${table.comment!}")
public class ${table.controllerName} {

    @Resource
    private ${table.serviceName} ${serviceNameLower};

    @Operation(summary = "创建${table.comment!}")
    @PostMapping
    public ResponseVO<Long> create(@RequestBody @Validated ${table.entityName}Param param) {
        Long id = ${serviceNameLower}.create(param);
        return ResponseVO.success(id);
    }

    @Operation(summary = "修改${table.comment!}")
    @PutMapping
    public ResponseVO<Void> update(@RequestBody @Validated ${table.entityName}Param param) {
        ${serviceNameLower}.update(param);
        return ResponseVO.success();
    }

    @Operation(summary = "批量删除${table.comment!}")
    @DeleteMapping
    public ResponseVO<Void> delete(@RequestBody List<Long> idList) {
        ${serviceNameLower}.delete(idList);
        return ResponseVO.success();
    }

    @Operation(summary = "分页查询${table.comment!}")
    @GetMapping("/page")
    public ResponseVO<PageResult<${table.entityName}VO>> page(@Validated ${table.entityName}Query query) {
        PageResult<${table.entityName}VO> pageResult = ${serviceNameLower}.page(query);
        return ResponseVO.success(pageResult);
    }

    @Operation(summary = "获取${table.comment!}详情")
    @GetMapping("/{id}")
    public ResponseVO<${table.entityName}VO> detail(@PathVariable("id") Long id) {
        ${table.entityName}VO vo = ${serviceNameLower}.detail(id);
        return ResponseVO.success(vo);
    }
}