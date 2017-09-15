<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<c:set var="page" value="${param.page}"/>
<c:set var="url">
	<c:choose>
		<c:when test="${page == 'edit'}">edit-debiteur.do</c:when>
		<c:otherwise>visu.do</c:otherwise>
	</c:choose>
</c:set>

<c:if test="${not empty command.lrs}">
	<display:table name="command.lrs" id="lr" pagesize="10" requestURI="${url}" class="display" sort="list" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
		<c:if test="${page == 'edit'}">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncune.lr.trouvee" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.lr.trouvee" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.lrs.trouvees" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.lrs.trouvees" /></span></display:setProperty>
		</c:if>
					
		<display:column sortable ="true" titleKey="label.periode" sortProperty="dateDebut">
			<unireg:regdate regdate="${lr.dateDebut}"/>&nbsp;-&nbsp;<unireg:regdate regdate="${lr.dateFin}"/>
		</display:column>
		<display:column sortable ="true" titleKey="label.date.retour" sortProperty="dateRetour">
			<unireg:regdate regdate="${lr.dateRetour}"/>
		</display:column>
		<display:column sortable ="true" titleKey="label.date.delai.accorde" sortProperty="delaiAccorde" >
			<unireg:regdate regdate="${lr.delaiAccorde}"/>
		</display:column>
		<display:column sortable ="true" titleKey="label.etat.avancement" >
			<fmt:message key="option.etat.avancement.f.${lr.etat}" />
		</display:column>
		<display:column style="action">
			<c:if test="${page == 'visu' }">
				<c:if test="${!lr.annule}">
					<a href="#" class="detail" title="LR" onclick="Decl.open_details_lr(<c:out value="${lr.id}"/>); return false;">&nbsp;</a>
				</c:if>
				<unireg:consulterLog entityNature="LR" entityId="${lr.id}"/>
			</c:if>
			<c:if test="${page == 'edit' }">
				<c:if test="${!lr.annule}">
					<unireg:raccourciModifier link="edit-lr.do?id=${lr.id}" tooltip="LR"/>
				</c:if>
			</c:if>
		</display:column>
		<display:setProperty name="paging.banner.all_items_found" value=""/>
		<display:setProperty name="paging.banner.one_item_found" value=""/>
	</display:table>
</c:if>