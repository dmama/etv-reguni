<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<!-- Debut Dossiers Apparentes -->
<c:if test="${command.allowedOnglet.DOS || command.allowedOnglet.DBT}">
	<table border="0">
		<tr>
			<td>
				<c:if test="${empty param['message'] && empty param['retour']}">
						<unireg:raccourciModifier link="../dossiers-apparentes/edit.do?id=${command.tiers.numero}" tooltip="Modifier les dossiers apparentés" display="label.bouton.modifier"/>
				</c:if>		
			</td>
		</tr>
	</table>
</c:if>
<c:if test="${not empty command.dossiersApparentes}">
<fieldset><legend><span><fmt:message
	key="label.dossiers.apparentes" /></span></legend>


	<table id="tableTypeRapport">
		<tr
			class="<unireg:nextRowClass/>">
			<td width="25%"><input name="rapport_actif" type="checkbox"
				id="isRapportHisto"
				onclick="toggleRowsIsHisto('dossierApparente','isRapportHisto', 2)" />
				<label for="isRapportHisto"><fmt:message key="label.historique" /></label>
				</td>
			<td width="75%">&nbsp;</td>
		</tr>
		<form name="form" id="form">
		<tr
			class="<unireg:nextRowClass/>">
			<td width="25%"><fmt:message key="label.type.rapport.entre.tiers" />&nbsp;:</td>
			<td width="75%"><select name="typeRapport" id="typeRapportId"
				onchange="toggleRowsIsHisto('dossierApparente','isRapportHisto', 2)">
				<OPTION value="tous">Tous</OPTION>
				<c:forEach var="rapport" items="${typesRapportTiers}">
					<OPTION value="${rapport.value}">${rapport.value}</OPTION>
				</c:forEach>
			</select></td>
		</tr>
		</form>
	</table>

	<jsp:include page="../common/dossiers-apparentes.jsp">
		<jsp:param name="page" value="visu"/>
	</jsp:include>
	
</fieldset>
</c:if>
<!-- Fin Dossiers Apparentes -->
