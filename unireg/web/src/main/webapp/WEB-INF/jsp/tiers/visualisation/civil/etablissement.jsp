<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<fieldset>
	<legend><span><fmt:message key="label.etablissement" /></span></legend>
	<unireg:nextRowClass reset="1"/>
	<table>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.raison.sociale" />&nbsp;:</td>
			<td><c:out value="${command.tiers.raisonSociale}"/></td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.nom.enseigne" />&nbsp;:</td>
			<td><c:out value="${command.tiers.enseigne}"/></td>
		</tr>
	</table>
</fieldset>
<fieldset>
	<legend><span><fmt:message key="label.etablissement.domiciles"/></span></legend>

	<input class="noprint" id="showDomicilesHisto" type="checkbox" onclick="refreshDomicilesTable(this);" />
	<label class="noprint" for="showDomicilesHisto"><fmt:message key="label.historique" /></label>

	<display:table name="${command.entreprise.domicilesEtablissement}" id="domicilesEtablissement" requestURI="visu.do" class="display">
		<display:column sortable="true" titleKey="label.date.debut" sortProperty="dateDebut">
			<unireg:regdate regdate="${domicilesEtablissement.dateDebut}"/>
		</display:column>
		<display:column sortable="true" titleKey="label.date.fin" sortProperty="dateFin">
			<unireg:regdate regdate="${domicilesEtablissement.dateFin}"/>
		</display:column>
		<display:column sortable="true" titleKey="label.commune.pays">
			<c:choose>
				<c:when test="${domicilesEtablissement.type == 'COMMUNE_CH' }">
					<unireg:commune ofs="${domicilesEtablissement.noOfsSiege}" displayProperty="nomOfficielAvecCanton" date="${domicilesEtablissement.dateFin}"/>
				</c:when>
				<c:when test="${domicilesEtablissement.type == 'PAYS_HS' }">
					<unireg:pays ofs="${domicilesEtablissement.noOfsSiege}" displayProperty="nomCourt" date="${domicilesEtablissement.dateFin}"/>
				</c:when>
			</c:choose>
		</display:column>
	</display:table>
</fieldset>

<script type="text/javascript">

	/**
	 * Affiche ou filtre les données historiques de la table des capitaux
	 */
	function refreshDomicilesTable(checkbox) {
		var showHisto = $(checkbox).attr('checked');
		var table = $('#domicilesEtablissement');
		Histo.refreshHistoTable(showHisto, table, 1);
	}

	// on rafraîchit toutes les tables une première fois à l'affichage de la page
	refreshDomicilesTable($('#showDomicilesHisto'));

</script>
