<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.creation.droit.acces" /></tiles:put>
  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onClick="ouvrirAide('<c:url value='/docs/acces-par-utilisateur.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
	<tiles:put name="body">
		<%--@elvariable id="command" type="ch.vd.unireg.acces.parUtilisateur.view.RecapPersonneUtilisateurView"--%>
	<form:form method="post" id="formEditRestriction"  name="theForm" action="sauver-restriction.do">
		<form:hidden path="noIndividuOperateur"/>
		<form:hidden path="noDossier"/>
		<unireg:nextRowClass reset="1"/>
		<!-- Debut Caracteristiques generales -->
		<jsp:include page="../../general/utilisateur.jsp">
			<jsp:param name="titleKey" value="title.droits.operateur" />
			<jsp:param name="path" value="utilisateur" />
		</jsp:include>
		<unireg:bandeauTiers numero="${command.dossier.numero}" titre="label.caracteristiques.dossier" cssClass="informations"
		                     showValidation="false" showEvenementsCivils="false" showLinks="false" showAvatar="false" showComplements="true"/>
		<jsp:include page="restriction.jsp" />
		<input type="button" name="retourRecherche" value="<fmt:message key="label.bouton.retour" />" onClick="document.location.href='ajouter-restriction.do?noIndividuOperateur=${command.utilisateur.numeroIndividu}';" />
		<input type="submit" name="save" value="<fmt:message key="label.bouton.sauver" />" />
	</form:form>
	</tiles:put>
</tiles:insert>