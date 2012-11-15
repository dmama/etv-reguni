<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<%@page import="ch.vd.uniregctb.common.LengthConstants"%>
<!-- Debut Complements -->
<c:set var="lengthpersonne" value="<%=LengthConstants.TIERS_PERSONNE%>" scope="request" />
<c:if test="${command.autorisations.complementsCommunications}">
<fieldset>
	<legend><span><fmt:message key="label.complement.pointCommunication" /></span></legend>
	<unireg:nextRowClass reset="1"/>
	<c:set var="lengthnumTel" value="<%=LengthConstants.TIERS_NUMTEL%>" scope="request" />
	<c:set var="lengthnom" value="<%=LengthConstants.TIERS_NOM%>" scope="request" />
	<c:set var="lengthemail" value="<%=LengthConstants.TIERS_EMAIL%>" scope="request" />
	<table border="0">
		<c:if test="${command.natureTiers == 'DebiteurPrestationImposable' && command.addContactISAllowed == true}">
			<tr class="<unireg:nextRowClass/>" >
				<td width="30%"><fmt:message key="label.nom1" />&nbsp;:</td>
				<td width="70%">
				<form:input path="tiers.nom1" id="tiers_nom1" cssErrorClass="input-with-errors" size ="35" tabindex="1" onfocus="true" maxlength="${lengthnom}" />
					<span class="jTip formInfo" title="<c:url value="/htm/nom1.htm?width=375"/>" id="tipNom1">?</span>
				</td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				<td width="30%"><fmt:message key="label.nom2" />&nbsp;:</td>
				<td width="70%">
				<form:input path="tiers.nom2" id="tiers_nom2" cssErrorClass="input-with-errors" size ="35" tabindex="2" onfocus="true" maxlength="${lengthnom}" />
					<span class="jTip formInfo" title="<c:url value="/htm/nom2.htm?width=375"/>" id="tipNom2">?</span>
				</td>
			</tr>
		</c:if>
		<tr class="<unireg:nextRowClass/>" >
			<td width="30%"><fmt:message key="label.complement.contact" />&nbsp;:</td>
			<td width="70%">
			<form:input path="complement.personneContact" id="tiers_personneContact" cssErrorClass="input-with-errors" size ="35" tabindex="3" onfocus="true" maxlength="${lengthpersonne}" />
				<span class="jTip formInfo" title="<c:url value="/htm/personneContact.htm?width=375"/>" id="tipPersonneContact">?</span>
				<form:errors path="tiers.personneContact" cssClass="error"/>
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="30%"><fmt:message key="label.complement" />&nbsp;:</td>
			<td width="70%">
				<form:input path="complement.complementNom" id="tiers_complementNom" cssErrorClass="input-with-errors" size ="35" tabindex="4" maxlength="${lengthnom}" />
				<span class="jTip formInfo" title="<c:url value="/htm/complementNom.htm?width=375"/>" id="tipComplementNom">?</span>
				<form:errors path="tiers.complementNom" cssClass="error"/>
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="30%"><fmt:message key="label.complement.numeroTelFixe" />&nbsp;:</td>
			<td width="70%">
				<form:input path="complement.numeroTelephonePrive" tabindex="5" id="tiers_numeroTelephonePrive" cssErrorClass="input-with-errors" size ="20" maxlength="${lengthnumTel}" />
				<span class="jTip formInfo" title="<c:url value="/htm/numeroTelephone.htm?width=375"/>" id="telPrive">?</span>
				<form:errors path="tiers.numeroTelephonePrive" cssClass="error"/>
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="30%"><fmt:message key="label.complement.numeroTelPortable" />&nbsp;:</td>
			<td width="70%">
				<form:input path="complement.numeroTelephonePortable" tabindex="6" id="tiers_numeroTelephonePortable" cssErrorClass="input-with-errors" size ="20" maxlength="${lengthnumTel}" />
				<span class="jTip formInfo" title="<c:url value="/htm/numeroTelephone.htm?width=375"/>" id="telPortable">?</span>
				<form:errors path="tiers.numeroTelephonePortable" cssClass="error"/>
			</td>
		</tr>			
		<tr class="<unireg:nextRowClass/>" >
			<td width="30%"><fmt:message key="label.complement.numeroTelProfessionnel" />&nbsp;:</td>
			<td width="70%">
				<form:input path="complement.numeroTelephoneProfessionnel" tabindex="7" id="tiers_numeroTelephoneProfessionnel" cssErrorClass="input-with-errors" size ="20" maxlength="${lengthnumTel}" />
				<span class="jTip formInfo" title="<c:url value="/htm/numeroTelephone.htm?width=375"/>" id="telProfessionnel">?</span>
				<form:errors path="tiers.numeroTelephoneProfessionnel" cssClass="error"/>
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="30%"><fmt:message key="label.complement.numeroFax" />&nbsp;:</td>
			<td width="70%">
				<form:input path="complement.numeroTelecopie" id="tiers_numeroTelecopie" tabindex="8" cssErrorClass="input-with-errors" size ="20" maxlength="${lengthnumTel}" />
				<span class="jTip formInfo" title="<c:url value="/htm/numeroTelephone.htm?width=375"/>" id="fax">?</span>
				<form:errors path="tiers.numeroTelecopie" cssClass="error"/>
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="30%"><fmt:message key="label.complement.email" />&nbsp;:</td>
			<td width="70%">
				<form:input path="complement.adresseCourrierElectronique" tabindex="9" id="tiers_adresseCourrierElectronique" cssErrorClass="input-with-errors" size ="35" maxlength="${lengthpersonne}" />
				<span class="jTip formInfo" title="<c:url value="/htm/email.htm?width=375"/>" id="email">?</span>
				<form:errors path="tiers.adresseCourrierElectronique" cssClass="error"/>
			</td>
		</tr>
	</table>
</fieldset>
</c:if>
<c:if test="${command.autorisations.complementsCoordonneesFinancieres}">
<fieldset>
	<legend><span><fmt:message key="label.complement.coordFinancieres" /></span></legend>
	<unireg:nextRowClass reset="1"/>
	<table>
	
		<c:set var="lengthnumcompte" value="<%=LengthConstants.TIERS_NUMCOMPTE%>" scope="request" />
		<tr class="<unireg:nextRowClass/>" >
			<td width="30%"><fmt:message key="label.complement.numeroCompteBancaire" />&nbsp;:</td>
			<td width="70%">
				<form:input path="complement.compteBancaire.iban" tabindex="10"  id="tiers_numeroCompteBancaire" cssErrorClass="input-with-errors" size ="${lengthnumcompte}" maxlength="${lengthnumcompte}"/>
				<span class="jTip formInfo" title="<c:url value="/htm/iban.htm?width=375"/>" id="tipIban">?</span>
				<form:errors path="tiers.numeroCompteBancaire" cssClass="error"/>
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="30%"><fmt:message key="label.complement.titulaireCompte" />&nbsp;:</td>
			<td width="70%">
				<form:input path="complement.compteBancaire.titulaireCompteBancaire" tabindex="11" id="tiers_titulaireCompteBancaire" cssErrorClass="input-with-errors" size ="30" maxlength="${lengthpersonne}" />
				<span class="jTip formInfo" title="<c:url value="/htm/titulaireCompte.htm?width=375"/>" id="titulaireCompte">?</span>
				<form:errors path="tiers.titulaireCompteBancaire" cssClass="error"/>
			</td>
		</tr>
		<c:set var="lengthbic" value="<%=LengthConstants.TIERS_ADRESSEBICSWIFT%>" scope="request" />
		<tr class="<unireg:nextRowClass/>" >
			<td width="30%"><fmt:message key="label.complement.bicSwift" />&nbsp;:</td>
			<td width="70%">
				<form:input path="complement.compteBancaire.adresseBicSwift" tabindex="12"  id="tiers_adresseBicSwift" cssErrorClass="input-with-errors" size ="26" maxlength="${lengthbic}" />
				<span class="jTip formInfo" title="<c:url value="/htm/bic.htm?width=375"/>" id="bic">?</span>
				<form:errors path="tiers.adresseBicSwift" cssClass="error"/>
			</td>
		</tr>	
	</table>

	<script>
		$(function() {
			Tooltips.activate_ajax_tooltips();
		});
	</script>

</fieldset>
</c:if>
<!-- Fin Complements -->