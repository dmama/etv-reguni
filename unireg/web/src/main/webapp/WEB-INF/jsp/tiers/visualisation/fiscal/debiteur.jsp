<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Fiscal pour debiteurs impot a la source -->
<fieldset><legend><span><fmt:message key="label.fiscal" /></span></legend>
	<table border="0">
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.mode.communication"/>&nbsp;:</td>
			<td width="25%">
				<fmt:message key="option.mode.communication.${command.tiers.modeCommunication}" />
			</td>
			<td width="25%"><fmt:message key="label.periodicite.decompte"/>&nbsp;:</td>
			<td width="25%">
				<fmt:message key="option.periodicite.decompte.${command.tiers.periodiciteDecompte}" />
				<c:if test="${command.tiers.periodiciteDecompte == 'UNIQUE'}">
					&nbsp;(<fmt:message key="option.periode.decompte.${command.tiers.periodeDecompte}" />)
				</c:if>
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.sans.sommation"/>&nbsp;:</td>
			<td width="25%">
				<input type="checkbox" name="sansRappel" value="True"   
					<c:if test="${command.tiers.sansRappel}">checked </c:if> disabled="disabled" />
			</td>
			<td width="25%"><fmt:message key="label.sans.lr"/>&nbsp;:</td>
			<td width="25%">
				<input type="checkbox" name="sansListeRecapitulative" value="True"   
					<c:if test="${command.tiers.sansListeRecapitulative}">checked </c:if> disabled="disabled" />
			</td>
		</tr>
	</table>
</fieldset>

<!-- Fin fiscal pour debiteurs impot a la source-->

