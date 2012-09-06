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
	<display:table name="command.dis" id="di" pagesize="10" requestURI="${url}" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
		<c:if test="${page == 'edit' }">		
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncune.di.trouvee" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.di.trouvee" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.dis.trouvees" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.dis.trouvees" /></span></display:setProperty>
		</c:if>
					
		<display:column sortable="true" titleKey="label.periode.fiscale">
			${di.periodeFiscale}
		</display:column>
		<display:column sortable ="true" titleKey="label.periode.imposition" sortProperty="dateDebutPeriodeImposition">
			<fmt:formatDate value="${di.dateDebutPeriodeImposition}" pattern="dd.MM.yyyy"/>&nbsp;-&nbsp;<fmt:formatDate value="${di.dateFinPeriodeImposition}" pattern="dd.MM.yyyy"/>
		</display:column>
		<display:column sortable ="true" titleKey="label.date.delai.accorde" sortProperty="delaiAccorde">
			<fmt:formatDate value="${di.delaiAccorde}" pattern="dd.MM.yyyy"/>
		</display:column>
		<display:column sortable ="true" titleKey="label.date.retour" sortProperty="dateRetour">
			<fmt:formatDate value="${di.dateRetour}" pattern="dd.MM.yyyy"/>
		</display:column>
		<display:column sortable ="true" titleKey="label.etat.avancement" >
			<fmt:message key="option.etat.avancement.${di.etat}" />
			<c:if test="${di.dateRetour != null}">
				<c:if test="${di.sourceRetour == null}">
					(<fmt:message key="option.source.quittancement.UNKNOWN" />)
				</c:if>
				<c:if test="${di.sourceRetour != null}">
					(<fmt:message key="option.source.quittancement.${di.sourceRetour}" />)
				</c:if>
			</c:if>
		</display:column>
		<display:column style="action">
			<c:if test="${page == 'visu' }">
				<c:if test="${!di.annule}">
					<a href="#" class="detail" title="Détails de la déclaration" onclick="Decl.open_details_di(<c:out value="${di.id}"/>); return false;">&nbsp;</a>
				</c:if>
				<unireg:consulterLog entityNature="DI" entityId="${di.id}"/>
			</c:if>
			<c:if test="${page == 'edit' }">
				<c:if test="${!di.annule}">
					<unireg:linkTo name="&nbsp;" action="/decl/editer.do" method="get" params="{id:${di.id}}" title="Editer la déclaration" link_class="edit"/>
				</c:if>
				<authz:authorize ifAnyGranted="ROLE_DI_DESANNUL_PP">
					<c:if test="${di.annule}">
						<unireg:linkTo name="" title="Désannuler la déclaration" action="/decl/desannuler.do" method="post" params="{id:${di.id}}"
						               confirm="Voulez-vous vraiment désannuler cette déclaration d'impôt ?" link_class="undelete" />
					</c:if>
				</authz:authorize>
			</c:if>
		</display:column>
		<display:setProperty name="paging.banner.all_items_found" value=""/>
		<display:setProperty name="paging.banner.one_item_found" value=""/>
	</display:table>
</c:if>