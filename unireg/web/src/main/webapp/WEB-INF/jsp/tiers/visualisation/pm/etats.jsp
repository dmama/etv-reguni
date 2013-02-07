<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
<fieldset>
	<legend><span><fmt:message key="label.etats.pm"/></span></legend>

	<c:if test="${empty command.entreprise.etats}">
		<fmt:message key="no.data" />
	</c:if>

	<c:if test="${not empty command.entreprise.etats}">

		<input class="noprint" id="showEtatsPMHisto" type="checkbox" onclick="refreshEtatsPM(this);" />
		<label class="noprint" for="showEtatsPMHisto"><fmt:message key="label.historique" /></label>

		<display:table name="${command.entreprise.etats}" id="etatsPM" requestURI="visu.do" class="display">
			<display:column sortable="true" titleKey="label.date.debut">
				<unireg:regdate regdate="${etatsPM.dateDebut}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.date.fin">
				<unireg:regdate regdate="${etatsPM.dateFin}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.type">
				<c:out value="${etatsPM.libelle}"/>&nbsp;(<c:out value="${etatsPM.code}"/>)
			</display:column>
		</display:table>

	</c:if>

</fieldset>

<script type="text/javascript">

	/**
	 * Affiche ou filtre les données historiques de la table des sièges
	 */
	function refreshEtatsPM(checkbox) {
		var showHisto = $(checkbox).attr('checked');
		var table = $('#etatsPM');
		Histo.refreshHistoTable(showHisto, table, 1);
	}

	// on rafraîchit toutes les tables une première fois à l'affichage de la page
	refreshEtatsPM($('#showEtatsPMHisto'));

</script>
