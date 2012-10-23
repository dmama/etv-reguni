<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<%@page import="ch.vd.uniregctb.common.LengthConstants"%>
<span><%-- span vide pour que IE6 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
<fieldset class="information">
<legend><span><fmt:message key="label.nonHabitant" /></span></legend>
<unireg:nextRowClass reset="1"/>
<table border="0">

	<c:set var="lengthnom" value="<%=LengthConstants.TIERS_NOM%>" scope="request" />
	<tr class="<unireg:nextRowClass/>">
		<td width="40%"><fmt:message key="label.prenom" />&nbsp;:</td>
		<td width="60%">
			<form:input path="tiers.prenom" tabindex="1" id="tiers_prenom" cssErrorClass="input-with-errors"
			            size="20" maxlength="${lengthnom}" />
			<span class="jTip formInfo" title="<c:url value="/htm/prenom.htm?width=375"/>" id="prenom">?</span>
			<div id="empty_tiers_prenom_warning" style="display:inline;" class="warn warning_icon"><fmt:message key="warning.prenom.vide"/></div>
			<form:errors path="tiers.prenom" cssClass="error" />
		</td>
	</tr>

	<tr class="<unireg:nextRowClass/>">
		<td><fmt:message key="label.nom" />&nbsp;:</td>
		<td>
			<form:input path="tiers.nom" tabindex="2" id="tiers_nom" cssErrorClass="input-with-errors"
			size="20" maxlength="${lengthnom}" />
			<span class="jTip formInfo" title="<c:url value="/htm/nom.htm?width=375"/>" id="nom">?</span>
			<FONT COLOR="#FF0000">*</FONT>
			<form:errors path="tiers.nom" cssClass="error" />
		</td>
	</tr>

	<tr class="<unireg:nextRowClass/>">
		<td><fmt:message key="label.nouveau.numero.avs" />&nbsp;:</td>
		<td>
			<form:input path="tiers.numeroAssureSocial" id="tiers_numeroAssureSocial" tabindex="3"  
			cssErrorClass="input-with-errors" size="20" maxlength="16" />
			<span class="jTip formInfo" title="<c:url value="/htm/numeroAVS.htm?width=375"/>" id="numeroAVS">?</span>
			<form:errors path="tiers.numeroAssureSocial" cssClass="error" />
		</td>
	</tr>

	<tr class="<unireg:nextRowClass/>">
		<td><fmt:message key="label.ancien.numero.avs" />&nbsp;:</td>
		<td>
			<form:input path="identificationPersonne.ancienNumAVS" id="tiers_ancienNumAVS" tabindex="4"  
			cssErrorClass="input-with-errors" size="20" maxlength="14" />
			<span class="jTip formInfo" title="<c:url value="/htm/ancienNumeroAVS.htm?width=375"/>" id="ancienNumeroAVS">?</span>
			<form:errors path="identificationPersonne.ancienNumAVS" cssClass="error" />
		</td>
	</tr>

	<tr class="<unireg:nextRowClass/>">
		<td><fmt:message key="label.sexe" />&nbsp;:</td>
		<td>
			<form:select path="tiers.sexe" tabindex="5" >
				<form:option value="" ></form:option>
				<form:options items="${sexes}" />
			</form:select>
		</td>
	</tr>
	
	<tr
		class="<unireg:nextRowClass/>">
		<td><fmt:message key="label.date.naissance" />&nbsp;:</td>
		<td>
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
				<jsp:param name="tabindex" value="6"/>
			</jsp:include>
		</td>
	</tr>
	
	<tr
		class="<unireg:nextRowClass/>">
		<td><fmt:message key="label.date.deces" />&nbsp;:</td>
		<td>
			<unireg:regdate regdate="${command.tiers.dateDeces}"/>
		</td>
	</tr>
	<tr
		class="<unireg:nextRowClass/>">
		<td><fmt:message key="label.numero.registre.etranger" />&nbsp;:</td>
		<td>
			<form:input path="identificationPersonne.numRegistreEtranger" tabindex="7" id="tiers_numRegistreEtranger"
			cssErrorClass="input-with-errors" size="20" maxlength="13" />
			<form:errors path="identificationPersonne.numRegistreEtranger" cssClass="error" />
			<span class="jTip formInfo" title="<c:url value="/htm/numRegistreEtranger.htm?width=375"/>" id="numRegistre">?</span>
		</td>
	</tr>
	
		<tr
		class="<unireg:nextRowClass/>">
		<td><fmt:message key="label.categorie.etranger" />&nbsp;:</td>
		<td>
			<form:select path="tiers.categorieEtranger" tabindex="8" >
				<form:option value=""></form:option>
				<form:options items="${categoriesEtrangers}" />
			</form:select>
		</td>
	</tr>
	
	<tr
		class="<unireg:nextRowClass/>">
		<td><fmt:message key="label.date.debut.validite.autorisation" />&nbsp;:</td>
		<td>
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
				<jsp:param name="tabindex" value="9"/>
			</jsp:include>
		</td>
	</tr>
	
	<tr
		class="<unireg:nextRowClass/>">
		<td><fmt:message key="label.nationalite" />&nbsp;:</td>
		<td>
			<form:hidden path="tiers.numeroOfsNationalite" id="tiers_numeroOfsNationalite" />
			<form:input path="libelleOfsPaysOrigine" id="tiers_libelleOfsPaysOrigine" cssErrorClass="input-with-errors" tabindex="10" size="20" />
			<script>
				$(function() {
					Autocomplete.infra('etatOuTerritoire', '#tiers_libelleOfsPaysOrigine', true, function(item) {
						$('#tiers_numeroOfsNationalite').val(item ? item.id1 : null);
					});
				});
			</script>
			<form:errors path="tiers.numeroOfsNationalite" cssClass="error" /></td>
	</tr>
	
	<tr
		class="<unireg:nextRowClass/>">
		<td><fmt:message key="label.commune.origine" />&nbsp;:</td>
		<td>
			<form:hidden path="tiers.numeroOfsCommuneOrigine" id="tiers_numeroOfsCommuneOrigine" />
			<form:input path="libelleOfsCommuneOrigine" id="tiers_libelleOfsCommuneOrigine" cssErrorClass="input-with-errors" tabindex="11" size="20" />
			<script>
				$(function() {
					Autocomplete.infra('commune', '#tiers_libelleOfsCommuneOrigine', true, function(item) {
						$('#tiers_numeroOfsCommuneOrigine').val(item ? item.id1 : null);
					});
				});
			</script>
			<form:errors path="tiers.numeroOfsCommuneOrigine" cssClass="error" /></td>
	</tr>
	
</table>

<script>
	$(function() {
		Tooltips.activate_ajax_tooltips();

		$('#empty_tiers_prenom_warning').hide();

		var refresh_prenom_warning = function() {
			var prenom = $('#tiers_prenom').val();
			var nom = $('#tiers_nom').val();
			if (StringUtils.isBlank(prenom) && /\s*\S+\s+\S+\s*/.test(nom)) {
				// on affiche le warning si le prénom est vide que le nom contient deux mots distincts
				$('#empty_tiers_prenom_warning').show();
			}
			else {
				$('#empty_tiers_prenom_warning').hide();
			}
		};

		$('#tiers_nom').change(refresh_prenom_warning);
		$('#tiers_prenom').change(refresh_prenom_warning);

		refresh_prenom_warning();
	});

</script>

</fieldset>
<form:hidden path="tiers.numero" id="numero" />


