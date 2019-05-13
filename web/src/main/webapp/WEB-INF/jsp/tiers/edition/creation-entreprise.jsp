<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<%@ page import="ch.vd.unireg.common.LengthConstants" %>
<c:set var="id" value="${param.id}" />

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.creation.entreprise" />
	</tiles:put>
	<tiles:put name="fichierAide">
		<li>
			<%--<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/creation-inconnuCdH.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>--%>
		</li>
	</tiles:put>

	<tiles:put name="body">

		<unireg:setAuth var="autorisations" tiersId=""/>
		<form:form method="post" id="creationForm" name="createEntrepriseForm" modelAttribute="data" action="create.do">

			<!--onglets-->
			<div id="tiersCreationTabs">
				<ul>
					<li id="civilTab"><a href="#tabContent_civilTab"><fmt:message key="label.civil" /></a></li>
					<li id="complementsTab"><a href="#tabContent_complementsTab"><fmt:message key="label.complements" /></a></li>
				</ul>

				<div id="tabContent_civilTab" class="editTiers">

					<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
					<fieldset class="information">
						<legend><span><fmt:message key="label.entreprise" /></span></legend>
						<unireg:nextRowClass reset="1"/>
						<table border="0">

							<tr class="<unireg:nextRowClass/>">
								<td style="width: 20%;"><fmt:message key="label.date.ouverture" />&nbsp;:</td>
								<td colspan="3">
									<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
										<jsp:param name="path" value="civil.dateOuverture" />
										<jsp:param name="id" value="dateOuverture"  />
										<jsp:param name="tabindex" value="1"/>
										<jsp:param name="mandatory" value="true" />
									</jsp:include>
								</td>
							</tr>

							<c:set var="lengthRaisonSociale" value="<%=LengthConstants.ETB_RAISON_SOCIALE%>" scope="request" />
							<tr class="<unireg:nextRowClass/>">
								<td><fmt:message key="label.raison.sociale" />&nbsp;:</td>
								<td colspan="3">
									<form:input path="civil.raisonSociale" tabindex="2" id="raisonSociale" size="80" maxlength="${lengthRaisonSociale}"/>
									<span class="mandatory">*</span>
									<form:errors path="civil.raisonSociale" cssClass="error" />
								</td>
							</tr>

							<tr class="<unireg:nextRowClass/>">
								<td><fmt:message key="label.forme.juridique" />&nbsp;:</td>
								<td colspan="3">
									<form:select path="civil.formeJuridique" id="selectFormeJuridique" tabindex="3" onchange="CreateEntreprise.onChangeFormeJuridique(this.options[this.selectedIndex].value);">
										<form:option value=""/>
										<form:options items="${formesJuridiquesEntreprise}"/>
									</form:select>
									<span class="mandatory">*</span>
									<form:errors path="civil.formeJuridique" cssClass="error"/>
								</td>
							</tr>

							<tr class="<unireg:nextRowClass/> apresFJ">
								<td><fmt:message key="label.type.autorite.fiscale"/>&nbsp;:</td>
								<td>
									<div id="select_type_for">
										<form:select path="civil.typeAutoriteFiscale" id="optionTypeAutoriteFiscale" tabindex="4" cssStyle="width: 20ex;"
										             onchange="CreateEntreprise.selectAutoriteFiscale(this.options[this.selectedIndex].value, false);" />
									</div>
									<div id="mandatory_type_for" style="display: none;"></div>
								</td>

								<td>
									<label for="nomSiege">
										<span id="for_commune_label"><fmt:message key="label.commune"/></span>
										<span id="for_pays_label"><fmt:message key="label.pays"/></span>
										&nbsp;:
									</label>
								</td>
								<td>
									<input tabindex="5" id="siege" size="25" value="${data.civil.nomSiege}" />
									<span class="mandatory">*</span>
									<form:errors path="civil.numeroOfsSiege" cssClass="error" />
									<form:hidden path="civil.numeroOfsSiege" id="numeroOfsSiege" />
									<form:hidden path="civil.nomSiege" id="nomSiege" />
								</td>
							</tr>

							<tr class="<unireg:nextRowClass/> apresFJ">
								<td><fmt:message key="label.date.debut.exercice.commercial"/></td>
								<td colspan="3">
									<div style="float: left; margin-right: 2em; width: 30%;">
										<form:radiobutton id="debutExComm_DEFAULT" path="civil.typeDateDebutExerciceCommercial" value="DEFAULT" onclick="CreateEntreprise.onDateDebutExerciceDefautChange(true);" tabindex="6"/><fmt:message key="label.debut.annee.ouverture"/><br/>
										<form:radiobutton id="debutExComm_EXPLICIT" path="civil.typeDateDebutExerciceCommercial" value="EXPLICT" onclick="CreateEntreprise.onDateDebutExerciceDefautChange(false);" tabindex="7"/><fmt:message key="label.valeur.explicite"/>
									</div>
									<div style="margin-top: 0.5em; display: none;" id="specificDateDebutExercice">
										<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
											<jsp:param name="path" value="civil.dateDebutExerciceCommercial" />
											<jsp:param name="id" value="dateDebutExerciceCommercial"  />
											<jsp:param name="tabindex" value="8"/>
											<jsp:param name="mandatory" value="true" />
										</jsp:include>
									</div>
								</td>
							</tr>

							<tr class="<unireg:nextRowClass/> apresFJ">
								<td><fmt:message key="label.date.fondation"/></td>
								<td colspan="3">
									<div style="float: left; margin-right: 2em; width: 30%;">
										<form:radiobutton id="typeDateFondation_DEFAULT" path="civil.typeDateFondation" value="DEFAULT" onclick="CreateEntreprise.onDateFondationDefautChange(true);" tabindex="9"/><fmt:message key="label.identique.date.ouverture"/><br/>
										<form:radiobutton id="typeDateFondation_EXPLICIT" path="civil.typeDateFondation" value="EXPLICT" onclick="CreateEntreprise.onDateFondationDefautChange(false);" tabindex="10"/><fmt:message key="label.valeur.explicite"/>
									</div>
									<div style="margin-top: 0.5em; display: none;" id="specificDateFondation">
										<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
											<jsp:param name="path" value="civil.dateFondation" />
											<jsp:param name="id" value="dateFondation"  />
											<jsp:param name="tabindex" value="11"/>
											<jsp:param name="mandatory" value="true" />
										</jsp:include>
									</div>
								</td>
							</tr>

							<c:set var="lengthmonnaie" value="<%=LengthConstants.MONNAIE_ISO%>" scope="request" />
							<tr class="<unireg:nextRowClass/> apresFJ">
								<td><fmt:message key="label.capital.libere" />&nbsp;:</td>
								<td>
									<form:input path="civil.capitalLibere" tabindex="12" id="capitalLibere"
									            size="25" />
									<form:errors path="civil.capitalLibere" cssClass="error" />
								</td>
								<td width="20%"><fmt:message key="label.capital.monnaie"/>&nbsp;:</td>
								<td>
									<form:input id="devise" path="civil.devise" size="3" maxlength="${lengthmonnaie}" tabindex="13" />
									<form:errors path="civil.devise" cssClass="error" />
								</td>
							</tr>

							<c:set var="length_ide_min" value="<%=LengthConstants.IDENT_ENTREPRISE_IDE%>" scope="request" />
							<c:set var="length_ide_max" value="${length_ide_min + 3}" scope="request" />
							<tr class="<unireg:nextRowClass/> apresFJ">
								<td>
									<fmt:message key="label.numero.ide"/>&nbsp;:
								</td>
								<td colspan="3">
									<form:input size="25" id="numeroIde" path="civil.numeroIde" tabindex="14" maxlength="${length_ide_max}"
									            onchange="NumeroIDE.checkValue(this.value, '${length_ide_min}', null, 'ide_utilise_warning');"
									            onkeyup="NumeroIDE.checkValue(this.value, '${length_ide_min}', null, 'ide_utilise_warning');"/>
									<span style="margin-left: 2em;">
										<form:errors path="civil.numeroIde" cssClass="error" />
									</span>
									<span id="ide_utilise_warning" style="display:none; margin-left: 1em;" class="warn warning_icon"></span>
								</td>
							</tr>

							<tr class="<unireg:nextRowClass/>" id="trInscriptionRC">
								<td>
									<label for="inscriteRC"><fmt:message key="label.inscrite.rc" /></label>&nbsp;:
								</td>
								<td colspan="3">
									<form:checkbox path="civil.inscriteRC" id="inscriteRC" tabindex="15" />
								</td>
							</tr>
						</table>
					</fieldset>
				</div>

				<div id="tabContent_complementsTab" class="editTiers">

					<c:if test="${autorisations.complementsCommunications}">
						<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
						<fieldset>
							<legend><span><fmt:message key="label.complement.pointCommunication" /></span></legend>
							<unireg:nextRowClass reset="1"/>
							<c:set var="lengthnumTel" value="<%=LengthConstants.TIERS_NUMTEL%>" scope="request" />
							<c:set var="lengthnom" value="<%=LengthConstants.TIERS_NOM%>" scope="request" />
							<c:set var="lengthemail" value="<%=LengthConstants.TIERS_EMAIL%>" scope="request" />
							<table border="0">
								<tr class="<unireg:nextRowClass/>" >
									<td width="30%"><fmt:message key="label.complement.contact" />&nbsp;:</td>
									<td width="70%">
										<form:input path="complementCommunication.personneContact" id="tiers_personneContact" cssErrorClass="input-with-errors" size ="35" tabindex="3" onfocus="true" maxlength="${lengthpersonne}" />
										<span class="jTip formInfo" title="<c:url value="/htm/personneContact.htm?width=375"/>" id="tipPersonneContact">?</span>
										<form:errors path="complementCommunication.personneContact" cssClass="error"/>
									</td>
								</tr>
								<tr class="<unireg:nextRowClass/>" >
									<td width="30%"><fmt:message key="label.complement" />&nbsp;:</td>
									<td width="70%">
										<form:input path="complementCommunication.complementNom" id="tiers_complementNom" cssErrorClass="input-with-errors" size ="35" tabindex="4" maxlength="${lengthnom}" />
										<span class="jTip formInfo" title="<c:url value="/htm/complementNom.htm?width=375"/>" id="tipComplementNom">?</span>
										<form:errors path="complementCommunication.complementNom" cssClass="error"/>
									</td>
								</tr>
								<tr class="<unireg:nextRowClass/>" >
									<td width="30%"><fmt:message key="label.complement.numeroTelFixe" />&nbsp;:</td>
									<td width="70%">
										<form:input path="complementCommunication.numeroTelephonePrive" tabindex="5" id="tiers_numeroTelephonePrive" cssErrorClass="input-with-errors" size ="20" maxlength="${lengthnumTel}" />
										<span class="jTip formInfo" title="<c:url value="/htm/numeroTelephone.htm?width=375"/>" id="telPrive">?</span>
										<form:errors path="complementCommunication.numeroTelephonePrive" cssClass="error"/>
									</td>
								</tr>
								<tr class="<unireg:nextRowClass/>" >
									<td width="30%"><fmt:message key="label.complement.numeroTelPortable" />&nbsp;:</td>
									<td width="70%">
										<form:input path="complementCommunication.numeroTelephonePortable" tabindex="6" id="tiers_numeroTelephonePortable" cssErrorClass="input-with-errors" size ="20" maxlength="${lengthnumTel}" />
										<span class="jTip formInfo" title="<c:url value="/htm/numeroTelephone.htm?width=375"/>" id="telPortable">?</span>
										<form:errors path="complementCommunication.numeroTelephonePortable" cssClass="error"/>
									</td>
								</tr>
								<tr class="<unireg:nextRowClass/>" >
									<td width="30%"><fmt:message key="label.complement.numeroTelProfessionnel" />&nbsp;:</td>
									<td width="70%">
										<form:input path="complementCommunication.numeroTelephoneProfessionnel" tabindex="7" id="tiers_numeroTelephoneProfessionnel" cssErrorClass="input-with-errors" size ="20" maxlength="${lengthnumTel}" />
										<span class="jTip formInfo" title="<c:url value="/htm/numeroTelephone.htm?width=375"/>" id="telProfessionnel">?</span>
										<form:errors path="complementCommunication.numeroTelephoneProfessionnel" cssClass="error"/>
									</td>
								</tr>
								<tr class="<unireg:nextRowClass/>" >
									<td width="30%"><fmt:message key="label.complement.numeroFax" />&nbsp;:</td>
									<td width="70%">
										<form:input path="complementCommunication.numeroTelecopie" id="tiers_numeroTelecopie" tabindex="8" cssErrorClass="input-with-errors" size ="20" maxlength="${lengthnumTel}" />
										<span class="jTip formInfo" title="<c:url value="/htm/numeroTelephone.htm?width=375"/>" id="fax">?</span>
										<form:errors path="complementCommunication.numeroTelecopie" cssClass="error"/>
									</td>
								</tr>
								<tr class="<unireg:nextRowClass/>" >
									<td width="30%"><fmt:message key="label.complement.email" />&nbsp;:</td>
									<td width="70%">
										<form:input path="complementCommunication.adresseCourrierElectronique" tabindex="9" id="tiers_adresseCourrierElectronique" cssErrorClass="input-with-errors" size ="35" maxlength="${lengthpersonne}" />
										<span class="jTip formInfo" title="<c:url value="/htm/email.htm?width=375"/>" id="email">?</span>
										<form:errors path="complementCommunication.adresseCourrierElectronique" cssClass="error"/>
									</td>
								</tr>
							</table>
						</fieldset>
					</c:if>

					<c:if test="${autorisations.complementsCoordonneesFinancieres}">
						<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
						<fieldset>
							<legend><span><fmt:message key="label.complement.coordFinancieres" /></span></legend>
							<unireg:nextRowClass reset="1"/>
							<table>

								<c:set var="lengthnumcompte" value="<%=LengthConstants.TIERS_NUMCOMPTE%>" scope="request" />
								<tr class="<unireg:nextRowClass/>" >
									<td width="30%"><fmt:message key="label.complement.numeroCompteBancaire" />&nbsp;:</td>
									<td width="70%">
										<form:input path="complementCoordFinanciere.iban" tabindex="10"  id="tiers_numeroCompteBancaire" cssErrorClass="input-with-errors" size ="${lengthnumcompte}" maxlength="${lengthnumcompte}"/>
										<span class="jTip formInfo" title="<c:url value="/htm/iban.htm?width=375"/>" id="tipIban">?</span>
										<form:errors path="complementCoordFinanciere.iban" cssClass="error"/>
										<form:hidden path="complementCoordFinanciere.oldIban"/>
									</td>
								</tr>
								<tr class="<unireg:nextRowClass/>" >
									<td width="30%"><fmt:message key="label.complement.titulaireCompte" />&nbsp;:</td>
									<td width="70%">
										<form:input path="complementCoordFinanciere.titulaireCompteBancaire" tabindex="11" id="tiers_titulaireCompteBancaire" cssErrorClass="input-with-errors" size ="30" maxlength="${lengthpersonne}" />
										<span class="jTip formInfo" title="<c:url value="/htm/titulaireCompte.htm?width=375"/>" id="titulaireCompte">?</span>
										<form:errors path="complementCoordFinanciere.titulaireCompteBancaire" cssClass="error"/>
									</td>
								</tr>
								<c:set var="lengthbic" value="<%=LengthConstants.TIERS_ADRESSEBICSWIFT%>" scope="request" />
								<tr class="<unireg:nextRowClass/>" >
									<td width="30%"><fmt:message key="label.complement.bicSwift" />&nbsp;:</td>
									<td width="70%">
										<form:input path="complementCoordFinanciere.adresseBicSwift" tabindex="12"  id="tiers_adresseBicSwift" cssErrorClass="input-with-errors" size ="26" maxlength="${lengthbic}" />
										<span class="jTip formInfo" title="<c:url value="/htm/bic.htm?width=375"/>" id="bic">?</span>
										<form:errors path="complementCoordFinanciere.adresseBicSwift" cssClass="error"/>
									</td>
								</tr>
							</table>
						</fieldset>
					</c:if>

				</div>
			</div>

			<!-- Fin onglets -->

			<!-- Debut Boutons -->
			<unireg:buttonTo method="get" action="/tiers/list.do" name="label.bouton.retour" confirm="message.confirm.quit"/>

			<c:set var="confirmationMessageSauvegarde">
				<fmt:message key="label.demande.confirmation.sauvegarde"/>
			</c:set>
			<script type="text/javascript">
				var CreateEntreprise = {
					onSave : function(myform) {
						if (confirm('${confirmationMessageSauvegarde}')) {
							myform.submit();
						}
					},
					onDateFondationDefautChange: function(value) {
						if (value) {
							$('#specificDateFondation').hide();
						}
						else {
							$('#specificDateFondation').show();
						}
					},
					onDateDebutExerciceDefautChange: function(value) {
						if (value) {
							$('#specificDateDebutExercice').hide();
						}
						else {
							$('#specificDateDebutExercice').show();
						}
					},
					onChangeFormeJuridique: function(newFormeJuridique) {
						if (newFormeJuridique) {
							$('tr.apresFJ').show();

							const select = $('#optionTypeAutoriteFiscale');
							var oldtaf = select.find('option:selected').val();
							var url = App.curl('/tiers/entreprise/types-autorite-fiscale.do?fj=') + newFormeJuridique;
							$.get(url + '&' + new Date().getTime(), function(tafs) {
								var list = '';
								$.each(tafs, function (key, val) {
									list += '<option value="' + key + '"' + (oldtaf == key ? ' selected' : '') + '>' + StringUtils.escapeHTML(val) + '</option>';
								});
								select.html(list);

								var newtaf = select.find('option:selected').val();
								if (newtaf != oldtaf) {
									newtaf = newtaf || select.find("option:first").val();
									CreateEntreprise.selectAutoriteFiscale(newtaf, false);
								}

								const trInscriptionRC = $('#trInscriptionRC');
								if ('COMMUNE_OU_FRACTION_VD' in tafs) {
									trInscriptionRC.hide();
								}
								else {
									trInscriptionRC.show();
								}
							}, 'json')
							.error(Ajax.popupErrorHandler);
						}
						else {
							$('tr.apresFJ').hide();
							$('#trInscriptionRC').hide();
						}
					},
					selectAutoriteFiscale: function(name, firstLoad) {
						if (name == 'COMMUNE_OU_FRACTION_VD') {
							$('#for_commune_label').show();
							$('#for_pays_label').hide();
							if (!firstLoad) {
								$('#siege').val(null);
								$('#nomSiege').val(null);
								$('#numeroOfsSiege').val(null);
							}
							Autocomplete.infra('communeVD', '#siege', true, function(item) {
								$('#numeroOfsSiege').val(item ? item.id1 : null);
								$('#nomSiege').val(item ? item.label : null);
							});
						}
						else if (name == 'COMMUNE_HC') {
							$('#for_commune_label').show();
							$('#for_pays_label').hide();
							if (!firstLoad) {
								$('#siege').val(null);
								$('#nomSiege').val(null);
								$('#numeroOfsSiege').val(null);
							}
							Autocomplete.infra('communeHC', '#siege', true, function(item) {
								$('#numeroOfsSiege').val(item ? item.id1 : null);
								$('#nomSiege').val(item ? item.label : null);
							});
						}
						else if (name == 'PAYS_HS') {
							$('#for_commune_label').hide();
							$('#for_pays_label').show();
							if (!firstLoad) {
								$('#siege').val(null);
								$('#nomSiege').val(null);
								$('#numeroOfsSiege').val(null);
							}
							Autocomplete.infra('etat', '#siege', true, function(item) {
								$('#numeroOfsSiege').val(item ? item.id1 : null);
								$('#nomSiege').val(item ? item.label : null);
							});
						}
					}
				}
			</script>
			<input id="saveButton" type="button" name="save" value="<fmt:message key='label.bouton.sauver'/>" onclick="CreateEntreprise.onSave($('#creationForm'))" disabled="true"/>
			<!-- Fin Boutons -->

		</form:form>

		<script type="text/javascript">
			$(function() {
				$("#tiersCreationTabs").tabs();

				// on initialise l'auto-completion de l'autorité fiscale
				CreateEntreprise.selectAutoriteFiscale('${data.civil.typeAutoriteFiscale}', true);

				$('#typeDateFondation_${data.civil.typeDateDebutExerciceCommercial}').prop('checked', true);
				$('#debutExComm_${data.civil.typeDateDebutExerciceCommercial}').prop('checked', true);
				CreateEntreprise.onDateFondationDefautChange(${data.civil.typeDateFondation == 'DEFAULT'});
				CreateEntreprise.onDateDebutExerciceDefautChange(${data.civil.typeDateDebutExerciceCommercial == 'DEFAULT'});

				const value = $('#selectFormeJuridique').get(0);
				CreateEntreprise.onChangeFormeJuridique(value.options[value.selectedIndex].value);
			});
		</script>

		<script type="text/javascript">
			<spring:hasBindErrors name="data">
				<c:forEach items="${errors.globalErrors}" var="error">
					<c:if test="${unireg:startsWith(error.code, 'onglet.error')}">
						<c:set var="elementId">
							<spring:message message="${error}"/>
						</c:set>
						$('#${elementId}').addClass('error');
					</c:if>
				</c:forEach>
			</spring:hasBindErrors>

			$("#creationForm").find(":text,select").on('keyup change', function() {
				$('#saveButton').enable();
			});
		</script>

	</tiles:put>
</tiles:insert>
