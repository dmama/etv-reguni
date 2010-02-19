<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut DI -->
<c:if test="${!command.tiers.annule && command.allowedOnglet.DI}">
	<table border="0">
		<tr><td>
			<c:if test="${empty param['message'] && empty param['retour']}">
				<unireg:raccourciModifier link="../di/edit.do?action=listdis&numero=${command.tiers.numero}" tooltip="Modifier les DI" display="label.bouton.modifier"/>
			</c:if>	
		</td></tr>
	</table>
</c:if>
<c:if test="${not empty command.dis}">
<fieldset>
	<legend><span><fmt:message key="label.declarations.impot" /></span></legend>

	<jsp:include page="../../common/di/dis.jsp">
		<jsp:param name="page" value="visu"/>
	</jsp:include>

</fieldset>
</c:if>
<!-- Fin DI -->