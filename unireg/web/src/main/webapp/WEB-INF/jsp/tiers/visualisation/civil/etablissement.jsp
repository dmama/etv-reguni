<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="command" type="ch.vd.uniregctb.entreprise.TiersView"--%>
<%--@elvariable id="data" type="ch.vd.uniregctb.entreprise.TiersView"--%>
<%--@elvariable id="etablissement" type="ch.vd.uniregctb.entreprise.EtablissementView"--%>

<c:choose>
	<c:when test="${command != null}">
		<c:set var="etablissement" value="${command.etablissement}" /><%-- TiersVisuController --%>
	</c:when>
	<c:when test="${data != null}">
		<c:set var="etablissement" value="${data}" /><%-- CivilEditController --%>
	</c:when>
</c:choose>
<c:set var="page" value="${param.page}"/>
<c:set var="nombreElementsTable" value="${param.nombreElementsTable}"/>

<c:set var="domicilesHisto" value="${command.domicilesHisto != null ? command.domicilesHisto : false}" />

<unireg:setAuth var="autorisations" tiersId="${etablissement.id}"/>

<fieldset>
<legend><span><fmt:message key="label.etablissement"/></span></legend>
<c:choose>
	<c:when test="${not empty command.exceptionDonneesCiviles}">
		<unireg:nextRowClass reset="1"/>
		<table>
			<tr class="<unireg:nextRowClass/>">
				<td class="erreur">
					<c:out value="Erreur lors de l'accès au service civil: ${command.exceptionDonneesCiviles}"/>
				</td>
			</tr>
		</table>

	</c:when>
	<c:otherwise>

		<c:if test="${page == 'edit' && (etablissement.degreAssocCivilEntreprise == 'FISCAL' || empty etablissement.numerosIDE) && autorisations.donneesCiviles && autorisations.identificationEntreprise}">
			<table border="0">
				<tr>
					<td>
						<unireg:raccourciModifier link="../etablissement/ide/edit.do?id=${etablissement.id}" tooltip="Modifier le numéro IDE" display="label.bouton.modifier"/>
					</td>
				</tr>
			</table>
		</c:if>
		<unireg:nextRowClass reset="1"/>
		<table>
			<tr class="<unireg:nextRowClass/>">
				<td width="20%"><fmt:message key="label.numero.registre.entreprises"/>&nbsp;:</td>
				<td>
					<c:if test="${etablissement.noCantonal != null}">
						<c:out value="${etablissement.noCantonal}"/>
					</c:if>
				</td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				<td width="20%"><fmt:message key="label.numero.ide"/>&nbsp;:</td>
				<td>
					<c:forEach var="noIde" items="${etablissement.numerosIDE}">
						<unireg:numIDE numeroIDE="${noIde}"/><br/>
					</c:forEach>
				</td>
			</tr>
		</table>
		</fieldset>
		<fieldset>
			<legend><span><fmt:message key="label.raison.enseigne" /></span></legend>
			<c:if test="${page == 'edit' && autorisations.donneesCiviles}">
				<table border="0">
					<tr>
						<td>
							<unireg:raccourciModifier link="../etablissement/raisonenseigne/edit.do?tiersId=${etablissement.id}" tooltip="Editer les noms de l'établissement" display="label.bouton.modifier"/>
						</td>
					</tr>
				</table>
			</c:if>
			<unireg:nextRowClass reset="1"/>
			<table>
				<tr class="<unireg:nextRowClass/>" >
					<td width="20%"><fmt:message key="label.raison.sociale" />&nbsp;:</td>
					<td><c:out value="${etablissement.raisonSociale}"/></td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td width="15%"><fmt:message key="label.nom.enseigne" />&nbsp;:</td>
					<td><c:out value="${etablissement.enseigne}"/></td>
					<td width="10%">
						<c:if test="${page != 'edit' && etablissement.degreAssocCivilEntreprise == 'CIVIL_ESCLAVE' && autorisations.donneesCiviles}">
							<unireg:linkTo name="" action="/civil/etablissement/enseigne/edit.do?tiersId=${etablissement.id}" link_class="edit" title="Editer l'enseigne de l'établissement"/>
						</c:if>
					</td>
				</tr>
			</table>
		</fieldset>
		<fieldset>
			<legend><span><fmt:message key="label.etablissement.domiciles"/></span></legend>

			<c:if test="${page == 'visu' }">
				<input class="noprint" id="showDomicilesHisto" type="checkbox" <c:if test="${domicilesHisto}">checked</c:if> onclick="window.location = App.toggleBooleanParam(window.location, 'domicilesHisto', true);" />
				<label class="noprint" for="showDomicilesHisto"><fmt:message key="label.historique" /></label>
			</c:if>
			<c:if test="${page == 'edit' && autorisations.donneesCiviles}">
				<table border="0">
					<tr>
						<td>
							<unireg:linkTo name="Ajouter" title="Ajouter un domicile" action="/civil/etablissement/domicile/add.do" params="{tiersId:${etablissement.id}}" link_class="add"/>
						</td>
					</tr>
				</table>
			</c:if>

			<display:table name="${etablissement.domiciles}" id="domicile" requestURI="${page}.do" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
				<display:column style="width:10%" sortable="true" titleKey="label.date.debut" sortProperty="dateDebut">
					<unireg:regdate regdate="${domicile.dateDebut}"/>
				</display:column>
				<display:column style="width:10%" sortable="true" titleKey="label.date.fin" sortProperty="dateFin">
					<unireg:regdate regdate="${domicile.dateFin}"/>
				</display:column>
				<display:column style="width:60%" sortable="true" titleKey="label.commune.pays">
					<c:choose>
						<c:when test="${domicile.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD' || domicile.typeAutoriteFiscale == 'COMMUNE_HC'}">
							<unireg:commune ofs="${domicile.numeroOfsAutoriteFiscale}" displayProperty="nomOfficielAvecCanton" date="${domicile.dateFin}"/>
						</c:when>
						<c:when test="${domicile.typeAutoriteFiscale == 'PAYS_HS' }">
							<unireg:pays ofs="${domicile.numeroOfsAutoriteFiscale}" displayProperty="nomCourt" date="${domicile.dateFin}"/>
						</c:when>
					</c:choose>
				</display:column>
				<display:column style="width:10%" titleKey="label.source">
					<fmt:message key="option.entreprise.source.${domicile.source}"/>
				</display:column>
				<display:column style="width:10%">
					<c:if test="${domicile.source == 'FISCALE'}" >
						<c:if test="${page == 'visu' }">
							<unireg:consulterLog entityNature="DomicileEtablissement" entityId="${domicile.id}"/>
						</c:if>
						<c:if test="${page == 'edit' }">
							<c:if test="${!domicile.annule}">
								<unireg:linkTo name="" action="/civil/etablissement/domicile/edit.do" method="GET" params="{domicileId:${domicile.id}, peutEditerDateFin:${domicile.peutEditerDateFin}}" link_class="edit" title="Edition du domicile" />
								<c:if test="${domicile.dernierElement}">
									<unireg:linkTo name="" action="/civil/etablissement/domicile/cancel.do" method="POST" params="{domicileId:${domicile.id}}" link_class="delete"
									               title="Annulation du domicile" confirm="Voulez-vous vraiment annuler ce domicile ?"/>
								</c:if>
							</c:if>
						</c:if>
					</c:if>
				</display:column>
			</display:table>
		</fieldset>

	</c:otherwise>
</c:choose>

<c:if test="${page == 'visu' }">
	<script type="text/javascript">

		/**
		 * Affiche ou filtre les données historiques de la table des capitaux
		 */
		function refreshDomicilesTable(checkbox) {
			var showHisto = $(checkbox).attr('checked');
			var table = $('#domicile');
			Histo.refreshHistoTable(showHisto, table, 1);
		}

		// on rafraîchit toutes les tables une première fois à l'affichage de la page
		refreshDomicilesTable($('#showDomicilesHisto'));

	</script>
</c:if>