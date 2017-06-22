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
			<input class="noprint" name="periodicites_histo" <c:if test="${command.periodicitesHisto}">checked</c:if> id="isPeriodiciteHisto" type="checkbox" onClick="window.location.href = App.toggleBooleanParam(window.location, 'periodicitesHisto', true);"/>
			<label class="noprint" for="isPeriodiciteHisto"><fmt:message key="label.historique" /></label>

			<display:table name="command.periodicites" id="periodicite" pagesize="10" requestURI="visu.do" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator" sort="external">

				<display:column titleKey="label.periodicite.decompte" style="width: 30%;">
					<fmt:message key="option.periodicite.decompte.${periodicite.periodiciteDecompte}" />
					<c:if test="${periodicite.periodiciteDecompte == 'UNIQUE'}">
						&nbsp;(<fmt:message key="option.periode.decompte.${periodicite.periodeDecompte}" />)
					</c:if>
				</display:column>
				<display:column titleKey="label.periodicite.debut.validite" style="width: 20%;">
					<unireg:regdate regdate="${periodicite.dateDebut}" format="dd.MM.yyyy"/>
				</display:column>
				<display:column titleKey="label.periodicite.fin.validite" style="width: 20%;">
					<unireg:regdate regdate="${periodicite.dateFin}" format="dd.MM.yyyy"/>
				</display:column>
				<display:column titleKey="label.periodicite.statut" style="width: 15%;">
					<fmt:message key="option.periodicite.statut.${periodicite.active}" />
				</display:column>
				<display:column class="action" style="width: 15%;">
					<unireg:consulterLog entityNature="Periodicite" entityId="${periodicite.id}"/>
				</display:column>

				<display:setProperty name="paging.banner.all_items_found" value=""/>
				<display:setProperty name="paging.banner.one_item_found" value=""/>

			</display:table>
		</c:if>
</fieldset>

<!-- Fin fiscal pour debiteurs impot a la source-->

