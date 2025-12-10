<#assign mapperNameLower = table.mapperName?uncap_first>
<#assign entityNameLower = table.entityName?uncap_first>
<#assign entityNameVO = table.entityName + "VO">

package ${package.ServiceImpl};

import ${package.Entity}.${entity};
import ${package.Mapper}.${table.mapperName};
<#if generateService>
import ${package.Service}.${table.serviceName};
</#if>
import ${superServiceImplClassPackage};
import ${package.Param}.${table.entityName}Param;
import ${package.Query}.${table.entityName}Query;
import ${package.VO}.${table.entityName}VO;
import cn.hutool.core.collection.CollUtil;
import com.plasticene.boot.common.utils.PtcBeanUtils;
import com.plasticene.boot.mybatis.core.query.PtcLambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.plasticene.boot.common.pojo.PageResult;
import java.util.List;

/**
 * <p>
 * ${table.comment!}
 * </p>
 *
 * @author ${author}
 * @since ${date}
 */
@Service
public class ${table.serviceImplName} extends ${superServiceImplClass}<${table.mapperName}, ${entity}><#if generateService> implements ${table.serviceName}</#if> {
    @Resource
    private ${table.mapperName} ${mapperNameLower};

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long create(${table.entityName}Param param) {
        ${table.entityName} ${entityNameLower} = PtcBeanUtils.copy(param, ${table.entityName}.class);
        ${mapperNameLower}.insert(${entityNameLower});
        return ${entityNameLower}.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(${table.entityName}Param param) {
        ${table.entityName} ${entityNameLower} = PtcBeanUtils.copy(param, ${table.entityName}.class);
        ${mapperNameLower}.updateById(${entityNameLower});
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<Long> idList) {
        if (CollUtil.isEmpty(idList)) {
            return;
        }
        ${mapperNameLower}.deleteByIds(idList);
    }

    @Override
    public PageResult<${entityNameVO}> page(${table.entityName}Query query) {
        PtcLambdaQueryWrapper<${table.entityName}> queryWrapper = new PtcLambdaQueryWrapper<>();
        PageResult<${table.entityName}> result = ${mapperNameLower}.selectPage(query, queryWrapper);
        List<${entityNameVO}> voList = PtcBeanUtils.copyList(result.getList(), ${entityNameVO}.class);
        return new PageResult<>(voList, result.getTotal(), result.getPages());
    }

    @Override
    public ${entityNameVO} detail(Long id) {
        ${table.entityName} ${entityNameLower} = ${mapperNameLower}.selectById(id);
        return PtcBeanUtils.copy(${entityNameLower}, ${entityNameVO}.class);
    }




}

