<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<fieldset>
	<legend><span><fmt:message key="label.source.donnees"/></span></legend>

	<unireg:nextRowClass reset="1"/>
	<table>
		<tr>
			<td width="30%"><fmt:message key="label.source.donnees.provenance"/>&nbsp;:</td>
			<td><fmt:message key="${command.entreprise.sourceKey}"/></td>
		</tr>
	</table>
</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.entreprise"/></span></legend>

	<unireg:nextRowClass reset="1"/>
	<table>
		<tr class="<unireg:nextRowClass/>" >
			<td width="30%"><fmt:message key="label.numero.ide"/>&nbsp;:</td>
			<td>
				<c:forEach var="noIde" items="${command.entreprise.numerosIDE}">
					<unireg:numIDE numeroIDE="${noIde}"/><br/>
				</c:forEach>
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="30%"><fmt:message key="label.raison.sociale"/>&nbsp;:</td>
			<td><c:out value="${command.entreprise.raisonSociale}"/></td>
		</tr>
		<c:forEach items="${command.entreprise.autresRaisonsSociales}" var="autreRaisonSociale">
			<tr class="<unireg:nextRowClass/>" >
				<td width="30%"><fmt:message key="label.raison.sociale.autre"/>&nbsp;:</td>
				<td><c:out value="${autreRaisonSociale}"/></td>
			</tr>
		</c:forEach>
	</table>

</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.sieges"/></span></legend>

	<input class="noprint" id="showSiegesHisto" type="checkbox" onclick="refreshSiegesTable(this);" />
	<label class="noprint" for="showSiegesHisto"><fmt:message key="label.historique" /></label>

	<display:table name="${command.entreprise.sieges}" id="sieges" requestURI="visu.do" class="display">
		<display:column sortable="true" titleKey="label.date.debut" sortProperty="dateDebut">
			<unireg:regdate regdate="${sieges.dateDebut}"/>
		</display:column>
		<display:column sortable="true" titleKey="label.date.fin" sortProperty="dateFin">
			<unireg:regdate regdate="${sieges.dateFin}"/>
		</display:column>
		<display:column titleKey="label.commune.pays">
			<c:choose>
				<c:when test="${sieges.type == 'COMMUNE_CH' }">
					<unireg:commune ofs="${sieges.noOfsSiege}" displayProperty="nomOfficiel" date="${sieges.dateFin}"/>
					(<unireg:commune ofs="${sieges.noOfsSiege}" displayProperty="sigleCanton" date="${sieges.dateFin}"/>)
				</c:when>
				<c:when test="${sieges.type == 'PAYS_HS' }">
					<unireg:pays ofs="${sieges.noOfsSiege}" displayProperty="nomCourt" date="${sieges.dateFin}"/>
				</c:when>
			</c:choose>
		</display:column>
	</display:table>
</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.formes.juridiques"/></span></legend>

	<input class="noprint" id="showFormesJuridiquesHisto" type="checkbox" onclick="refreshFormesJuridiquesTable(this);" />
	<label class="noprint" for="showFormesJuridiquesHisto"><fmt:message key="label.historique" /></label>

	<display:table name="${command.entreprise.formesJuridiques}" id="formesJuridiques" requestURI="visu.do" class="display">
		<display:column sortable="true" titleKey="label.date.debut" sortProperty="dateDebut">
			<unireg:regdate regdate="${formesJuridiques.dateDebut}"/>
		</display:column>
		<display:column sortable="true" titleKey="label.date.fin" sortProperty="dateFin">
			<unireg:regdate regdate="${formesJuridiques.dateFin}"/>
		</display:column>
		<display:column sortable="true" titleKey="label.forme.juridique" property="type"/>
	</display:table>
</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.capitaux"/></span></legend>

	<input class="noprint" id="showCapitauxHisto" type="checkbox" onclick="refreshCapitauxTable(this);" />
	<label class="noprint" for="showCapitauxHisto"><fmt:message key="label.historique" /></label>

	<fmt:setLocale value="ch" scope="page"/>
	<display:table name="${command.entreprise.capitaux}" id="capitaux" requestURI="visu.do" class="display">
		<display:column sortable="true" titleKey="label.date.debut" sortProperty="dateDebut">
			<unireg:regdate regdate="${capitaux.dateDebut}"/>
		</display:column>
		<display:column sortable="true" titleKey="label.date.fin" sortProperty="dateFin">
			<unireg:regdate regdate="${capitaux.dateFin}"/>
		</display:column>
		<display:column sortable="true" titleKey="label.capital.action" style="text-align:right" sortProperty="capitalActions.montant">
			<c:if test="${capitaux.capitalActions != null}">
				<unireg:currency value="${capitaux.capitalActions.montant}"/>&nbsp;<c:out value="${capitaux.capitalActions.monnaie}"/>
			</c:if>
		</display:column>
		<display:column sortable="true" titleKey="label.capital.libere" style="text-align:right" sortProperty="capitalLibere.montant">
			<c:if test="${capitaux.capitalLibere != null}">
				<unireg:currency value="${capitaux.capitalLibere.montant}"/>&nbsp;<c:out value="${capitaux.capitalLibere.monnaie}"/>
			</c:if>
		</display:column>
	</display:table>

</fieldset>

<script type="text/javascript">

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
	refreshSiegesTable($('#showSiegesHisto'));
	refreshFormesJuridiquesTable($('#showFormesJuridiquesHisto'));
	refreshCapitauxTable($('#showCapitauxHisto'));

</script>
