<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<script type="text/javascript">

	var EditDebiteur = {

		selectPeriodeDecompte: function(name) {
			if( name == 'UNIQUE' ){
				$('#div_periodeDecompte_label').show();
				$('#div_periodeDecompte_input').show();
			} else {
				$('#div_periodeDecompte_label').hide();
				$('#div_periodeDecompte_input').hide();
			}
		},

		selectLogiciel: function(name) {
			if( name == 'ELECTRONIQUE' ){
				$('#div_logiciel_label').show();
				$('#div_logiciel_input').show();
			} else {
				$('#div_logiciel_label').hide();
				$('#div_logiciel_input').hide();
			}
		}
	};

</script>

<!-- Debut Fiscal pour debiteurs impot a la source -->
<fieldset><legend><span><fmt:message key="label.fiscal" /></span></legend>
	<table border="0">
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.mode.communication"/>&nbsp;:</td>
			<td width="25%"><form:select id="modeCommunication" path="modeCommunication" items="${modesCommunication}"/></td>
			<td width="25%"><fmt:message key="label.categorie.impot.source"/>&nbsp;:</td>
			<td width="25%">
            <c:choose>
                <c:when test="${command.sansLREmises}">
                    <form:select path="categorieImpotSource" items="${categoriesImpotSource}"/>
                </c:when>
             <c:otherwise>
                    <form:select path="categorieImpotSource" items="${categoriesImpotSource}" disabled="true"/>
             </c:otherwise>
            </c:choose>
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.periodicite.decompte"/>&nbsp;:</td>
			<td width="25%">
				<form:select id="periodiciteCourante" path="periodiciteCourante" items="${periodicitesDecomptes}" onchange="EditDebiteur.selectPeriodeDecompte(this.options[this.selectedIndex].value);"/>
			</td>
			<td width="25%">
				<div id="div_periodeDecompte_label" style="display:none;" ><fmt:message key="label.periode.decompte"/>&nbsp;:</div>
			</td>
			<td width="25%">
				<div id="div_periodeDecompte_input" style="display:none;" ><form:select path="periodeDecompte" items="${periodesDecomptes}" /></div>
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td>
				<div id="div_logiciel_label" style="display:none;" ><fmt:message key="label.complement.logicielPaye" />&nbsp;:</div>
			</td>
			<td>
				<div id="div_logiciel_input" style="display:none;" >
					<form:select path="logicielId">
						<form:option value=""/>
						<form:options items="${libellesLogiciel}"/>
					</form:select>
				</div>
			</td>
		</tr>
	</table>
</fieldset>

<script type="text/javascript">
	EditDebiteur.selectPeriodeDecompte('${command.periodiciteCourante}');
	EditDebiteur.selectLogiciel('${command.modeCommunication}');
</script>
<!-- Fin fiscal pour debiteurs impot a la source-->

