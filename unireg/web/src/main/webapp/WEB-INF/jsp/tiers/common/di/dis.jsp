<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="page" value="${param.page}" />
<c:if test="${page == 'visu' }">
	<c:set var="url" value="visu.do" />
</c:if>
<c:if test="${page == 'edit' }">
	<c:set var="url" value="edit.do" />
</c:if>
<c:if test="${not empty command.dis}">
	<display:table 	name="command.dis" id="di" 
					pagesize="10" 
					requestURI="${url}"
					class="display">
		<c:if test="${page == 'edit' }">		
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncune.di.trouvee" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.di.trouvee" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.dis.trouvees" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.dis.trouvees" /></span></display:setProperty>
		</c:if>
					
		<display:column sortable ="true" titleKey="label.periode.fiscale" >
			<c:if test="${di.annule}"><strike></c:if>
				${di.periodeFiscale}
			<c:if test="${di.annule}"></strike></c:if>				
		</display:column>
		<display:column sortable ="true" titleKey="label.periode.imposition" sortProperty="dateDebutPeriodeImposition">
			<c:if test="${di.annule}"><strike></c:if>
				<fmt:formatDate value="${di.dateDebutPeriodeImposition}" pattern="dd.MM.yyyy"/>&nbsp;-&nbsp;<fmt:formatDate value="${di.dateFinPeriodeImposition}" pattern="dd.MM.yyyy"/>
			<c:if test="${di.annule}"></strike></c:if>				
		</display:column>
		<display:column sortable ="true" titleKey="label.date.delai.accorde" sortProperty="delaiAccorde">
			<c:if test="${di.annule}"><strike></c:if>
				<fmt:formatDate value="${di.delaiAccorde}" pattern="dd.MM.yyyy"/>
			<c:if test="${di.annule}"></strike></c:if>
		</display:column>
		<display:column sortable ="true" titleKey="label.date.retour" sortProperty="dateRetour">
			<c:if test="${di.annule}"><strike></c:if>
				<fmt:formatDate value="${di.dateRetour}" pattern="dd.MM.yyyy"/>
			<c:if test="${di.annule}"></strike></c:if>
		</display:column>
		<display:column sortable ="true" titleKey="label.etat.avancement" >
			<c:if test="${di.annule}"><strike></c:if>
				<fmt:message key="option.etat.avancement.${di.etat}" />
			<c:if test="${di.annule}"></strike></c:if>
		</display:column>
		<display:column style="action">
			<c:if test="${page == 'visu' }">
				<c:if test="${!di.annule}">
					<a href="di.do?idDi=<c:out value="${di.id}" />&height=650&width=650&TB_iframe=true&modal=true" class="thickbox detail" title="DI">&nbsp;</a>
				</c:if>
				<unireg:raccourciConsulter link="../common/consult-log.do?height=200&width=800&nature=DI&id=${di.id}&TB_iframe=true&modal=true" thickbox="true" tooltip="Edition des logs"/>
			</c:if>
			<c:if test="${page == 'edit' }">
				<c:if test="${!di.annule}">
					<unireg:raccourciModifier link="edit.do?action=editdi&id=${di.id}" tooltip="di"/>
				</c:if>
			</c:if>
		</display:column>
		<display:setProperty name="paging.banner.all_items_found" value=""/>
		<display:setProperty name="paging.banner.one_item_found" value=""/>
	</display:table>
</c:if>