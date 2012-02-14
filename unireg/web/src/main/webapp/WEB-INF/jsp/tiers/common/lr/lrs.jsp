<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="page" value="${param.page}" />
<c:if test="${page == 'visu' }">
	<c:set var="url" value="visu.do" />
</c:if>
<c:if test="${page == 'edit' }">
	<c:set var="url" value="edit-debiteur.do" />
</c:if>
<c:if test="${not empty command.lrs}">	
	<display:table 	name="command.lrs" id="lr" 
					pagesize="4" 
					requestURI="${url}"
					class="display">
		<c:if test="${page == 'edit' }">			
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncune.lr.trouvee" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.lr.trouvee" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.lrs.trouvees" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.lrs.trouvees" /></span></display:setProperty>
		</c:if>
					
		<display:column sortable ="true" titleKey="label.periode" sortProperty="dateDebutPeriode">
			<c:if test="${lr.annule}"><strike></c:if>
				<fmt:formatDate value="${lr.dateDebutPeriode}" pattern="dd.MM.yyyy"/>&nbsp;-&nbsp;<fmt:formatDate value="${lr.dateFinPeriode}" pattern="dd.MM.yyyy"/>
			<c:if test="${lr.annule}"></strike></c:if>				
		</display:column>
		<display:column sortable ="true" titleKey="label.date.retour" sortProperty="dateRetour">
			<c:if test="${lr.annule}"><strike></c:if>
				<fmt:formatDate value="${lr.dateRetour}" pattern="dd.MM.yyyy"/>
			<c:if test="${lr.annule}"></strike></c:if>
		</display:column>
		<display:column sortable ="true" titleKey="label.date.delai.accorde" sortProperty="delaiAccorde" >
			<c:if test="${lr.annule}"><strike></c:if>
				<fmt:formatDate value="${lr.delaiAccorde}" pattern="dd.MM.yyyy"/>
			<c:if test="${lr.annule}"></strike></c:if>
		</display:column>
		<display:column sortable ="true" titleKey="label.etat.avancement" >
			<c:if test="${lr.annule}"><strike></c:if>
				<fmt:message key="option.etat.avancement.${lr.etat}" />
			<c:if test="${lr.annule}"></strike></c:if>
		</display:column>
		<display:column style="action">
			<c:if test="${page == 'visu' }">
				<c:if test="${!lr.annule}">
					<a href="#" class="detail" title="LR" onclick="return open_details_lr(<c:out value="${lr.id}"/>);">&nbsp;</a>
					<script>
					function open_details_lr(id) {
					   var dialog = Dialog.create_dialog_div('details-lr-dialog');

					   // charge le contenu de la boîte de dialogue
					   dialog.load('lr.do?idLr=' + id + '&' + new Date().getTime());

					   dialog.dialog({
						   title: "Détails de la déclaration d'impôt source",
						   height: 650,
						   width: 650,
						   modal: true,
						   buttons: {
							   Ok: function() {
								   dialog.dialog("close");
							   }
						   }
					   });
					}
					</script>
				</c:if>
				<unireg:consulterLog entityNature="LR" entityId="${lr.id}"/>
			</c:if>
			<c:if test="${page == 'edit' }">
				<c:if test="${!lr.annule}">
					<unireg:raccourciModifier link="edit.do?id=${lr.id}" tooltip="LR"/>
				</c:if>
			</c:if>
		</display:column>
		<display:setProperty name="paging.banner.all_items_found" value=""/>
		<display:setProperty name="paging.banner.one_item_found" value=""/>
	</display:table>
</c:if>