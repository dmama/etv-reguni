<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:choose>
	<c:when test="${command.natureTiers == 'NonHabitant'}"><jsp:include page="non-habitant.jsp"/></c:when>
	<c:when test="${command.natureTiers == 'AutreCommunaute'}"><jsp:include page="organisation.jsp"/></c:when>
</c:choose>

