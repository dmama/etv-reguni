<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="command" type="ch.vd.uniregctb.tiers.view.TiersVisuView"--%>

<unireg:setAuth var="autorisations" tiersId="${command.tiers.numero}"/>

<c:set var="desactiverEdition" value="${(command.natureTiers == 'Entreprise' && command.entreprise.degreAssocCivil == 'CIVIL_ESCLAVE') || (command.natureTiers == 'Etablissement' && command.etablissement.degreAssocCivilEntreprise == 'CIVIL_ESCLAVE')}"/>

<c:if test="${autorisations.donneesCiviles && empty param['message'] && empty param['retour'] && !desactiverEdition}">
	<table border="0">
		<tr>
			<td>
				<unireg:raccourciModifier link="../civil/${fn:toLowerCase(command.natureTiers)}/edit.do?id=${command.tiers.numero}" tooltip="Modifier la partie civile" display="label.bouton.modifier"/>
			</td>
		</tr>
	</table>
</c:if>	

<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
<c:choose>
	<c:when test="${command.natureTiers == 'Habitant'}"><jsp:include page="individu.jsp"/></c:when>
	<c:when test="${command.natureTiers == 'NonHabitant'}"><jsp:include page="non-habitant.jsp"/></c:when>
	<c:when test="${command.natureTiers == 'MenageCommun'}"><jsp:include page="menage-commun.jsp"/></c:when>
	<c:when test="${command.natureTiers == 'AutreCommunaute'}"><jsp:include page="organisation.jsp"/></c:when>
	<c:when test="${command.natureTiers == 'Entreprise'}">
		<jsp:include page="entreprise.jsp">
			<jsp:param name="page" value="visu"/>
			<jsp:param name="nombreElementsTable" value="0"/>
		</jsp:include>
	</c:when>
	<c:when test="${command.natureTiers == 'Etablissement'}">
		<jsp:include page="etablissement.jsp">
			<jsp:param name="page" value="visu"/>
			<jsp:param name="nombreElementsTable" value="0"/>
		</jsp:include>
	</c:when>
</c:choose>
