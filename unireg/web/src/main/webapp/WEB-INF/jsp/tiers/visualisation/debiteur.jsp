<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Debiteur Prestation Imposable -->
<c:if test="${not empty command.debiteurs}">
<fieldset>
	<legend><span><fmt:message key="label.debiteur.is" /></span></legend>
	<jsp:include page="../common/debiteur.jsp">
		<jsp:param name="page" value="visu"/>
	</jsp:include>
</fieldset>
</c:if>
<!-- Fin Debiteur Prestation Imposable -->