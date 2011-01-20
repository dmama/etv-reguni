<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<fieldset>
	<legend><span><fmt:message key="label.organisation" /></span></legend>
	<unireg:nextRowClass reset="1"/>
	<table>
		<tr class="<unireg:nextRowClass/>" >
			<td width="50%"><fmt:message key="label.nom" />&nbsp;:</td>
			<td width="50%">${command.tiers.nom}</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="50%"><fmt:message key="label.forme.juridique" />&nbsp;:</td>
			<td width="50%"><c:if test="${command.tiers.formeJuridique != null}"><fmt:message key="option.forme.juridique.${command.tiers.formeJuridique}" /></c:if></td>
		</tr>
	</table>
</fieldset>

