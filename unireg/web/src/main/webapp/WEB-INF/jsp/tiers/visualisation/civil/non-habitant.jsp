<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="path" value="tiers" />
<c:if test="${not empty param.path}">
	<c:set var="path" value="${param.path}" />
</c:if>
<fieldset>
	<legend><span><fmt:message key="label.nonHabitant" /></span></legend>
	<jsp:include page="non-habitant-core.jsp">
		<jsp:param name="path" value="${path}" />
	</jsp:include>
</fieldset>

