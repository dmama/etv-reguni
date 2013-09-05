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
		},

		onModeCommunicationChange: function(name) {
			this.selectLogiciel(name);
			this.updatePeriodicitesDecompte($('#periodiciteCourante'), name, null);
		},

		onPeriodiciteDecompteChange: function(name) {
			this.selectPeriodeDecompte(name);
			this.updateModesCommunication($('#modeCommunication'), name, null);
		},

		/**
		 * Cette fonction met-à-jour la liste des modes de communication en fonction de la périodicité actuelle
		 *
		 * @param modeCommunicationSelect
		 *            le select contenant les modes de communication à mettre à jour
		 * @param periodiciteDecompte
		 *            la périodicité de décompte courante
		 * @param defaultModeCommunication
		 *            (optionel) la valeur par défaut du mode de communication
		 *            lorsqu'aucune valeur n'est sélectionné dans la liste
		 *            'modeCommunicationSelect'
		 */
		updateModesCommunication: function(modeCommunicationSelect, periodiciteDecompte, defaultModeCommunication) {

			var modeCommunication = modeCommunicationSelect.val();
			if (modeCommunication == null) modeCommunication = defaultModeCommunication;

			// appels ajax pour mettre-à-jour les modes de communication
			$.get(App.curl('/debiteur/modesCommunication.do?periodicite=') + periodiciteDecompte + '&modeActuel=' + modeCommunication + '&' + new Date().getTime(), function(modesComm) {
				var list = '';
				for(var i = 0; i < modesComm.length; ++i) {
					var modeComm = modesComm[i];
					list += '<option value="' + modeComm.value + '"' + (modeComm.value == modeCommunication ? ' selected=true' : '') + '>' + StringUtils.escapeHTML(modeComm.label) + '</option>';
				}
				modeCommunicationSelect.html(list);
			}, 'json')
			.error(Ajax.popupErrorHandler);
		},

		/**
		 * Cette fonction met-à-jour la liste des périodicités de décompte en fonction du mode de communication actuel
		 *
		 * @param periodiciteSelect
		 *            le select contenant les périodicités à mettre à jour
		 * @param modeCommunication
		 *            le mode de communication courant
		 * @param defaultPeriodicite
		 *            (optionel) la valeur par défaut de la périodicite
		 *            lorsqu'aucune valeur n'est sélectionné dans la liste
		 *            'periodiciteSelect'
		 */
		updatePeriodicitesDecompte: function(periodiciteSelect, modeCommunication, defaultPeriodicite) {

			var periodiciteSelectionnee = periodiciteSelect.val();
			var periodicite = periodiciteSelectionnee;
			if (modeCommunication == '${command.modeCommunication}') {
				periodicite = '${command.periodiciteCourante}';
			}
			if (periodicite == null) {
				periodicite = defaultPeriodicite;
			}
			if (periodiciteSelectionnee == null) {
				periodiciteSelectionnee = periodicite;
			}

			// appels ajax pour mettre-à-jour les modes de communication
			$.get(App.curl('/debiteur/periodicitesDecompte.do?modeCommunication=') + modeCommunication + '&periodiciteActuelle=' + periodicite + '&' + new Date().getTime(), function(periodicites) {
				var list = '';
				for(var i = 0; i < periodicites.length; ++i) {
					var p = periodicites[i];
					list += '<option value="' + p.value + '"' + (p.value == periodiciteSelectionnee ? ' selected=true' : '') + '>' + StringUtils.escapeHTML(p.label) + '</option>';
				}
				periodiciteSelect.html(list);
			}, 'json')
			.error(Ajax.popupErrorHandler);
		}
	};

</script>

<!-- Debut Fiscal pour debiteurs impot a la source -->
<fieldset><legend><span><fmt:message key="label.fiscal" /></span></legend>
	<table border="0">
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.mode.communication"/>&nbsp;:</td>
			<td width="25%"><form:select id="modeCommunication" path="modeCommunication" onchange="EditDebiteur.onModeCommunicationChange(this.options[this.selectedIndex].value);"/></td>
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
				<form:select id="periodiciteCourante" path="periodiciteCourante" onchange="EditDebiteur.onPeriodiciteDecompteChange(this.options[this.selectedIndex].value);"/>
			</td>
			<td width="25%">
				<div id="div_periodeDecompte_label" style="display:none;" ><fmt:message key="label.periode.decompte"/>&nbsp;:</div>
			</td>
			<td width="25%">
				<div id="div_periodeDecompte_input" style="display:none;" ><form:select path="periodeDecompte" items="${periodeDecomptes}" /></div>
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
	EditDebiteur.updateModesCommunication($('#modeCommunication'), '${command.periodiciteCourante}', '${command.modeCommunication}');
	EditDebiteur.updatePeriodicitesDecompte($('#periodiciteCourante'), '${command.modeCommunication}', '${command.periodiciteCourante}');
</script>
<!-- Fin fiscal pour debiteurs impot a la source-->

