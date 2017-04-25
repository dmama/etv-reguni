<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.ajout.allegement.fiscal">
			<fmt:param>
				<unireg:numCTB numero="${command.pmId}"/>
			</fmt:param>
		</fmt:message>
	</tiles:put>
	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
		<unireg:bandeauTiers numero="${command.pmId}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" titre="Caractéristiques du contribuable"/>

		<form:form id="addAllegementForm" commandName="command" action="add.do">

			<fieldset>
				<legend><span><fmt:message key="label.allegement.fiscal"/></span></legend>

				<form:hidden path="pmId"/>
				<unireg:nextRowClass reset="0"/>
				<table border="0">
					<tr class="<unireg:nextRowClass/>">
						<td width="20%"><fmt:message key="label.date.debut"/>&nbsp;:</td>
						<td width="20%">
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateDebut"/>
								<jsp:param name="id" value="dateDebut"/>
								<jsp:param name="mandatory" value="true" />
							</jsp:include>
						</td>
						<td width="20%"><fmt:message key="label.date.fin"/>&nbsp;:</td>
						<td width="40%">
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateFin"/>
								<jsp:param name="id" value="dateFin"/>
							</jsp:include>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td><fmt:message key="label.type.impot"/>&nbsp;</td>
						<td>
							<form:select path="typeImpot">
							    <form:option value=""/>
								<form:options items="${typesImpot}"/>
							</form:select>
							<span style="color: red;">*</span>
							<form:errors path="typeImpot" cssClass="error"/>
						</td>
						<td><fmt:message key="label.type.collectivite"/>&nbsp;</td>
						<td>
							<div style="float: left; margin-right: 2em;">
								<form:select path="typeCollectivite" id="typeCollectivite" onchange="AddAllegement.onTypeCollectiviteChange();">
									<form:option value=""/>
									<form:options items="${typesCollectivite}"/>
								</form:select>
								<span style="color: red;">*</span>
								<form:errors path="typeCollectivite" cssClass="error"/>
							</div>
							<div style="display: none; float: left; margin-right: 2em;" id="collectiviteCommune">
								<form:checkbox path="toutesCommunes" id="toutesCommunes"
								               style="margin-top: 0.25em;"
								               onchange="AddAllegement.onToutesCommunesChange();" onclick="AddAllegement.onToutesCommunesChange();"/>
								<span style="vertical-align: 15%;"><fmt:message key="label.toutes"/></span>
							</div>
							<div style="display: none;" id="choixCommune">
								<div style="float: left;"><input id="commune" size="25" value="${command.nomCommune}"/></div>
								<div style="float: left; width: 30%; margin-left: 2em;"><form:errors path="noOfsCommune" cssClass="error" /></div>
								<form:hidden path="noOfsCommune" />
								<form:hidden path="nomCommune" />
							</div>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td style="vertical-align: text-top;"><fmt:message key="label.type.allegement.fiscal"/>&nbsp;</td>
						<td style="vertical-align: text-top;">
							<div style="display: none;" id="choixTypeICC">
								<form:select path="typeICC">
									<form:option value=""/>
									<form:options items="${typesICC}"/>
								</form:select>
								<span style="color: red;">*</span>
								<div style="float: left;">
									<form:errors path="typeICC" cssClass="error"/>
								</div>
							</div>
							<div style="display: none;" id="choixTypeIFD">
								<form:select path="typeIFD">
									<form:option value=""/>
									<form:options items="${typesIFD}"/>
								</form:select>
								<span style="color: red;">*</span>
								<div style="float: left;">
									<form:errors path="typeIFD" cssClass="error"/>
								</div>
							</div>
						</td>
						<td style="vertical-align: text-top;"><fmt:message key="label.montant.pourcentage.allegement"/>&nbsp;</td>
						<td>
							<form:errors path="flagPourcentageMontant" cssClass="error"/>
							<div style="float: left; margin-right: 2em;">
								<form:radiobutton path="flagPourcentageMontant" value="POURCENTAGE" onclick="AddAllegement.onFlagChange('POURCENTAGE');"/>&nbsp;<fmt:message key="label.pourcentage.allegement"/><br/>
								<form:radiobutton path="flagPourcentageMontant" value="MONTANT" onclick="AddAllegement.onFlagChange('MONTANT');"/>&nbsp;<fmt:message key="label.montant"/>
							</div>
							<span style="width: 50%" id="valeurPourcentage">
								<form:input path="pourcentageAllegement" size="10" style="margin-top: 0.5em;"/>&nbsp;%
								<span style="color: red;">*</span>
								<form:errors path="pourcentageAllegement" cssClass="error"/>
							</span>
						</td>
					</tr>
				</table>

			</fieldset>

			<!-- Scripts -->
			<script type="text/javascript">

				var AddAllegement = {

					onTypeCollectiviteChange: function() {
						var typeSelect = $('#typeCollectivite').get(0);
						var selectedCollectivite = typeSelect.options[typeSelect.selectedIndex].value;
						if (selectedCollectivite == 'COMMUNE') {
							$('#collectiviteCommune').show();
							this.onToutesCommunesChange();
						}
						else {
							$('#collectiviteCommune').hide();
							$('#choixCommune').hide();
						}
						var choixTypeICC = $('#choixTypeICC');
						var choixTypeIFD = $('#choixTypeIFD');
						if (selectedCollectivite == 'COMMUNE' || selectedCollectivite == 'CANTON') {
							choixTypeICC.show();
							choixTypeIFD.hide();
						}
						else if (selectedCollectivite == 'CONFEDERATION') {
							choixTypeICC.hide();
							choixTypeIFD.show();
						}
						else {
							choixTypeICC.hide();
							choixTypeIFD.hide();
						}
					},

					onToutesCommunesChange: function() {
						var toutesCommunesCheckbox = $('#toutesCommunes').get(0);
						if (toutesCommunesCheckbox.checked) {
							$('#choixCommune').hide();
						}
						else {
							$('#choixCommune').show();
						}
					},

					onFlagChange: function(value) {
						if (value == 'POURCENTAGE') {
							$('#valeurPourcentage').show();
						}
						else {
							$('#valeurPourcentage').hide();
						}
					}
				};

				$(function() {

					// initialisation de l'autocomplétion sur le champ de la commune
					Autocomplete.infra('communeVD', '#commune', true, function(item) {
						$('#noOfsCommune').val(item ? item.id1 : null);
						$('#nomCommune').val(item ? item.label : null);
					});

					// initialisation des différents champs
					AddAllegement.onTypeCollectiviteChange();
					AddAllegement.onFlagChange('${command.flagPourcentageMontant}');
				});

			</script>

			<!-- Debut Bouton -->
			<table border="0">
				<tr>
					<td width="25%">&nbsp;</td>
					<td width="25%"><input type="submit" value="<fmt:message key="label.bouton.ajouter" />"></td>
					<td width="25%"><unireg:buttonTo name="Retour" action="/allegement/edit-list.do" params="{pmId:${command.pmId}}" method="GET"/></td>
					<td width="25%">&nbsp;</td>
				</tr>
			</table>
			<!-- Fin Bouton -->

		</form:form>

	</tiles:put>

</tiles:insert>