<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Rapports Prestations -->
<fieldset>
	<legend><span><fmt:message key="label.contribuable.associe" /></span></legend>
	<c:if test="${command.addContactISAllowed}">
		<table border="0">
			<tr>
				<td>
					<unireg:raccourciAjouter link="../contribuable-associe/list.do?numeroDpi=${command.tiers.numero}" tooltip="Lier à un contribuable" display="label.bouton.ajouter"/>
				</td>
			</tr>
		</table>
	</c:if>
	<jsp:include page="../../common/contribuable-associe.jsp">
		<jsp:param name="page" value="edit"/>
	</jsp:include>
	
</fieldset>
<!-- Fin Rapports Prestations -->
