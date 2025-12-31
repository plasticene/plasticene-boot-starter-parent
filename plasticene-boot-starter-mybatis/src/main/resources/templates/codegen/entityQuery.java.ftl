package ${package.Query};

import com.plasticene.boot.common.pojo.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 *
 * <p> ${table.comment!} </p>
 *
 * @author ${author}
 * @since ${date}
 */

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "${table.comment!}")
public class ${entity}Query extends PageQuery {
<#-- ----------  BEGIN 字段循环遍历  ---------->
<#list table.fields as field>
    @Schema(description = "${field.comment!}")
    private ${field.propertyType} ${field.propertyName};
</#list>
<#------------  END 字段循环遍历  ---------->
}