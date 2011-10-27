<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<!-- Debut Dossiers Apparentes -->
<c:if test="${command.allowedOnglet.DOS || command.allowedOnglet.DBT}">
	<table border="0">
		<tr><td>
			<c:if test="${empty param['message'] && empty param['retour']}">
					<unireg:raccourciModifier link="../dossiers-apparentes/edit.do?id=${command.tiers.numero}" tooltip="Modifier les dossiers apparentés" display="label.bouton.modifier"/>
			</c:if>
		</td></tr>
	</table>
</c:if>

<div id="dossiersApparentesDiv" style="position:relative"><img src="<c:url value="/images/loading.gif"/>"/></div>

<script>
	// chargement Ajax des rapports-entre-tiers
	$(function() {
		$('#dossiersApparentesDiv').load('../rapport/list.do?tiers=${command.tiersGeneral.numero}');
	});
</script>

<!-- Fin Dossiers Apparentes -->
