<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<span><%-- span vide pour que IE6 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
<fieldset>
	<legend><span><fmt:message key="label.regimes.fiscaux.VD"/></span></legend>

	<c:if test="${empty command.entreprise.regimesFiscauxVD}">
		<fmt:message key="no.data" />
	</c:if>

	<c:if test="${not empty command.entreprise.regimesFiscauxVD}">
	
		<input id="showRegimesVDHisto" type="checkbox" onclick="refreshRegimesVDTable(this);" />
		<label for="showRegimesVDHisto"><fmt:message key="label.historique" /></label>
	
		<display:table name="${command.entreprise.regimesFiscauxVD}" id="regimesVD" class="display">
			<display:column sortable="true" titleKey="label.date.debut">
				<unireg:regdate regdate="${regimesVD.dateDebut}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.date.fin">
				<unireg:regdate regdate="${regimesVD.dateFin}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.type">
				<c:out value="${regimesVD.libelle}"/>&nbsp;(<c:out value="${regimesVD.code}"/>)
			</display:column>
		</display:table>

	</c:if>

</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.regimes.fiscaux.CH"/></span></legend>

	<c:if test="${empty command.entreprise.regimesFiscauxCH}">
		<fmt:message key="no.data" />
	</c:if>

	<c:if test="${not empty command.entreprise.regimesFiscauxCH}">
	
		<input id="showRegimesCHHisto" type="checkbox" onclick="refreshRegimesCHTable(this);" />
		<label for="showRegimesCHHisto"><fmt:message key="label.historique" /></label>
	
		<display:table name="${command.entreprise.regimesFiscauxCH}" id="regimesCH" class="display">
			<display:column sortable="true" titleKey="label.date.debut">
				<unireg:regdate regdate="${regimesCH.dateDebut}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.date.fin">
				<unireg:regdate regdate="${regimesCH.dateFin}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.type">
				<c:out value="${regimesCH.libelle}"/>&nbsp;(<c:out value="${regimesCH.code}"/>)
			</display:column>
		</display:table>

	</c:if>

</fieldset>

<script type="text/javascript">

	/**
	 * Affiche ou filtre les données historiques de la table des sièges
	 */
	function refreshRegimesVDTable(checkbox) {
		var showHisto = $(checkbox).get(0).checked;
		var table = $('#regimesVD');
		refreshHistoTable(showHisto, table, 1);
	}

	/**
	 * Affiche ou filtre les données historiques de la table des sièges
	 */
	function refreshRegimesCHTable(checkbox) {
		var showHisto = $(checkbox).get(0).checked;
		var table = $('#regimesCH');
		refreshHistoTable(showHisto, table, 1);
	}

	// on rafraîchit toutes les tables une première fois à l'affichage de la page
	refreshRegimesVDTable($('#showRegimesVDHisto'));
	refreshRegimesCHTable($('#showRegimesCHHisto'));

</script>
