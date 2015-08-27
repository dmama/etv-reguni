<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="printparam" value='<%= request.getParameter("printview") %>' />
<c:set var="printview" value="${(empty printparam)? false :printparam }" />
<fieldset>
	<legend><span><fmt:message key="label.etablissement.domiciles" /></span></legend>
	<c:if test="${not empty command.domicilesEtablissement}">
		<jsp:include page="../../common/fiscal/domicile-etablissement.jsp"/>
	</c:if>
</fieldset>
