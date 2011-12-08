<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Mouvement dossier -->
<c:if test="${!command.tiers.annule}">
	<c:if test="${command.allowedOnglet.MVT}">
		<table border="0">
			<tr>
				<td>
					<c:if test="${empty param['message'] && empty param['retour']}">
						<unireg:raccourciModifier link="../mouvement/edit-contribuable.do?numero=${command.tiers.numero}" tooltip="Modifier les mouvements" display="label.bouton.modifier"/>
					</c:if>	
				</td>
			</tr>
		</table>
	</c:if>
</c:if>
<c:if test="${not empty command.mouvements}">
<fieldset>
	<legend><span><fmt:message key="label.mouvements.dossiers" /></span></legend>
	<jsp:include page="../../common/mouvement/mouvements.jsp">
		<jsp:param name="page" value="visu"/>
	</jsp:include>
	
</fieldset>
</c:if>
<!-- Fin Mouvement dossier -->