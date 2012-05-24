<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Fiscal pour debiteurs impot a la source -->
<fieldset><legend><span><fmt:message key="label.fiscal" /></span></legend>
	<table border="0">
		<tr class="<unireg:nextRowClass/>" >
			<td width="50%"><fmt:message key="label.mode.communication"/>&nbsp;:</td>
			<td width="50%">
				<fmt:message key="option.mode.communication.${command.tiers.modeCommunication}" />
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="50%"><fmt:message key="label.sans.sommation"/>&nbsp;:</td>
			<td width="50%">
				<input type="checkbox" name="sansRappel" value="True"
					<c:if test="${command.tiers.sansRappel}">checked </c:if> disabled="disabled" />
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
		<td width="25%"><fmt:message key="label.sans.lr"/>&nbsp;:</td>
			<td width="25%">
				<input type="checkbox" name="sansListeRecapitulative" value="True"
					<c:if test="${command.tiers.sansListeRecapitulative}">checked </c:if> disabled="disabled" />
			</td>		
		</tr>
	</table>
</fieldset>
<c:if test="${command.tiers.modeCommunication == 'ELECTRONIQUE'}">
	<fieldset>
			<legend><span><fmt:message key="label.complement.logicielPaye" /></span></legend>
				<c:if test="${command.logiciel != null}">
					<table>
						<tr class="<unireg:nextRowClass/>" >
							<td><fmt:message key="label.complement.logiciel.designation" />&nbsp;:</td>
							<td><c:out value="${command.logiciel.libelleComplet}"/></td>
						</tr>
					</table>
				</c:if>
	</fieldset>
</c:if>
<fieldset>
		<legend><span><fmt:message key="label.periodicites" /></span></legend>
		<c:if test="${not empty command.periodicites}">
			<input class="noprint" name="periodicites_histo"  id="isPeriodiciteHisto" type="checkbox" onClick="Histo.toggleRowsIsHistoPeriodicite('periodicite','isPeriodiciteHisto', 2,3);"/>
			<label class="noprint" for="isPeriodiciteHisto"><fmt:message key="label.historique" /></label>

			<jsp:include page="../../common/fiscal/periodicites.jsp">
				<jsp:param name="page" value="visu"/>
			</jsp:include>
		</c:if>
</fieldset>

<!-- Fin fiscal pour debiteurs impot a la source-->

