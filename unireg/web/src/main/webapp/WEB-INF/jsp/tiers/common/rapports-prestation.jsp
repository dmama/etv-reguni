<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="page" value="${param.page}" />
<c:if test="${page == 'visu' }">
	<c:set var="url" value="visu.do" />
</c:if>
<c:if test="${page == 'edit' }">
	<c:set var="url" value="edit.do" />
</c:if>
<c:if test="${not empty command.rapportsPrestation}">
		<display:table 	name="command.rapportsPrestation" id="rapportPrestation" pagesize="${pageSize}" 
						requestURI="${url}" class="display"
						sort="external" partialList="true" size="resultSize">
			<display:column sortable ="true" titleKey="label.date.debut" sortProperty="dateDebut" sortName="dateDebut">
				<c:if test="${rapportPrestation.annule}"><strike></c:if>
					<fmt:formatDate value="${rapportPrestation.dateDebut}" pattern="dd.MM.yyyy"/>
				<c:if test="${rapportPrestation.annule}"></strike></c:if>
			</display:column>
			
			<display:column sortable ="true" titleKey="label.date.fin" sortProperty="dateFin" sortName="dateFin">
				<c:if test="${rapportPrestation.annule}"><strike></c:if>
					<fmt:formatDate value="${rapportPrestation.dateFin}" pattern="dd.MM.yyyy"/>
				<c:if test="${rapportPrestation.annule}"></strike></c:if>
			</display:column>
			<display:column titleKey="label.type.activite" >
				<c:if test="${rapportPrestation.annule}"><strike></c:if>
					<c:if test="${rapportPrestation.typeActivite != ''}">
						<fmt:message key="option.type.activite.${rapportPrestation.typeActivite}" />
					</c:if>
				<c:if test="${rapportPrestation.annule}"></strike></c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.taux.activite" sortProperty="tauxActivite" sortName="tauxActivite">
				<c:if test="${rapportPrestation.annule}"><strike></c:if>
					<c:if test="${rapportPrestation.tauxActivite  != null}">
						${rapportPrestation.tauxActivite}%
					</c:if>
				<c:if test="${rapportPrestation.annule}"></strike></c:if>	
			</display:column>
			<display:column sortable="true" titleKey="label.numero.contribuable" sortProperty="sujetId" sortName="sujetId" >
				<c:if test="${rapportPrestation.annule}"><strike></c:if>
					<a href="../tiers/visu.do?id=${rapportPrestation.numero}"><unireg:numCTB numero="${rapportPrestation.numero}"></unireg:numCTB></a>
				<c:if test="${rapportPrestation.annule}"></strike></c:if>
			</display:column>
			<display:column titleKey="label.nom.prenom" >
				<c:if test="${rapportPrestation.annule}"><strike></c:if>
					<c:if test="${rapportPrestation.nomCourrier1 != null }">
						${rapportPrestation.nomCourrier1}
					</c:if>
					<c:if test="${rapportPrestation.nomCourrier2 != null }">
						<br />${rapportPrestation.nomCourrier2}
					</c:if>
				<c:if test="${rapportPrestation.annule}"></strike></c:if>
			</display:column>
			<display:column titleKey="label.numero.avs" >
				<c:if test="${rapportPrestation.annule}"><strike></c:if>
					${rapportPrestation.numeroAVS}
				<c:if test="${rapportPrestation.annule}"></strike></c:if>
			</display:column>
			<display:column style="action">
				<c:if test="${page == 'visu' }">
					<unireg:raccourciConsulter link="../common/consult-log.do?height=200&width=800&nature=RapportEntreTiers&id=${rapportPrestation.id}&TB_iframe=true&modal=true" thickbox="true" tooltip="Edition des logs"/>
				</c:if>
				<c:if test="${page == 'edit' }">
					<c:if test="${!rapportPrestation.annule}">
						<unireg:raccourciModifier link="../tiers/rapport.do?height=250&width=800&idRapport=${rapportPrestation.id}&sens=SUJET&TB_iframe=true&modal=true" thickbox="true" tooltip="Edition de rapport"/>
						<unireg:raccourciAnnuler onClick="javascript:annulerRapport(${rapportPrestation.id});"/>
					</c:if>
				</c:if>
			</display:column>
			<display:setProperty name="paging.banner.all_items_found" value=""/>
			<display:setProperty name="paging.banner.one_item_found" value=""/>
		</display:table>
</c:if>