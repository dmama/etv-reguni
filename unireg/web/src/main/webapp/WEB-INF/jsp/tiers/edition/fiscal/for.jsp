<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut For -->

<fieldset><legend><span><fmt:message key="label.fors.fiscaux" /></span></legend>

	<table border="0">
		<tr>
			<td>
				<a href="for.do?numero=<c:out value="${command.tiers.numero}"></c:out>&height=250&width=900&index=&TB_iframe=true&modal=true" 
				class="add thickbox" title="Ajouter for">&nbsp;<fmt:message key="label.bouton.ajouter" /></a>
			</td>
		</tr>
	</table>
	
	<jsp:include page="../../common/fiscal/for.jsp">
		<jsp:param name="page" value="edit"/>
	</jsp:include>
			

</fieldset>
<!-- Fin For -->

