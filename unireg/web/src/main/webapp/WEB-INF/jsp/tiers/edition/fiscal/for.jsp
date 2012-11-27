<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut For -->

<fieldset><legend><span><fmt:message key="label.fors.fiscaux" /></span></legend>

	<table border="0">
		<tr>
			<td>
				<unireg:setAuth var="autorisations" tiersId="${command.tiers.numero}"/>
				<c:if test="${autorisations.forsPrincipaux}">
					<unireg:linkTo name="Ajouter" title="Ajouter un for" action="/fors/principal/add.do" params="{tiersId:${command.tiers.numero}}" link_class="add"/>
				</c:if>
				<c:if test="${!autorisations.forsPrincipaux && autorisations.forsSecondaires}">
					<unireg:linkTo name="Ajouter" title="Ajouter un for" action="/fors/secondaire/add.do" params="{tiersId:${command.tiers.numero}}" link_class="add"/>
				</c:if>
				<c:if test="${!autorisations.forsPrincipaux && !autorisations.forsSecondaires && autorisations.forsAutresElementsImposables}">
					<unireg:linkTo name="Ajouter" title="Ajouter un for" action="/fors/autreelementimposable/add.do" params="{tiersId:${command.tiers.numero}}" link_class="add"/>
				</c:if>

				<c:if test="${command.forsPrincipalActif != null && autorisations.forsPrincipaux}">
					<unireg:linkTo name="Changer le mode d'imposition" title="Changer le mode d'imposition" action="/fors/principal/editModeImposition.do" params="{forId:${command.forsPrincipalActif.id}}" link_class="add"/>
				</c:if>
			</td>
		</tr>
	</table>
	
	<jsp:include page="../../common/fiscal/for.jsp">
		<jsp:param name="page" value="edit"/>
	</jsp:include>
			

</fieldset>
<!-- Fin For -->

