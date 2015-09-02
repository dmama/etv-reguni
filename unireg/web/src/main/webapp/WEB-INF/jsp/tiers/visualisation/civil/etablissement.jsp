<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<fieldset>
	<legend><span><fmt:message key="label.etablissement" /></span></legend>
	<unireg:nextRowClass reset="1"/>
	<table>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.raison.sociale" />&nbsp;:</td>
			<td><c:out value="${command.tiers.raisonSociale}"/></td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.nom.enseigne" />&nbsp;:</td>
			<td><c:out value="${command.tiers.enseigne}"/></td>
		</tr>
	</table>
</fieldset>

