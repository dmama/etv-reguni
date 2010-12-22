<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<unireg:nextRowClass reset="1"/>
<tiles:insert template="/WEB-INF/jsp/templates/templateDialog.jsp">

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
			<tr class="<unireg:nextRowClass/>" >
				<td width="25%"><fmt:message key="label.type.mouvement" />&nbsp;:</td>
				<td width="25%"><fmt:message key="option.type.mouvement.${command.typeMouvement}" /></td>
				<td width="25%"><fmt:message key="label.date.mouvement" />&nbsp;:</td>
				<td width="25%"><fmt:formatDate value="${command.dateMouvement}" pattern="dd.MM.yyyy"/></td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				<td width="25%"><fmt:message key="label.executant" />&nbsp;:</td>
				<td width="25%">${command.executant}</td>
				<td width="25%"><fmt:message key="label.date.heure.execution" />&nbsp;:</td>
				<td width="25%"><fmt:formatDate value="${command.dateExecution}" pattern="dd.MM.yyyy HH:mm:ss"/></td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
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
			<tr class="<unireg:nextRowClass/>" >
				<td width="25%"><fmt:message key="label.prenom.nom" />&nbsp;:</td>
				<td width="25%">${command.nomPrenomUtilisateur}</td>
				<td width="25%"><fmt:message key="label.numero.telephone.fixe" />&nbsp;:</td>
				<td width="25%">${command.numeroTelephoneUtilisateur}</td>
			</tr>
		</table>
	</fieldset>

<!-- Fin Uilisateur  -->

	</tiles:put>
</tiles:insert>