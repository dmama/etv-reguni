<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="addListeCommand" type="ch.vd.unireg.lr.view.ListeRecapitulativeAddView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.edition.lr" /></tiles:put>

	<tiles:put name="body">
	<form:form method="post" id="formEditLR" name="theForm" action="add-lr.do" commandName="addListeCommand">

		<form:hidden path="idDebiteur"/>
		<form:hidden path="dateDebut"/>
		<form:hidden path="dateFin"/>

		<unireg:nextRowClass reset="1"/>

		<!-- Debut Caracteristiques generales -->
		<c:set var="titre"><fmt:message key="caracteristiques.debiteur.is"/></c:set>
		<unireg:bandeauTiers numero="${addListeCommand.idDebiteur}" titre="${titre}" showValidation="false" showEvenementsCivils="false" showLinks="false" showAvatar="false" showComplements="true"/>
		<!-- Fin Caracteristiques generales -->

		<!-- Debut LR -->
		<fieldset class="information">
			<legend><span><fmt:message key="caracteristiques.lr" /></span></legend>

			<table>
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.date.debut.periode" />&nbsp;:</td>
					<td width="25%"><unireg:regdate regdate="${addListeCommand.dateDebut}"/></td>
					<td width="25%"><fmt:message key="label.date.fin.periode" />&nbsp;:</td>
					<td width="25%"><unireg:regdate regdate="${addListeCommand.dateFin}"/></td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.date.delai.accorde" />&nbsp;:</td>
					<td width="75%" colspan="3">
						<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
							<jsp:param name="path" value="delaiAccorde" />
							<jsp:param name="id" value="delaiAccorde" />
							<jsp:param name="mandatory" value="true" />
						</jsp:include>
					</td>
				</tr>
			</table>
		</fieldset>
		<!-- Fin  Listes recapitulatives -->

		<!-- Fin LR -->

		<!-- Debut Boutons -->
		<c:set var="RetourName"><fmt:message key="label.bouton.retour"/></c:set>
		<unireg:buttonTo name="${RetourName}" action="/lr/edit-debiteur.do" params="{numero:${addListeCommand.idDebiteur}}" method="get" confirm="Voulez-vous vraiment quitter cette page sans sauver ?"/>
		<c:set var="ImprimerName"><fmt:message key="label.bouton.imprimer"/></c:set>
		<c:choose>
			<c:when test="${addListeCommand.imprimable}">
				<input type="button" value="${ImprimerName}" onclick="if (confirm('Voulez-vous vraiment imprimer cette nouvelle liste récapitulative ?')) { this.disabled = true; $('#formEditLR').submit(); } return false;"/>
			</c:when>
			<c:otherwise>
				<input type="button" value="${ImprimerName}" disabled="disabled"/>
			</c:otherwise>
		</c:choose>
		<!-- Fin Boutons -->
	</form:form>

	</tiles:put>
</tiles:insert>
