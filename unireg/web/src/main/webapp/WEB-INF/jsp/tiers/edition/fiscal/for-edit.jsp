<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="index" value="${param.index}" />

<c:if test="${index == 'modeimposition'}">
	<%-- Modification du mode d'imposition du for principal courant --%>
	<jsp:include page="for-modif-mode-imposition.jsp"/>
</c:if>
