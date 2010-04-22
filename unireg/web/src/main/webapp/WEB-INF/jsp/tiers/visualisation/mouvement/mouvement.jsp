<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="ligneTableau" value="${1}" scope="request" />
<tiles:insert template="/WEB-INF/jsp/templates/templateIFrame.jsp">
	<tiles:put name="head"></tiles:put>

	<tiles:put name="title"></tiles:put>
	<tiles:put name="body">
	
<!-- Debut Contribuable -->
	<jsp:include page="../../../general/contribuable.jsp">
		<jsp:param name="page" value="edit"/>
		<jsp:param name="path" value="contribuable"/>
	</jsp:include>
<!-- Fin Contribuable -->

<!-- Debut Mouvements de dossier -->
	<fieldset class="information">
	<legend><span><fmt:message key="label.caracteristiques.mouvement" /></span></legend>
		<table>
			<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
			<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
				<td width="25%"><fmt:message key="label.type.mouvement" />&nbsp;:</td>
				<td width="25%"><fmt:message key="option.type.mouvement.${command.typeMouvement}" /></td>
				<td width="25%"><fmt:message key="label.date.mouvement" />&nbsp;:</td>
				<td width="25%"><fmt:formatDate value="${command.dateMouvement}" pattern="dd.MM.yyyy"/></td>
			</tr>
			<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
			<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
				<td width="25%"><fmt:message key="label.executant" />&nbsp;:</td>
				<td width="25%">${command.executant}</td>
				<td width="25%"><fmt:message key="label.date.heure.execution" />&nbsp;:</td>
				<td width="25%"><fmt:formatDate value="${command.dateExecution}" pattern="dd.MM.yyyy HH:mm:ss"/></td>
			</tr>
			<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
			<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
				<td width="25%"><fmt:message key="label.collectivite.administrative" />&nbsp;:</td>
				<td width="25%">${command.collectiviteAdministrative}</td>
				<td width="25%">&nbsp;</td>
				<td width="25%">&nbsp;</td>
			</tr>
		</table>
	</fieldset>

<!-- Fin Mouvements de dossier  -->

<!-- Debut Uilisateur -->
	<fieldset>
	<legend><span><fmt:message key="label.coordonnees.utilisateur" /></span></legend>
		<table>
			<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
			<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
				<td width="25%"><fmt:message key="label.prenom.nom" />&nbsp;:</td>
				<td width="25%">${command.nomPrenomUtilisateur}</td>
				<td width="25%"><fmt:message key="label.numero.telephone.fixe" />&nbsp;:</td>
				<td width="25%">${command.numeroTelephoneUtilisateur}</td>
			</tr>
		</table>
	</fieldset>

<!-- Fin Uilisateur  -->

<!-- Debut Boutons -->
	<table>
		<tr>
			<td><input type="button" id="annuler" value="<fmt:message key="label.bouton.fermer" />" onclick="self.parent.tb_remove()"></td>
		</tr>
	</table>
<!-- Fin Boutons -->
		</tiles:put>
</tiles:insert>