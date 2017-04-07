<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<script type="text/javascript">

	var EditDebiteur = {

		selectPeriodeDecompte: function(name) {
			if (name === 'UNIQUE') {
				$('#div_periodeDecompte_label').show();
				$('#div_periodeDecompte_input').show();
			}
			else {
				$('#div_periodeDecompte_label').hide();
				$('#div_periodeDecompte_input').hide();
			}

			<c:choose>
				<c:when test="${command.nouvellePeriodicite == 'UNIQUE' || command.periodiciteActive != command.nouvellePeriodicite || command.dateDebutPeriodiciteActive != command.dateDebutNouvellePeriodicite}">
					this.showDatesDebutPossibles(name);
				</c:when>
				<c:otherwise>
					if (name === 'UNIQUE' || name != '${command.periodiciteActive}') {
						this.showDatesDebutPossibles(name);
					}
					else {
						$('#periodiciteDepuis').hide();
						$('#pasDeDateDisponible').hide();

						var tempDisabled = $('.tempDisabled');
						tempDisabled.prop('disabled', false);
						tempDisabled.removeClass('tempDisabled');
					}
				</c:otherwise>
			</c:choose>
		},

		showDatesDebutPossibles: function(name) {
			var selectDate = $('#dateDebutPeriodicite');

			// appels ajax pour mettre-à-jour les dates possibles pour le début de la nouvelle périodicité
			$.get(App.curl('/debiteur/dates-nouvelle-periodicite.do?id=${command.id}&nouvellePeriodicite=' + name + '&' + new Date().getTime()), function(dates) {
				var list = '';
				for(var i = 0; i < dates.length; ++i) {
					var d = dates[i];
					var str = RegDate.format(d);
					list += '<option value="' + str + '"' + (str === '<unireg:regdate regdate="${command.dateDebutNouvellePeriodicite}"/>' ? ' selected=true' : '') + '">' + str + '</option>';
				}
				selectDate.html(list);

				var spanPeriodiciteDepuis = $('#periodiciteDepuis');
				var spanPasDeDateDisponible = $('#pasDeDateDisponible');
				if (dates.length > 0) {
					spanPeriodiciteDepuis.show();
					spanPasDeDateDisponible.hide();

					var tempDisabled = $('.tempDisabled');
					tempDisabled.prop('disabled', false);
					tempDisabled.removeClass('tempDisabled');
				}
				else {
					spanPeriodiciteDepuis.hide();
					spanPasDeDateDisponible.show();

					var disablable = $('#formEditDebiteur').find(':input').not('#periodiciteCourante, #retourButton');
					disablable.addClass('tempDisabled');
					disablable.prop('disabled', true);        // on empêche de sauvegarder quoi que ce soit
				}
			}, 'json').error(Ajax.popupErrorHandler);
		},

		selectLogiciel: function(name) {
			if (name === 'ELECTRONIQUE'){
				$('#div_logiciel_label').show();
				$('#div_logiciel_input').show();
			}
			else {
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
			<td width="20%"><fmt:message key="label.mode.communication"/>&nbsp;:</td>
			<td width="35%"><form:select id="modeCommunication" path="modeCommunication" items="${modesCommunication}" onchange="EditDebiteur.selectLogiciel(this.options[this.selectedIndex].value);"/></td>
			<td width="20%"><fmt:message key="label.categorie.impot.source"/>&nbsp;:</td>
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
			<td width="20%"><fmt:message key="label.periodicite.decompte"/>&nbsp;:</td>
			<td width="35%">
				<form:select id="periodiciteCourante" path="nouvellePeriodicite" items="${periodicitesDecomptes}" onchange="EditDebiteur.selectPeriodeDecompte(this.options[this.selectedIndex].value);"/>
				<span id="periodiciteDepuis" style="display: none;">
					&nbsp;<fmt:message key="label.des.le"/>&nbsp;
					<form:select id="dateDebutPeriodicite" path="dateDebutNouvellePeriodicite"/>
					<span style="color: red;">*</span>
				</span>
				<span id="pasDeDateDisponible" style="display: none;" class="error">
					&nbsp;<fmt:message key="label.pas.de.date.disponible.dans.lannee"/>
				</span>
			</td>
			<td width="20%">
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
			<td colspan="3">
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
	EditDebiteur.selectPeriodeDecompte('${command.nouvellePeriodicite}');
	EditDebiteur.selectLogiciel('${command.modeCommunication}');
</script>
<!-- Fin fiscal pour debiteurs impot a la source-->

