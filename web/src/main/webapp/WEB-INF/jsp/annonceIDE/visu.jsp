<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/templateDialog.jsp">
	<%--@elvariable id="annonce" type="ch.vd.unireg.annonceIDE.AnnonceIDEView"--%>
	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
		<fieldset class="information">
			<legend><span>Enveloppe</span></legend>
			<table>
				<tr class="<unireg:nextRowClass/>">
					<td width="25%">Numéro d'annonce&nbsp;:</td>
					<td width="25%">${annonce.numero}</td>
					<td width="25%">Utilisateur&nbsp;:</td>
					<td width="25%">${annonce.utilisateur == null ? null : annonce.utilisateur.userId}</td>
				</tr>
				<tr class="<unireg:nextRowClass/>">
					<td width="25%">Date d'annonce&nbsp;:</td>
					<td width="25%"><fmt:formatDate value="${annonce.dateAnnonce}" pattern="dd.MM.yyyy HH:mm:ss"/></td>
					<td width="25%">Application&nbsp;:</td>
					<td width="25%">${annonce.serviceIDE.applicationName}</td>
				</tr>
			</table>
		</fieldset>
		<fieldset class="information">
			<legend><span>Annonce</span></legend>
			<table>
				<tr class="<unireg:nextRowClass/>">
					<td width="25%">Type&nbsp;:</td>
					<td width="25%"><fmt:message key="option.type.annonce.${annonce.type}"/></td>
					<td width="25%">Statut&nbsp;:</td>
					<td width="25%"><fmt:message key="option.statut.annonce.${annonce.statut.statut}"/></td>
				</tr>
				<c:if test="${annonce.type == 'RADIATION'}">
					<tr class="<unireg:nextRowClass/>">
						<td width="25%">Raison de radiation&nbsp;:</td>
						<td width="25%"><fmt:message key="option.radiation.ide.${annonce.raisonDeRadiation}"/></td>
					</tr>
				</c:if>
				<c:if test="${annonce.type == 'CREATION'}">
					<tr class="<unireg:nextRowClass/>">
						<td width="25%">N° IDE de l'établissement de remplacement&nbsp;:</td>
						<td width="25%"><unireg:numIDE numeroIDE="${annonce.noIdeRemplacant}"/></td>
						<td width="25%">N° Cantonal de l'établissement de remplacement&nbsp;:</td>
						<td width="25%"><unireg:cantonalId cantonalId="${annonce.informationOrganisation.numeroSiteRemplacant}"/></td>
					</tr>
				</c:if>
			</table>
		</fieldset>
		<unireg:nextRowClass reset="1"/>
		<fieldset class="information">
			<legend><span>Contenu</span></legend>
			<table>
				<tr class="<unireg:nextRowClass/>">
					<td width="25%">Type d'établissement&nbsp;:</td>
					<td width="25%"><fmt:message key="option.type.etablissement.${annonce.typeDeSite}"/></td>
				</tr>
				<tr class="<unireg:nextRowClass/>">
					<td width="25%">N° IDE&nbsp;:</td>
					<td width="25%"><unireg:numIDE numeroIDE="${annonce.noIde}"/></td>
					<td width="25%">N° Cantonal&nbsp;:</td>
					<td width="25%"><unireg:cantonalId cantonalId="${annonce.informationOrganisation.numeroSite}"/></td>
				</tr>
				<c:if test="${annonce.typeDeSite != 'ETABLISSEMENT_PRINCIPAL'}">
					<tr class="<unireg:nextRowClass/>">
						<td width="25%">N° IDE de l'établissement principal&nbsp;:</td>
						<td width="25%"><unireg:numIDE numeroIDE="${annonce.noIdeEtablissementPrincipal}"/></td>
						<td width="25%">N° Cantonal de l'établissement principal&nbsp;:</td>
						<td width="25%"><unireg:cantonalId cantonalId="${annonce.informationOrganisation.numeroOrganisation}"/></td>
					</tr>
				</c:if>
				<tr class="<unireg:nextRowClass/>">
					<td width="25%">Nom de l'établissement&nbsp;:</td>
					<td width="25%"><c:out value="${annonce.contenu.nom}"/></td>
					<td rowspan="4" width="25%">Adresse&nbsp;:</td>
					<td rowspan="4" width="25%"><unireg:adresseAnnonce adresse="${annonce.contenu.adresse}"/></td>
				</tr>
				<tr class="<unireg:nextRowClass/>">
					<td width="25%">Nom additionel&nbsp;:</td>
					<td width="25%"><c:out value="${annonce.contenu.nomAdditionnel}"/></td>
				</tr>
				<tr class="<unireg:nextRowClass/>">
					<td width="25%">Forme juridique&nbsp;:</td>
					<td width="25%"><fmt:message key="option.forme.legale.${annonce.contenu.formeLegale}"/></td>
				</tr>
				<tr class="<unireg:nextRowClass/>">
					<td width="25%">Secteur d'activité&nbsp;:</td>
					<td width="25%"><c:out value="${annonce.contenu.secteurActivite}"/></td>
				</tr>
			</table>
		</fieldset>
		<fieldset class="information">
			<legend><span>Commentaire</span></legend>
			<table>
				<tr class="<unireg:nextRowClass/>">
					<td><c:out value="${annonce.commentaire}"/></td>
				</tr>
			</table>
		</fieldset>
	</tiles:put>
</tiles:insert>
