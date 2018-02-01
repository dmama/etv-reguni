<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<%@page import="ch.vd.unireg.common.LengthConstants"%>

<c:set var="lengthpersonne" value="<%=LengthConstants.TIERS_PERSONNE%>" scope="request" />
<c:set var="lengthnumTel" value="<%=LengthConstants.TIERS_NUMTEL%>" scope="request" />
<c:set var="lengthnom" value="<%=LengthConstants.TIERS_NOM%>" scope="request" />
<unireg:setAuth var="autorisations" tiersId="${command.id}"/>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.edition.complements.communications" />
	</tiles:put>

	<tiles:put name="fichierAide">
		<li>
			<a href="#" onClick="ouvrirAide('<c:url value='/docs/maj-civil-complement.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
		</li>
	</tiles:put>

	<tiles:put name="body">
		<unireg:bandeauTiers numero="${command.id}" showAvatar="true" showLinks="false"/>

		<form:form method="post" name="theForm">

			<fieldset>
				<legend><span><fmt:message key="label.complement.pointCommunication" /></span></legend>
				<unireg:nextRowClass reset="1"/>
				<table border="0">
					<c:if test="${command.debiteurWithoutCtb}">
						<tr class="<unireg:nextRowClass/>" >
							<td><fmt:message key="label.nom1" />&nbsp;:</td>
							<td>
								<form:input path="nom1" id="tiers_nom1" cssErrorClass="input-with-errors" size ="35" tabindex="1" maxlength="${lengthnom}" />
								<span class="jTip formInfo" title="<c:url value="/htm/nom1.htm?width=375"/>" id="tipNom1">?</span>
							</td>
						</tr>
						<tr class="<unireg:nextRowClass/>" >
							<td><fmt:message key="label.nom2" />&nbsp;:</td>
							<td>
								<form:input path="nom2" id="tiers_nom2" cssErrorClass="input-with-errors" size ="35" tabindex="2" maxlength="${lengthnom}" />
								<span class="jTip formInfo" title="<c:url value="/htm/nom2.htm?width=375"/>" id="tipNom2">?</span>
							</td>
						</tr>
						<form:hidden path="debiteurWithoutCtb"/>
					</c:if>
					<tr class="<unireg:nextRowClass/>" >
						<td width="30%"><fmt:message key="label.complement.contact" />&nbsp;:</td>
						<td width="70%">
							<form:input path="personneContact" cssErrorClass="input-with-errors" size ="35" tabindex="3" maxlength="${lengthpersonne}" />
							<span class="jTip formInfo" title="<c:url value="/htm/personneContact.htm?width=375"/>" id="tipPersonneContact">?</span>
							<form:errors path="personneContact" cssClass="error"/>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.complement" />&nbsp;:</td>
						<td>
							<form:input path="complementNom" cssErrorClass="input-with-errors" size ="35" tabindex="4" maxlength="${lengthnom}" />
							<span class="jTip formInfo" title="<c:url value="/htm/complementNom.htm?width=375"/>" id="tipComplementNom">?</span>
							<form:errors path="complementNom" cssClass="error"/>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.complement.numeroTelFixe" />&nbsp;:</td>
						<td>
							<form:input path="numeroTelephonePrive" cssErrorClass="input-with-errors" size ="20" tabindex="5" maxlength="${lengthnumTel}" />
							<span class="jTip formInfo" title="<c:url value="/htm/numeroTelephone.htm?width=375"/>" id="telPrive">?</span>
							<form:errors path="numeroTelephonePrive" cssClass="error"/>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.complement.numeroTelPortable" />&nbsp;:</td>
						<td>
							<form:input path="numeroTelephonePortable" cssErrorClass="input-with-errors" size ="20" tabindex="6" maxlength="${lengthnumTel}" />
							<span class="jTip formInfo" title="<c:url value="/htm/numeroTelephone.htm?width=375"/>" id="telPortable">?</span>
							<form:errors path="numeroTelephonePortable" cssClass="error"/>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.complement.numeroTelProfessionnel" />&nbsp;:</td>
						<td>
							<form:input path="numeroTelephoneProfessionnel" cssErrorClass="input-with-errors" size ="20" tabindex="7" maxlength="${lengthnumTel}" />
							<span class="jTip formInfo" title="<c:url value="/htm/numeroTelephone.htm?width=375"/>" id="telProfessionnel">?</span>
							<form:errors path="numeroTelephoneProfessionnel" cssClass="error"/>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.complement.numeroFax" />&nbsp;:</td>
						<td>
							<form:input path="numeroTelecopie" cssErrorClass="input-with-errors" size ="20" tabindex="8" maxlength="${lengthnumTel}" />
							<span class="jTip formInfo" title="<c:url value="/htm/numeroTelephone.htm?width=375"/>" id="fax">?</span>
							<form:errors path="numeroTelecopie" cssClass="error"/>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.complement.email" />&nbsp;:</td>
						<td>
							<form:input path="adresseCourrierElectronique" cssErrorClass="input-with-errors" size ="35" tabindex="9" maxlength="${lengthpersonne}" />
							<span class="jTip formInfo" title="<c:url value="/htm/email.htm?width=375"/>" id="email">?</span>
							<form:errors path="adresseCourrierElectronique" cssClass="error"/>
						</td>
					</tr>
				</table>
			</fieldset>

			<!-- Debut Boutons -->
			<div style="margin-top: 1em;">
				<unireg:RetourButton link="../../tiers/visu.do?id=${command.id}" checkIfModified="true" tabIndex="10"/>
				<input type="submit" name="save" value="<fmt:message key="label.bouton.sauver"/>" tabindex="11"/>
			</div>
			<!-- Fin Boutons -->

		</form:form>

		<script type="text/javascript">
			$(function() {
				Tooltips.activate_ajax_tooltips();

				// Initialisation de l'observeur du flag 'modifier'
				<%--@elvariable id="modifie" type="java.lang.Boolean"--%>
				Modifier.attachObserver("theForm", ${modifie != null && modifie});
				Modifier.messageSaveSubmitConfirmation = 'Voulez-vous vraiment sauver ce tiers ?';
			});
		</script>
	</tiles:put>
</tiles:insert>
