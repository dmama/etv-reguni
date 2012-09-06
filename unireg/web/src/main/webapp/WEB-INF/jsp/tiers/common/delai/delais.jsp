<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="page" value="${param.page}" />
<c:set var="depuisTache" value="${param.depuisTache}" />
<c:if test="${not empty command.delais}">
<fieldset>
	<legend><span><fmt:message key="label.delais" /></span></legend>
	
	<display:table 	name="command.delais" id="delai" pagesize="10" class="display">
		<display:column titleKey="label.date.demande">
			<c:if test="${delai.annule}"><strike></c:if>
				<unireg:regdate regdate="${delai.dateDemande}" />
			<c:if test="${delai.annule}"></strike></c:if>
		</display:column>
		<display:column titleKey="label.date.delai.accorde">
			<c:if test="${delai.annule}"><strike></c:if>
				<unireg:regdate regdate="${delai.delaiAccordeAu}" />
			<c:if test="${delai.annule}"></strike></c:if>
		</display:column>
		<display:column titleKey="label.confirmation.ecrite">
			<input type="checkbox" name="decede" value="True"   
			<c:if test="${delai.confirmationEcrite}">checked </c:if> disabled="disabled" />
				<c:if test="${page == 'visu' }">
					<c:if test="${delai.confirmationEcrite}">
						<a href="../declaration/copie-conforme-delai.do?idDelai=${delai.id}" class="pdf" id="print-delai-${delai.id}" onclick="Link.tempSwap(this, '#disabled-print-delai-${delai.id}');">&nbsp;</a>
						<span class="pdf-grayed" id="disabled-print-delai-${delai.id}" style="display: none;">&nbsp;</span>
					</c:if>
				</c:if>
		</display:column>
		<display:column titleKey="label.date.traitement">
			<c:if test="${delai.annule}"><strike></c:if>
				<unireg:regdate regdate="${delai.dateTraitement}" />
			<c:if test="${delai.annule}"></strike></c:if>
		</display:column>
		<display:column style="action">
			<c:if test="${page == 'visu' }">
				<img src="../images/consult_off.gif" title="${delai.logModifUser}-<fmt:formatDate value="${delai.logModifDate}" pattern="dd.MM.yyyy HH:mm:ss"/>" />
			</c:if>
			<c:if test="${depuisTache == null && command.allowedDelai && page == 'edit'}">
				<c:if test="${(!delai.annule) && (!delai.first)}">
					<unireg:linkTo name="&nbsp;" title="Annuler le délai"  confirm="Voulez-vous vraiment annuler ce delai ?"
					               action="/decl/delai/annuler.do" method="post" params="{id:${delai.id}}" link_class="delete"/>
				</c:if>
			</c:if>
		</display:column>

		<display:setProperty name="paging.banner.all_items_found" value=""/>
		<display:setProperty name="paging.banner.one_item_found" value=""/>
	</display:table>

</fieldset>
</c:if>