<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="path" value="${param.path}" />
<c:set var="id" value="${param.id}" />
<c:set var="inputFieldClass" value="${param.inputFieldClass}" />

<%-- Le datepicker lui-même --%>
<spring:bind path="${path}">
	<input type="text" name="${status.expression}" value="${status.value}" id="${id}" size="10" maxlength ="10" class="date ${inputFieldClass}"
		<c:if test="${param.onChange != null && not empty param.onChange }">onchange="<c:out value="${param.onChange}"/>(this);"</c:if>
		<c:if test="${param.onkeyup  != null && not empty param.onkeyup  }">onkeyup= "<c:out value="${param.onkeyup}" />(this);"</c:if> />
	<script>
		$(function() {
			$('#${id}').datepicker({
				showOn: "button",
				yearRange: '1900:+10',
				buttonImage: "<c:url value='/css/x/calendar_off.gif'/>",
				buttonImageOnly: true,
				changeMonth: true,
				changeYear: true
			});
		});
	</script>

</spring:bind>

<%-- Tooltips sur le format de la date --%>
<c:if test="${id == 'dateNaissance'}">
	<span class="formInfo"><a href="<c:url value="/htm/dateNaissance.htm?width=375"/>" class="jTip" id="dateNaissance2">?</a></span>
</c:if>
<c:if test="${id == 'dateDeces'}">
	<span class="formInfo"><a href="<c:url value="/htm/dateDeces.htm?width=375"/>" class="jTip" id="dateDeces2">?</a></span>
</c:if>

<form:errors path="${path}" cssClass="error"/>
