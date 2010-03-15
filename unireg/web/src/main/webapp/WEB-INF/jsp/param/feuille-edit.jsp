<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="index" value="${param.index}" />
<tiles:insert template="/WEB-INF/jsp/templates/templateIFrame.jsp">
	<tiles:put name="head">
	</tiles:put>

	<tiles:put name="title"></tiles:put>
	<tiles:put name="body">
	<form:form name="form" id="formFeuilleEdit">
		<fieldset>
			<legend><fmt:message key="label.param.feuille-edit" /></legend>
			<jsp:include page="feuille.jsp"/>
		</fieldset>
		<div>
			<input type="submit" value="<fmt:message key="label.bouton.mettre.a.jour" />">
			<input type="button" value="<fmt:message key="label.bouton.annuler" />" onclick="self.parent.tb_remove()">
		</div>
	</form:form>	
	</tiles:put>
</tiles:insert>
