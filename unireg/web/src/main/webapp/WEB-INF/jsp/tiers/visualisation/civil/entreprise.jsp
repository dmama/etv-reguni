<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:choose>
	<c:when test="${command != null}">
		<c:set var="entreprise" value="${command.entreprise}" /><%-- TiersVisuController --%>
	</c:when>
	<c:when test="${data != null}">
		<c:set var="entreprise" value="${data}" /><%-- CivilEditController --%>
	</c:when>
</c:choose>
<%--@elvariable id="entreprise" type="ch.vd.uniregctb.entreprise.EntrepriseView"--%>

<c:set var="page" value="${param.page}"/>
<c:set var="nombreElementsTable" value="${param.nombreElementsTable}"/>
<unireg:setAuth var="autorisations" tiersId="${entreprise.id}"/>

<fieldset>
	<legend><span><fmt:message key="label.entreprise"/></span></legend>
	<c:if test="${page == 'edit' && (entreprise.degreAssocCivil == 'FISCAL' || empty entreprise.numerosIDE) && autorisations.donneesCiviles && autorisations.identificationEntreprise}">
		<table border="0">
			<tr>
				<td>
					<unireg:raccourciModifier link="../entreprise/ide/edit.do?id=${entreprise.id}" tooltip="Modifier le numéro IDE" display="label.bouton.modifier"/>
				</td>
			</tr>
		</table>
	</c:if>
	<unireg:nextRowClass reset="1"/>
	<table>
		<tr class="<unireg:nextRowClass/>">
			<td width="20%"><fmt:message key="label.numero.registre.entreprises"/>&nbsp;:</td>
			<td>
				<c:if test="${entreprise.noCantonal != null}">
					<c:out value="${entreprise.noCantonal}"/>
				</c:if>
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="20%"><fmt:message key="label.numero.ide"/>&nbsp;:</td>
			<td>
				<unireg:numIDE numeroIDE="${entreprise.numerosIDE}"/>
			</td>
		</tr>
	</table>
</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.secteur.activite" /></span></legend>
	<c:if test="${page == 'edit' && autorisations.donneesCiviles}">
		<table border="0">
			<tr>
				<td>
					<unireg:raccourciModifier link="../entreprise/secteuractivite/edit.do?tiersId=${entreprise.id}" tooltip="Editer la description du secteur d'activité de l'entreprise" display="label.bouton.modifier"/>
				</td>
			</tr>
		</table>
	</c:if>
	<unireg:nextRowClass reset="1"/>
	<table>
		<tr class="<unireg:nextRowClass/>" >
			<td width="20%"><fmt:message key="label.secteur.activite.long" />&nbsp;:</td>
			<td><c:out value="${entreprise.secteurActivite}"/></td>
		</tr>
	</table>
</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.raisons.sociales"/></span></legend>

	<c:if test="${page == 'visu' }">
		<input class="noprint" id="showRaisonSocialeHisto" type="checkbox" onclick="refreshRaisonSocialeTable(this);" />
		<label class="noprint" for="showRaisonSocialeHisto"><fmt:message key="label.historique" /></label>
	</c:if>

	<c:if test="${page == 'edit' && autorisations.donneesCiviles}">
		<table border="0">
			<tr>
				<td>
					<unireg:linkTo name="Ajouter" title="Ajouter une raison sociale" action="/civil/entreprise/raisonsociale/add.do" params="{tiersId:${entreprise.id}}" link_class="add"/>
				</td>
			</tr>
		</table>
	</c:if>
<%-- Ca ne marche pas avec : pagesize="${nombreElementsTable}" --%>
	<display:table name="${entreprise.raisonsSociales}" id="raisonSociale" requestURI="${page}.do" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
		<display:column style="width:10%" sortable="true" titleKey="label.date.debut" sortProperty="dateDebut">
			<unireg:regdate regdate="${raisonSociale.dateDebut}"/>
		</display:column>
		<display:column style="width:10%" sortable="true" titleKey="label.date.fin" sortProperty="dateFin">
			<unireg:regdate regdate="${raisonSociale.dateFin}"/>
		</display:column>
		<display:column style="width:60%" sortable="true" titleKey="label.raison.sociale" sortProperty="raisonSociale">
			<c:out value="${raisonSociale.raisonSociale}"/>
		</display:column>
		<display:column style="width:10%" titleKey="label.source">
			<fmt:message key="option.entreprise.source.${raisonSociale.source}"/>
		</display:column>
		<display:column style="width:10%">
			<c:if test="${raisonSociale.source == 'FISCALE'}" >
				<c:if test="${page == 'visu' }">
					<unireg:consulterLog entityNature="DonneeCivileEntreprise" entityId="${raisonSociale.id}"/>
				</c:if>
				<c:if test="${page == 'edit' }">
					<c:if test="${!raisonSociale.annule}">
						<unireg:linkTo name="" action="/civil/entreprise/raisonsociale/edit.do" method="GET" params="{raisonSocialeId:${raisonSociale.id}}" link_class="edit" title="Edition de la raison sociale" />
						<c:if test="${raisonSociale.dernierElement}">
							<unireg:linkTo name="" action="/civil/entreprise/cancel.do" method="POST" params="{raisonSocialeId:${raisonSociale.id}}" link_class="delete"
							               title="Annulation de raison sociale" confirm="Voulez-vous vraiment annuler cette raison sociale ?"/>
						</c:if>
					</c:if>
				</c:if>
			</c:if>
		</display:column>
	</display:table>
</fieldset>

<c:if test="${not empty entreprise.nomsAdditionnels}">
	<fieldset>
		<legend><span><fmt:message key="label.noms.additionnels"/></span></legend>
		<c:if test="${page == 'visu' }">
			<input class="noprint" id="showNomsAdditionnelsHisto" type="checkbox" onclick="refreshNomsAdditionnelsTable(this);" />
			<label class="noprint" for="showNomsAdditionnelsHisto"><fmt:message key="label.historique" /></label>
		</c:if>

		<display:table name="${entreprise.nomsAdditionnels}" id="nomsAdditionnels" requestURI="${page}.do" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
			<display:column style="width:10%" sortable="true" titleKey="label.date.debut" sortProperty="dateDebut">
				<unireg:regdate regdate="${nomsAdditionnels.dateDebut}"/>
			</display:column>
			<display:column style="width:10%" sortable="true" titleKey="label.date.fin" sortProperty="dateFin">
				<unireg:regdate regdate="${nomsAdditionnels.dateFin}"/>
			</display:column>
			<display:column style="width:60%" sortable="true" titleKey="label.raison.sociale" property="payload"/>
			<display:column style="width:10%" titleKey="label.source">
				<fmt:message key="option.entreprise.source.CIVILE"/>
			</display:column>
			<display:column style="width:10%">&nbsp;</display:column>
		</display:table>
	</fieldset>
</c:if>

<fieldset>
	<legend><span><fmt:message key="label.sieges"/></span></legend>

	<c:if test="${page == 'visu' }">
		<input class="noprint" id="showSiegesHisto" type="checkbox" onclick="refreshSiegesTable(this);" />
		<label class="noprint" for="showSiegesHisto"><fmt:message key="label.historique" /></label>
	</c:if>

	<c:if test="${page == 'edit' && autorisations.donneesCiviles}">
		<table border="0">
			<tr>
				<td>
					<unireg:linkTo name="Ajouter" title="Ajouter un siège social" action="/civil/entreprise/siege/add.do" params="{tiersId:${entreprise.id}}" link_class="add"/>
				</td>
			</tr>
		</table>
	</c:if>

	<display:table name="${entreprise.sieges}" id="sieges" requestURI="${page}.do" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
		<display:column style="width:10%" sortable="true" titleKey="label.date.debut" sortProperty="dateDebut">
			<unireg:regdate regdate="${sieges.dateDebut}"/>
		</display:column>
		<display:column style="width:10%" sortable="true" titleKey="label.date.fin" sortProperty="dateFin">
			<unireg:regdate regdate="${sieges.dateFin}"/>
		</display:column>
		<display:column style="width:60%" sortable="true" titleKey="label.commune.pays">
			<c:choose>
				<c:when test="${sieges.type == 'COMMUNE_CH' }">
					<unireg:commune ofs="${sieges.noOfsSiege}" displayProperty="nomOfficielAvecCanton" date="${sieges.dateFin}"/>
				</c:when>
				<c:when test="${sieges.type == 'PAYS_HS' }">
					<unireg:pays ofs="${sieges.noOfsSiege}" displayProperty="nomCourt" date="${sieges.dateFin}"/>
				</c:when>
			</c:choose>
		</display:column>
		<display:column style="width:10%" titleKey="label.source">
			<fmt:message key="option.entreprise.source.${sieges.source}"/>
		</display:column>
		<display:column style="width:10%">
			<c:if test="${sieges.source == 'FISCALE'}" >
				<c:if test="${page == 'visu' }">
					<unireg:consulterLog entityNature="DomicileEtablissement" entityId="${sieges.id}"/>
				</c:if>
				<c:if test="${page == 'edit' }">
					<c:if test="${!sieges.annule}">
						<unireg:linkTo name="" action="/civil/entreprise/siege/edit.do" method="GET" params="{domicileId:${sieges.id}, peutEditerDateFin:${sieges.peutEditerDateFin}, entrepriseId:${entreprise.id}}" link_class="edit" title="Edition du siège" />
						<c:if test="${sieges.dernierElement}">
							<unireg:linkTo name="" action="/civil/entreprise/siege/cancel.do" method="POST" params="{domicileId:${sieges.id}, entrepriseId:${entreprise.id}}" link_class="delete"
							               title="Annulation du siège" confirm="Voulez-vous vraiment annuler ce siège ?"/>
						</c:if>
					</c:if>
				</c:if>
			</c:if>
		</display:column>
	</display:table>
</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.formes.juridiques"/></span></legend>

	<c:if test="${page == 'visu' }">
		<input class="noprint" id="showFormesJuridiquesHisto" type="checkbox" onclick="refreshFormesJuridiquesTable(this);" />
		<label class="noprint" for="showFormesJuridiquesHisto"><fmt:message key="label.historique" /></label>
	</c:if>

	<c:if test="${page == 'edit' && autorisations.donneesCiviles}">
		<table border="0">
			<tr>
				<td>
					<unireg:linkTo name="Ajouter" title="Ajouter une forme juridique" action="/civil/entreprise/formejuridique/add.do" params="{tiersId:${entreprise.id}}" link_class="add"/>
				</td>
			</tr>
		</table>
	</c:if>

	<display:table name="${entreprise.formesJuridiques}" id="formeJuridique" requestURI="${page}.do" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
		<display:column style="width:10%" sortable="true" titleKey="label.date.debut" sortProperty="dateDebut">
			<unireg:regdate regdate="${formeJuridique.dateDebut}"/>
		</display:column>
		<display:column style="width:10%" sortable="true" titleKey="label.date.fin" sortProperty="dateFin">
			<unireg:regdate regdate="${formeJuridique.dateFin}"/>
		</display:column>
		<display:column style="width:60%" sortable="true" titleKey="label.forme.juridique" property="type"/>
		<display:column style="width:10%" titleKey="label.source">
			<fmt:message key="option.entreprise.source.${formeJuridique.source}"/>
		</display:column>
		<display:column style="width:10%">
			<c:if test="${formeJuridique.source == 'FISCALE'}" >
				<c:if test="${page == 'visu' }">
					<unireg:consulterLog entityNature="DonneeCivileEntreprise" entityId="${formeJuridique.id}"/>
				</c:if>
				<c:if test="${page == 'edit' }">
					<c:if test="${!formeJuridique.annule}">
						<unireg:linkTo name="" action="/civil/entreprise/formejuridique/edit.do" method="GET" params="{formeJuridiqueId:${formeJuridique.id}}" link_class="edit" title="Edition de la forme juridique" />
						<c:if test="${formeJuridique.dernierElement}">
							<unireg:linkTo name="" action="/civil/entreprise/formejuridique/cancel.do" method="POST" params="{formeJuridiqueId:${formeJuridique.id}}" link_class="delete"
							               title="Annulation de forme juridique" confirm="Voulez-vous vraiment annuler cette forme juridique ?"/>
						</c:if>
					</c:if>
				</c:if>
			</c:if>
		</display:column>
	</display:table>
</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.capitaux"/></span></legend>

	<c:if test="${page == 'visu'}">
		<input class="noprint" id="showCapitauxHisto" type="checkbox" onclick="refreshCapitauxTable(this);" />
		<label class="noprint" for="showCapitauxHisto"><fmt:message key="label.historique" /></label>
	</c:if>

	<c:if test="${page == 'edit' && autorisations.donneesCiviles}">
		<table border="0">
			<tr>
				<td>
					<unireg:linkTo name="Ajouter" title="Ajouter un capital" action="/civil/entreprise/capital/add.do" params="{tiersId:${entreprise.id}}" link_class="add"/>
				</td>
			</tr>
		</table>
	</c:if>

	<fmt:setLocale value="ch" scope="page"/>
	<display:table name="${entreprise.capitaux}" id="capital" requestURI="${page}.do" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
		<display:column style="width:10%" sortable="true" titleKey="label.date.debut" sortProperty="dateDebut">
			<unireg:regdate regdate="${capital.dateDebut}"/>
		</display:column>
		<display:column style="width:10%" sortable="true" titleKey="label.date.fin" sortProperty="dateFin">
			<unireg:regdate regdate="${capital.dateFin}"/>
		</display:column>
		<display:column style="width:60%" sortable="true" titleKey="label.capital.libere" sortProperty="capitalLibere.montant">
			<c:if test="${capital.capitalLibere != null}">
				<unireg:currency value="${capital.capitalLibere.montant}"/>&nbsp;<c:out value="${capital.capitalLibere.monnaie}"/>
			</c:if>
		</display:column>
		<display:column style="width:10%" titleKey="label.source">
			<fmt:message key="option.entreprise.source.${capital.source}"/>
		</display:column>
		<display:column style="width:10%">
			<c:if test="${capital.source == 'FISCALE'}" >
				<c:if test="${page == 'visu' }">
					<unireg:consulterLog entityNature="DonneeCivileEntreprise" entityId="${capital.id}"/>
				</c:if>
				<c:if test="${page == 'edit' }">
					<c:if test="${!capital.annule}">
						<unireg:linkTo name="" action="/civil/entreprise/capital/edit.do" method="GET" params="{capitalId:${capital.id}}" link_class="edit" title="Edition du capital" />
						<unireg:linkTo name="" action="/civil/entreprise/capital/cancel.do" method="POST" params="{capitalId:${capital.id}}" link_class="delete"
						               title="Annulation d'un capital" confirm="Voulez-vous vraiment annuler ce capital ?"/>
					</c:if>
				</c:if>
			</c:if>
		</display:column>
	</display:table>

</fieldset>

<c:if test="${page == 'visu'}">
	<fieldset>
		<legend><span><fmt:message key="label.rc"/></span></legend>

		<unireg:nextRowClass reset="1"/>
		<table>
			<tr class="<unireg:nextRowClass/>" >
				<td width="10%"><fmt:message key="label.date.inscription"/>&nbsp;:</td>
				<td width="10%"><fmt:message key="label.vaud"/>&nbsp;:</td>
				<td>
					<unireg:regdate regdate="${entreprise.dateInscriptionRCVD}"/>
				</td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				<td width="10%">&nbsp;</td>
				<td width="10%"><fmt:message key="label.suisse"/>&nbsp;:</td>
				<td>
					<unireg:regdate regdate="${entreprise.dateInscriptionRC}"/>
				</td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				<td width="10%"><fmt:message key="label.status"/>&nbsp;:</td>
				<td width="10%">&nbsp;</td>
				<td>
					<c:if test="${! empty entreprise.statusRC}">
						<fmt:message key="option.statut.rc.${entreprise.statusRC}"/>
					</c:if>
				</td>
			</tr>
			<c:if test="${!empty entreprise.dateRadiationRCVD || !empty entreprise.dateRadiationRC}">
				<tr class="<unireg:nextRowClass/>" >
					<td width="10%"><fmt:message key="label.date.radiation"/>&nbsp;:</td>
					<td width="10%"><fmt:message key="label.vaud"/>&nbsp;:</td>
					<td>
						<unireg:regdate regdate="${entreprise.dateRadiationRCVD}"/>
					</td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td width="10%">&nbsp;</td>
					<td width="10%"><fmt:message key="label.suisse"/>&nbsp;:</td>
					<td>
						<unireg:regdate regdate="${entreprise.dateRadiationRC}"/>
					</td>
				</tr>
			</c:if>
		</table>
	</fieldset>

	<fieldset>
		<legend><span><fmt:message key="label.ide"/></span></legend>

		<unireg:nextRowClass reset="1"/>
		<table>
			<tr class="<unireg:nextRowClass/>" >
				<td width="20%"><fmt:message key="label.date.inscription"/>&nbsp;:</td>
				<td>
					<unireg:regdate regdate="${entreprise.dateInscriptionIde}"/>
				</td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				<td width="20%"><fmt:message key="label.status"/>&nbsp;:</td>
				<td>
					<c:if test="${! empty entreprise.statusIde}">
						<fmt:message key="option.statut.ide.${entreprise.statusIde}"/><span class="jTip formInfo" title="<c:url value="/htm/statutIDE.htm"/>" id="statutIDE">?</span>
					</c:if>
				</td>
			</tr>
		</table>
	</fieldset>
</c:if>

<c:if test="${page == 'visu' }">
	<script type="text/javascript">

		$(function() {
			Tooltips.activate_ajax_tooltips();
		});

		/**
		 * Affiche ou filtre les données historiques de la table des raisons sociales
		 */
		function refreshRaisonSocialeTable(checkbox) {
			var showHisto = $(checkbox).attr('checked');
			var table = $('#raisonSociale');
			Histo.refreshHistoTable(showHisto, table, 1);
		}

		/**
		 * Affiche ou filtre les données historiques de la table des noms additionnels
		 */
		function refreshNomsAdditionnelsTable(checkbox) {
			var showHisto = $(checkbox).attr('checked');
			var table = $('#nomsAdditionnels');
			Histo.refreshHistoTable(showHisto, table, 1);
		}

		/**
		 * Affiche ou filtre les données historiques de la table des sièges
		 */
		function refreshSiegesTable(checkbox) {
			var showHisto = $(checkbox).attr('checked');
			var table = $('#sieges');
			Histo.refreshHistoTable(showHisto, table, 1);
		}

		/**
		 * Affiche ou filtre les données historiques de la table des formes juridiques
		 */
		function refreshFormesJuridiquesTable(checkbox) {
			var showHisto = $(checkbox).attr('checked');
			var table = $('#formeJuridique');
			Histo.refreshHistoTable(showHisto, table, 1);
		}

		/**
		 * Affiche ou filtre les données historiques de la table des capitaux
		 */
		function refreshCapitauxTable(checkbox) {
			var showHisto = $(checkbox).attr('checked');
			var table = $('#capital');
			Histo.refreshHistoTable(showHisto, table, 1);
		}

		// on rafraîchit toutes les tables une première fois à l'affichage de la page
		refreshRaisonSocialeTable($('#raisonSociale'));
		refreshNomsAdditionnelsTable($('#nomsAdditionnels'));
		refreshSiegesTable($('#showSiegesHisto'));
		refreshFormesJuridiquesTable($('#showFormesJuridiquesHisto'));
		refreshCapitauxTable($('#showCapitauxHisto'));

	</script>
</c:if>