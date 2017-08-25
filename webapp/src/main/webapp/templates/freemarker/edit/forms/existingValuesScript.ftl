    <script type="text/javascript">
 var urisInScope = {};
    var literalsInScope = {};
    <#list editConfiguration.pageData.urisInScope?keys as uriKey>
    	urisInScope["${uriKey}"]=[];
    	<#assign uriValue = editConfiguration.pageData.urisInScope[uriKey] />
    	<#list uriValue as val>
    		urisInScope["${uriKey}"].push("${val}");
    	</#list>
    	
    </#list>
    
    </script>