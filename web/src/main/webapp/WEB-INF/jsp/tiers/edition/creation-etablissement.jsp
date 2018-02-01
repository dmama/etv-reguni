<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<%@ page import="ch.vd.unireg.common.LengthConstants" %>
<c:set var="id" value="${param.id}" />

<%--@elvariable id="data" type="ch.vd.unireg.tiers.view.CreateEtablissementView"--%>
<%--@elvariable id="ctbAssocie" type="java.lang.Long"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.creation.etablissement.secondaire" />
	</tiles:put>

	<tiles:put name="body">

		<unireg:setAuth var="autorisations" tiersId=""/>
		<form:form method="post" id="creationForm" name="createEtb" commandName="data" action="create.do?numeroCtbAss=${ctbAssocie}">

			<!--onglets-->
			<div id="tiersCreationTabs">
				<ul>
					<li id="civilTab"><a href="#tabContent_civilTab"><fmt:message key="label.civil" /></a></li>
					<li id="complementsTab"><a href="#tabContent_complementsTab"><fmt:message key="label.complements" /></a></li>
				</ul>

				<div id="tabContent_civilTab" class="editTiers">

					<c:if test="${autorisations.donneesFiscales}">
						<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
						<fieldset><legend><span><fmt:message key="label.civil" /></span></legend>
							<table border="0">

								<c:set var="length_raisonSociale" value="<%=LengthConstants.ETB_RAISON_SOCIALE%>" scope="request" />
								<tr class="<unireg:nextRowClass/>" >
									<td style="width: 25%;">
										<fmt:message key="label.raison.sociale"/>&nbsp;:
									</td>
									<td>
										<form:input id="raisonSociale" path="civil.raisonSociale" tabindex="1" size="65" maxlength="${length_raisonSociale}" />
										<span class="mandatory">*</span>
										<span style="margin-left: 2em;">
											<form:errors path="civil.raisonSociale" cssClass="error" />
										</span>
									</td>
								</tr>
								<tr class="<unireg:nextRowClass/>" >
									<td>
										<fmt:message key="label.domicile"/>&nbsp;:
									</td>
									<td>
										<div id="choixCommune">
											<div style="float: left;">
												<input id="commune" size="25" value="${data.civil.nomCommune}" tabindex="2" />
												<span style="color: red;">&nbsp;*</span>
											</div>
											<div style="float: left; margin-left: 2em;">
												<form:errors path="civil.noOfsCommune" cssClass="error" />
											</div>
											<form:hidden path="civil.noOfsCommune" id="noOfsCommune" />
											<form:hidden path="civil.nomCommune" id="nomCommune" />
										</div>

									</td>
								</tr>
								<tr class="<unireg:nextRowClass/>" >
									<td>
										<fmt:message key="label.date.debut"/>&nbsp;:
									</td>
									<td>
										<script type="text/javascript">
											function dateDebut_OnChange( element) {
												if ( element)
													$("#dateDebutFormate").val(element.value);
											}
										</script>
										<form:hidden path="civil.sDateDebut" id="dateDebutFormate" />
										<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
											<jsp:param name="path" value="civil.dateDebut"/>
											<jsp:param name="id" value="dateDebut"/>
											<jsp:param name="onChange" value="dateDebut_OnChange"/>
											<jsp:param name="tabindex" value="3"/>
											<jsp:param name="mandatory" value="true" />
										</jsp:include>
										<span style="margin-left: 2em;">
											<form:errors path="civil.sDateDebut" cssClass="error" />
										</span>
									</td>
								</tr>
								<tr class="<unireg:nextRowClass/>" >
									<td>
										<fmt:message key="label.date.fin"/>&nbsp;:
									</td>
									<td>
										<script type="text/javascript">
											function dateFin_OnChange( element) {
												if ( element)
													$("#dateFinFormate").val(element.value);
											}
										</script>
										<form:hidden path="civil.sDateFin" id="dateFinFormate" />
										<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
											<jsp:param name="path" value="civil.dateFin"/>
											<jsp:param name="id" value="dateFin"/>
											<jsp:param name="onChange" value="dateFin_OnChange"/>
											<jsp:param name="tabindex" value="4"/>
										</jsp:include>
										<span style="margin-left: 2em;">
											<form:errors path="civil.sDateFin" cssClass="error" />
										</span>
									</td>
								</tr>

								<c:set var="length_ide_min" value="<%=LengthConstants.IDENT_ENTREPRISE_IDE%>" scope="request" />
								<c:set var="length_ide_max" value="${length_ide_min + 3}" scope="request" />
								<tr class="<unireg:nextRowClass/>" >
									<td>
										<fmt:message key="label.numero.ide"/>&nbsp;:
									</td>
									<td>
										<form:input id="numeroIde" path="civil.numeroIDE" tabindex="5" maxlength="${length_ide_max}"
										            onchange="NumeroIDE.checkValue(this.value, '${length_ide_min}', null, 'ide_utilise_warning');"
										            onkeyup="NumeroIDE.checkValue(this.value, '${length_ide_min}', null, 'ide_utilise_warning');"/>
										<span style="margin-left: 2em;">
											<form:errors path="civil.numeroIDE" cssClass="error" />
										</span>
										<span id="ide_utilise_warning" style="display:none; margin-left: 1em;" class="warn warning_icon"></span>
									</td>
								</tr>
								<tr class="<unireg:nextRowClass/>" >
									<td>
										<fmt:message key="label.nom.enseigne"/>&nbsp;:
									</td>
									<td>
										<form:input id="nomEnseigne" path="civil.nomEnseigne" tabindex="6"/>
									</td>
								</tr>

							</table>
						</fieldset>

						<script type="text/javascript">
							$(function() {

								// initialisation de l'autocomplétion sur le champ de la commune
								Autocomplete.infra('communeVD', '#commune', true, function(item) {
									$('#noOfsCommune').val(item ? item.id1 : null);
									$('#nomCommune').val(item ? item.label : null);
								});
							});
						</script>
					</c:if>

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
				$(function() {
					Tooltips.activate_ajax_tooltips();
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
			<unireg:buttonTo method="get" action="/tiers/visu.do" params="{id:${ctbAssocie}}" name="${libelleBoutonRetour}" confirm="${confirmationMessageRetour}"/>

			<c:set var="confirmationMessageSauvegarde">
				<fmt:message key="label.demande.confirmation.sauvegarde"/>
			</c:set>
			<script type="text/javascript">
				var createEtablissement = {
					onSave : function(myform) {
						if (confirm('${confirmationMessageSauvegarde}')) {
							myform.submit();
						}
					}
				}
			</script>
			<input id="saveButton" type="button" name="save" value="<fmt:message key='label.bouton.sauver'/>" onclick="createEtablissement.onSave($('#creationForm'))" disabled="true"/>
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
