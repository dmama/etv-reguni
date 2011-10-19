<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="index" value="${param.index}" />
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title"><fmt:message key="title.ajout.feuille.modele.document"/></tiles:put>

	<tiles:put name="body">
	<form:form name="form" id="formFeuilleAdd">
		<fieldset>
			<legend><fmt:message key="label.param.feuille-add" /></legend>
				<jsp:include page="feuille.jsp"/>
		</fieldset>
		<form:hidden path="idPeriode" value="${command.idPeriode}"/>
		<form:hidden path="idModele" value="${command.idModele}"/>
		<form:hidden path="modeleDocumentTypeDocument" value="${command.modeleDocumentTypeDocument}"/>
		<form:hidden path="periodeAnnee" value="${command.periodeAnnee}"/>
		<div>
			<input type="submit" id="ajout" value="<fmt:message key="label.bouton.ajouter" />">
			<input type="button" id="annuler" value="<fmt:message key="label.bouton.annuler" />" onclick="document.location.href='../periode.do?pf=${command.idPeriode}&md=${command.idModele}'">
		</div>
		</form:form>		
	</tiles:put>
</tiles:insert>
