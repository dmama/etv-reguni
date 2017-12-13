<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="depuisTache" value="${param.depuisTache}" />

<%--@elvariable id="command" type="ch.vd.uniregctb.documentfiscal.AutreDocumentFiscalView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.edition.document.fiscal" /></tiles:put>
  	<%--<tiles:put name="fichierAide"><li><a href="#" onClick="ouvrirAide('<c:url value="/docs/maj-di.pdf"/>');" title="AccessKey: a" accesskey="e">Aide</a></li></tiles:put>--%>
  	<tiles:put name="fichierAide"><li><a href="#" title="AccessKey: a" accesskey="e">Aide</a></li></tiles:put>
	<tiles:put name="body">

		<unireg:nextRowClass reset="1"/>
		<unireg:bandeauTiers numero="${command.tiersId}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" />

		<!-- Debut Document fiscal -->
		<fieldset class="information">
			<legend><span><fmt:message key="label.caracteristiques.document.fiscal" /></span></legend>

			<table border="0">
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.autre.document.type" />&nbsp;:</td>
					<td width="25%"><c:if test="${command.libelleTypeDocument != null}"><fmt:message key="${command.libelleTypeDocument}"/></c:if></td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.autre.document.sous.type" />&nbsp;:</td>
					<td width="75%"><c:if test="${command.libelleSousType != null}"><fmt:message key="${command.libelleSousType}"/></c:if></td>
				</tr>
				<c:if test="${!(command.periodeFiscale == 0)}">
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.periode.fiscale" />&nbsp;:</td>
						<td width="25%">${command.periodeFiscale}</td>
						<td width="50%"></td>
					</tr>
				</c:if>
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.etat.courant" />&nbsp;:</td>
					<td width="25%"><fmt:message key="option.etat.avancement.m.${command.etat}"/></td>
					<td width="50%"></td>
				</tr>
			</table>
		</fieldset>
		<!-- Fin  Document fiscal -->

		<!-- Debut Delais -->
		<jsp:include page="delai/lister.jsp"/>
		<!-- Fin Delais -->

		<!-- Debut Etats -->
		<jsp:include page="etat/lister.jsp"/>
		<!-- Fin Etats -->

		<div style="margin-top:1em;">
			<!-- Debut Boutons -->
			<unireg:buttonTo name="Retour" action="/autresdocs/edit-list.do" id="boutonRetour" method="get" params="{pmId:${command.tiersId}}"/>

			<!-- Duplicata Document fiscal -->
			<unireg:buttonTo name="Imprimer duplicata" action="/autresdocs/duplicata.do" id="bouton_duplicata" method="post" params="{id:${command.id}}"
			                 confirm="Voulez-vous imprimer un duplicata pour la lettre de bienvenue (impression locale)?"/>

			<!-- Annulation Document fiscal -->
			<unireg:buttonTo name="Annuler document fiscal" confirm="Voulez-vous vraiment annuler ce document fiscal ?"
			                 action="/autresdocs/annuler.do" method="post" params='{id:${command.id}}'/>
		</div>

	</tiles:put>
</tiles:insert>
