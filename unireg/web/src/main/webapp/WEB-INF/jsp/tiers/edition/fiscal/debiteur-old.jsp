<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<script type="text/javascript">

	var CreateDebiteur = {

		selectPeriodeDecompte: function(name) {
			if( name == 'UNIQUE' ){
				$('#div_periodeDecompte_label').show();
				$('#div_periodeDecompte_input').show();
			} else {
				$('#div_periodeDecompte_label').hide();
				$('#div_periodeDecompte_input').hide();
			}
		}
	};

</script>

<!-- Debut Fiscal pour debiteurs impot a la source -->
<fieldset><legend><span><fmt:message key="label.fiscal" /></span></legend>
	<table border="0">
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.mode.communication"/>&nbsp;:</td>
			<td width="25%"><form:select id="modeCommunication" path="tiers.modeCommunication" items="${modesCommunication}"/></td>
			<td width="25%"><fmt:message key="label.categorie.impot.source"/>&nbsp;:</td>
			<td width="25%">
				<form:select path="tiers.categorieImpotSource" items="${categoriesImpotSource}" />
				<span style="color: red;">*</span>
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.periodicite.decompte"/>&nbsp;:</td>
			<td width="25%">
				<form:select id="periodiciteCourante" path="periodicite.periodiciteDecompte" items="${periodicitesDecompte}"
				             onchange="CreateDebiteur.selectPeriodeDecompte(this.options[this.selectedIndex].value);"/>
				<span style="color: red;">*</span>
			</td>
			<td width="25%">
				<div id="div_periodeDecompte_label" style="display:none;" ><fmt:message key="label.periode.decompte"/>&nbsp;:</div>
			</td>
			<td width="25%">
				<div id="div_periodeDecompte_input" style="display:none;" ><form:select path="periodicite.periodeDecompte" items="${periodeDecomptes}" /></div>
			</td>
		</tr>

	</table>
</fieldset>

<script type="text/javascript">
	CreateDebiteur.selectPeriodeDecompte('${command.periodicite.periodiciteDecompte}');
</script>
<!-- Fin fiscal pour debiteurs impot a la source-->

