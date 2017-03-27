<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<c:set var="commandName" value="${param.commandName}"/>

<table border="0">
	<tr class="even">
		<td style="width: 15%;"><fmt:message key="label.periode.fiscale.debut"/>&nbsp;:</td>
		<td style="width: 35%;">
			<form:input size="4" path="anneeDebut"/>
			<span style="color: red;">*</span>
			<form:errors path="anneeDebut" cssClass="error"/>
		</td>
		<td style="width: 15%;"><fmt:message key="label.periode.fiscale.fin"/>&nbsp;:</td>
		<td style="width: 35%;">
			<c:set var="pfFinName" value="${commandName}.anneeFin"/>
			<spring:bind path="${pfFinName}">
				<span style="font-style: italic; color: gray; padding: 0 0 0 1em;"><c:out value="${status.value}"/></span>
			</spring:bind>
			<form:hidden path="anneeFin"/>
		</td>
	</tr>
	<tr class="odd">
		<td><fmt:message key="label.pourcentage.exoneration"/>&nbsp;:</td>
		<td>
			<form:input path="pourcentageExoneration" cssClass="nombre"/>
			<span style="color: red;">*</span>
			<form:errors path="pourcentageExoneration" cssClass="error"/>
		</td>
		<td colspan="2">&nbsp;</td>
	</tr>
</table>
