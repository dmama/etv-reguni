<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="tiers" value="${param.tiers}" />
<c:set var="tiersLie" value="${param.tiersLie}" />
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.recapitulatif.rapport.entre.tiers" /></tiles:put>

	<tiles:put name="body">
	<form:form method="post" id="formRapport">
		<c:if test="${command.allowed}">
		<unireg:nextRowClass reset="1"/>

		<table>
		<tr>
			<td id="td_tiers_gauche">
				<div id="div_tiers">
					<%-- Premier tiers --%>
					<jsp:include page="../../../../general/tiers.jsp" >
						<jsp:param name="page" value="rapport" />
						<jsp:param name="path" value="tiers" />
					</jsp:include>
				</div>
			</td>
			<td>
				<%-- Flèche du sens du rapport  --%>
				<table id="flecheSensRapport" cellpadding="0" cellspacing="0">
					<tr>
						<td style="width:1em;"/>
						<td id="flecheGauche" class="fleche_droite_bord_gauche"/>
						<td id="flecheMilieu" class="fleche_milieu"/>
						<td id="flecheDroite" class="fleche_droite_bord_droit"/>
						<td style="width:1em;"/>
					</tr>
				</table>
			</td>
			<td id="td_tiers_droite">
				<div id="div_tiers_lie">
					<%-- Second tiers --%>
					<jsp:include page="../../../../general/tiers.jsp" >
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
						<form:select id="typeRapport" path="typeRapportEntreTiers" onchange="onTypeChange(this);">
							<form:option value="REPRESENTATION"><fmt:message key="option.rapport.entre.tiers.SUJET.REPRESENTATION" /></form:option>
<form:option value="CONSEIL_LEGAL"><fmt:message key="option.rapport.entre.tiers.SUJET.CONSEIL_LEGAL" /></form:option>							
							<form:option value="TUTELLE"><fmt:message key="option.rapport.entre.tiers.SUJET.TUTELLE" /></form:option>
							<form:option value="CURATELLE"><fmt:message key="option.rapport.entre.tiers.SUJET.CURATELLE" /></form:option>
						</form:select>
					</p>
					
					<p><%-- Autorité tutélaire, uniquement pour tutelle --%>
					<script type="text/javascript">
				      function autoriteTutelaire_onChange(row) {
				              document.forms[0].autoriteTutelaireId.value = (row ? row.noColAdm : "");				              
				      }
					</script>
						<label id="autoriteTutelaireLabel"  for="autoriteTutelaire"><fmt:message key="label.autorite.tutelaire" />&nbsp;:</label>
						<form:input path="nomAutoriteTutelaire" id="nomAutoriteTutelaire" size ="65"/>
						<form:hidden path="autoriteTutelaireId" id="autoriteTutelaireId" />
						<jsp:include page="/WEB-INF/jsp/include/autocomplete.jsp">
							<jsp:param name="inputId" value="nomAutoriteTutelaire" />
							<jsp:param name="dataValueField" value="nomComplet" />
							<jsp:param name="dataTextField" value="{nomComplet}" />
							<jsp:param name="dataSource" value="selectionnerAutoriteTutelaire" />
							<jsp:param name="onChange" value="autoriteTutelaire_onChange" />
						</jsp:include>
					</p>
					<p>
						<%-- Date de début --%>					
						<label for="dateDebut"><fmt:message key="label.date.debut" />&nbsp;:</label>
						<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
							<jsp:param name="path" value="dateDebut"  />
							<jsp:param name="id" value="dateDebut" />
						</jsp:include>
						<FONT COLOR="#FF0000">*</FONT>
					</p>

					<p>
						<%-- Sens du rapport --%>					
						<label for="sensRapport"><fmt:message key="label.sens.rapport" />&nbsp;:</label>
						<form:select cssStyle="display:none;" id="sensRapport" path="sensRapportEntreTiers">
							<form:option value="SUJET"/>
							<form:option value="OBJET"/>
						</form:select>
						<input type="button" value="Inverser le sens" onclick="inverseSens();">
					</p>

					<p>
						<%-- Extension de l'exécution forcée (seulement pour REPRESENTATION) --%>					
						<label id="executionForceeLabel" for="executionForcee">Extension de l'exécution forcée :</label>
						<form:checkbox id="executionForcee" path="extensionExecutionForcee" />
					</p>

				</fieldset>

				<script type="text/javascript">
					function refreshLegend(element) {
						var type = E$('typeRapport');
						var sens = E$('sensRapport');

						// détermine le sens du rapport
						var contenuGauche;
						var contenuDroite;

						var divTiers = E$('div_tiers');
						var divTiersLie = E$('div_tiers_lie');
						
						switch (sens.value) {
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
						
						switch (type.value) {
						case 'TUTELLE':
							autorite = "tuteur";
							break;
						case 'CURATELLE':
							autorite = "curateur";
							break;							
						case 'CONSEIL_LEGAL':
							autorite = "conseil légal";
							break;
						case 'REPRESENTATION':
							autorite = "représentant";
							break;
						}

						// mis-à-jour du DOM
						setChild(E$('td_tiers_gauche'), contenuGauche);
						setChild(E$('td_tiers_droite'), contenuDroite);


						E$('flecheMilieu').innerHTML = 'est le ' + autorite + ' de';
					}

					function setChild(td, child) {
						while (td.childNodes[0]) {
							td.removeChild(td.childNodes[0]);
						}
						td.appendChild(child);
					}

					function refreshExecutionForcee() {
						var type = E$('typeRapport');
						if (type.value == 'REPRESENTATION') {
							E$('executionForcee').style.display = '';
							E$('executionForceeLabel').style.display = '';

							// [UNIREG-1341] execution forcee est seulement valable pour les non-habitants
							var sens = E$('sensRapport');
							var tiersRepresente;
							switch (sens.value) {
							case 'SUJET':
								// le tiers lié est le sujet
								tiersRepresente = E$('div_tiers_lie');
								break;
							case 'OBJET':
								// le tiers lié est l'objet
								tiersRepresente = E$('div_tiers');
								break;
							}

							var natureRepresente = getNatureTiers(tiersRepresente);

							if (natureRepresente == 'Habitant') {
								E$('executionForcee').disabled = 'disabled';
								E$('executionForceeLabel').style.color = 'gray';
								E$('executionForceeLabel').title = "L'extension de l'exécution forcée est uniquement autorisée pour les tiers domiciliés à l'étranger";
							}
							else {
								E$('executionForcee').disabled = null;
								E$('executionForceeLabel').style.color = '';
								E$('executionForceeLabel').title = "";
							}
						}
						else {
							E$('executionForcee').style.display = 'none';
							E$('executionForceeLabel').style.display = 'none';
						}
					}

					 function refreshAutoriteTutelaire() {
						var type = E$('typeRapport');
						if (type.value == 'TUTELLE' || type.value == 'CURATELLE' || type.value == 'CONSEIL_LEGAL') {
							E$('autoriteTutelaireLabel').style.display = '';
							E$('nomAutoriteTutelaire').style.display = '';

						}
						else {
							E$('autoriteTutelaireLabel').style.display = 'none';
							E$('nomAutoriteTutelaire').style.display = 'none';
						}
					 }

				
					function getNatureTiers(divTiers) {
						var inputs = divTiers.getElementsByTagName('input');
						var natureInput;
						for (var i = 0; i < inputs.length; ++i) {
							if (inputs[i].name == 'debugNatureTiers') {
								natureInput = inputs[i];
								break;
							}
						}
						return natureInput.value;
					}

					function onTypeChange(element) {
						refreshExecutionForcee();
						refreshLegend(element);
						refreshAutoriteTutelaire();
					}

					function inverseSens() {
						var sens = E$('sensRapport');
						if (sens.value == 'SUJET') {
							sens.value = 'OBJET';
						}
						else {						
							sens.value = 'SUJET';
						}
						refreshLegend(sens);
						refreshExecutionForcee();
						refreshAutoriteTutelaire();
					}

					refreshExecutionForcee();
					refreshLegend(E$('sensRapport'));
					refreshAutoriteTutelaire();
				</script>
			</td>
		</tr>
		</table>
		
		<br/>
		
		<!-- Debut Boutons -->
		<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="retourRapport(${command.tiers.numero});" />
		<input type="submit" value="<fmt:message key="label.bouton.sauver" />" />
		<form:errors cssClass="error"/>
		<!-- Fin Boutons -->
		</c:if>
		<c:if test="${!command.allowed}">
			<span class="error"><fmt:message key="error.rapport.interdit" /></span>
		</c:if>
	</form:form>
	</tiles:put>
</tiles:insert>