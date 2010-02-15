<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<%@page import="ch.vd.uniregctb.common.LengthConstants"%>
<fieldset class="information">
	<legend><span><fmt:message key="label.organisation" /></span></legend>
	<c:set var="ligneTableau" value="${1}" scope="request" />
	<table>
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
		<c:set var="lengthnom" value="<%=LengthConstants.TIERS_NOM%>" scope="request" />
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td><fmt:message key="label.nom" />&nbsp;:</td>
			<td>
				<form:input path="tiers.nom" id="tiers_nom1" cssErrorClass="input-with-errors" size ="65" maxlength="${lengthnom}" />
				<FONT COLOR="#FF0000">*</FONT>
				<form:errors path="tiers.nom" cssClass="error"/>
			</td>
		</tr>
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td><fmt:message key="label.forme.juridique" />&nbsp;:</td>
			<td>
				<form:select path="tiers.formeJuridique" items="${formesJuridiques}" />
			</td>
		</tr>
	</table>
</fieldset>
