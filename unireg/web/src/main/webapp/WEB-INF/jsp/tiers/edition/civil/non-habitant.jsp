<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<%@page import="ch.vd.uniregctb.common.LengthConstants"%>
<fieldset class="information">
<legend><span><fmt:message key="label.nonHabitant" /></span></legend>
<unireg:nextRowClass reset="1"/>
<table border="0">

	<c:set var="lengthnom" value="<%=LengthConstants.TIERS_NOM%>" scope="request" />
	<tr class="<unireg:nextRowClass/>">
		<td width="50%"><fmt:message key="label.nom" />&nbsp;:</td>
		<td width="50%">
			<form:input path="tiers.nom" tabindex="1" id="tiers_nom" cssErrorClass="input-with-errors" 
			size="20" maxlength="${lengthnom}" />
			<span class="formInfo">
				<a href="#" title="<c:url value="/htm/nom.htm?width=375"/>" class="jTip" id="nom">?</a>
			</span>
			<FONT COLOR="#FF0000">*</FONT>
			<form:errors path="tiers.nom" cssClass="error" />
		</td>
	</tr>

	<tr class="<unireg:nextRowClass/>">
		<td width="50%"><fmt:message key="label.prenom" />&nbsp;:</td>
		<td width="50%">
			<form:input path="tiers.prenom" tabindex="2" id="tiers_prenom" cssErrorClass="input-with-errors" 
			size="20" maxlength="${lengthnom}" />
			<span class="formInfo">
				<a href="#" title="<c:url value="/htm/prenom.htm?width=375"/>" class="jTip" id="prenom">?</a>
			</span>
			<form:errors path="tiers.prenom" cssClass="error" />
		</td>
	</tr>

	<tr class="<unireg:nextRowClass/>">
		<td width="50%"><fmt:message key="label.nouveau.numero.avs" />&nbsp;:</td>
		<td width="50%">
			<form:input path="tiers.numeroAssureSocial" id="tiers_numeroAssureSocial" tabindex="3"  
			cssErrorClass="input-with-errors" size="20" maxlength="16" />
			<span class="formInfo">
				<a href="#" title="<c:url value="/htm/numeroAVS.htm?width=375"/>" class="jTip" id="numeroAVS">?</a>
			</span>
			<form:errors path="tiers.numeroAssureSocial" cssClass="error" />
		</td>
	</tr>

	<tr class="<unireg:nextRowClass/>">
		<td width="50%"><fmt:message key="label.ancien.numero.avs" />&nbsp;:</td>
		<td width="50%">
			<form:input path="identificationPersonne.ancienNumAVS" id="tiers_ancienNumAVS" tabindex="4"  
			cssErrorClass="input-with-errors" size="20" maxlength="14" />
			<span class="formInfo">
				<a href="#" title="<c:url value="/htm/ancienNumeroAVS.htm?width=375"/>" class="jTip" id="ancienNumeroAVS">?</a>
			</span>
			<form:errors path="identificationPersonne.ancienNumAVS" cssClass="error" />
		</td>
	</tr>

	<tr class="<unireg:nextRowClass/>">
		<td width="50%"><fmt:message key="label.sexe" />&nbsp;:</td>
		<td width="50%">
			<form:select path="tiers.sexe" tabindex="5" >
				<form:option value="" ></form:option>
				<form:options items="${sexes}" />
			</form:select>
		</td>
	</tr>
	
	<tr
		class="<unireg:nextRowClass/>">
		<td width="50%"><fmt:message key="label.date.naissance" />&nbsp;:</td>
		<td width="50%">
			<script type="text/javascript">
				function dateNaissance_OnChange( element) {
					if ( element)
						$("#dateNaissanceFormate").val(element.value);
				}
			</script>
			<form:hidden path="sdateNaissance" id="dateNaissanceFormate" />
			<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
				<jsp:param name="path" value="tiers.dateNaissance" />
				<jsp:param name="id" value="dateNaissance"  />
				<jsp:param name="onChange" value="dateNaissance_OnChange"/>
			</jsp:include>
		</td>
	</tr>
	
	<tr
		class="<unireg:nextRowClass/>">
		<td width="50%"><fmt:message key="label.date.deces" />&nbsp;:</td>
		<td width="50%">
			<script type="text/javascript">
				function dateDeces_OnChange( element) {
					if ( element)
						$("#dateDecesFormate").val(element.value);
				}
			</script>
			<form:hidden path="sdateDeces" id="dateDecesFormate" />
			<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
				<jsp:param name="path" value="tiers.dateDeces" />
				<jsp:param name="id" value="dateDeces"  />
				<jsp:param name="onChange" value="dateDeces_OnChange"/>
			</jsp:include>
		</td>
	</tr>

	<tr
		class="<unireg:nextRowClass/>">
		<td width="50%"><fmt:message key="label.numero.registre.etranger" />&nbsp;:</td>
		<td width="50%">
			<form:input path="identificationPersonne.numRegistreEtranger" tabindex="11" id="tiers_numRegistreEtranger" 
			cssErrorClass="input-with-errors" size="20" maxlength="13" />
			<form:errors path="identificationPersonne.numRegistreEtranger" cssClass="error" />
			<span class="formInfo"><a href="#" title="<c:url value="/htm/numRegistreEtranger.htm?width=375"/>" class="jTip" id="numRegistre">?</a></span>
		</td>
	</tr>
	
		<tr
		class="<unireg:nextRowClass/>">
		<td width="50%"><fmt:message key="label.categorie.etranger" />&nbsp;:</td>
		<td width="50%">
			<form:select path="tiers.categorieEtranger" tabindex="12" >
				<form:option value=""></form:option>
				<form:options items="${categoriesEtrangers}" />
			</form:select>
		</td>
	</tr>
	
	<tr
		class="<unireg:nextRowClass/>">
		<td width="50%"><fmt:message key="label.date.debut.validite.autorisation" />&nbsp;:</td>
		<td width="50%">
			<script type="text/javascript">
				function dateDebutValiditeAutorisation_OnChange( element) {
					if ( element)
						$("#dateDebutValiditeAutorisationFormate").val(element.value);
				}
			</script>
			<form:hidden path="sdateDebutValiditeAutorisation" id="dateDebutValiditeAutorisationFormate" />
			<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
				<jsp:param name="path" value="tiers.dateDebutValiditeAutorisation" />
				<jsp:param name="id" value="dateDebutValiditeAutorisation"  />
				<jsp:param name="onChange" value="dateDebutValiditeAutorisation_OnChange"/>
			</jsp:include>
		</td>
	</tr>
	
	<tr
		class="<unireg:nextRowClass/>">
		<td width="50%"><fmt:message key="label.nationalite" />&nbsp;:</td>
		<td width="50%">
			<form:hidden path="tiers.numeroOfsNationalite" id="tiers_numeroOfsNationalite" />
			<form:input path="libelleOfsPaysOrigine" id="tiers_libelleOfsPaysOrigine" cssErrorClass="input-with-errors" tabindex="13" size="20" />
			<script>
				$(function() {
					autocomplete_infra('pays', '#tiers_libelleOfsPaysOrigine', function(item) {
						$('#tiers_numeroOfsNationalite').val(item ? item.id1 : null);
					});
				});
			</script>
			<form:errors path="tiers.numeroOfsNationalite" cssClass="error" /></td>
	</tr>
	
	<tr
		class="<unireg:nextRowClass/>">
		<td width="50%"><fmt:message key="label.commune.origine" />&nbsp;:</td>
		<td width="50%">
			<form:hidden path="tiers.numeroOfsCommuneOrigine" id="tiers_numeroOfsCommuneOrigine" />
			<form:input path="libelleOfsCommuneOrigine" id="tiers_libelleOfsCommuneOrigine" cssErrorClass="input-with-errors" tabindex="14" size="20" />
			<script>
				$(function() {
					autocomplete_infra('commune', '#tiers_libelleOfsCommuneOrigine', function(item) {
						if (item) {
							$('#tiers_numeroOfsCommuneOrigine').val(item.id1);
						}
						else {
							$('#tiers_libelleOfsCommuneOrigine').val(null);
							$('#tiers_numeroOfsCommuneOrigine').val(null);
						}
					});
				});
			</script>
			<form:errors path="tiers.numeroOfsCommuneOrigine" cssClass="error" /></td>
	</tr>
	
</table>

<script>
	$(function() {
		activate_ajax_tooltips();
	});
</script>

</fieldset>
<form:hidden path="tiers.numero" id="numero" />


