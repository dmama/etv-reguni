<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<%@page import="ch.vd.unireg.common.LengthConstants"%>

<c:set var="lengthnumcompte" value="<%=LengthConstants.TIERS_NUMCOMPTE%>" scope="request" />
<c:set var="lengthpersonne" value="<%=LengthConstants.TIERS_PERSONNE%>" scope="request" />
<c:set var="lengthtel" value="<%=LengthConstants.TIERS_NUMTEL%>" scope="request" />

<c:set var="lengthadrnom" value="<%=LengthConstants.ADRESSE_NOM%>" scope="request" />
<c:set var="lengthadrnum" value="<%=LengthConstants.ADRESSE_NUM%>" scope="request" />

<%--@elvariable id="idMandant" type="java.lang.Long"--%>
<%--@elvariable id="mode" type="java.lang.String"--%>                   <%-- valeurs : courrier ou perception --%>
<%--@elvariable id="forcageAvecSansTiers" type="java.lang.String"--%>   <%-- valeurs : avec ou sans --%>
<%--@elvariable id="errorMessage" type="java.lang.String"--%>
<%--@elvariable id="addLienCourrierAutorise" type="java.lang.Boolean"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.ajout.mandataire"/>
	</tiles:put>

	<tiles:put name="body">

		<unireg:nextRowClass reset="1"/>
		<unireg:bandeauTiers numero="${idMandant}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" titre="Caractéristiques du mandant" />

		<!-- sauf pour les mandats tiers, on commence par un radio-button pour sélectionner si oui ou non on fait une recherche de tiers -->

		<c:if test="${mode == 'courrier' && addLienCourrierAutorise}">
			<div style="left: 10%; width: 90%; position: relative;">
				<input type="radio" id="avec-tiers" checked="checked" name="avec-sans-tiers" onclick="AddMandataire.showRecherche();"/>
				<label for="avec-tiers">
					<fmt:message key="label.recherche.tiers.mandataire"/>
				</label>
				<br/>
				<input type="radio" id="sans-tiers" name="avec-sans-tiers" onclick="AddMandataire.hideRecherche();"/>
				<label for="sans-tiers">
					<fmt:message key="label.aucun.tiers.mandataire.identifie"/>
				</label>
			</div>
		</c:if>

		<!-- cas de la recherche de tiers -->
		<c:if test="${mode != 'courrier' || addLienCourrierAutorise}">
			<div id="recherche-tiers-mandataire">
				<form:form method="post" id="formRecherche" action="ajouter-list.do">
					<input type="hidden" name="idMandant" value="${idMandant}"/>
					<fieldset>
						<legend><span><fmt:message key="label.criteres.recherche.tiers.mandataire"/></span></legend>
						<form:errors  cssClass="error"/>
						<c:if test="${errorMessage != null}">
							<span class="error"><c:out value="${errorMessage}"/></span>
						</c:if>
						<form:hidden path="typeTiers"/>
						<unireg:nextRowClass reset="0"/>
						<jsp:include page="../../../tiers/recherche/form.jsp">
							<jsp:param name="typeRecherche" value="mandataire" />
							<jsp:param name="paramsEffacer" value="idMandant:${idMandant}"/>
							<jsp:param name="prefixeEffacer" value="/mandataire/${mode}"/>
						</jsp:include>
					</fieldset>
				</form:form>

				<display:table name="list" id="row" pagesize="25" requestURI="/mandataire/${mode}/ajouter-list.do" class="display" sort="list">
					<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncun.tiers.trouve" /></span></display:setProperty>
					<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.tiers.trouve" /></span></display:setProperty>
					<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.tiers.trouves" /></span></display:setProperty>
					<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.tiers.trouves" /></span></display:setProperty>

					<display:column sortable="true" titleKey="label.numero.contribuable" sortProperty="numero" style="width: 15ex;">
						<c:choose>
							<c:when test="${idMandant == row.numero}">
								<unireg:numCTB numero="${row.numero}"/>
							</c:when>
							<c:otherwise>
								<a href="<c:url value="/mandataire/${mode}/ajouter-mandataire-choisi.do"/>?idMandataire=${row.numero}&idMandant=${idMandant}" style="vertical-align: middle; line-height: 2em;"><unireg:numCTB numero="${row.numero}" /></a>
							</c:otherwise>
						</c:choose>
						<c:if test="${row.typeAvatar != null}">
							<div style="float: right;">
								<img alt="" src="<c:url value='/tiers/avatar.do'/>?type=${row.typeAvatar}&url_memorize=false" style="height: 2em;"/>
							</div>
						</c:if>
					</display:column>
					<display:column sortable="true" titleKey="label.numero.ide" sortProperty="numeroIDE">
						<unireg:numIDE numeroIDE="${row.numeroIDE}"/>
					</display:column>
					<display:column sortable="true" titleKey="label.numero.avs" sortProperty="numeroAVS1">
						<unireg:numAVS numeroAssureSocial="${row.numeroAVS1}"/>
					</display:column>
					<display:column sortable="true" titleKey="label.date.naissance.ou.rc" sortProperty="dateNaissanceInscriptionRC">
						<unireg:date date="${row.dateNaissanceInscriptionRC}"/>
					</display:column>
					<display:column sortable="true" titleKey="label.nom.raison">
						<c:out value="${row.nom1}"/>
						<unireg:raccourciDetail tooltip="Adresse de représentation" onClick="Mandataires.showAdresseRepresentation(${row.numero});"/>
					</display:column>
					<display:column titleKey="label.domicile.siege">
						<c:choose>
							<c:when test="${row.domicileEtablissementPrincipal != null && row.domicileEtablissementPrincipal != ''}">
								<c:out value="${row.domicileEtablissementPrincipal}"/>
							</c:when>
							<c:when test="${row.noOfsCommuneDomicile != null}">
								<unireg:commune ofs="${row.noOfsCommuneDomicile}" displayProperty="nomOfficiel"/>
							</c:when>
							<c:otherwise>
								<c:out value="${row.forPrincipal}"/>
							</c:otherwise>
						</c:choose>
					</display:column>
					<display:column sortable="true" titleKey="label.forme.juridique" sortProperty="formeJuridique.code">
						<c:if test="${row.formeJuridique != null}">
							<c:out value="${row.formeJuridique}"/>
						</c:if>
					</display:column>
					<display:column sortable="true" titleKey="label.etat.entreprise.actuel">
						<c:if test="${row.etatEntreprise != null}">
							<fmt:message key="option.etat.entreprise.${row.etatEntreprise}"/>
						</c:if>
					</display:column>
				</display:table>

				<!-- Debut Bouton -->
				<table>
					<tr><td>
						<unireg:buttonTo name="Retour" action="/mandataire/${mode}/edit-list.do" method="get" params="{ctbId:${idMandant}}" />
					</td></tr>
				</table>

			</div>
		</c:if>

		<c:if test="${mode == 'courrier'}">

			<!-- cas sans tiers identifié -->
			<div id="sans-tiers-mandataire" <c:if test="${addLienCourrierAutorise}">style="display: none;"</c:if>>
				<form:form method="post" id="formDonneesMandat" commandName="donneesMandat" action="add-adresse.do">

					<!-- inclusion du tableau sur les données additionnelles d'un mandat -->
					<jsp:include page="add-donnees-mandat.jsp"/>

					<!-- Debut Boutons -->
					<table>
						<tr>
							<td style="text-align: center;">
								<input type="submit" value="<fmt:message key="label.param.add"/>"/>
							</td>
							<td>
								<unireg:buttonTo name="Retour" action="/mandataire/${mode}/edit-list.do" method="get" params="{ctbId:${idMandant}}" />
							</td>
						</tr>
					</table>

				</form:form>
			</div>

			<c:if test="${addLienCourrierAutorise}">
				<script type="application/javascript">
					var AddMandataire = {
						showRecherche: function() {
							$('#recherche-tiers-mandataire').show();
							$('#sans-tiers-mandataire').hide();
						},

						hideRecherche: function() {
							$('#recherche-tiers-mandataire').hide();
							$('#sans-tiers-mandataire').show();
						},

						initShowHideRecherche: function() {
							if ($('#avec-tiers')[0].checked) {
								AddMandataire.showRecherche();
							}
							else {
								AddMandataire.hideRecherche();
							}
						}
					};

					$(function() {
						<c:if test="${forcageAvecSansTiers == 'avec'}">
							$('#avec-tiers').prop('checked', true);
						</c:if>
						<c:if test="${forcageAvecSansTiers == 'sans'}">
							$('#sans-tiers').prop('checked', true);
						</c:if>
						AddMandataire.initShowHideRecherche();
					});
				</script>
			</c:if>

		</c:if>

	</tiles:put>
</tiles:insert>
