<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<%@page import="ch.vd.uniregctb.common.LengthConstants"%>

<!-- Debut Adresse -->
<form:form name="formAddAdresse" id="formAddAdresse">

<form:hidden path="mode"/>
<form:hidden path="index"/>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.creation.adresse">
  			<fmt:param><unireg:numCTB numero="${command.numCTB}"/></fmt:param>
  		</fmt:message>
  	</tiles:put>

	<tiles:put name="body">

	<fieldset>
		<legend><span><fmt:message key="label.adresse.caracteristique" /></span></legend>
		<table>
			<tr class="<unireg:nextRowClass/>" >
				<td width="25%"><fmt:message key="label.adresse.utilisation" />:</td>
				<td width="75%">
				<c:choose>
						<c:when test="${command.id !=null}">
						<form:select disabled="true" path="usage"  items="${typeAdresseFiscaleTiers}"  />
						</c:when>
						<c:otherwise>
						<form:select  path="usage"  items="${typeAdresseFiscaleTiers}"  />
						<form:errors path="usage" cssClass="error"/>
						</c:otherwise>
				</c:choose>
				</td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				<td width="25%"><fmt:message key="label.date.ouverture" />&nbsp;:</td>
				<td width="75%">
					<c:choose>
						<c:when test="${command.id !=null}">
							<form:input disabled="true" path="dateDebut"  id="dateDebut" cssErrorClass="input-with-errors" size ="10" />
						</c:when>
						<c:otherwise>
						<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
							<jsp:param  name="path" value="dateDebut" />
							<jsp:param name="id" value="dateDebut" />
						</jsp:include>
						</c:otherwise>
					</c:choose>
				</td>
			</tr>
		</table>
	</fieldset>

	<div id="tabs">
		<ul id="adresseEditTabs">
			<li id="createNewAdresseTab">
				<a href="#" onclick="javascript:Tabulation.show(this);"><fmt:message key="label.creer.nouvelle.adresse" /></a>
			</li>
			<li id="repriseAdresseTab">
				<a href="#" onclick="javascript:Tabulation.show(this);"><fmt:message key="label.reutiliser.adresse.civile" /></a>
			</li>
		</ul>
	</div>

	<div id="tabContent_repriseAdresseTab" class="editTiers" style="display: none;">
		<c:if test="${not empty command.adresseDisponibles}">
			<c:if test="${command.nature != 'DebiteurPrestationImposable' && command.nature != 'NonHabitant'}">
				<fieldset>
				<legend><span><fmt:message key="title.edition.adresseActives" /></span></legend>
				<table>
					<display:table 	name="command.adresseDisponibles" id="adresse" pagesize="10" class="display" sort="list">

							<display:column sortable ="true" titleKey="label.adresse.source">
								<fmt:message key="option.source.${adresse.source}" />
							</display:column>
							<display:column  sortable ="true" titleKey="label.type.adresseCivil">
								<c:if test="${adresse.typeAdresseToString != null}">
									<fmt:message key="option.type.adresse.civil.${adresse.typeAdresseToString}" />
								</c:if>
							</display:column>
							<display:column property="representantLegal" sortable ="true" titleKey="label.representant"  />
							<display:column property="rue"  sortable ="true" titleKey="label.rueCasePostale">
							</display:column>
							<display:column property ="localite" sortable ="true" titleKey="label.localite" />
							<display:column  property ="paysNpa" sortable ="true" titleKey="label.pays" />
							<display:column  sortable ="true" titleKey="label.reprise">
							<c:if test="${adresse.source != null && adresse.source =='CIVILE'}">
								<a href="#" class="copy" onclick="reprise('repriseCivil','<c:out value="${adresse_rowNum - 1}"/>')"><span class="copy">Reprise Civil</span></a>
							</c:if>
							<c:if test="${adresse.source != null && (adresse.source =='CONSEIL_LEGAL' || adresse.source =='TUTELLE')}">
								<a href="#" class="copy" onclick="reprise('reprise','<c:out value="${adresse_rowNum - 1}"/>')"><span class="copy">Reprise Representant</span></a>
							</c:if>
							</display:column>
					</display:table>
				</table>
				</fieldset>
			</c:if>
		</c:if>
	</div>


	<div id="tabContent_createNewAdresseTab" class="situation_fiscale" style="display: none;">
		<c:set var="lengthadrnom" value="<%=LengthConstants.ADRESSE_NOM%>" scope="request" />
		<c:set var="lengthadrnum" value="<%=LengthConstants.ADRESSE_NUM%>" scope="request" />

		<fieldset><legend><span><fmt:message key="label.nouvelleAdresse" /></span></legend>
			<div id="adresse_saisie" >
				<table border="0">
				<tr class="odd">
					<td width="25%"><fmt:message key="label.complements" />:</td>
					<td colspan="3" width="75%">
					<form:input path="complements" cssErrorClass="input-with-errors"
					size ="100" maxlength="${lengthadrnom}" /></td>
				</tr>
				<tr class="even">
					<td colspan="2" width="50%">
					<c:if test="${command.id != null}">
						<form:radiobutton path="typeLocalite" onclick="selectLocalite('localite_suisse');" value="suisse" disabled="true"/>
					</c:if>
					<c:if test="${command.id == null}">
						<form:radiobutton path="typeLocalite" onclick="selectLocalite('localite_suisse');" value="suisse" disabled="false"/>
					</c:if>
						<label for="typeLocalite1"><fmt:message key="label.suisse" /></label>
					</td>
					<td colspan="2" width="50%">
						<c:if test="${command.id != null}">
							<form:radiobutton path="typeLocalite" onclick="selectLocalite('pays');" value="pays"  disabled="true" />
						</c:if>
						<c:if test="${command.id == null}">
							<form:radiobutton path="typeLocalite" onclick="selectLocalite('pays');" value="pays"  disabled="false" />
						</c:if>
						<label for="typeLocalite2"><fmt:message key="label.etranger" /></label>
					</td>
				</tr>
				<tr class="odd">
					<td width="25%">
						<div id="div_label_localite_suisse"><fmt:message key="label.localite.suisse" />:</div>
						<div id="div_label_pays_npa" style="display:none;"><fmt:message key="label.npa.localite" /> :</div>
					</td>
					<td width="25%">
						<div id="div_input_localite_suisse">
							<form:input path="localiteSuisse"  id="localiteSuisse" cssErrorClass="input-with-errors" size ="25" />
							<form:hidden path="numeroOrdrePoste" id="numeroOrdrePoste"  />
							<form:hidden path="numCommune" id="numCommune"  />
							<script>
								$(function() {
									autocomplete_infra('localite', '#localiteSuisse', function(item) {
										if (item) {
											$('#numeroOrdrePoste').val(item.id1);
											$('#numCommune').val(item.id2);
										}
										else {
											$('#localiteSuisse').val(null);
											$('#numeroOrdrePoste').val(null);
											$('#numCommune').val(null);
										}
										$('#numeroRue').val('');
										$('#rue').val('');
										
										// à chaque changement de localité, on adapte l'autocompletion sur la rue en conséquence
										autocomplete_infra('rue&numCommune=' + $('#numCommune').val(), '#rue', function(i) {
											if (i) {
												$('#numeroRue').val(i.id1);
												$('#numeroOrdrePoste').val(i.id2);
											}
											else {
												$('#numeroRue').val(null);
												$('#numeroOrdrePoste').val(null);
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
					<td width="25%">
						<div id="div_label_lieu" style="display:none;">
						<fmt:message key="label.complement.localite" />:</div>
					</td>
					<td width="35%">
						<div id="div_input_lieu" style="display:none;">
							<form:input path="complementLocalite" id="complementLocalite" cssErrorClass="input-with-errors"
							size ="20" maxlength="${lengthadrnom}" />
							<form:errors path="complementLocalite" cssClass="error"/>
						</div>
					</td>
				</tr>
				<tr class="even" id="div_pays" style="display:none;">
					<td width="25%"><fmt:message key="label.paysEtranger" />:</td>
					<td colspan="3"  width="75%">
							<form:hidden path="paysOFS" id="tiers_numeroOfsNationalite"/>
							<form:input path="paysNpa" id="pays" cssErrorClass="input-with-errors" size ="20" />
							<form:errors path="paysNpa" cssClass="error"/>
							<script>
								$(function() {
									autocomplete_infra('pays', '#pays', function(item) {
										$('#tiers_numeroOfsNationalite').val(item ? item.id1 : null);
									});
								});
							</script>
					</td>
				</tr>
				<tr class="even">
					<td width="25%"><fmt:message key="label.rue" />&nbsp;:</td>
					<td width="25%">
						<form:input path="rue" id="rue" size ="25" cssErrorClass="input-with-errors" maxlength="${lengthadrnom}" />
						<form:errors path="rue" cssClass="error"/>
						<form:hidden path="numeroRue" id="numeroRue"/>
						<script>
							$(function() {
								autocomplete_infra('rue&numCommune=' + $('#numCommune').val(), '#rue', function(i) {
									if (i) {
										$('#numeroRue').val(i.id1);
										$('#numeroOrdrePoste').val(i.id2);
									}
									else {
										$('#numeroRue').val(null);
										$('#numeroOrdrePoste').val(null);
									}
								});
							}):
						</script>
					</td>
					<td width="25%"><fmt:message key="label.numero.maison" />:</td>
					<td width="25%">
						<form:input path="numeroMaison" id="numeroMaison" cssErrorClass="input-with-errors"
						size ="5" maxlength="${lengthadrnum}" />
						<form:errors path="numeroMaison" cssClass="error"/>
					</td>
				</tr>
				<tr class="odd">
					<td width="25%"><fmt:message key="label.numero.appartement" />&nbsp;:</td>
					<td width="75%" colspan="3">
						<form:input path="numeroAppartement" id="numeroAppartement" cssErrorClass="input-with-errors"
						size ="5" maxlength="${lengthadrnum}" />
						<form:errors path="numeroAppartement" cssClass="error"/>
					</td>
				</tr>
				<tr class="even">
					<td width="25%"><fmt:message key="label.texte.case.postale" />&nbsp;:</td>
					<td width="25%">
						<form:select path="texteCasePostale" onchange="javascript:TexteCasePostale_OnChange(this);">
							<form:option value="" ></form:option>
							<form:options items="${textesCasePostale}" />
						</form:select>
						<script type="text/javascript" language="Javascript.1.3">
							function TexteCasePostale_OnChange( select) {
								var value = select.options[select.selectedIndex].value;
								if ( value == null || value === "") {
									var e =  document.forms["formAddAdresse"].numeroCasePostale;
									e.value = "";
									Element.hide('td_case_postale');
									Element.hide('td_case_postale_label');
								} else {
									Element.show('td_case_postale_label');
									Element.show('td_case_postale');
								}
							}
						</script>
					</td>
					<td width="25%" id="td_case_postale_label"><fmt:message key="label.case.postale" /> :</td>
					<td width="25%"	id="td_case_postale">
						<form:input path="numeroCasePostale" id="numeroCasePostale" cssErrorClass="input-with-errors" size ="5" />
						<form:errors path="numeroCasePostale" cssClass="error"/>
					</td>
				</tr>
				<tr class="odd">
					<td width="25%"><fmt:message key="label.adresse.permanente" />&nbsp;:</td>
					<td width="75%" colspan="3">
						<form:checkbox path="permanente" id="adressePermanente" cssErrorClass="input-with-errors" />
						<form:errors path="permanente" cssClass="error"/>
					</td>
				</tr>

				</table>
			</div>
		</fieldset>


		<div id="adresse_add">
			<table border="0">
			<tr>
				<td width="25%"></td>
				<td width="25%">
					<c:choose>
						<c:when test="${command.id !=null}">
							<input type="submit" id="maj" name="update" value="<fmt:message key="label.bouton.mettre.a.jour" />">
						</c:when>
						<c:otherwise>
							<input type="submit" name="update" value="<fmt:message key="label.bouton.ajouter" />" />
						</c:otherwise>
					</c:choose>
				</td>
				<td width="25%"><input type="button" name="cancel" value="<fmt:message key="label.bouton.retour" />" onClick="document.location.href='../adresses/edit.do?id=' + ${command.numCTB}" /></td>
				<td width="25%"></td>
			</tr>
			</table>
		</div>
	</div>


	</tiles:put>
</tiles:insert>

<input name="numCTB" type="hidden"  value="${command.numCTB}" />

</form:form>
<!-- Fin Adresse -->

<script type="text/javascript" language="Javascript1.3">
		Tabulation.attachObserver("change", Tab_Change);
		var tabulationInitalized = false;
		var onglet = request.getParameter("onglet");
		if ( onglet) {
			Tabulation.show(onglet);
		} else {
			Tabulation.restoreCurrentTabulation("adresseEditTabs");
		}

		function Tab_Change( selectedTab) {
			if( selectedTab) {
				tabulationInitalized = true;
			}
			if (!tabulationInitalized) {
				Tabulation.showFirst( "adresseEditTabs");
			}
		}

		function showPrintView() {
			Tabulation.showAll('adresseEditTabs');
			E$("tabs").style.display="none";
			E$("tabnav-disable").style.display="none";
			E$("tabnav-enable").style.display="";
		}

		function showScreenView() {
			Tabulation.showFirst('adresseEditTabs');
			E$("tabs").style.display="";
			E$("tabnav-disable").style.display="";
			E$("tabnav-enable").style.display="none";
		}
</script>

<script type="text/javascript" language="Javascript.1.3">
	Element.addObserver( window, "load", function() {
		AddressEdit_Adjust();
		Element.fireObserver( F$("formAddAdresse").texteCasePostale, "change");
	});
	
	function AddressEdit_Adjust() {
		var form = F$("formAddAdresse");
		if (!isBlankString(form.numeroOrdrePoste.value) || !form.typeLocalite1.checked) {
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

	function selectLocalite(name) {
		if( name == 'pays' ){
			Element.hide('div_label_localite_suisse');
			Element.hide('div_input_localite_suisse');
			Element.show('div_pays');	
			Element.show('div_label_lieu');
			Element.show('div_input_lieu');	
			Element.show('div_label_pays_npa');	
			Element.show('div_input_pays_npa');
		}
		if( name == 'localite_suisse' ){
			Element.show('div_label_localite_suisse');
			Element.show('div_input_localite_suisse');
			Element.hide('div_pays');	
			Element.hide('div_label_lieu');
			Element.hide('div_input_lieu');	
			Element.hide('div_label_pays_npa');	
			Element.hide('div_input_pays_npa');
		}
		AddressEdit_Adjust();
	}
	<c:if test="${command.typeLocalite == 'suisse'}">
	selectLocalite('suisse');
	</c:if>
	<c:if test="${command.typeLocalite == 'pays'}">
	selectLocalite('pays');
	</c:if>
</script>