<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<fieldset>
	<legend><span><fmt:message key="label.nonHabitant" /></span></legend>
	<jsp:include page="non-habitant-core.jsp">
		<jsp:param name="path" value="tiers" />
		<jsp:param name="pathIndividu" value="individu" />
	</jsp:include>
</fieldset>

