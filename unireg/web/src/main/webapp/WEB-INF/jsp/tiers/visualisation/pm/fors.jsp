<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<fieldset>
	<legend><span><fmt:message key="label.fors.fiscaux"/></span></legend>

	<c:if test="${empty command.entreprise.forsFiscaux}">
		<fmt:message key="no.data" />
	</c:if>

	<c:if test="${not empty command.entreprise.forsFiscaux}">

		<input id="showforsFiscauxPMHisto" type="checkbox" onclick="refreshforsFiscauxPM(this);" />
		<label for="showforsFiscauxPMHisto"><fmt:message key="label.historique" /></label>

		<display:table name="${command.entreprise.forsFiscaux}" id="forsFiscauxPM" class="display">

			<display:column sortable ="true" titleKey="label.genre.impot">
				<fmt:message key="option.genre.impot.${forsFiscauxPM.genreImpot}"  />
			</display:column>
			<display:column sortable ="true" titleKey="label.rattachement" >
				<fmt:message key="option.rattachement.${forsFiscauxPM.motifRattachement}" />
			</display:column>

			<display:column sortable ="true" titleKey="label.for.abrege">
				<c:choose>
					<c:when test="${forsFiscauxPM.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD' }">
						<unireg:infra entityId="${forsFiscauxPM.numeroForFiscalCommune}" entityType="commune" entityPropertyName="nomMinuscule"></unireg:infra>
					</c:when>
					<c:when test="${forsFiscauxPM.typeAutoriteFiscale == 'COMMUNE_HC' }">
						<unireg:infra entityId="${forsFiscauxPM.numeroForFiscalCommuneHorsCanton}" entityType="commune" entityPropertyName="nomMinuscule"></unireg:infra>
						(<unireg:infra entityId="${forsFiscauxPM.numeroForFiscalCommuneHorsCanton}" entityType="commune" entityPropertyName="sigleCanton"></unireg:infra>)
					</c:when>
					<c:when test="${forsFiscauxPM.typeAutoriteFiscale == 'PAYS_HS' }">
						<unireg:infra entityId="${forsFiscauxPM.numeroForFiscalPays}" entityType="pays" entityPropertyName="nomMinuscule"></unireg:infra>
					</c:when>
				</c:choose>
			</display:column>

			<display:column sortable="true" titleKey="label.date.ouv">
				<unireg:regdate regdate="${forsFiscauxPM.dateDebut}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.date.fer">
				<unireg:regdate regdate="${forsFiscauxPM.dateFin}"/>
			</display:column>
		</display:table>

	</c:if>

</fieldset>

<script type="text/javascript">

	/**
	 * Affiche ou filtre les données historiques de la table des sièges
	 */
	function refreshforsFiscauxPM(checkbox) {
		var showHisto = checkbox.checked;
		var table = E$('forsFiscauxPM');
		refreshHistoTable(showHisto, table, 4);
	}

	// on rafraîchit toutes les tables une première fois à l'affichage de la page
	refreshforsFiscauxPM(E$('showforsFiscauxPMHisto'));

</script>
