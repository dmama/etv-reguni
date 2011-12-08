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
	class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
	
	<display:column sortable ="true" titleKey="label.periodicite.decompte">
			<fmt:message key="option.periodicite.decompte.${periodicite.periodiciteDecompte}" />
				<c:if test="${periodicite.periodiciteDecompte == 'UNIQUE'}">
					&nbsp;(<fmt:message key="option.periode.decompte.${periodicite.periodeDecompte}" />)
				</c:if>
	</display:column>
	<display:column sortable ="true" titleKey="label.periodicite.debut.validite" sortProperty="dateDebut">
			<fmt:formatDate value="${periodicite.dateDebut}" pattern="dd.MM.yyyy"/>
	</display:column>
	<display:column sortable ="true" titleKey="label.periodicite.fin.validite" sortProperty="dateFin">
			<fmt:formatDate value="${periodicite.dateFin}" pattern="dd.MM.yyyy"/>
	</display:column>
	<display:column  titleKey="label.periodicite.statut" >
		<fmt:message key="option.periodicite.statut.${periodicite.active}" />		
	</display:column>
	<display:column style="action">
			<unireg:consulterLog entityNature="periodicite" entityId="${periodicite.id}"/>
	</display:column>



	<display:setProperty name="paging.banner.all_items_found" value=""/>
	<display:setProperty name="paging.banner.one_item_found" value=""/>
	
</display:table>
</c:if>