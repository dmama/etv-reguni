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
		updatePeriodicitesDecompte: function(periodiciteSelect, defaultPeriodicite) {

			var periodiciteSelectionnee = periodiciteSelect.val();
			if (periodiciteSelectionnee == null) {
				periodiciteSelectionnee = 'MENSUEL';
			}
			var periodicite = periodiciteSelectionnee;
			if (periodicite == null) {
				periodicite = defaultPeriodicite;
			}

			// appels ajax pour mettre-à-jour les modes de communication
			$.get(App.curl('/debiteur/periodicitesDecompte.do?periodiciteActuelle=') + periodicite + '&' + new Date().getTime(), function(periodicites) {
				var list = '';
				for(var i = 0; i < periodicites.length; ++i) {
					var p = periodicites[i];
					list += '<option value="' + p.value + '"' + (p.value == periodiciteSelectionnee ? ' selected=true' : '') + '>' + StringUtils.escapeHTML(p.label) + '</option>';
				}
				periodiciteSelect.html(list);
			}, 'json').error(Ajax.popupErrorHandler);
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
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.periodicite.decompte"/>&nbsp;:</td>
			<td width="25%">
				<form:select id="periodiciteCourante" path="periodicite.periodiciteDecompte" onchange="CreateDebiteur.selectPeriodeDecompte(this.options[this.selectedIndex].value);"/>
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
	CreateDebiteur.updatePeriodicitesDecompte($('#periodiciteCourante'), 'MENSUEL');
	CreateDebiteur.selectPeriodeDecompte('${command.periodicite.periodiciteDecompte}');
</script>
<!-- Fin fiscal pour debiteurs impot a la source-->

