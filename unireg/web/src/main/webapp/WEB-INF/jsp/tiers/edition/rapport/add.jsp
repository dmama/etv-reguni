<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="tiers" value="${param.tiers}" />
<c:set var="tiersLie" value="${param.tiersLie}" />
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.recapitulatif.rapport.entre.tiers" /></tiles:put>

	<tiles:put name="body">
	<form:form method="post" modelAttribute="rapportAddView" id="formRapport">
		<unireg:nextRowClass reset="1"/>
		<form:hidden path="tiers.numero"/>
		<form:hidden path="tiersLie.numero"/>

		<table>
		<tr>
			<td id="td_tiers_gauche">
				<div id="div_tiers">
					<%-- Premier tiers --%>
					<jsp:include page="../../../general/tiers.jsp" >
						<jsp:param name="commandName" value="rapportAddView" />
						<jsp:param name="page" value="rapport" />
						<jsp:param name="path" value="tiers" />
					</jsp:include>
				</div>
			</td>
			<td>
				<%-- Flèche du sens du rapport  --%>
				<table id="flecheSensRapport" cellpadding="0" cellspacing="0">
					<tr>
						<td style="width:1em;"></td>
						<td id="flecheGauche" class="fleche_droite_bord_gauche iepngfix"></td>
						<td id="flecheMilieu" class="fleche_milieu"></td>
						<td id="flecheDroite" class="fleche_droite_bord_droit iepngfix"></td>
						<td style="width:1em;"></td>
					</tr>
				</table>
			</td>
			<td id="td_tiers_droite">
				<div id="div_tiers_lie">
					<%-- Second tiers --%>
					<jsp:include page="../../../general/tiers.jsp" >
						<jsp:param name="commandName" value="rapportAddView" />
						<jsp:param name="page" value="rapport" />
						<jsp:param name="path" value="tiersLie" />
					</jsp:include>
				</div>
			</td>
		</tr>		
		<tr>
			<td colspan="3">
				<%-- Formulaire de saisie des détails du rapport --%>
				<fieldset class="rapport-form">
					<legend><span><fmt:message key="label.caracteristiques.rapport.entre.tiers" /></span></legend>

					<p>
						<%-- Type de rapport --%>					
						<label for="typeRapport"><fmt:message key="label.type.rapport.entre.tiers" />&nbsp;:</label>
						<form:select id="typeRapport" path="typeRapportEntreTiers" onchange="EditRapports.onTypeChange(this);" onkeyup="EditRapports.onTypeChange(this);">
							<form:option value="REPRESENTATION"><fmt:message key="option.rapport.entre.tiers.SUJET.REPRESENTATION" /></form:option>
							<option disabled="disabled">&mdash;&mdash;</option>
							<form:option value="CONSEIL_LEGAL"><fmt:message key="option.rapport.entre.tiers.SUJET.CONSEIL_LEGAL" /></form:option>
							<form:option value="CURATELLE"><fmt:message key="option.rapport.entre.tiers.SUJET.CURATELLE" /></form:option>
							<form:option value="TUTELLE"><fmt:message key="option.rapport.entre.tiers.SUJET.TUTELLE" /></form:option>
							<option disabled="disabled">&mdash;&mdash;</option>
                            <form:option value="ASSUJETTISSEMENT_PAR_SUBSTITUTION"><fmt:message key="option.rapport.entre.tiers.SUJET.ASSUJETTISSEMENT_PAR_SUBSTITUTION" /></form:option>
							<form:option value="HERITAGE"><fmt:message key="option.rapport.entre.tiers.HERITAGE"/></form:option>
						</form:select>
					</p>
					
					<p>
						<%-- Autorité tutélaire, uniquement pour tutelle --%>
						<label id="autoriteTutelaireLabel"  for="nomAutoriteTutelaire"><fmt:message key="label.autorite.tutelaire" />&nbsp;:</label>
						<form:input path="nomAutoriteTutelaire" id="nomAutoriteTutelaire" size ="65"/>
						<form:hidden path="autoriteTutelaireId" id="autoriteTutelaireId" />
						<script>
							$(function() {
								Autocomplete.infra('justicePaix', '#nomAutoriteTutelaire', true, function(item) {
									$('#autoriteTutelaireId').val(item ? item.id1 : null);
								});
							});
						</script>
					</p>
					<p>
						<%-- Date de début --%>					
						<label for="dateDebut"><fmt:message key="label.date.debut" />&nbsp;:</label>
						<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
							<jsp:param name="path" value="dateDebut"  />
							<jsp:param name="id" value="dateDebut" />
							<jsp:param name="mandatory" value="true" />
						</jsp:include>
					</p>

					<p>
						<%-- Sens du rapport --%>					
						<label for="sensRapport"><fmt:message key="label.sens.rapport" />&nbsp;:</label>
						<form:select cssStyle="display:none;" id="sensRapport" path="sensRapportEntreTiers">
							<form:option value="SUJET"/>
							<form:option value="OBJET"/>
						</form:select>
						<input type="button" value="Inverser le sens" onclick="EditRapports.inverseSens();">
					</p>

					<p>
						<%-- Extension de l'exécution forcée (seulement pour REPRESENTATION) --%>					
						<label id="executionForceeLabel" for="executionForcee">Extension de l'exécution forcée :</label>
						<form:checkbox id="executionForcee" path="extensionExecutionForcee" />
					</p>

				</fieldset>

				<script type="text/javascript">

					const EditRapports = {

						refreshLegend: function() {
							var type = $('#typeRapport');
							var sens = $('#sensRapport');

							// détermine le sens du rapport
							var contenuGauche;
							var contenuDroite;

							var divTiers = $('#div_tiers');
							var divTiersLie = $('#div_tiers_lie');

							switch (sens.val()) {
							case 'SUJET':
								// le tiers lié est le sujet
								contenuGauche = divTiers;
								contenuDroite = divTiersLie;
								break;
							case 'OBJET':
								// le tiers lié est l'objet
								contenuGauche = divTiersLie;
								contenuDroite = divTiers;
								break;
							}

							// détermine le texte du rapport
							var autorite;

							switch (type.val()) {
							case 'TUTELLE':
								autorite = "est le tuteur de";
								break;
							case 'CURATELLE':
								autorite = "est le curateur de";
								break;
							case 'CONSEIL_LEGAL':
								autorite = "est le conseil légal de";
								break;
							case 'REPRESENTATION':
								autorite = " est le représentant de";
								break;
							case 'ASSUJETTISSEMENT_PAR_SUBSTITUTION':
								autorite = "se substitue à";
								break;
							case 'HERITAGE':
								autorite = "a pour héritier";
								break;
							}

							// mis-à-jour du DOM
							this._setChild($('#td_tiers_gauche'), contenuGauche);
							this._setChild($('#td_tiers_droite'), contenuDroite);


							$('#flecheMilieu').html(autorite);
						},

						_setChild: function(td, child) {
							td.empty();
							td.append(child);
						},

						refreshExecutionForcee: function() {
							var type = $('#typeRapport');
							if (type.val() === 'REPRESENTATION') {
								$('#executionForcee').show();
								$('#executionForceeLabel').show();

								// [UNIREG-1341/UNIREG-2655] execution forcee est seulement valable pour les contribuables hors-Suisse
								var sens = $('#sensRapport');
								var tiersRepresente;
								switch (sens.val()) {
								case 'SUJET':
									// le tiers lié est le sujet
									tiersRepresente = $('#div_tiers_lie');
									break;
								case 'OBJET':
									// le tiers lié est l'objet
									tiersRepresente = $('#div_tiers');
									break;
								}

								var typeFFP = this._getTypeForPrincipalActif(tiersRepresente);

								if (typeFFP === 'PAYS_HS') {
									$('#executionForcee').attr('disabled', null);
									$('#executionForceeLabel').css('color', '');
									$('#executionForceeLabel').attr('title', "");
								}
								else {
									$('#executionForcee').attr('checked', null);
									$('#executionForcee').attr('disabled', 'disabled');
									$('#executionForceeLabel').css('color', 'gray');
									$('#executionForceeLabel').attr('title', "Uniquement autorisée pour les tiers avec un for fiscal principal hors-Suisse");
								}
							}
							else {
								$('#executionForcee').hide();
								$('#executionForceeLabel').hide();
							}
						},

						refreshAutoriteTutelaire: function() {
							var type = $('#typeRapport');
							if (type.val() === 'TUTELLE' || type.val() === 'CURATELLE' || type.val() === 'CONSEIL_LEGAL') {
								$('#autoriteTutelaireLabel').show();
								$('#nomAutoriteTutelaire').show();

							}
							else {
								$('#autoriteTutelaireLabel').hide();
								$('#nomAutoriteTutelaire').hide();
							}
						},

						_getTypeForPrincipalActif: function(divTiers) {
							return $('input[name=\"debugTypeForPrincipalActif\"]', divTiers).val();
						},

						refreshAll: function() {
							this.refreshExecutionForcee();
							this.refreshLegend();
							this.refreshAutoriteTutelaire();
						},

						onTypeChange: function(element) {
							this.refreshAll();
						},

						inverseSens: function() {
							var sens = $('#sensRapport');
							if (sens.val() === 'SUJET') {
								sens.val('OBJET');
							}
							else {
								sens.val('SUJET');
							}

							this.refreshAll();
						}
					};

					EditRapports.refreshAll();
				</script>
			</td>
		</tr>
		</table>
		
		<br/>
		
		<!-- Debut Boutons -->
		<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="retourRapport(${rapportAddView.tiers.numero});" />
		<input type="submit" value="<fmt:message key="label.bouton.sauver" />" />
		<!-- Fin Boutons -->
		<script>
			function retourRapport(numero) {
				if(confirm('Voulez-vous quitter cette page sans sauver ?')) {
					document.location.href='add-search.do?tiersId=' + numero ;
				}
			}
		</script>
	</form:form>
	</tiles:put>
</tiles:insert>