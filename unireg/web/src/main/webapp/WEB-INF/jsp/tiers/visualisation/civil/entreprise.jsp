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
<c:set var="page" value="${param.page}"/>
<c:set var="nombreElementsTable" value="${param.nombreElementsTable}"/>

<fieldset>
	<legend><span><fmt:message key="label.entreprise"/></span></legend>

	<unireg:nextRowClass reset="1"/>
	<table>
		<tr class="<unireg:nextRowClass/>" >
			<td width="20%"><fmt:message key="label.numero.ide"/>&nbsp;:</td>
			<td>
				<c:forEach var="noIde" items="${entreprise.numerosIDE}">
					<unireg:numIDE numeroIDE="${noIde}"/><br/>
				</c:forEach>
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>">
			<td width="20%"><fmt:message key="label.civil.registre"/>&nbsp;:</td>
			<td>
				<c:if test=" ${entreprise.connueAuCivil}">
					<fmt:message key="label.connue.civil.rcent"/>
				</c:if>
				<c:if test="${! entreprise.connueAuCivil}">
					<fmt:message key="label.inconnue.civil.rcent"/>
				</c:if>
			</td>
		</tr>
	</table>
</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.raisons.sociales"/></span></legend>

	<c:if test="${page == 'visu' }">
		<input class="noprint" id="showRaisonSocialeHisto" type="checkbox" onclick="refreshRaisonSocialeTable(this);" />
		<label class="noprint" for="showRaisonSocialeHisto"><fmt:message key="label.historique" /></label>
	</c:if>

	<c:if test="${page == 'edit' }"> <%--${autorisations.raisonSociale}--%>
		<table border="0">
			<tr>
				<td>
					<unireg:linkTo name="Ajouter" title="Ajouter une raison sociale" action="/civil/entreprise/raisonsociale/add.do" params="{tiersId:${entreprise.id}}" link_class="add"/>
				</td>
			</tr>
		</table>
	</c:if>
<%-- Ca ne marche pas avec : pagesize="${nombreElementsTable}" --%>
	<display:table name="${entreprise.raisonsSociales}" id="raisonSociale" requestURI="edit.do" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
		<display:column style="width:10%" sortable="true" titleKey="label.date.debut" sortProperty="dateDebut">
			<unireg:regdate regdate="${raisonSociale.dateDebut}"/>
		</display:column>
		<display:column style="width:10%" sortable="true" titleKey="label.date.fin" sortProperty="dateFin">
			<unireg:regdate regdate="${raisonSociale.dateFin}"/>
		</display:column>
		<display:column style="width:60%" sortable="true" titleKey="label.raison.sociale" property="raisonSociale"/>
		<display:column style="width:10%" titleKey="label.source">
			<fmt:message key="option.entreprise.source.${raisonSociale.source}"/>
		</display:column>
		<display:column style="width:10%">
			<c:if test="${page == 'visu' }">
				<c:if test="${raisonSociale.source == 'FISCALE'}" >
					<unireg:consulterLog entityNature="DonneeCivileEntreprise" entityId="${raisonSociale.id}"/>
				</c:if>
			</c:if>
			<c:if test="${page == 'edit' }">
				<c:if test="${!raisonSociale.annule}">
					<unireg:linkTo name="" action="XXXXXXXXX/edit.do" method="GET" params="{raisonSocialeId:${raisonSociale.id}}" link_class="edit" title="Edition de la raison sociale" />
					<c:if test="${raisonSociale.dernierElement}">
						<unireg:linkTo name="" action="/fors/principal/cancel.do" method="POST" params="{raisonSocialeId:${raisonSociale.id}}" link_class="delete"
						               title="Annulation de raison sociale" confirm="Voulez-vous vraiment annuler cette raison sociale ?"/>
					</c:if>
				</c:if>
			</c:if>

		</display:column>
	</display:table>
</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.noms.additionnels"/></span></legend>

	<c:choose>
		<c:when test="${not empty entreprise.nomsAdditionnels}">
			<input class="noprint" id="showNomsAdditionnelsHisto" type="checkbox" onclick="refreshNomsAdditionnelsTable(this);" />
			<label class="noprint" for="showNomsAdditionnelsHisto"><fmt:message key="label.historique" /></label>

			<display:table name="${entreprise.nomsAdditionnels}" id="nomsAdditionnels" requestURI="edit.do" class="display">
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
		</c:when>
		<c:otherwise>
			<unireg:nextRowClass reset="1"/>
			<table>
				<tr class="<unireg:nextRowClass/>" >
					<td width="20%">&nbsp;</td>
					<td></td>
				</tr>
			</table>
		</c:otherwise>
	</c:choose>
</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.sieges"/></span></legend>

	<input class="noprint" id="showSiegesHisto" type="checkbox" onclick="refreshSiegesTable(this);" />
	<label class="noprint" for="showSiegesHisto"><fmt:message key="label.historique" /></label>

	<display:table name="${entreprise.sieges}" id="sieges" requestURI="edit.do" class="display">
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
		<display:column style="width:10%">&nbsp;</display:column>
	</display:table>
</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.formes.juridiques"/></span></legend>

	<input class="noprint" id="showFormesJuridiquesHisto" type="checkbox" onclick="refreshFormesJuridiquesTable(this);" />
	<label class="noprint" for="showFormesJuridiquesHisto"><fmt:message key="label.historique" /></label>

	<display:table name="${entreprise.formesJuridiques}" id="formesJuridiques" requestURI="edit.do" class="display">
		<display:column style="width:10%" sortable="true" titleKey="label.date.debut" sortProperty="dateDebut">
			<unireg:regdate regdate="${formesJuridiques.dateDebut}"/>
		</display:column>
		<display:column style="width:10%" sortable="true" titleKey="label.date.fin" sortProperty="dateFin">
			<unireg:regdate regdate="${formesJuridiques.dateFin}"/>
		</display:column>
		<display:column style="width:60%" sortable="true" titleKey="label.forme.juridique" property="type"/>
		<display:column style="width:10%" titleKey="label.source">
			<fmt:message key="option.entreprise.source.${formesJuridiques.source}"/>
		</display:column>
		<display:column style="width:10%">
			<c:if test="${formesJuridiques.source == 'FISCALE'}" >
				<unireg:consulterLog entityNature="DonneeCivileEntreprise" entityId="${formesJuridiques.id}"/>
			</c:if>
		</display:column>
	</display:table>
</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.capitaux"/></span></legend>

	<input class="noprint" id="showCapitauxHisto" type="checkbox" onclick="refreshCapitauxTable(this);" />
	<label class="noprint" for="showCapitauxHisto"><fmt:message key="label.historique" /></label>

	<fmt:setLocale value="ch" scope="page"/>
	<display:table name="${entreprise.capitaux}" id="capitaux" requestURI="edit.do" class="display">
		<display:column style="width:10%" sortable="true" titleKey="label.date.debut" sortProperty="dateDebut">
			<unireg:regdate regdate="${capitaux.dateDebut}"/>
		</display:column>
		<display:column style="width:10%" sortable="true" titleKey="label.date.fin" sortProperty="dateFin">
			<unireg:regdate regdate="${capitaux.dateFin}"/>
		</display:column>
		<display:column style="width:60%" sortable="true" titleKey="label.capital.libere" sortProperty="capitalLibere.montant">
			<c:if test="${capitaux.capitalLibere != null}">
				<unireg:currency value="${capitaux.capitalLibere.montant}"/>&nbsp;<c:out value="${capitaux.capitalLibere.monnaie}"/>
			</c:if>
		</display:column>
		<display:column style="width:10%" titleKey="label.source">
			<fmt:message key="option.entreprise.source.${capitaux.source}"/>
		</display:column>
		<display:column style="width:10%" class="action">
			<c:if test="${capitaux.source == 'FISCALE'}" >
				<unireg:consulterLog entityNature="DonneeCivileEntreprise" entityId="${capitaux.id}"/>
			</c:if>
		</display:column>
	</display:table>

</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.rc"/></span></legend>

	<unireg:nextRowClass reset="1"/>
	<table>
		<tr class="<unireg:nextRowClass/>" >
			<td width="20%"><fmt:message key="label.date.inscription"/>&nbsp;:</td>
			<td>
				<unireg:regdate regdate="${entreprise.dateInscriptionRC}"/>
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="20%"><fmt:message key="label.status"/>&nbsp;:</td>
			<td>
				${entreprise.statusRC}
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="20%"><fmt:message key="label.date.radiation"/>&nbsp;:</td>
			<td>
				<unireg:regdate regdate="${entreprise.dateRadiationRC}"/>
			</td>
		</tr>
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
				${entreprise.statusIde}
			</td>
		</tr>
	</table>
</fieldset>

<c:if test="${page == 'visu' }">
	<script type="text/javascript">

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
			var table = $('#formesJuridiques');
			Histo.refreshHistoTable(showHisto, table, 1);
		}

		/**
		 * Affiche ou filtre les données historiques de la table des capitaux
		 */
		function refreshCapitauxTable(checkbox) {
			var showHisto = $(checkbox).attr('checked');
			var table = $('#capitaux');
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