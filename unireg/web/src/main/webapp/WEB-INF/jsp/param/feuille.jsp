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
	<tr>
		<th>
			<fmt:message key="title.param.form"/>
		</th>
		<td>
			<form:select path="modeleFeuille">
					<c:choose>
						<c:when test="${(command.modeleDocumentTypeDocument == 'DECLARATION_IMPOT_COMPLETE_BATCH') ||
						(command.modeleDocumentTypeDocument == 'DECLARATION_IMPOT_COMPLETE_LOCAL')}">
							<form:options items="${modelesFeuillesForCompletes}" />
						</c:when>
						<c:when test="${(command.modeleDocumentTypeDocument == 'DECLARATION_IMPOT_VAUDTAX')}">
                            <form:options items="${modelesFeuillesForVaudTax}" />
						</c:when>
						<c:when test="${(command.modeleDocumentTypeDocument == 'DECLARATION_IMPOT_DEPENSE')}">
                            <form:options items="${modelesFeuillesForDepense}" />
						</c:when>
						<c:when test="${(command.modeleDocumentTypeDocument == 'DECLARATION_IMPOT_HC_IMMEUBLE')}">
                            <form:options items="${modelesFeuillesForHC}" />
						</c:when>
					</c:choose>
			</form:select>
		<td>
	<td width="50%">&nbsp;<td>
	</tr>
</table>
