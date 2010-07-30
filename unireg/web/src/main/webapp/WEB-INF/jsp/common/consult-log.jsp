<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<unireg:nextRowClass reset="1"/>
<tiles:insert template="/WEB-INF/jsp/templates/templateIFrame.jsp">
	<tiles:put name="head"></tiles:put>

	<tiles:put name="title"></tiles:put>
	<tiles:put name="body">
	
<!-- Debut Consultation logs -->
	<fieldset class="information">
	<legend><span><fmt:message key="label.consultation.log" /></span></legend>
	<table>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.utilisateur.creation" />&nbsp;:</td>
			<td width="25%">${command.utilisateurCreation}</td>
			<td width="25%"><fmt:message key="label.date.heure.creation" />&nbsp;:</td>
			<td width="25%">
				<fmt:formatDate value="${command.dateHeureCreation}" pattern="dd.MM.yyyy HH:mm:ss" />
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.utilisateur.derniere.modif" />&nbsp;:</td>
			<td width="25%">${command.utilisateurDerniereModif}</td>
			<td width="25%"><fmt:message key="label.date.heure.derniere.modif" />&nbsp;:</td>
			<td width="25%">
				<fmt:formatDate value="${command.dateHeureDerniereModif}" pattern="dd.MM.yyyy HH:mm:ss" />
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.utilisateur.annulation" />&nbsp;:</td>
			<td width="25%">${command.utilisateurAnnulation}</td>
			<td width="25%"><fmt:message key="label.date.heure.annulation" />&nbsp;:</td>
			<td width="25%">
				<fmt:formatDate value="${command.dateHeureAnnulation}" pattern="dd.MM.yyyy HH:mm:ss" />
			</td>
		</tr>
	</table>
	</fieldset>
<!-- Fin Consultation logs -->

<!-- Debut Boutons -->
	<table>
		<tr>
			<td><input type="button" id="annuler" value="<fmt:message key="label.bouton.fermer" />" onclick="self.parent.tb_remove()"></td>
		</tr>
	</table>
<!-- Fin Boutons -->
		</tiles:put>
</tiles:insert>