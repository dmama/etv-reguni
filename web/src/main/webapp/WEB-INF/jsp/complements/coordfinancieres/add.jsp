<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<%@page import="ch.vd.unireg.common.LengthConstants"%>

<c:set var="lengthnumcompte" value="<%=LengthConstants.TIERS_NUMCOMPTE%>" scope="request" />
<c:set var="lengthpersonne" value="<%=LengthConstants.TIERS_PERSONNE%>" scope="request" />
<c:set var="lengthbic" value="<%=LengthConstants.TIERS_ADRESSEBICSWIFT%>" scope="request" />
<%--@elvariable id="addCoords" type="ch.vd.unireg.complements.CoordonneesFinancieresEditView"--%>
<%--@elvariable id="mode" type="java.lang.String"--%>
<%--@elvariable id="tiersId" type="java.lang.Long"--%>
<unireg:setAuth var="autorisations" tiersId="${tiersId}"/>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.ajout.complements.coordfinancieres" />
	</tiles:put>

	<tiles:put name="fichierAide">
		<li>
			<a href="#" onClick="ouvrirAide('<c:url value='/docs/maj-civil-complement.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
		</li>
	</tiles:put>

	<tiles:put name="body">

		<form:form method="post" name="theForm" commandName="addCoords">
			<form:hidden path="id"/>

			<fieldset>
				<legend><span><fmt:message key="label.complement.coordFinancieres" /></span></legend>
				<unireg:nextRowClass reset="1"/>
				<table>
					<tr class="<unireg:nextRowClass/>" >
						<td width="30%"><fmt:message key="label.date.debut"/>&nbsp;:</td>
 						<td width="70%">
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateDebut" />
								<jsp:param name="id" value="dateDebut" />
							</jsp:include>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.date.fin"/>&nbsp;:</td>
						<td>
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateFin" />
								<jsp:param name="id" value="dateFin" />
							</jsp:include>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.complement.numeroCompteBancaire" />&nbsp;:</td>
						<td>
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
				<c:set var="libelleBoutonRetour">
					<fmt:message key="label.bouton.retour"/>
				</c:set>
				<unireg:buttonTo method="get" action="/complements/coordfinancieres/list.do" params="{tiersId:${tiersId}}" name="${libelleBoutonRetour}"/>
				<input type="submit" name="save" value="<fmt:message key="label.bouton.sauver"/>" tabindex="5"/>
			</div>
			<!-- Fin Boutons -->

		</form:form>

	</tiles:put>
</tiles:insert>
