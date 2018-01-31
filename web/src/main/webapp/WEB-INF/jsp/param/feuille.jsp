<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<table>
	<tr>
		<th>
			<fmt:message key="title.param.periode.fiscale"/>
		</th>
		<td>
			<c:out value="${command.periodeAnnee}"/>
		<td>
		<td width="50%">&nbsp;<td>
	</tr>
	<tr>
		<th>
			<fmt:message key="label.param.modele"/>
		</th>
		<td>
			<fmt:message key="option.type.document.${command.modeleDocumentTypeDocument}" />
		<td>
		<td width="50%">&nbsp;<td>
	</tr>
	<spring:bind path="modeleFeuille">
	<tr>
		<th>
			<fmt:message key="title.param.form"/>
		</th>
		<td>
			<form:select path="modeleFeuille">
				<form:options items="${modelesFeuilles}"/>
			</form:select>
			<c:if test="${status.error}">
				&nbsp;<span class="erreur">${status.errorMessage}</span>
			</c:if>

		<td>
	<td width="50%">&nbsp;<td>
	</tr>
	</spring:bind>
</table>
