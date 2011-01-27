<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title"><fmt:message key="title.admin.audit" /></tiles:put>
  	
  	<tiles:put name="body">
  	<form:form method="get" id="formGestionTracing">
  	
		<script type="text/javascript">
			function submitForm(){
				$('#formGestionTracing').submit();
			}
		</script>
  	
  		<span id="logs_header">
	  		Afficher les messages de type : <form:checkbox path="showInfo" id="showInfo" label="info" onchange="submitForm()" onclick="submitForm()" />
	  		<form:checkbox path="showWarning" id="showWarning" label="warnings" onchange="submitForm()" onclick="submitForm()" />
	  		<form:checkbox path="showError" id="showError" label="erreurs" onchange="submitForm()" onclick="submitForm()" />
	  		<form:checkbox path="showSuccess" id="showSuccess" label="succès" onchange="submitForm()" onclick="submitForm()" />
	  		<form:checkbox path="showEvCivil" id="showEvCivil" label="événements civils" onchange="submitForm()" onclick="submitForm()" />
  		</span> 
  	
		<display:table name="${command.list}" id="logs" pagesize="50" partialList="true" size="${command.totalSize}" requestURI="/admin/audit.do" 
			class="list" decorator="ch.vd.uniregctb.admin.AuditTableDecorator" cellspacing="2" > 
			<display:column titleKey="label.admin.audit.date" style="width:120px;">
				<unireg:sdate sdate="${logs.date}"></unireg:sdate>	
			</display:column>
			<display:column titleKey="label.admin.audit.user">
				<c:out value="${logs.user}" />
			</display:column>
			<display:column titleKey="label.admin.audit.level">
				<c:out value="${logs.level}" />
			</display:column>
			<display:column titleKey="label.admin.audit.thread" style="width:50px;">
				<c:out value="${logs.threadId}" />
			</display:column>
			<display:column titleKey="label.admin.audit.evenement" style="width:50px;">
				<c:out value="${logs.evenementId}" />
			</display:column>
			<display:column titleKey="label.admin.audit.message">
				<c:out value="${logs.message}" />
			</display:column>
			<display:column titleKey="label.admin.audit.document" style="text-align:center">
				<unireg:document doc="${logs.document}" />
			</display:column>
			
			<display:setProperty name="paging.banner.all_items_found" value=""/>
		</display:table>
		
	</form:form>
  	</tiles:put>
</tiles:insert>
