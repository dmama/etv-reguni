<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<%@page import="ch.vd.uniregctb.common.LengthConstants"%>

<c:set var="lengthnumcompte" value="<%=LengthConstants.TIERS_NUMCOMPTE%>" scope="request" />
<c:set var="lengthpersonne" value="<%=LengthConstants.TIERS_PERSONNE%>" scope="request" />
<c:set var="lengthtel" value="<%=LengthConstants.TIERS_NUMTEL%>" scope="request" />

<c:set var="lengthadrnom" value="<%=LengthConstants.ADRESSE_NOM%>" scope="request" />
<c:set var="lengthadrnum" value="<%=LengthConstants.ADRESSE_NUM%>" scope="request" />

<input type="hidden" name="idMandant" value="${idMandant}"/>
<c:if test="${idMandataire != null}">
	<input type="hidden" name="idTiersMandataire" value="${idMandataire}"/>
</c:if>
<fieldset>
	<legend><span><fmt:message key="label.donnees.mandat"/></span></legend>
	<unireg:nextRowClass reset="0"/>
	<table>
		<tr class="<unireg:nextRowClass/>" >
			<td style="width: 15%;"><fmt:message key="label.date.ouverture" />&nbsp;:</td>
			<td style="width: 35%;">
				<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
					<jsp:param name="path" value="dateDebut" />
					<jsp:param name="id" value="dateDebut" />
					<jsp:param name="mandatory" value="true" />
				</jsp:include>
			</td>
			<td style="width: 15%;"><fmt:message key="label.date.fermeture" />&nbsp;:</td>
			<td style="width: 35%;">
				<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
					<jsp:param name="path" value="dateFin" />
					<jsp:param name="id" value="dateFin" />
				</jsp:include>
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>">
			<td><fmt:message key="label.type.mandat"/>&nbsp;:</td>
			<td>
				<form:select path="typeMandat" id="typeMandatSelect" onchange="AddInfosMandat.initTypeMandat(this.options[this.selectedIndex].value)">
					<form:options items="${typesMandatAutorises}"/>
				</form:select>
				<form:errors path="typeMandat" cssClass="error"/>
			</td>
			<td class="mdt-gen mdt-spec"><fmt:message key="label.avec.copie.courriers"/>&nbsp;:</td>
			<td class="mdt-gen mdt-spec"><form:checkbox path="withCopy"/></td>
			<td class="mdt-tiers"><fmt:message key="label.complement.numeroIBAN"/>&nbsp;:</td>
			<td class="mdt-tiers">
				<form:input path="iban" size="${lengthnumcompte}" maxlength="${lengthnumcompte}"/>
				<span style="color: red;">*</span>
				<span class="jTip formInfo" title="<c:url value="/htm/iban.htm?width=375"/>" id="tipIban">?</span>
				<form:errors path="iban" cssClass="error"/>
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/> mdt-gen mdt-spec">
			<td><fmt:message key="label.prenom.contact"/>&nbsp;:</td>
			<td><form:input path="prenomPersonneContact" size="40" maxlength="${lengthpersonne}"/></td>
			<td><fmt:message key="label.nom.contact"/>&nbsp;:</td>
			<td><form:input path="nomPersonneContact" size="40" maxlength="${lengthpersonne}"/></td>
		</tr>
		<tr class="<unireg:nextRowClass/> mdt-gen mdt-spec">
			<td><fmt:message key="label.no.tel.contact"/>&nbsp;:</td>
			<td><form:input path="noTelContact" size="25" maxlength="${lengthtel}"/></td>
			<td class="mdt-spec"><fmt:message key="label.genre.impot"/>&nbsp;:</td>
			<td class="mdt-spec">
				<form:select path="codeGenreImpot">
					<form:option value=""/>
					<form:options items="${genresImpotAutorises}"/>
				</form:select>
				<span style="color: red;">*</span>
				<form:errors path="codeGenreImpot" cssClass="error"/>
			</td>
			<td class="mdt-gen" colspan="2">&nbsp;</td>
		</tr>
	</table>
</fieldset>

<c:if test="${idMandataire == null}">
	<fieldset class="mdt-gen mdt-spec">
		<legend><span><fmt:message key="label.adresse"/></span></legend>
		<table>
			<tr class="<unireg:nextRowClass/>">
				<td style="width: 15%;"><fmt:message key="label.nom.raison"/>&nbsp;:</td>
				<td colspan="5">
					<form:input path="raisonSociale" size="40" maxlength="${lengthpersonne}"/>
					<span style="color: red;">*</span>
					<form:errors path="raisonSociale" cssClass="error"/>
				</td>
			</tr>
			<tr class="<unireg:nextRowClass/>">
				<td><fmt:message key="label.complements" />&nbsp;:</td>
				<td colspan="5">
					<form:input path="adresse.complements" cssErrorClass="input-with-errors" size ="100" maxlength="${lengthadrnom}" />
				</td>
			</tr>
			<tr class="<unireg:nextRowClass/>">
				<td><fmt:message key="label.localite.suisse"/>&nbsp;:</td>
				<td colspan="5">
					<form:input path="adresse.localiteSuisse" id="localiteSuisse" cssErrorClass="input-with-errors" size ="25" />
					<form:hidden path="adresse.numeroOrdrePoste" id="numeroOrdrePoste"  />
					<form:hidden path="adresse.numCommune" id="numCommune"  />
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

								AddInfosMandat.adjustAdresseEdit();
							});
						});
					</script>
					<form:errors path="adresse.localiteSuisse" cssClass="error"/>
				</td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				<td><fmt:message key="label.rue" />&nbsp;:</td>
				<td style="width: 35%;">
					<form:input path="adresse.rue" id="rue" size ="40" cssErrorClass="input-with-errors" maxlength="${lengthadrnom}" />
					<form:errors path="adresse.rue" cssClass="error"/>
					<form:hidden path="adresse.numeroRue" id="numeroRue"/>
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
							});
						});
					</script>
				</td>
				<td style="width: 15%;"><fmt:message key="label.numero.maison" />:</td>
				<td style="width: 10%;">
					<form:input path="adresse.numeroMaison" id="numeroMaison" cssErrorClass="input-with-errors" size ="5" maxlength="${lengthadrnum}" />
					<form:errors path="adresse.numeroMaison" cssClass="error"/>
				</td>
				<td colspan="2">&nbsp;</td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				<td><fmt:message key="label.texte.case.postale" />&nbsp;:</td>
				<td>
					<form:select path="adresse.texteCasePostale" id="texteCasePostale">
						<form:option value=""/>
						<form:options items="${textesCasePostale}" />
					</form:select>
				</td>
				<td class="withcp"><fmt:message key="label.case.postale" /> :</td>
				<td	class="withcp">
					<form:input path="adresse.numeroCasePostale" id="numeroCasePostale" cssErrorClass="input-with-errors" size ="5" />
					<form:errors path="adresse.numeroCasePostale" cssClass="error"/>
				</td>
				<td class="withcp" style="width: 15%;"><fmt:message key="label.npa.case.postale" /> :</td>
				<td	class="withcp" style="width: 10%;">
					<form:input path="adresse.npaCasePostale" id="npaCasePostale" cssErrorClass="input-with-errors" size ="4" />
					<form:errors path="adresse.npaCasePostale" cssClass="error"/>
				</td>
				<td class="withoutcp" colspan="4">&nbsp;</td>
			</tr>
		</table>
	</fieldset>
</c:if>

<script type="application/javascript">
	var AddInfosMandat = {

		initTypeMandat: function(typeMandat) {
			$(".mdt-gen, .mdt-spec, .mdt-tiers").hide();
			if (typeMandat == 'GENERAL') {
				$('.mdt-gen').show();
			}
			else if (typeMandat == 'SPECIAL') {
				$('.mdt-spec').show();
			}
			else if (typeMandat == 'TIERS') {
				$('.mdt-tiers').show();
			}
		},

		onChangeTexteCasePostale: function () {
			var value = $(this).val();
			if (value == null || value === "") {
				$('#numeroCasePostale').val("");
				$('td.withcp').hide();
				$('td.withoutcp').show();
			}
			else {
				$('td.withcp').show();
				$('td.withoutcp').hide();
			}
		},

		adjustAdresseEdit : function() {
			const rue = $("#rue");
			const noOrdrePoste = $('#numeroOrdrePoste')[0].value;

			// localité suisse -> la localité est un prérequis pour que l'autocompletion soit activée sur le champs rue
			if (!StringUtils.isBlankString(noOrdrePoste)) {
				rue[0].readOnly = false;
				rue.removeClass('readonly');
				rue[0].autocomplete = 'on';
			}
			else {
				rue[0].autocomplete = 'off';
				rue[0].value = '^^^ entrez une localité ^^^';
				rue[0].readOnly = true;
				rue.addClass('readonly');
			}
		}
	};

	$(function() {
		// IE et Chrome ne sélectionnent pas l'élément par défaut (= le premier), même si c'est ce qu'ils affichent
		// (curieusement, FF le fait...)
		const selectedMandat = $('#typeMandatSelect').find(':selected');
		var type;
		if (selectedMandat.length == 0) {
			type = $('#typeMandatSelect')[0].options[0].value;
		}
		else {
			type = selectedMandat[0].value;
		}
		AddInfosMandat.initTypeMandat(type);

		AddInfosMandat.adjustAdresseEdit();
		const texteCasePostale = $("#texteCasePostale");
		texteCasePostale.keyup(AddInfosMandat.onChangeTexteCasePostale); // autrement les changements de sélection effectués au clavier ne sont pas pris en compte
		texteCasePostale.change(AddInfosMandat.onChangeTexteCasePostale);
		texteCasePostale.change();
	});
</script>
