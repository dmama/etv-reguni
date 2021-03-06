<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
<fieldset>
	<legend><span><fmt:message key="label.specificites.${param.group}"/></span></legend>

	<unireg:setAuth var="auth" tiersId="${command.tiers.numero}"/>
	<c:if test="${!command.tiers.annule && auth.flagsPM}">
		<table border="0">
			<tr><td>
				<c:if test="${empty param['message'] && empty param['retour']}">
					<unireg:raccourciModifier link="../flag-entreprise/edit-list.do?pmId=${command.tiers.numero}&group=${param.group}" tooltip="Modifier les spécificités" display="label.bouton.modifier"/>
				</c:if>
			</td></tr>
		</table>
	</c:if>

	<input class="noprint" name="flag_histo" type="checkbox" <c:if test="${command.isFlagsEntrepriseHisto(param.group)}">checked</c:if> onClick="window.location.href = App.toggleBooleanParam(window.location, 'flagsEntrepriseHisto-${param.group}', true);" id="flag_histo-${param.group}" />
	<label class="noprint" for="flag_histo-${param.group}"><fmt:message key="label.historique" /></label>

	<c:set var="flags" value="${command.getFlags(param.group)}"/>
	<c:if test="${not empty flags}">

		<display:table name="${flags}" id="flag" htmlId="flag-${param.group}" requestURI="visu.do" class="display" decorator="ch.vd.unireg.decorator.TableEntityDecorator">
			<display:column titleKey="label.date.debut" style="width: 20%;" sortable="true" sortProperty="dateDebut">
				<unireg:regdate regdate="${flag.dateDebut}"/>
			</display:column>
			<display:column titleKey="label.date.fin" style="width: 20%;" sortable="true" sortProperty="dateFin">
				<unireg:regdate regdate="${flag.dateFin}"/>
			</display:column>
			<display:column titleKey="label.type">
				<fmt:message key="option.flag.entreprise.${flag.type}"/>
			</display:column>
			<display:column class="action" style="width: 10%;">
				<unireg:consulterLog entityNature="FlagEntreprise" entityId="${flag.id}"/>
			</display:column>
		</display:table>

	</c:if>

</fieldset>
