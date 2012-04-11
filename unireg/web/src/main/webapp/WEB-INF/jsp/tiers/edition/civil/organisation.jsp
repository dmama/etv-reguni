<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<%@page import="ch.vd.uniregctb.common.LengthConstants"%>
<span><%-- span vide pour que IE6 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
<fieldset class="information">
	<legend><span><fmt:message key="label.organisation" /></span></legend>
	<unireg:nextRowClass reset="1"/>
	<table>
		<c:set var="lengthnom" value="<%=LengthConstants.TIERS_NOM%>" scope="request" />
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.nom" />&nbsp;:</td>
			<td>
				<form:input path="tiers.nom" id="tiers_nom1" cssErrorClass="input-with-errors" size ="65" maxlength="${lengthnom}" />
				<FONT COLOR="#FF0000">*</FONT>
				<form:errors path="tiers.nom" cssClass="error"/>
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.forme.juridique" />&nbsp;:</td>
			<td>
				<form:select path="tiers.formeJuridique" items="${formesJuridiques}" />
			</td>
		</tr>
	</table>
</fieldset>
