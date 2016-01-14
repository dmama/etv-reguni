<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="printparam" value='<%= request.getParameter("printview") %>' />
<c:set var="printview" value="${(empty printparam)? false :printparam }" />
<c:if test="${not empty command.forsFiscaux}">
	<jsp:include page="../../common/fiscal/for.jsp">
		<jsp:param name="page" value="visu"/>
	</jsp:include>
</c:if>