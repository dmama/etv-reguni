<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="page" value="${param.page}" />
<c:if test="${page == 'visu' }">
	<c:set var="url" value="visu.do" />
</c:if>
<c:if test="${page == 'edit' }">
	<c:set var="url" value="edit.do" />
</c:if>
<c:if test="${not empty command.debiteurs}">
<display:table 	name="command.debiteurs" id="debiteur" 
				pagesize="10" 
				requestURI="${url}" class="display">
	<display:column sortable ="true" titleKey="label.numero.debiteur" href="visu.do" paramId="id" paramProperty="numero" sortProperty="numero" >
		<c:if test="${debiteur.annule}"><strike></c:if>
			<a href="../tiers/visu.do?id=${debiteur.numero}"><unireg:numCTB numero="${debiteur.numero}"></unireg:numCTB></a>
		<c:if test="${debiteur.annule}"></strike></c:if>
	</display:column>
	<display:column sortable ="true" titleKey="label.nom.raison" >
		<c:if test="${debiteur.annule}"><strike></c:if>
			<c:if test="${debiteur.nomCourrier1 != null }">
				${debiteur.nomCourrier1}
			</c:if>
			<c:if test="${debiteur.nomCourrier2 != null }">
				<br />${debiteur.nomCourrier2}
			</c:if>
			<c:if test="${debiteur.complementNom != null }">
				<br />${debiteur.complementNom}
			</c:if>
		<c:if test="${debiteur.annule}"></strike></c:if>
	</display:column>
	<display:column sortable ="true" titleKey="label.categorie.is" >
		<c:if test="${debiteur.annule}"><strike></c:if>
			<fmt:message key="option.categorie.impot.source.${debiteur.categorieImpotSource}" />
		<c:if test="${debiteur.annule}"></strike></c:if>
	</display:column>
	<display:column sortable ="true" property="personneContact" titleKey="label.contact"  />
	<display:column style="action">
		<c:if test="${page == 'visu' }">
			<unireg:raccourciConsulter link="../common/consult-log.do?height=200&width=800&nature=RapportEntreTiers&id=${dossierApparente.id}&TB_iframe=true&modal=true" thickbox="true" tooltip="Edition des logs"/>
		</c:if>
		<c:if test="${page == 'edit' }">
			<c:if test="${!debiteur.annule}">
				<unireg:raccourciAnnuler onClick="javascript:annulerRapport(${debiteur.id});"/>
			</c:if>
		</c:if>
	</display:column>
	<display:setProperty name="paging.banner.all_items_found" value=""/>
	<display:setProperty name="paging.banner.one_item_found" value=""/>
</display:table>
</c:if>