<#assign entityNameLower = table.entityName?uncap_first>
<#assign entityNameVO = table.entityName + "VO">
package ${package.Service};

import ${package.Entity}.${entity};
import ${superServiceClassPackage};
import ${package.Param}.${table.entityName}Param;
import ${package.Query}.${table.entityName}Query;
import ${package.VO}.${table.entityName}VO;
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
public interface ${table.serviceName} extends ${superServiceClass}<${entity}> {

    /**
     * 创建${table.comment!}
     * @param param
     * @return id
     */
    Long create(${table.entityName}Param param);

    /**
     * 更新${table.comment!}
     * @param param
     */
    void update(${table.entityName}Param param);

    /**
     * 批量删除${table.comment!}
     * @param idList
     */
    void delete(List<Long> idList);

    /**
     * 分页查询${table.comment!}
     * @param query
     * @return pageResult
     */
    PageResult<${entityNameVO}> page(${table.entityName}Query query);

    /**
     * 查询${table.comment!}
     * @param id
     * @return ${entityNameVO}
     */
    ${entityNameVO} detail(Long id);



}

