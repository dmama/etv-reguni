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
	</tr>
	<tr>
		<th>
			<fmt:message key="label.param.modele"/>
		</th>
		<td>
			<fmt:message key="option.type.document.${command.modeleDocumentTypeDocument}" />
		<td>
	</tr>
	<tr>
		<th>
			<fmt:message key="title.param.num.form"/>
		</th>
		<td>
			<form:input path="numeroFormulaire" maxlength="10"/>
		<td>
	</tr>
	<tr>
		<th>
			<fmt:message key="title.param.int.feuille"/>
		</th>
		<td>
			<form:input path="intituleFeuille" maxlength="50" size="80"/>
		<td>
	</tr>	
</table>
