<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<script type="text/javascript">
function submitTrace() {
		var formGestionTracing = document.getElementById('formGestionTracing');
		formGestionTracing.action = 'gestionTracing.do';
		formGestionTracing.submit();
} 

</script>


<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title"><fmt:message key="label.gestion.tracing" /></tiles:put>
  	
  	<tiles:put name="body">
  	<form:form method="post" id="formGestionTracing">

		<fieldset>
			<legend><span><fmt:message key="label.gestion.performance" /></span></legend>
			<table>
				<tr class="<unireg:nextRowClass/>" >
					<td width="50%"><fmt:message key="label.gestion.performance.actif" />&nbsp;:</td>
					<td width="50%"><form:checkbox path="gestionPerfActif" onclick="submitTrace();" /></td>
				</tr>
			</table>
			
		</fieldset>

		<br> 
		<c:if test="${not empty traces}">
		
				<fieldset>			
					<table border="1">
					<%  int i = 0;%>
					<c:forEach items="${traces}" var="element">
					<tr>
						<td>Trace <%= i++%></td>
						<td><c:out value="${element}"></c:out></td>
					</tr>
					</c:forEach>
					</table>			
				</fieldset>
		
		</c:if>
	</form:form>
  	</tiles:put>
</tiles:insert>
