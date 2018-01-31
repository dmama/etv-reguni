<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<%@ page import="ch.vd.uniregctb.common.LengthConstants" %>
<c:set var="id" value="${param.id}" />

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.creation.pp" />
	</tiles:put>
	<tiles:put name="fichierAide">
		<li>
			<a href="#" onClick="ouvrirAide('<c:url value='/docs/creation-inconnuCdH.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
		</li>
	</tiles:put>

	<tiles:put name="body">

		<unireg:setAuth var="autorisations" tiersId=""/>
		<form:form method="post" id="creationForm" name="createNH" commandName="data" action="create.do">

			<!--onglets-->
			<div id="tiersCreationTabs">
				<ul>
					<li id="civilTab"><a href="#tabContent_civilTab"><fmt:message key="label.civil" /></a></li>
					<li id="complementsTab"><a href="#tabContent_complementsTab"><fmt:message key="label.complements" /></a></li>
				</ul>

				<div id="tabContent_civilTab" class="editTiers">

					<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
					<fieldset class="information">
						<legend><span><fmt:message key="label.nonHabitant" /></span></legend>
						<unireg:nextRowClass reset="1"/>
						<table border="0">

							<c:set var="lengthnom" value="<%=LengthConstants.TIERS_NOM%>" scope="request" />
							<tr class="<unireg:nextRowClass/>">
								<td><fmt:message key="label.nom" />&nbsp;:</td>
								<td>
									<form:input path="civil.nom" tabindex="1" id="tiers_nom" cssErrorClass="input-with-errors"
									            size="20" maxlength="${lengthnom}" />
									<span class="jTip formInfo" title="<c:url value="/htm/nom.htm?width=375"/>" id="nom">?</span>
									<span class="mandatory">*</span>
									<form:errors path="civil.nom" cssClass="error" />
								</td>
							</tr>

							<tr class="<unireg:nextRowClass/>">
								<td><fmt:message key="label.nom.naissance" />&nbsp;:</td>
								<td>
									<form:input path="civil.nomNaissance" tabindex="2" id="tiers_nomNaissance" cssErrorClass="input-with-errors"
									            size="20" maxlength="${lengthnom}" />
									<span class="jTip formInfo" title="<c:url value="/htm/nom.htm?width=375"/>" id="nomNaissance">?</span>
									<form:errors path="civil.nomNaissance" cssClass="error" />
								</td>
							</tr>

							<tr class="<unireg:nextRowClass/>">
								<td width="40%"><fmt:message key="label.prenom.usuel" />&nbsp;:</td>
								<td width="60%">
									<form:input path="civil.prenomUsuel" tabindex="3" id="tiers_prenom" cssErrorClass="input-with-errors"
									            size="20" maxlength="${lengthnom}" />
									<span class="jTip formInfo" title="<c:url value="/htm/prenom.htm?width=375"/>" id="prenomUsuel">?</span>
									<div id="empty_tiers_prenom_warning" style="display:none;" class="warn warning_icon"><fmt:message key="warning.prenom.vide"/></div>
									<form:errors path="civil.prenomUsuel" cssClass="error" />
								</td>
							</tr>

							<c:set var="lengthtousprenoms" value="<%=LengthConstants.TIERS_TOUS_PRENOMS%>" scope="request" />
							<tr class="<unireg:nextRowClass/>">
								<td width="40%"><fmt:message key="label.prenoms" />&nbsp;:</td>
								<td width="60%">
									<form:input path="civil.tousPrenoms" tabindex="4" id="tiers_prenoms" cssErrorClass="input-with-errors"
									            size="20" maxlength="${lengthtousprenoms}" />
									<span class="jTip formInfo" title="<c:url value="/htm/prenom.htm?width=375"/>" id="tousPrenoms">?</span>
									<form:errors path="civil.tousPrenoms" cssClass="error" />
								</td>
							</tr>

							<tr class="<unireg:nextRowClass/>">
								<td><fmt:message key="label.sexe" />&nbsp;:</td>
								<td>
									<form:select path="civil.sexe" tabindex="5" >
										<form:option value="" />
										<form:options items="${sexes}" />
									</form:select>
								</td>
							</tr>

							<tr class="<unireg:nextRowClass/>">
								<td><fmt:message key="label.date.naissance" />&nbsp;:</td>
								<td>
									<script type="text/javascript">
										function dateNaissance_OnChange( element) {
											if ( element)
												$("#dateNaissanceFormate").val(element.value);
										}
									</script>
									<form:hidden path="civil.sDateNaissance" id="dateNaissanceFormate" />
									<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
										<jsp:param name="path" value="civil.dateNaissance" />
										<jsp:param name="id" value="dateNaissance"  />
										<jsp:param name="onChange" value="dateNaissance_OnChange"/>
										<jsp:param name="tabindex" value="6"/>
									</jsp:include>
								</td>
							</tr>

							<tr class="<unireg:nextRowClass/>">
								<td><fmt:message key="label.date.deces" />&nbsp;:</td>
								<td>
									<unireg:regdate regdate="${data.civil.dateDeces}"/>
								</td>
							</tr>

							<tr class="<unireg:nextRowClass/>">
								<td><fmt:message key="label.nouveau.numero.avs" />&nbsp;:</td>
								<td>
									<form:input path="civil.numeroAssureSocial" id="tiers_numeroAssureSocial" tabindex="7"
									            cssErrorClass="input-with-errors" size="20" maxlength="16" />
									<span class="jTip formInfo" title="<c:url value="/htm/numeroAVS.htm?width=375"/>" id="numeroAVS">?</span>
									<form:errors path="civil.numeroAssureSocial" cssClass="error" />
								</td>
							</tr>

							<tr class="<unireg:nextRowClass/>">
								<td><fmt:message key="label.ancien.numero.avs" />&nbsp;:</td>
								<td>
									<form:input path="civil.identificationPersonne.ancienNumAVS" id="tiers_ancienNumAVS" tabindex="8"
									            cssErrorClass="input-with-errors" size="20" maxlength="14" />
									<span class="jTip formInfo" title="<c:url value="/htm/ancienNumeroAVS.htm?width=375"/>" id="ancienNumeroAVS">?</span>
									<form:errors path="civil.identificationPersonne.ancienNumAVS" cssClass="error" />
								</td>
							</tr>

							<tr class="<unireg:nextRowClass/>">
								<td><fmt:message key="label.commune.origine" />&nbsp;:</td>
								<td>
									<form:hidden path="civil.ofsCommuneOrigine" id="tiers_numeroOfsCommuneOrigine"/>
									<form:hidden path="civil.oldLibelleCommuneOrigine"/>
									<form:input path="civil.newLibelleCommuneOrigine" id="tiers_libelleCommuneOrigine" tabindex="9" cssErrorClass="input-with-errors" size="30" maxlength="250" />
									<script>
										$(function() {
											Autocomplete.infra('commune', '#tiers_libelleCommuneOrigine', true, function(item) {
												$('#tiers_numeroOfsCommuneOrigine').val(item ? item.id1 : null);
											});
										});
									</script>
									<form:errors path="civil.ofsCommuneOrigine" cssClass="error" />
								</td>
							</tr>

							<tr class="<unireg:nextRowClass/>">
								<td><fmt:message key="label.nationalite" />&nbsp;:</td>
								<td>
									<form:hidden path="civil.numeroOfsNationalite" id="tiers_numeroOfsNationalite" />
									<form:input path="civil.libelleOfsPaysOrigine" id="tiers_libelleOfsPaysOrigine" cssErrorClass="input-with-errors" tabindex="10" size="20" />
									<script>
										$(function() {
											Autocomplete.infra('etatOuTerritoire', '#tiers_libelleOfsPaysOrigine', true, function(item) {
												$('#tiers_numeroOfsNationalite').val(item ? item.id1 : null);
											});
										});
									</script>
									<form:errors path="civil.numeroOfsNationalite" cssClass="error" />
								</td>
							</tr>

							<tr class="<unireg:nextRowClass/>">
								<td><fmt:message key="label.categorie.etranger" />&nbsp;:</td>
								<td>
									<form:select path="civil.categorieEtranger" tabindex="11" >
										<form:option value=""/>
										<form:options items="${categoriesEtrangers}" />
									</form:select>
								</td>
							</tr>

							<tr class="<unireg:nextRowClass/>">
								<td><fmt:message key="label.date.debut.validite.autorisation" />&nbsp;:</td>
								<td>
									<script type="text/javascript">
										function dateDebutValiditeAutorisation_OnChange( element) {
											if (element) {
												$("#dateDebutValiditeAutorisationFormate").val(element.value);
											}
										}
									</script>
									<form:hidden path="civil.sDateDebutValiditeAutorisation" id="dateDebutValiditeAutorisationFormate" />
									<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
										<jsp:param name="path" value="civil.dateDebutValiditeAutorisation" />
										<jsp:param name="id" value="dateDebutValiditeAutorisation"  />
										<jsp:param name="onChange" value="dateDebutValiditeAutorisation_OnChange"/>
										<jsp:param name="tabindex" value="12"/>
									</jsp:include>
								</td>
							</tr>

							<tr class="<unireg:nextRowClass/>">
								<td><fmt:message key="label.numero.registre.etranger" />&nbsp;:</td>
								<td>
									<form:input path="civil.identificationPersonne.numRegistreEtranger" tabindex="13" id="tiers_numRegistreEtranger"
									            cssErrorClass="input-with-errors" size="20" maxlength="13" />
									<form:errors path="civil.identificationPersonne.numRegistreEtranger" cssClass="error" />
									<span class="jTip formInfo" title="<c:url value="/htm/numRegistreEtranger.htm?width=375"/>" id="numRegistre">?</span>
								</td>
							</tr>

							<c:set var="lengthnomparents" value="<%=LengthConstants.TIERS_NOM_PRENOMS_PARENT%>" scope="request" />
							<tr class="<unireg:nextRowClass/>">
								<td width="40%"><fmt:message key="label.prenoms.pere" />&nbsp;:</td>
								<td width="60%">
									<form:input path="civil.prenomsPere" tabindex="14" id="tiers_prenomsPere" cssErrorClass="input-with-errors"
									            size="20" maxlength="${lengthnomparents}" />
									<form:errors path="civil.prenomsPere" cssClass="error" />
								</td>
							</tr>
							<tr class="<unireg:nextRowClass/>">
								<td width="40%"><fmt:message key="label.nom.pere" />&nbsp;:</td>
								<td width="60%">
									<form:input path="civil.nomPere" tabindex="15" id="tiers_nomPere" cssErrorClass="input-with-errors"
									            size="20" maxlength="${lengthnomparents}" />
									<form:errors path="civil.nomPere" cssClass="error" />
								</td>
							</tr>
							<tr class="<unireg:nextRowClass/>">
								<td width="40%"><fmt:message key="label.prenoms.mere" />&nbsp;:</td>
								<td width="60%">
									<form:input path="civil.prenomsMere" tabindex="16" id="tiers_prenomsMere" cssErrorClass="input-with-errors"
									            size="20" maxlength="${lengthnomparents}" />
									<form:errors path="civil.prenomsMere" cssClass="error" />
								</td>
							</tr>
							<tr class="<unireg:nextRowClass/>">
								<td width="40%"><fmt:message key="label.nom.mere" />&nbsp;:</td>
								<td width="60%">
									<form:input path="civil.nomMere" tabindex="17" id="tiers_nomMere" cssErrorClass="input-with-errors"
									            size="20" maxlength="${lengthnomparents}" />
									<form:errors path="civil.nomMere" cssClass="error" />
								</td>
							</tr>

						</table>

						<script>
							$(function() {
								Tooltips.activate_ajax_tooltips();

								var refresh_prenom_warning = function() {
									var prenom = $('#tiers_prenom').val();
									var nom = $('#tiers_nom').val();
									if (StringUtils.isBlank(prenom) && /\s*\S+\s+\S+\s*/.test(nom)) {
										// on affiche le warning si le prénom est vide que le nom contient deux mots distincts
										$('#empty_tiers_prenom_warning').css('display', 'inline'); // on n'utilise pas show() parce que le display devient 'block' et on veut 'inline'
									}
									else {
										$('#empty_tiers_prenom_warning').hide();
									}
								};

								$('#tiers_nom').change(refresh_prenom_warning);
								$('#tiers_nom').keyup(refresh_prenom_warning);
								$('#tiers_prenom').change(refresh_prenom_warning);
								$('#tiers_prenom').keyup(refresh_prenom_warning);

								refresh_prenom_warning();
							});

						</script>
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

			<script type="text/javascript">
				$(function() {
					$("#tiersCreationTabs").tabs();
				});
			</script>

			<!-- Fin onglets -->

			<!-- Debut Boutons -->
			<c:set var="libelleBoutonRetour">
				<fmt:message key="label.bouton.retour"/>
			</c:set>
			<c:set var="confirmationMessageRetour">
				<fmt:message key="message.confirm.quit"/>
			</c:set>
			<unireg:buttonTo method="get" action="/tiers/list.do" name="${libelleBoutonRetour}" confirm="${confirmationMessageRetour}"/>

			<c:set var="confirmationMessageSauvegarde">
				<fmt:message key="label.demande.confirmation.sauvegarde"/>
			</c:set>
			<script type="text/javascript">
				var createNonHabitant = {
					onSave : function(myform) {
						if (confirm('${confirmationMessageSauvegarde}')) {
							myform.submit();
						}
					}
				}
			</script>
			<input id="saveButton" type="button" name="save" value="<fmt:message key='label.bouton.sauver'/>" onclick="createNonHabitant.onSave($('#creationForm'))" disabled="true"/>
			<!-- Fin Boutons -->

		</form:form>

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
