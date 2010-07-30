<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Fiscal pour debiteurs impot a la source -->
<fieldset><legend><span><fmt:message key="label.fiscal" /></span></legend>
	<table border="0">
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.mode.communication"/>&nbsp;:</td>
			<td width="25%"><form:select path="tiers.modeCommunication" items="${modesCommunication}" /></td>
			<td width="25%"><fmt:message key="label.categorie.impot.source"/>&nbsp;:</td>
			<td width="25%">
				<form:select path="tiers.categorieImpotSource" items="${categoriesImpotSource}" /> 
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.periodicite.decompte"/>&nbsp;:</td>
			<td width="25%">
				<form:select 	path="periodicite.periodiciteDecompte" items="${periodicitesDecompte}"
								onchange="selectPeriodeDecompte(this.options[this.selectedIndex].value);"/> 
			</td>
			<td width="25%">
				<div id="div_periodeDecompte_label" style="display:none;" ><fmt:message key="label.periode.decompte"/>&nbsp;:</div>
			</td>
			<td width="25%">
				<div id="div_periodeDecompte_input" style="display:none;" ><form:select path="periodicite.periodeDecompte" items="${periodeDecomptes}" /></div>
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.sans.sommation"/>&nbsp;:</td>
			<td width="25%"><form:checkbox path="tiers.sansRappel" /></td>
			<td width="25%"><fmt:message key="label.sans.lr"/>&nbsp;:</td>
			<td width="25%"><form:checkbox path="tiers.sansListeRecapitulative" /></td>
		</tr>
		
	</table>
</fieldset>
<script type="text/javascript">
	selectPeriodeDecompte('${command.tiers.periodiciteDecompte}');
</script>
<!-- Fin fiscal pour debiteurs impot a la source-->

