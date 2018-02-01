<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="printparam" value='<%= request.getParameter("printview") %>' />
<c:set var="printview" value="${(empty printparam)? false :printparam }" />
<fieldset>
	<legend><span><fmt:message key="label.fors.fiscaux" /></span></legend>
	<c:if test="${not empty command.forsFiscaux}">
		<unireg:raccourciToggleAffichage tableId="forFiscal" numeroColonne="2" nombreLignes="${fn:length(command.forsFiscaux)}" modeImpression="${printview}" />

		<display:table
				name="command.forsFiscaux" id="forFiscal" pagesize="10"
				requestURI="visu.do"
				class="display" decorator="ch.vd.unireg.decorator.TableEntityDecorator">

			<display:column sortable ="true" titleKey="label.commune">
				<c:choose>
					<c:when test="${forFiscal.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD' }">
						<unireg:commune ofs="${forFiscal.numeroForFiscalCommune}" displayProperty="nomOfficiel" titleProperty="noOFS" date="${forFiscal.regDateOuverture}"/>
					</c:when>
					<c:when test="${forFiscal.typeAutoriteFiscale == 'COMMUNE_HC' }">
						<unireg:commune ofs="${forFiscal.numeroForFiscalCommuneHorsCanton}" displayProperty="nomOfficielAvecCanton" titleProperty="noOFS" date="${forFiscal.regDateOuverture}"/>
					</c:when>
				</c:choose>
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
			</display:column>
			<display:column sortable ="true" titleKey="label.motif.fer">
				<c:if test="${forFiscal.motifFermeture != null}">
					<fmt:message key="option.motif.fermeture.${forFiscal.motifFermeture}" />
				</c:if>
			</display:column>
			<display:column style="action">
				<unireg:consulterLog entityNature="ForFiscal" entityId="${forFiscal.id}"/>
			</display:column>
			<display:setProperty name="paging.banner.all_items_found" value=""/>
			<display:setProperty name="paging.banner.one_item_found" value=""/>

		</display:table>
	</c:if>
</fieldset>
