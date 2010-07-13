<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="page" value="${param.page}" />
<c:if test="${page == 'visu' }">
	<c:set var="url" value="visu.do" />
</c:if>
<c:if test="${page == 'edit' }">
	<c:set var="url" value="edit.do" />
</c:if>
<c:if test="${not empty command.periodicites}">
<display:table
	name="command.periodicites" id="periodicite" pagesize="10"
	requestURI="${url}"
	class="display">
	
	<display:column sortable ="true" titleKey="label.periodicite.decompte">
		<c:if test="${periodicite.annule}"><strike></c:if>
			<fmt:message key="option.periodicite.decompte.${periodicite.periodiciteDecompte}" />
				<c:if test="${periodicite.periodiciteDecompte == 'UNIQUE'}">
					&nbsp;(<fmt:message key="option.periode.decompte.${command.tiers.periodeDecompte}" />)
				</c:if>
		<c:if test="${periodicite.annule}"></strike></c:if>
	</display:column>

	<display:column sortable ="true" titleKey="label.periodicite.debut.validite" sortProperty="dateDebut">
		<c:if test="${periodicite.annule}"><strike></c:if>
			<fmt:formatDate value="${periodicite.dateDebut}" pattern="dd.MM.yyyy"/>
		<c:if test="${periodicite.annule}"></strike></c:if>
	</display:column>
	<display:column sortable ="true" titleKey="label.periodicite.fin.validite" sortProperty="dateFin">
		<c:if test="${periodicite.annule}"><strike></c:if>
			<fmt:formatDate value="${periodicite.dateFin}" pattern="dd.MM.yyyy"/>
		<c:if test="${periodicite.annule}"></strike></c:if>
	</display:column>
		<display:column style="action">
		<c:if test="${page == 'visu' }">
			<unireg:raccourciConsulter link="../common/consult-log.do?height=200&width=800&nature=periodicite&id=${periodicite.id}&TB_iframe=true&modal=true" thickbox="true" tooltip="Edition des logs"/>
		</c:if>
	</display:column>
	<display:setProperty name="paging.banner.all_items_found" value=""/>
	<display:setProperty name="paging.banner.one_item_found" value=""/>
	
</display:table>
</c:if>