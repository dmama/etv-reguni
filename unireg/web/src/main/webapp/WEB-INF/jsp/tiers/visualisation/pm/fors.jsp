<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<fieldset>
	<legend><span><fmt:message key="label.fors.fiscaux"/></span></legend>

	<c:if test="${empty command.entreprise.forsFiscaux}">
		<fmt:message key="no.data" />
	</c:if>

	<c:if test="${not empty command.entreprise.forsFiscaux}">

		<input class="noprint" id="showforsFiscauxPMHisto" type="checkbox" onclick="refreshforsFiscauxPM(this);" />
		<label class="noprint" for="showforsFiscauxPMHisto"><fmt:message key="label.historique" /></label>

		<display:table name="${command.entreprise.forsFiscaux}" id="forsFiscauxPM" requestURI="visu.do" class="display">

			<display:column sortable ="true" titleKey="label.genre.impot">
				<fmt:message key="option.genre.impot.${forsFiscauxPM.genreImpot}"  />
			</display:column>
			<display:column sortable ="true" titleKey="label.rattachement" >
				<fmt:message key="option.rattachement.${forsFiscauxPM.motifRattachement}" />
			</display:column>

			<display:column sortable ="true" titleKey="label.for.abrege">
				<c:choose>
					<c:when test="${forsFiscauxPM.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD' }">
						<unireg:commune ofs="${forsFiscauxPM.numeroForFiscalCommune}" displayProperty="nomOfficiel" titleProperty="noOFS" date="${forsFiscauxPM.dateDebut}"/>
					</c:when>
					<c:when test="${forsFiscauxPM.typeAutoriteFiscale == 'COMMUNE_HC' }">
						<unireg:commune ofs="${forsFiscauxPM.numeroForFiscalCommuneHorsCanton}" displayProperty="nomOfficiel" titleProperty="noOFS" date="${forsFiscauxPM.dateDebut}"/>
						(<unireg:commune ofs="${forsFiscauxPM.numeroForFiscalCommuneHorsCanton}" displayProperty="sigleCanton" date="${forsFiscauxPM.dateDebut}"/>)
					</c:when>
					<c:when test="${forsFiscauxPM.typeAutoriteFiscale == 'PAYS_HS' }">
						<unireg:pays ofs="${forsFiscauxPM.numeroForFiscalPays}" displayProperty="nomCourt" date="${forsFiscauxPM.dateDebut}"/>
					</c:when>
				</c:choose>
			</display:column>

			<display:column sortable="true" titleKey="label.date.ouv" sortProperty="dateDebut">
				<unireg:regdate regdate="${forsFiscauxPM.dateDebut}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.date.fer" sortProperty="dateFin">
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
		var showHisto = $(checkbox).attr('checked');
		var table = $('#forsFiscauxPM');
		Histo.refreshHistoTable(showHisto, table, 4);
	}

	// on rafraîchit toutes les tables une première fois à l'affichage de la page
	refreshforsFiscauxPM($('#showforsFiscauxPMHisto'));

</script>
