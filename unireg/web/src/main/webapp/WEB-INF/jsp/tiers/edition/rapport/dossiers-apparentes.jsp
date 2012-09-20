<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Dossiers Apparentes -->
<fieldset>
	<legend><span><fmt:message key="label.dossiers.apparentes" /></span></legend>
	
	<c:if test="${command.allowedOnglet.DOS_NO_TRA}">
	<table border="0">
		<tr>
			<td>
				<a href="../rapport/search.do?action=effacer&numero=<c:out value="${command.tiers.numero}"></c:out>"
				class="add" title="Ajouter rapport">&nbsp;<fmt:message key="label.bouton.ajouter" /></a>
			</td>
		</tr>
		<authz:authorize ifAnyGranted="ROLE_RT">
		<c:if test="${(command.natureTiers == 'Habitant') || (command.natureTiers == 'NonHabitant')}">
		<tr>
			<td>
				<a href="../rt/list-debiteur.do?numeroSrc=<c:out value="${command.tiers.numero}"></c:out>" 
				class="add" title="Ajouter rapport de travail">&nbsp;<fmt:message key="label.bouton.ajouter.rt" /></a>
			</td>
		</tr>
		</c:if>
		</authz:authorize>
	</table>
	</c:if>
	
	<jsp:include page="../../common/dossiers-apparentes.jsp">
		<jsp:param name="page" value="edit"/>
	</jsp:include>	
</fieldset>
<!-- Fin Dossiers Apparentes -->
