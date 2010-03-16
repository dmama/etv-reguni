<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:if test="${not empty command.etats}">
<fieldset>
	<legend><span><fmt:message key="label.etats" /></span></legend>
	
	<display:table 	name="command.etats" id="etat" pagesize="10" class="display">
		<display:column titleKey="label.date.obtention" >
			<c:if test="${etat.annule}"><strike></c:if>
				<unireg:regdate regdate="${etat.dateObtention}" />
			<c:if test="${etat.annule}"></strike></c:if>
		</display:column>
 		<display:column titleKey="label.etat">
	 		<c:if test="${etat.annule}"><strike></c:if>
				<fmt:message key="option.etat.avancement.${etat.etat}" />
			<c:if test="${etat.annule}"></strike></c:if>
		</display:column>
		<display:column style="action">
			<c:if test="${!etat.annule}">
				<img src="../images/consult_off.gif" title="${etat.logModifUser}-<fmt:formatDate value="${etat.logModifDate}" pattern="dd.MM.yyyy HH:mm:ss"/>" />
			</c:if>
			<c:if test="${etat.annule}">
				<img src="../images/consult_off.gif" title="${etat.annulationUser}-<fmt:formatDate value="${etat.annulationDate}" pattern="dd.MM.yyyy HH:mm:ss"/>" />
			</c:if>
		</display:column>
	<display:setProperty name="paging.banner.all_items_found" value=""/>
	<display:setProperty name="paging.banner.one_item_found" value=""/>
	</display:table>
	
</fieldset>
</c:if>