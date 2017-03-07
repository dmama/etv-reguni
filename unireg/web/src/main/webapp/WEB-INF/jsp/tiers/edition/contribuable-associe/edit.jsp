<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="id" value="${param.id}" />

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">
		<fmt:message key="title.edition.contact.impot.source" />
	</tiles:put>
	<tiles:put name="fichierAide">
		<li>
			<a href="#" onclick="ouvrirAide('<c:url value='/docs/gestion-debiteurIS.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
		</li>
	</tiles:put>
	<tiles:put name="body">

		<%--@elvariable id="recap" type="ch.vd.uniregctb.contribuableAssocie.view.ContribuableAssocieEditView"--%>

		<form:form method="post" id="formRecapContactIS" name="formRecapContactIS" commandName="recap">
			<!-- Caractéristiques générales du débiteur -->
			<c:set var="titre"><fmt:message key="caracteristiques.debiteur.is"/></c:set>
			<unireg:bandeauTiers numero="${recap.numeroDpi}" titre="${titre}" showValidation="false" showEvenementsCivils="false" showLinks="false" showAvatar="false" showComplements="true"/>

			<!-- Caractéristiques générales du contribuable -->
			<c:set var="titre"><fmt:message key="caracteristiques.contribuable"/></c:set>
			<unireg:bandeauTiers numero="${recap.numeroContribuable}" titre="${titre}" cssClass="information" showValidation="false" showEvenementsCivils="false" showLinks="false" showAvatar="false"/>

			<!-- Debut Boutons -->
			<unireg:RetourButton link="list.do?numeroDpi=${recap.numeroDpi}"/>
			<input type="submit" value="<fmt:message key="label.bouton.sauver"/>" onclick="return Page_sauverContactIS(event || window.event);" />
			<!-- Fin Boutons -->
		</form:form>

		<script type="text/javascript" language="Javascript">
			function Page_sauverContactIS(event) {
				if(!confirm('Voulez-vous vraiment confirmer ce contact impôt source ?')) {
					return Event.stop(event);
			 	}
			 	return true;
			}
		</script>
	</tiles:put>
</tiles:insert>