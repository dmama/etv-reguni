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
				<c:if test="${di.dateRetour != null}">
					<c:if test="${di.sourceRetour == null}">
						(<fmt:message key="option.source.quittancement.UNKNOWN" />)
					</c:if>
					<c:if test="${di.sourceRetour != null}">
						(<fmt:message key="option.source.quittancement.${di.sourceRetour}" />)
					</c:if>
				</c:if>
			</td>
			<c:if test="${di.annule}"></strike></c:if>
		</display:column>
		<display:column style="action">
			<c:if test="${page == 'visu' }">
				<c:if test="${!di.annule}">
					<a href="#" class="detail" title="DI" onclick="return open_details_di(<c:out value="${di.id}"/>);">&nbsp;</a>
					<script>
					function open_details_di(id) {
						var dialog = create_dialog_div('details-di-dialog');

						// charge le contenu de la boîte de dialogue
						dialog.load('di.do?idDi=' + id);

						dialog.dialog({
							title: "Détails de la déclaration d'impôt",
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
				<unireg:consulterLog entityNature="DI" entityId="${di.id}"/>
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