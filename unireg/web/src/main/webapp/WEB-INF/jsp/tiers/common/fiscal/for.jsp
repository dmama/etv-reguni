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
		class="display">

	<display:column sortable ="true" titleKey="label.genre.impot">
		<c:if test="${forFiscal.annule}"><strike></c:if>
			<fmt:message key="option.genre.impot.${forFiscal.genreImpot}"  />
		<c:if test="${forFiscal.annule}"></strike></c:if>
	</display:column>
	<display:column sortable ="true" titleKey="label.rattachement" >
		<c:if test="${forFiscal.natureForFiscal != 'ForFiscalAutreImpot'}">
			<c:if test="${forFiscal.annule}"><strike></c:if>
				<fmt:message key="option.rattachement.${forFiscal.motifRattachement}" />
			<c:if test="${forFiscal.annule}"></strike></c:if>
		</c:if>
	</display:column>
	<display:column sortable ="true" titleKey="label.mode.imposition">
		<c:if test="${forFiscal.natureForFiscal == 'ForFiscalPrincipal'}">
			<c:if test="${forFiscal.annule}"><strike></c:if>
				<fmt:message key="option.mode.imposition.${forFiscal.modeImposition}" />
			<c:if test="${forFiscal.annule}"></strike></c:if>
		</c:if>
	</display:column>
	<display:column sortable ="true" titleKey="label.for.abrege">
		<c:if test="${forFiscal.annule}"><strike></c:if>
			<c:choose>
				<c:when test="${forFiscal.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD' }">			
					<unireg:infra entityId="${forFiscal.numeroForFiscalCommune}" entityType="commune" entityPropertyName="nomMinuscule"></unireg:infra>
				</c:when>
				<c:when test="${forFiscal.typeAutoriteFiscale == 'COMMUNE_HC' }">
					<unireg:infra entityId="${forFiscal.numeroForFiscalCommuneHorsCanton}" entityType="commune" entityPropertyName="nomMinuscule"></unireg:infra>
				</c:when>
				<c:when test="${forFiscal.typeAutoriteFiscale == 'PAYS_HS' }">
					<unireg:infra entityId="${forFiscal.numeroForFiscalPays}" entityType="pays" entityPropertyName="nomMinuscule"></unireg:infra>
				</c:when>
			</c:choose>
		<c:if test="${forFiscal.annule}"></strike></c:if>
	</display:column>
	<display:column sortable ="true" titleKey="label.date.ouv" sortProperty="dateOuverture">
		<c:if test="${forFiscal.annule}"><strike></c:if>
			<fmt:formatDate value="${forFiscal.dateOuverture}" pattern="dd.MM.yyyy"/>
		<c:if test="${forFiscal.annule}"></strike></c:if>
	</display:column>
	<display:column sortable ="true" titleKey="label.motif.ouv">
		<c:if test="${forFiscal.annule}"><strike></c:if>
			<c:if test="${forFiscal.natureForFiscal != 'ForFiscalAutreImpot'}">
				<c:if test="${forFiscal.motifOuverture != null}">
					<fmt:message key="option.motif.ouverture.${forFiscal.motifOuverture}" />
				</c:if>
			</c:if>
		<c:if test="${forFiscal.annule}"></strike></c:if>
	</display:column>
	<display:column sortable ="true" titleKey="label.date.fer" sortProperty="dateFermeture">
		<c:if test="${forFiscal.annule}"><strike></c:if>
			<fmt:formatDate value="${forFiscal.dateFermeture}" pattern="dd.MM.yyyy"/>
		<c:if test="${forFiscal.annule}"></strike></c:if>
	</display:column>
	<display:column sortable ="true" titleKey="label.motif.fer">
		<c:if test="${forFiscal.annule}"><strike></c:if>
			<c:if test="${forFiscal.natureForFiscal != 'ForFiscalAutreImpot'}">
				<c:if test="${forFiscal.motifFermeture != null}">
					<fmt:message key="option.motif.fermeture.${forFiscal.motifFermeture}" />
				</c:if>
			</c:if>
		<c:if test="${forFiscal.annule}"></strike></c:if>
	</display:column>
	<display:column sortable ="true" titleKey="label.for.gestion">
		<c:if test="${!forFiscal.annule}">
			<c:if test="${forFiscal.natureForFiscal != 'ForFiscalAutreImpot'}">
				<input type="checkbox" <c:if test="${forFiscal.forGestion}">checked</c:if> disabled="disabled">
			</c:if>
		</c:if>
	</display:column>
	<display:column style="action">
		<c:if test="${page == 'visu' }">
			<unireg:raccourciConsulter link="../common/consult-log.do?height=200&width=800&nature=ForFiscal&id=${forFiscal.id}&TB_iframe=true&modal=true" thickbox="true" tooltip="Edition des logs"/>
		</c:if>
		<c:if test="${page == 'edit' }">
			<c:if test="${!forFiscal.annule}">
				<c:if test="${((forFiscal.natureForFiscal == 'ForFiscalPrincipal') && (command.allowedOnglet.FOR_PRINC)) ||
					((forFiscal.natureForFiscal == 'ForFiscalSecondaire') && (command.allowedOnglet.FOR_SEC)) ||
					((forFiscal.natureForFiscal == 'ForFiscalAutreImpot') && (command.allowedOnglet.FOR_AUTRE)) ||
					((forFiscal.natureForFiscal == 'ForFiscalAutreElementImposable') && (command.allowedOnglet.FOR_AUTRE))}">
					<c:if test="${(forFiscal.natureForFiscal != 'ForFiscalAutreImpot') && (forFiscal.dateFermeture == null)}">
						<unireg:raccourciModifier link="for.do?idFor=${forFiscal.id}" tooltip="Edition de for"/>
					</c:if>
					<c:if test="${forFiscal.natureForFiscal != 'ForFiscalPrincipal' || forFiscal.dernierForPrincipal}">
						<unireg:raccourciAnnuler onClick="javascript:annulerFor(${forFiscal.id});"/>
					</c:if>
				</c:if>
			</c:if>
		</c:if>
	</display:column>
	<display:setProperty name="paging.banner.all_items_found" value=""/>
	<display:setProperty name="paging.banner.one_item_found" value=""/>
	
</display:table>
</c:if>