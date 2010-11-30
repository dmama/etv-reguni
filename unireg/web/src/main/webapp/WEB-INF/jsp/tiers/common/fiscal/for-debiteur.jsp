<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="page" value="${param.page}" />
<c:if test="${page == 'visu' }">
	<c:set var="url" value="visu.do" />
</c:if>
<c:if test="${page == 'edit' }">
	<c:set var="url" value="edit.do" />
</c:if>
<c:if test="${not empty command.forsFiscaux}">
<display:table
	name="command.forsFiscaux" id="forFiscal" pagesize="10" 
	requestURI="${url}"
	class="display">
	
	<display:column sortable ="true" titleKey="label.commune">
		<c:if test="${forFiscal.annule}"><strike></c:if>
			<unireg:infra entityId="${forFiscal.numeroForFiscalCommune}" entityType="commune" entityPropertyName="nomMinuscule"></unireg:infra>
		<c:if test="${forFiscal.annule}"></strike></c:if>
	</display:column>

	<display:column sortable ="true" titleKey="label.date.ouv" sortProperty="dateOuverture">
		<c:if test="${forFiscal.annule}"><strike></c:if>
			<fmt:formatDate value="${forFiscal.dateOuverture}" pattern="dd.MM.yyyy"/>
		<c:if test="${forFiscal.annule}"></strike></c:if>
	</display:column>
	<display:column sortable ="true" titleKey="label.date.fer" sortProperty="dateFermeture">
		<c:if test="${forFiscal.annule}"><strike></c:if>
			<fmt:formatDate value="${forFiscal.dateFermeture}" pattern="dd.MM.yyyy"/>
		<c:if test="${forFiscal.annule}"></strike></c:if>
	</display:column>
	<display:column style="action">
		<c:if test="${page == 'visu' }">
			<unireg:raccourciConsulter link="../common/consult-log.do?height=200&width=800&nature=ForFiscal&id=${forFiscal.id}&TB_iframe=true&modal=true" thickbox="true" tooltip="Edition des logs"/>
		</c:if>
		<c:if test="${page == 'edit' }">
			<c:if test="${!forFiscal.annule}">
				<c:if test="${forFiscal.dateFermeture == null}">
					<unireg:raccourciModifier link="for.do?idFor=${forFiscal.id}" tooltip="Edition de for"/>
				</c:if>
				<c:if test="${forFiscal.dernierForPrincipalOuDebiteur}">
					<unireg:raccourciAnnuler onClick="javascript:annulerFor(${forFiscal.id});" tooltip="Annulation de for"/>
				</c:if>
			</c:if>
		</c:if>
	</display:column>
	<display:setProperty name="paging.banner.all_items_found" value=""/>
	<display:setProperty name="paging.banner.one_item_found" value=""/>
	
</display:table>
</c:if>