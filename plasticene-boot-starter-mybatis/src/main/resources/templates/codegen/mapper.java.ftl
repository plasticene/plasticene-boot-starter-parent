package ${package.Mapper};

<#list importMapperFrameworkPackages as pkg>
import ${pkg};
</#list>
<#if importMapperJavaPackages?size !=0>

  <#list importMapperJavaPackages as pkg>
import ${pkg};
   </#list>
</#if>

/**
 * <p>
 * ${table.comment!}
 *
 * @author ${author}
 * @since ${date}
 */
<#if mapperAnnotationClass??>
@${mapperAnnotationClass.simpleName}
</#if>
<#if kotlin>
interface ${table.mapperName} : ${superMapperClass}<${entity}> {
<#else>
public interface ${table.mapperName} extends ${superMapperClass}<${entity}> {
</#if>

<#list mapperMethodList as m>
    /**
     * generate by ${m.indexName}
     *
    <#list m.tableFieldList as f>
     * @param ${f.propertyName} ${f.comment}
    </#list>
     */
    ${m.method}
</#list>
}
