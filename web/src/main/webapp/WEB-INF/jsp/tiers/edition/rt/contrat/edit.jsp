<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="provenance" value="${param.provenance}" />
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
<tiles:put name="title"><fmt:message key="title.recapitulatif.rapport.prestation" /></tiles:put>
<tiles:put name="fichierAide">
		<li>
			<a href="#" onClick="ouvrirAide('<c:url value='/docs/gestion-debiteurIS.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
		</li>
</tiles:put>
<tiles:put name="body">
	<%--@elvariable id="rapportAddView" type="ch.vd.unireg.rt.view.RapportPrestationView"--%>
	<form:form method="post" id="formRT" commandName="rapportAddView">
		<unireg:nextRowClass reset="1"/>
		<form:hidden path="debiteur.numero"/>
		<form:hidden path="sourcier.numero"/>

		<table>
		<tr>
			<td id="td_tiers_gauche">
				<!-- Caracteristiques debiteur -->
				<c:set var="titre"><fmt:message key="caracteristiques.debiteur.is"/></c:set>
				<unireg:bandeauTiers numero="${rapportAddView.debiteur.numero}" titre="${titre}" showValidation="false" showEvenementsCivils="false" showLinks="false" showAvatar="false" showComplements="true"/>
			</td>
			<td>
				<%-- Flèche du sens du rapport  --%>
				<table id="flecheSensRapport" cellpadding="0" cellspacing="0">
					<tr>
						<td style="width:1em;"></td>
						<td id="flecheGauche" class="fleche_droite_bord_gauche iepngfix"></td>
						<td id="flecheMilieu" class="fleche_milieu"><fmt:message key="label.fleche.debiteur.sourcier"/></td>
						<td id="flecheDroite" class="fleche_droite_bord_droit iepngfix"></td>
						<td style="width:1em;"></td>
					</tr>
				</table>
			</td>
			<td id="td_tiers_droite">
				<!-- Caracteristiques sourcier -->
				<c:set var="titre"><fmt:message key="label.caracteristiques.sourcier"/></c:set>
				<unireg:bandeauTiers numero="${rapportAddView.sourcier.numero}" titre="${titre}" showValidation="false" showEvenementsCivils="false" showLinks="false" showAvatar="false" showComplements="true"/>
			</td>
		</tr>		
		<tr>
			<td colspan="3">
				<%-- Formulaire de saisie des détails du rapport --%>
				<jsp:include page="rt.jsp"/>
			</td>
		</tr>
		</table>

		<!-- Debut Boutons -->
		<input type="button" value="<fmt:message key="label.bouton.retour" />" onclick="Navigation.back('/rapports-prestation/search-sourcier.do', 'numeroDebiteur=${rapportAddView.debiteur.numero}')" />
		<input type="submit" value="<fmt:message key="label.bouton.sauver" />" />
		<!-- Fin Boutons -->
	</form:form>
</tiles:put>
</tiles:insert>