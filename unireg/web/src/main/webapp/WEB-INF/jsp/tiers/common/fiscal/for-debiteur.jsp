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
	class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
	
	<display:column sortable ="true" titleKey="label.commune">
		<unireg:commune ofs="${forFiscal.numeroForFiscalCommune}" displayProperty="nomOfficiel" titleProperty="noOFS" date="${forFiscal.regDateOuverture}"/>
	</display:column>

	<display:column sortable ="true" titleKey="label.date.ouv" sortProperty="dateOuverture">
		<fmt:formatDate value="${forFiscal.dateOuverture}" pattern="dd.MM.yyyy"/>
	</display:column>
	<display:column sortable ="true" titleKey="label.motif.ouv">
		<c:if test="${forFiscal.motifOuverture != null}">
			<fmt:message key="option.motif.ouverture.${forFiscal.motifOuverture}" />
		</c:if>
	</display:column>
	<display:column sortable ="true" titleKey="label.date.fer" sortProperty="dateFermeture">
		<fmt:formatDate value="${forFiscal.dateFermeture}" pattern="dd.MM.yyyy"/>
		<c:if test="${forFiscal.dateFermeture != null}">
			<c:if test="${page == 'edit' }">
				<c:if test="${forFiscal.dernierForPrincipalOuDebiteur}">
					<unireg:linkTo name="" action="/fors/debiteur/reopen.do" method="POST" params="{forId:${forFiscal.id}}" link_class="reOpenFor"
					               title="Ré-ouvrir de for" confirm="Voulez-vous vraiment ré-ouvrir ce for fiscal ?" />
				</c:if>
			</c:if>
		</c:if>
	</display:column>
	<display:column sortable ="true" titleKey="label.motif.fer">
		<c:if test="${forFiscal.motifFermeture != null}">
			<fmt:message key="option.motif.fermeture.${forFiscal.motifFermeture}" />
		</c:if>
	</display:column>
	<display:column style="action">
		<c:if test="${page == 'visu' }">
			<unireg:consulterLog entityNature="ForFiscal" entityId="${forFiscal.id}"/>
		</c:if>
		<c:if test="${page == 'edit' }">
			<c:if test="${!forFiscal.annule}">
				<c:if test="${forFiscal.dateFermeture == null}">
					<unireg:linkTo name="" action="/fors/debiteur/edit.do" method="GET" params="{forId:${forFiscal.id}}" link_class="edit" title="Edition de for" />
				</c:if>
				<c:if test="${forFiscal.dernierForPrincipalOuDebiteur}">
					<unireg:linkTo name="" action="/fors/debiteur/cancel.do" method="POST" params="{forId:${forFiscal.id}}" link_class="delete"
					               title="Annulation de for" confirm="Voulez-vous vraiment annuler ce for fiscal ?"/>
				</c:if>
			</c:if>
		</c:if>
	</display:column>
	<display:setProperty name="paging.banner.all_items_found" value=""/>
	<display:setProperty name="paging.banner.one_item_found" value=""/>
	
</display:table>
</c:if>