<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<%@page import="ch.vd.uniregctb.common.LengthConstants"%>


<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.creation.adresse">
  			<fmt:param><unireg:numCTB numero="${editCommand.numCTB}"/></fmt:param>
  		</fmt:message>
  	</tiles:put>

	<tiles:put name="head">
		<style>
			input {
				padding: 2px;
			}
			.vignette {
				/* pour que la vignette ne prenne pas toute la largeur */
				display: inline-block;
				width: 30em;
				margin-left: 0.5em;
				margin-right: 0.5em;
				text-align: left; /* bug IE8 */
			}
			#adresseSuccessoral {
				background-color: #FAF3A3;
				margin: 1em 2em;
				padding: 1em;
			}
			.radioSelectIdent  {
				margin: 0.5em 2em;
			}
		</style>
	</tiles:put>

	<tiles:put name="body">

		<!-- Debut Adresse -->
		<form:form name="formAddAdresse" id="formAddAdresse" commandName="editCommand">
		<form:hidden path="mode"/>
		<form:hidden path="index"/>
		<input name="numCTB" type="hidden" value="${editCommand.numCTB}" />

		<fieldset>
		<legend><span><fmt:message key="label.adresse.caracteristique" /></span></legend>
		<table>
			<tr class="<unireg:nextRowClass/>" >
				<td width="25%"><fmt:message key="label.adresse.utilisation" />:</td>
				<td width="75%">
				<c:choose>
						<c:when test="${editCommand.id != null}">
							<form:select disabled="true" path="usage" items="${typeAdresseFiscaleTiers}" />
						</c:when>
						<c:otherwise>
							<form:select path="usage" items="${typeAdresseFiscaleTiers}" />
							<span style="color: red;">*</span>
							<form:errors path="usage" cssClass="error"/>
						</c:otherwise>
				</c:choose>
				</td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				<td width="25%"><fmt:message key="label.date.ouverture" />&nbsp;:</td>
				<td width="75%">
					<c:choose>
						<c:when test="${editCommand.id != null}">
							<form:input disabled="true" path="dateDebut"  id="dateDebut" cssErrorClass="input-with-errors" size ="10" />
						</c:when>
						<c:otherwise>
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param  name="path" value="dateDebut" />
								<jsp:param name="id" value="dateDebut" />
							</jsp:include>
							<span style="color: red;">*</span>
						</c:otherwise>
					</c:choose>
				</td>
			</tr>
		</table>
	</fieldset>

	<c:set var="lengthadrnom" value="<%=LengthConstants.ADRESSE_NOM%>" scope="request" />
	<c:set var="lengthadrnum" value="<%=LengthConstants.ADRESSE_NUM%>" scope="request" />

	<fieldset><legend><span><fmt:message key="label.nouvelleAdresse" /></span></legend>
		<div id="adresse_saisie" >
			<table border="0">
				<unireg:nextRowClass reset="0"/>
				<tr class="<unireg:nextRowClass/>" >
					<td width="30em"></td>
					<td>
						<div style="margin-left:1em">
							<c:choose>
								<c:when test="${editCommand.id != null}">
									<form:radiobutton path="typeLocalite" onclick="selectLocalite('localite_suisse');" value="suisse" disabled="true"/>
									<label for="typeLocalite"><fmt:message key="label.suisse" /></label><br>
									<form:radiobutton path="typeLocalite" onclick="selectLocalite('pays');" value="pays"  disabled="true" />
									<label for="typeLocalite"><fmt:message key="label.etranger" /></label>
								</c:when>
								<c:otherwise>
									<form:radiobutton path="typeLocalite" onclick="selectLocalite('localite_suisse');" value="suisse" disabled="false"/>
									<label for="typeLocalite"><fmt:message key="label.suisse" /></label><br>
									<form:radiobutton path="typeLocalite" onclick="selectLocalite('pays');" value="pays"  disabled="false" />
									<label for="typeLocalite"><fmt:message key="label.etranger" /></label>
								</c:otherwise>
							</c:choose>
						</div>
					</td>
					<td></td>
					<td></td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td><fmt:message key="label.complements" />:</td>
					<td colspan="3">
						<form:input path="complements" cssErrorClass="input-with-errors"
						            size ="100" maxlength="${lengthadrnom}" /></td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td>
						<div id="div_label_localite_suisse"><fmt:message key="label.localite.suisse" />:</div>
						<div id="div_label_pays_npa" style="display:none;"><fmt:message key="label.npa.localite" /> :</div>
					</td>
					<td>
						<div id="div_input_localite_suisse">
							<form:input path="localiteSuisse" id="localiteSuisse" cssErrorClass="input-with-errors" size ="25" />
							<form:hidden path="numeroOrdrePoste" id="numeroOrdrePoste"  />
							<form:hidden path="numCommune" id="numCommune"  />
							<script>
								$(function() {
									Autocomplete.infra('localite', '#localiteSuisse', true, function(item) {
										if (item) {
											$('#numeroOrdrePoste').val(item.id1);
											$('#numCommune').val(item.id2);
											// [SIFISC-1507] en cas de saisie correcte d'une localité, on supprime un éventuel message d'erreur Spring associé au champ
											$('#localiteSuisse').removeClass('input-with-errors');
											$('#localiteSuisse\\.errors').hide();
										}
										else {
											$('#numeroOrdrePoste').val(null);
											$('#numCommune').val(null);
										}
										$('#numeroRue').val('');
										$('#rue').val('');

										// à chaque changement de localité, on adapte l'autocompletion sur la rue en conséquence
										Autocomplete.infra('rue&numCommune=' + $('#numCommune').val(), '#rue', true, function(i) {
											if (i) {
												$('#numeroRue').val(i.id1);
												$('#numeroOrdrePoste').val(i.id2);
											}
											else {
												$('#numeroRue').val(null);
												// [UNIREG-3408] On n'annule pas le numéro de localité car il doit être possible de saisir une rue non-référencée
											}
										});

										AddressEdit_Adjust();
									});
								});
							</script>
							<form:errors path="localiteSuisse" cssClass="error"/>
						</div>
						<div id="div_input_pays_npa" style="display:none;">
							<form:input path="localiteNpa" cssErrorClass="input-with-errors"
							            size ="20" maxlength="${lengthadrnum}" />
							<form:errors path="localiteNpa" cssClass="error"/>
						</div>
					</td>
					<td>
						<div id="div_label_lieu" style="display:none;">
							<fmt:message key="label.complement.localite" />:</div>
					</td>
					<td>
						<div id="div_input_lieu" style="display:none;">
							<form:input path="complementLocalite" id="complementLocalite" cssErrorClass="input-with-errors"
							            size ="20" maxlength="${lengthadrnom}" />
							<form:errors path="complementLocalite" cssClass="error"/>
						</div>
					</td>
				</tr>
				<tr class="<unireg:nextRowClass frozen="true"/>" id="div_pays" style="display:none;">
					<td><fmt:message key="label.paysEtranger" />:</td>
					<td colspan="3" >
						<form:hidden path="paysOFS" id="paysOFS"/>
						<form:input path="paysNpa" id="pays" cssErrorClass="input-with-errors" size ="20" />
						<form:errors path="paysNpa" cssClass="error"/>
						<script>
							$(function() {
								Autocomplete.infra('etatOuTerritoire', '#pays', true, function(item) {
									$('#paysOFS').val(item ? item.id1 : null);
								});
							});
						</script>
					</td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td><fmt:message key="label.rue" />&nbsp;:</td>
					<td>
						<form:input path="rue" id="rue" size ="25" cssErrorClass="input-with-errors" maxlength="${lengthadrnom}" />
						<form:errors path="rue" cssClass="error"/>
						<form:hidden path="numeroRue" id="numeroRue"/>
						<script>
							$(function() {
								Autocomplete.infra('rue&numCommune=' + $('#numCommune').val(), '#rue', true, function(i) {
									if (i) {
										$('#numeroRue').val(i.id1);
										$('#numeroOrdrePoste').val(i.id2);
									}
									else {
										$('#numeroRue').val(null);
										// [UNIREG-3408] On n'annule pas le numéro de localité car il doit être possible de saisir une rue non-référencée
									}
									if ($('#div_pays').is(":visible")) {
										// [SIFISC-832] on ne valide pas les rues pour les adresses étrangères
										$('#rue').removeClass('error');
									}
								});
							});
						</script>
					</td>
					<td><fmt:message key="label.numero.maison" />:</td>
					<td>
						<form:input path="numeroMaison" id="numeroMaison" cssErrorClass="input-with-errors"
						            size ="5" maxlength="${lengthadrnum}" />
						<form:errors path="numeroMaison" cssClass="error"/>
					</td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td><fmt:message key="label.texte.case.postale" />&nbsp;:</td>
					<td>
						<form:select path="texteCasePostale" id="texteCasePostale">
							<form:option value=""/>
							<form:options items="${textesCasePostale}" />
						</form:select>
					</td>
					<td id="td_case_postale_label"><fmt:message key="label.case.postale" /> :</td>
					<td	id="td_case_postale">
						<form:input path="numeroCasePostale" id="numeroCasePostale" cssErrorClass="input-with-errors" size ="5" />
						<form:errors path="numeroCasePostale" cssClass="error"/>
					</td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td><fmt:message key="label.adresse.permanente" />&nbsp;:</td>
					<td>
						<form:checkbox path="permanente" id="adressePermanente" cssErrorClass="input-with-errors" />
						<form:errors path="permanente" cssClass="error"/>
					</td>
					<td id="td_npa_case_postale_label"><fmt:message key="label.npa.case.postale" /> :</td>
					<td	id="td_npa_case_postale">
						<form:input path="npaCasePostale" id="npaCasePostale" cssErrorClass="input-with-errors" size ="4" />
						<form:errors path="npaCasePostale" cssClass="error"/>
					</td>

				</tr>

			</table>
		</div>
	</fieldset>

	<div id="adresse_add">

		<%-- [SIFISC-156] Mise-à-jour automatique des adresses successorales --%>
		<form:hidden path="etatSuccessoral.numeroPrincipalDecede"/>
		<form:hidden path="etatSuccessoral.numeroConjointDecede"/>

		<table border="0">
		<authz:authorize ifAnyGranted="ROLE_ADR_PP_C_DCD">
		<c:if test="${editCommand.etatSuccessoral.numeroPrincipalDecede != null || editCommand.etatSuccessoral.numeroConjointDecede != null}">
			<tr>
				<td colspan="4">
					<div id="adresseSuccessoral">
						<image src="<c:url value="/css/x/info.png"/>" style="position:relative; top:5px"/>
						<c:if test="${editCommand.etatSuccessoral.numeroPrincipalDecede != null && editCommand.etatSuccessoral.numeroConjointDecede != null}">
							<fmt:message key="label.adresse.successorale.tous.decedes"/> :<br/>
							<div class="vignette"><unireg:bandeauTiers numero="${editCommand.etatSuccessoral.numeroPrincipalDecede}" titre="Personne principale" showValidation="false" showEvenementsCivils="false" showLinks="false"/></div>
							<div class="vignette"><unireg:bandeauTiers numero="${editCommand.etatSuccessoral.numeroConjointDecede}" titre="Conjoint" showValidation="false" showEvenementsCivils="false" showLinks="false"/></div><br/>
							<fmt:message key="label.adresse.successorale.tous.decedes.question"/>
						</c:if>
						<c:if test="${editCommand.etatSuccessoral.numeroConjointDecede == null}">
							<fmt:message key="label.adresse.successorale.principal.decede"/> :<br/>
							<div class="vignette"><unireg:bandeauTiers numero="${editCommand.etatSuccessoral.numeroPrincipalDecede}" titre="Personne principale" showValidation="false" showEvenementsCivils="false" showLinks="false"/></div><br/>
							<fmt:message key="label.adresse.successorale.un.decede.question"/>
						</c:if>
						<c:if test="${editCommand.etatSuccessoral.numeroPrincipalDecede == null}">
							<fmt:message key="label.adresse.successorale.conjoint.decede"/> :<br/>
							<div class="vignette"><unireg:bandeauTiers numero="${editCommand.etatSuccessoral.numeroConjointDecede}" titre="Conjoint" showValidation="false" showEvenementsCivils="false" showLinks="false"/></div><br/>
							<fmt:message key="label.adresse.successorale.un.decede.question"/>
						</c:if>
						<div class="radioSelectIdent">
							<form:radiobutton path="mettreAJourDecedes" value="true"/>
							<label for="mettreAJourDecedes1">Oui</label><br>
							<form:radiobutton path="mettreAJourDecedes" value="false"/>
							<label for="mettreAJourDecedes2">Non</label><br>
						</div>
					</div>
				</td>
			</tr>
		</c:if>
		</authz:authorize>
		<tr>
			<td width="25%"></td>
			<td width="25%">
				<c:choose>
					<c:when test="${editCommand.id !=null}">
						<input type="submit" id="maj" name="update" value="<fmt:message key="label.bouton.mettre.a.jour" />">
					</c:when>
					<c:otherwise>
						<input type="submit" name="update" value="<fmt:message key="label.bouton.ajouter" />" />
					</c:otherwise>
				</c:choose>
			</td>
			<td width="25%"><input type="button" name="cancel" value="<fmt:message key="label.bouton.retour" />" onClick="document.location.href='../adresses/edit.do?id=${editCommand.numCTB}'" /></td>
			<td width="25%"></td>
		</tr>
		</table>
	</div>

	</form:form>
	<!-- Fin Adresse -->

	</tiles:put>
</tiles:insert>


<script type="text/javascript" language="Javascript.1.3">

	$(function () {
		AddressEdit_Adjust();
		$("#texteCasePostale").keyup(TexteCasePostale_OnChange); // autrement les changements de sélection effectués au clavier ne sont pas pris en compte
		$("#texteCasePostale").change(TexteCasePostale_OnChange);
		$("#texteCasePostale").change();
	});

	function TexteCasePostale_OnChange() {
		var value = $(this).val();
		if (value == null || value === "") {
			var e = document.forms["formAddAdresse"].numeroCasePostale;
			e.value = "";
			$('#td_case_postale').hide();
			$('#td_case_postale_label').hide();
			$('#td_npa_case_postale').hide();
			$('#td_npa_case_postale_label').hide();
		}
		else {
			$('#td_case_postale_label').show();
			$('#td_case_postale').show();
			$('#td_npa_case_postale').show();
			$('#td_npa_case_postale_label').show();
		}
	}

	function AddressEdit_Adjust() {
		var form = $("#formAddAdresse").get(0);
		if (form.typeLocalite1.checked) {
			// localité suisse -> la localité est un prérequis pour que l'autocompletion soit activée sur le champs rue
			if (!StringUtils.isBlankString(form.numeroOrdrePoste.value)) {
				form.rue.readOnly = '';
				$('#rue').removeClass("readonly");
				$('#rue').autocomplete("enable");
			}
			else {
				form.rue.readOnly = 'readonly';
				$('#rue').addClass("readonly");
				$('#rue').autocomplete("disable");
				$('#rue').val('^^^ entrez une localité ^^^');
			}
		}
		else {
			// pays étranger -> le champs rue est une chaîne de caractères libre [UNIREG-3255]
			form.rue.readOnly = '';
			$('#rue').removeClass("readonly");
			$('#rue').autocomplete("disable");
			if ($('#rue').val() == '^^^ entrez une localité ^^^') { // [SIFISC-6339] on ne resette la rue que pour supprimer le placeholder
				$('#rue').val('');
			}
		}
	}

	function selectLocalite(name) {
		if (name == 'pays') {
			$('#div_label_localite_suisse').hide();
			$('#div_input_localite_suisse').hide();
			$('#div_pays').show();
			$('#div_label_lieu').show();
			$('#div_input_lieu').show();
			$('#div_label_pays_npa').show();
			$('#div_input_pays_npa').show();
		}
		if (name == 'localite_suisse') {
			$('#div_label_localite_suisse').show();
			$('#div_input_localite_suisse').show();
			$('#div_pays').hide();
			$('#div_label_lieu').hide();
			$('#div_input_lieu').hide();
			$('#div_label_pays_npa').hide();
			$('#div_input_pays_npa').hide();
		}
		AddressEdit_Adjust();

		var table = $('#adresse_saisie');
		table.find('tr').removeClass('even');
		table.find('tr').removeClass('odd');
		table.find('tr:visible:even').addClass('even');
		table.find('tr:visible:odd').addClass('odd');
	}

	<c:if test="${editCommand.typeLocalite == 'suisse'}">
		selectLocalite('localite_suisse');
	</c:if>
	<c:if test="${editCommand.typeLocalite == 'pays'}">
		selectLocalite('pays');
	</c:if>
</script>
