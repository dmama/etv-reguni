<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="index" value="${param.index}" />
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title"><fmt:message key="title.edit.feuille.modele.document"/></tiles:put>

	<tiles:put name="body">
	<form:form name="form" id="formFeuilleEdit">
		<fieldset>
			<legend><fmt:message key="label.param.feuille-edit" /></legend>
			<jsp:include page="feuille.jsp"/>
		</fieldset>
		<form:hidden path="idPeriode" value="${command.idPeriode}"/>
		<form:hidden path="idModele" value="${command.idModele}"/>
		<form:hidden path="idFeuille" value="${command.idFeuille}"/>
		<form:hidden path="modeleDocumentTypeDocument" value="${command.modeleDocumentTypeDocument}"/>
		<form:hidden path="periodeAnnee" value="${command.periodeAnnee}"/>
		<div>
			<input type="submit" value="<fmt:message key="label.bouton.mettre.a.jour" />">
			<input type="button" value="<fmt:message key="label.bouton.annuler" />" onclick="document.location.href='../periode.do?pf=${command.idPeriode}&md=${command.idModele}'">
		</div>
	</form:form>	
	</tiles:put>
</tiles:insert>
