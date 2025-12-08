package ${package.Entity};

<#list importEntityFrameworkPackages as pkg>
    import ${pkg};
</#list>

<#list importEntityJavaPackages as pkg>
    import ${pkg};
</#list>

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
public class ${entity}Param {
<#-- ----------  BEGIN 字段循环遍历  ---------->
<#list table.fields as field>
    @Schema(description = "${field.comment!}")
    private ${field.propertyType} ${field.propertyName};
</#list>
<#------------  END 字段循环遍历  ---------->
}
