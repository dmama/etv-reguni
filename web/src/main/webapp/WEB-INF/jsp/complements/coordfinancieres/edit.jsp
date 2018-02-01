<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<%@page import="ch.vd.unireg.common.LengthConstants"%>

<c:set var="lengthnumcompte" value="<%=LengthConstants.TIERS_NUMCOMPTE%>" scope="request" />
<c:set var="lengthpersonne" value="<%=LengthConstants.TIERS_PERSONNE%>" scope="request" />
<c:set var="lengthbic" value="<%=LengthConstants.TIERS_ADRESSEBICSWIFT%>" scope="request" />
<unireg:setAuth var="autorisations" tiersId="${command.id}"/>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.edition.complements.coordfinancieres" />
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
				<legend><span><fmt:message key="label.complement.coordFinancieres" /></span></legend>
				<unireg:nextRowClass reset="1"/>
				<table>

					<tr class="<unireg:nextRowClass/>" >
						<td width="30%"><fmt:message key="label.complement.numeroCompteBancaire" />&nbsp;:</td>
						<td width="70%">
							<form:input path="iban" cssErrorClass="input-with-errors" size ="${lengthnumcompte}" tabindex="1" maxlength="${lengthnumcompte}"/>
							<span class="jTip formInfo" title="<c:url value="/htm/iban.htm?width=375"/>" id="tipIban">?</span>
							<form:errors path="iban" cssClass="error"/>
							<form:hidden path="oldIban"/>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.complement.titulaireCompte" />&nbsp;:</td>
						<td>
							<form:input path="titulaireCompteBancaire" cssErrorClass="input-with-errors" size ="30" tabindex="2" maxlength="${lengthpersonne}" />
							<span class="jTip formInfo" title="<c:url value="/htm/titulaireCompte.htm?width=375"/>" id="titulaireCompte">?</span>
							<form:errors path="titulaireCompteBancaire" cssClass="error"/>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.complement.bicSwift" />&nbsp;:</td>
						<td>
							<form:input path="adresseBicSwift" cssErrorClass="input-with-errors" size ="26" tabindex="3" maxlength="${lengthbic}" />
							<span class="jTip formInfo" title="<c:url value="/htm/bic.htm?width=375"/>" id="bic">?</span>
							<form:errors path="adresseBicSwift" cssClass="error"/>
						</td>
					</tr>
				</table>

				<script>
					$(function() {
						Tooltips.activate_ajax_tooltips();
					});
				</script>

			</fieldset>

			<!-- Debut Boutons -->
			<div style="margin-top: 1em;">
				<unireg:RetourButton link="../../tiers/visu.do?id=${command.id}" checkIfModified="true" tabIndex="4"/>
				<input type="submit" name="save" value="<fmt:message key="label.bouton.sauver"/>" tabindex="5"/>
			</div>
			<!-- Fin Boutons -->

		</form:form>

		<script type="text/javascript">
			// Initialisation de l'observeur du flag 'modifier'
			<%--@elvariable id="modifie" type="java.lang.Boolean"--%>
			Modifier.attachObserver("theForm", ${modifie != null && modifie});
			Modifier.messageSaveSubmitConfirmation = 'Voulez-vous vraiment sauver ce tiers ?';	
		</script>
	</tiles:put>
</tiles:insert>
