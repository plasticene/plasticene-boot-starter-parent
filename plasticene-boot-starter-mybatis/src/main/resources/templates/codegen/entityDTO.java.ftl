package ${package.DTO};

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
* <p>
* ${table.comment!}
* </p>
*
* @author ${author}
* @since ${date}
*/
@Data
@Schema(description = "${table.comment!}")
public class ${entity}DTO {
<#-- ----------  BEGIN 字段循环遍历  ---------->
<#list table.fields as field>
    @Schema(description = "${field.comment!}")
    private ${field.propertyType} ${field.propertyName};
</#list>
<#------------  END 字段循环遍历  ---------->
}