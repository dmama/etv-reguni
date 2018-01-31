<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut LR -->
<c:if test="${!command.tiers.annule}">
	<authz:authorize ifAnyGranted="ROLE_LR">
		<table border="0">
			<tr>
				<td>
					<unireg:raccourciModifier link="../lr/edit-debiteur.do?numero=${command.tiers.numero}" tooltip="Modifier les listes récapitulatives" display="label.bouton.modifier"/>
				</td>
			</tr>
		</table>
	</authz:authorize>
</c:if>
<fieldset>
	<legend><span><fmt:message key="lablel.listes.recapitulatives" /></span></legend>
	<jsp:include page="../../common/lr/lrs.jsp">
		<jsp:param name="page" value="visu"/>
	</jsp:include>
	
</fieldset>

<!-- Fin LR -->