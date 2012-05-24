<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<fieldset>
	<legend><span><fmt:message key="label.entreprise"/></span></legend>

	<unireg:nextRowClass reset="1"/>
	<table>
		<tr class="<unireg:nextRowClass/>" >
			<td width="30%"><fmt:message key="label.numero.ipmro"/>&nbsp;:</td>
			<td><c:out value="${command.entreprise.numeroIPMRO}"/></td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="30%"><fmt:message key="label.designation.abregee"/>&nbsp;:</td>
			<td><c:out value="${command.entreprise.designationAbregee}"/></td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="30%"><fmt:message key="label.raison.sociale.courte"/>&nbsp;:</td>
			<td><c:out value="${command.entreprise.raisonSociale}"/></td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="30%"><fmt:message key="label.raison.sociale.complete"/>&nbsp;:</td>
			<td><c:out value="${command.entreprise.raisonSociale1}"/></td>
		</tr>
		<c:if test="${command.entreprise.raisonSociale2 != null}">
			<tr class="<unireg:nextRowClass/>" >
				<td width="30%"></td>
				<td><c:out value="${command.entreprise.raisonSociale2}"/></td>
			</tr>
		</c:if>
		<c:if test="${command.entreprise.raisonSociale3 != null}">
			<tr class="<unireg:nextRowClass/>" >
				<td width="30%"></td>
				<td><c:out value="${command.entreprise.raisonSociale3}"/></td>
			</tr>
		</c:if>
		<tr class="<unireg:nextRowClass/>" >
			<td width="30%"><fmt:message key="label.date.fin.dernier.exercice.commercial"/>&nbsp;:</td>
			<td><unireg:regdate regdate="${command.entreprise.dateFinDernierExerciceCommercial}"/></td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="30%"><fmt:message key="label.date.bouclement.futur"/>&nbsp;:</td>
			<td><unireg:regdate regdate="${command.entreprise.dateBouclementFuture}"/></td>
		</tr>
	</table>

</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.sieges"/></span></legend>

	<input class="noprint" id="showSiegesHisto" type="checkbox" onclick="refreshSiegesTable(this);" />
	<label class="noprint" for="showSiegesHisto"><fmt:message key="label.historique" /></label>

	<display:table name="${command.entreprise.sieges}" id="sieges" class="display">
		<display:column sortable="true" titleKey="label.date.debut">
			<unireg:regdate regdate="${sieges.dateDebut}"/>
		</display:column>
		<display:column sortable="true" titleKey="label.date.fin">
			<unireg:regdate regdate="${sieges.dateFin}"/>
		</display:column>
		<display:column sortable="true" titleKey="label.commune.pays">
			<c:choose>
				<c:when test="${sieges.type == 'COMMUNE_CH' }">
					<unireg:commune ofs="${sieges.noOfsSiege}" displayProperty="nomMinuscule" date="${sieges.dateFin}"/>
					(<unireg:commune ofs="${sieges.noOfsSiege}" displayProperty="sigleCanton" date="${sieges.dateFin}"/>)
				</c:when>
				<c:when test="${sieges.type == 'PAYS_HS' }">
					<unireg:infra entityId="${sieges.noOfsSiege}" entityType="pays" entityPropertyName="nomMinuscule"></unireg:infra>
				</c:when>
			</c:choose>
		</display:column>
	</display:table>
</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.formes.juridiques"/></span></legend>

	<input class="noprint" id="showFormesJuridiquesHisto" type="checkbox" onclick="refreshFormesJuridiquesTable(this);" />
	<label class="noprint" for="showFormesJuridiquesHisto"><fmt:message key="label.historique" /></label>

	<display:table name="${command.entreprise.formesJuridiques}" id="formesJuridiques" class="display">
		<display:column sortable="true" titleKey="label.date.debut">
			<unireg:regdate regdate="${formesJuridiques.dateDebut}"/>
		</display:column>
		<display:column sortable="true" titleKey="label.date.fin">
			<unireg:regdate regdate="${formesJuridiques.dateFin}"/>
		</display:column>
		<display:column sortable="true" titleKey="label.forme.juridique" property="code"/>
	</display:table>
</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.capitaux"/></span></legend>

	<input class="noprint" id="showCapitauxHisto" type="checkbox" onclick="refreshCapitauxTable(this);" />
	<label class="noprint" for="showCapitauxHisto"><fmt:message key="label.historique" /></label>

	<fmt:setLocale value="ch" scope="page"/>
	<display:table name="${command.entreprise.capitaux}" id="capitaux" class="display">
		<display:column sortable="true" titleKey="label.date.debut">
			<unireg:regdate regdate="${capitaux.dateDebut}"/>
		</display:column>
		<display:column sortable="true" titleKey="label.date.fin">
			<unireg:regdate regdate="${capitaux.dateFin}"/>
		</display:column>
		<display:column sortable="true" titleKey="label.capital.action" style="text-align:right">
			<unireg:currency value="${capitaux.capitalAction}"/>
		</display:column>
		<display:column sortable="true" titleKey="label.capital.libere" style="text-align:right">
			<unireg:currency value="${capitaux.capitalLibere}"/>
		</display:column>
		<display:column sortable="true" titleKey="label.absence.capital.libere.normale" property="absenceCapitalLibereNormale"/>
		<display:column sortable="true" titleKey="label.edition.fosc">
			N°<c:out value="${capitaux.editionFosc.numero}"/> du <unireg:regdate regdate="${capitaux.editionFosc.dateParution}"/>
		</display:column>
	</display:table>

</fieldset>

<script type="text/javascript">

	/**
	 * Affiche ou filtre les données historiques de la table des sièges
	 */
	function refreshSiegesTable(checkbox) {
		var showHisto = $(checkbox).get(0).checked;
		var table = $('#sieges');
		Histo.refreshHistoTable(showHisto, table, 1);
	}

	/**
	 * Affiche ou filtre les données historiques de la table des formes juridiques
	 */
	function refreshFormesJuridiquesTable(checkbox) {
		var showHisto = $(checkbox).get(0).checked;
		var table = $('#formesJuridiques');
		Histo.refreshHistoTable(showHisto, table, 1);
	}

	/**
	 * Affiche ou filtre les données historiques de la table des capitaux
	 */
	function refreshCapitauxTable(checkbox) {
		var showHisto = $(checkbox).get(0).checked;
		var table = $('#capitaux');
		Histo.refreshHistoTable(showHisto, table, 1);
	}

	// on rafraîchit toutes les tables une première fois à l'affichage de la page
	refreshSiegesTable($('#showSiegesHisto'));
	refreshFormesJuridiquesTable($('#showFormesJuridiquesHisto'));
	refreshCapitauxTable($('#showCapitauxHisto'));

</script>
