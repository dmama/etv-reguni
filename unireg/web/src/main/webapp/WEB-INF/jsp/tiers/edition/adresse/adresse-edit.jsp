<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<%@page import="ch.vd.uniregctb.common.LengthConstants"%>

<!-- Debut Adresse -->
<form:form name="formAddAdresse" id="formAddAdresse">

<form:hidden path="mode"/>
<form:hidden path="index"/>
<tiles:insert template="/WEB-INF/jsp/templates/templateIFrame.jsp">
	<tiles:put name="head"></tiles:put>
	<tiles:put name="title"></tiles:put>
	<tiles:put name="body">
	<div id="adresses" class="adresses">
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
						<script type="text/javascript">
								function localiteSuisse_onChange(row) {	
									var form = document.forms["formAddAdresse"];
									form.numeroOrdrePoste.value = ( row ? row.noOrdre : "");
									form.numCommune.value = ( row ? row.numCommune : "");
									form.numeroRue.value ='';
									form.rue.value ='';
									AddressEdit_Adjust();
								}
						</script>
						<jsp:include page="/WEB-INF/jsp/include/autocomplete.jsp">
							<jsp:param name="inputId" value="localiteSuisse" />
							<jsp:param name="dataValueField" value="nomCommune" />
							<jsp:param name="dataTextField" value="{nomCommune} ({npa})" />
							<jsp:param name="dataSource" value="selectionnerLocalite" />
							<jsp:param name="onChange" value="localiteSuisse_onChange" />
							<jsp:param name="autoSynchrone" value="false"/>
						</jsp:include>
						
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
						<script type="text/javascript">
								function paysNpa_onChange(row) {
									document.forms["formAddAdresse"].paysOFS.value = (row ? row.noOFS : "");									
								}
						</script>
						<jsp:include page="/WEB-INF/jsp/include/autocomplete.jsp">
							<jsp:param name="inputId" value="pays" />
							<jsp:param name="dataValueField" value="nomMinuscule" />
							<jsp:param name="dataTextField" value="{nomMinuscule}" />
							<jsp:param name="dataSource" value="selectionnerPays" />
							<jsp:param name="onChange" value="paysNpa_onChange" />
							<jsp:param name="autoSynchrone" value="false"/>
						</jsp:include>
						
				</td>
			</tr>
			<tr class="even">
				<td width="25%"><fmt:message key="label.rue" />&nbsp;:</td>
				<td width="25%">
					<form:input path="rue" id="rue" size ="25" cssErrorClass="input-with-errors" 
					maxlength="${lengthadrnom}" />
					<form:errors path="rue" cssClass="error"/>
					<form:hidden path="numeroRue" id="numeroRue"/>
					<script type="text/javascript">
								function rue_onChange(row) {
									var form = document.forms["formAddAdresse"];
									form.numeroRue.value = ( row ? row.noRue : "") ;
									if (row) {
										form.localiteSuisse.value =row.nomLocalite;
										form.numeroOrdrePoste.value = row.noLocalite;
									}
								}
					</script>
					<jsp:include page="/WEB-INF/jsp/include/autocomplete.jsp">
							<jsp:param name="inputId" value="rue" />
							<jsp:param name="dataValueField" value="designationCourrier" />
							<jsp:param name="dataTextField" value="{designationCourrier}" />
							<jsp:param name="dataSource" value="selectionnerRue" />
							<jsp:param name="onChange" value="rue_onChange" />
							<jsp:param name="autoSynchrone" value="true"/>
					</jsp:include>
					
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
	</div>
	

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
			<td width="25%"><input type="button" name="cancel" value="<fmt:message key="label.bouton.annuler" />" onclick="self.parent.tb_remove()"/></td>
			<td width="25%"></td>
		</tr>
		</table>
	</div>
	
	</tiles:put>
</tiles:insert>

<input name="numCTB" type="hidden"  value="${command.numCTB}" />

</form:form>
<!-- Fin Adresse -->
<script type="text/javascript" language="Javascript.1.3">
	Element.addObserver( window, "load", function() {
		AddressEdit_Adjust();
		Element.fireObserver( F$("formAddAdresse").texteCasePostale, "change");
	});
	function AddressEdit_Adjust() {
		var form = F$("formAddAdresse");
		Element.removeClassName( form.rue, "readonly");
		if ( form.typeLocalite1.checked) {
			rue_autoComplete.start();
		} else {
			rue_autoComplete.stop();
		}
		var readonly =  form.numeroOrdrePoste.value === "" && form.typeLocalite1.checked;
		form.rue.readOnly = readonly;
		if (readonly) {
			Element.addClassName( form.rue, "readonly");
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