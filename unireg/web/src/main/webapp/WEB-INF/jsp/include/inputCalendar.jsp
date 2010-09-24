<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="path" value="${param.path}" />
<c:set var="id" value="${param.id}" />
<c:set var="inputFieldClass" value="${param.inputFieldClass}" />
<c:set var="form" value="0"/>
<c:if test="${not empty param.form}">
	<c:set var="form" value="'${param.form}'"/>
</c:if>
<spring:bind path="${path}">
	<input  type="text" name="${status.expression}" value="${status.value}" id="${id}" size="10" maxlength ="10" class="date ${inputFieldClass}"
		<c:if test="${param.onChange != null && not empty param.onChange }">onchange="<c:out value="${param.onChange}" />(this);"</c:if>
		<c:if test="${param.onkeyup != null && not empty param.onkeyup }">onkeyup="<c:out value="${param.onkeyup}" />(this);"</c:if> 
	/>
	<a	href="#" name="<c:out value="${status.expression}"/>_Anchor" id="<c:out value="${id}"/>_Anchor" tabindex="9999" class="calendar"
			onclick="calendar(document.forms[<c:out value="${form}"/>]['<c:out value="${status.expression}"/>'], '<c:out value="${id}"/>_Anchor');" >&nbsp;</a>
	<c:if test="${id == 'dateNaissance'}">
		<span class="formInfo"><a href="<c:url value="/htm/dateNaissance.htm?width=375"/>" class="jTip" id="dateNaissance2">?</a></span>
	</c:if>
	<c:if test="${id == 'dateDeces'}">
		<span class="formInfo"><a href="<c:url value="/htm/dateDeces.htm?width=375"/>" class="jTip" id="dateDeces2">?</a></span>
	</c:if>
</spring:bind>
<form:errors path="${path}" cssClass="error"/>
<c:if test="${param.onChange != null && not empty param.onChange }">
<script type="text/javascript" language="Javascript1.3">
	function Calendar<c:out value="${id}"/>_OnChange(element) {		
			element = E$(element);
			<c:out value="${param.onChange}" />(element);
	}
</script>
</c:if>