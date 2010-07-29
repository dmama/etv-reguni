<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="path" value="${param.path}" />
<c:set var="bind" value="command.${path}" scope="request"/>
<spring:bind path="${bind}" >
	<c:set var="tiersGeneral" value="${status.value}"  scope="request"/>
</spring:bind>
<spring:bind path="${bind}" >
	${tiersGeneral.role.ligne1}
	<c:if test="${tiersGeneral.role.ligne2 != null}"><br>${tiersGeneral.role.ligne2}</c:if>
</spring:bind>		