<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="page" value="${param.page}" />
<c:if test="${page == 'visu' }">
	<c:set var="url" value="visu.do" />
</c:if>
<c:if test="${page == 'edit' }">
	<c:set var="url" value="edit.do" />
</c:if>
<c:if test="${not empty command.contribuablesAssocies}">
	<display:table 	name="command.contribuablesAssocies" id="contribuableAssocie" pagesize="10" 
					requestURI="${url}" class="display">
		<display:column sortable ="true" titleKey="label.numero.contribuable" sortProperty="numero" >
			<c:if test="${contribuableAssocie.annule}"><strike></c:if>
			<a href="../tiers/visu.do?id=${contribuableAssocie.numero}"><unireg:numCTB
				numero="${contribuableAssocie.numero}"></unireg:numCTB></a>
			<c:if test="${contribuableAssocie.annule}"></strike></c:if>
		</display:column>
		<display:column sortable ="true" titleKey="label.nom.raison">
			<c:if test="${contribuableAssocie.annule}"><strike></c:if>
			<c:if test="${contribuableAssocie.nomCourrier1 != null }">
				${contribuableAssocie.nomCourrier1}
			</c:if>
			<c:if test="${contribuableAssocie.nomCourrier2 != null }">
				<br />${contribuableAssocie.nomCourrier2}
			</c:if>
			<c:if test="${contribuableAssocie.annule}"></strike></c:if>
		</display:column>
		<display:column style="action">
			<c:if test="${page == 'visu' }">
				<unireg:raccourciConsulter link="../common/consult-log.do?height=200&width=800&nature=RapportEntreTiers&id=${contribuableAssocie.id}&TB_iframe=true&modal=true" thickbox="true" tooltip="Edition des logs"/>
			</c:if>
			<c:if test="${page == 'edit' }">
				<c:if test="${!contribuableAssocie.annule}">
					<unireg:raccourciAnnuler onClick="javascript:annulerRapport(${contribuableAssocie.id});"/>
				</c:if>
			</c:if>
		</display:column>
		<display:setProperty name="paging.banner.all_items_found" value=""/>
		<display:setProperty name="paging.banner.one_item_found" value=""/>
	</display:table>
</c:if>