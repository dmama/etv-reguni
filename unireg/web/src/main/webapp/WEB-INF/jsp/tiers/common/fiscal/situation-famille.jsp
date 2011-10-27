<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="page" value="${param.page}" />
<c:if test="${page == 'visu' }">
	<c:set var="url" value="visu.do" />
</c:if>
<c:if test="${page == 'edit' }">
	<c:set var="url" value="edit.do" />
</c:if>
<c:if test="${not empty command.situationsFamille}">
	<display:table name="command.situationsFamille" id="situationFamille" pagesize="10"	requestURI="${url}"	class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">

		<display:column sortable ="true" titleKey="label.etat.civil">
		 <c:if test="${situationFamille.etatCivil != null}">
			<fmt:message key="option.etat.civil.${situationFamille.etatCivil}"  />
		 </c:if>
		</display:column>
	
	<display:column sortable ="true" titleKey="label.nombre.enfants" >
		${situationFamille.nombreEnfants}
	</display:column>
	<display:column sortable ="true" titleKey="label.bareme.is.applicable">
		<c:if test="${situationFamille.tarifImpotSource != null}">
			<fmt:message key="option.tarif.impot.source.${situationFamille.tarifImpotSource}" />
		</c:if>
	</display:column>
	<display:column sortable ="true" titleKey="label.revenu.plus.eleve" >
		<c:if test="${situationFamille.numeroTiersRevenuPlusEleve != null}">
			<a href="../tiers/visu.do?id=${situationFamille.numeroTiersRevenuPlusEleve}">
				<unireg:numCTB numero="${situationFamille.numeroTiersRevenuPlusEleve}" />
			</a>&nbsp;-&nbsp;${situationFamille.nomCourrier1TiersRevenuPlusEleve}
		</c:if>
	</display:column>
	<display:column sortable ="true" titleKey="label.date.debut" sortProperty="dateDebut">
		<fmt:formatDate value="${situationFamille.dateDebut}" pattern="dd.MM.yyyy"/>
	</display:column>
	<display:column sortable ="true" titleKey="label.date.fin" sortProperty="dateFin">
		<fmt:formatDate value="${situationFamille.dateFin}" pattern="dd.MM.yyyy"/>
	</display:column>
	<display:column titleKey="label.situationfamille.source">
		<c:if test="${situationFamille.source != null}">
			<fmt:message key="option.situationfamille.source.${situationFamille.source}"/>
		</c:if>
	</display:column>
	<display:column style="action">
		<c:if test="${(page == 'visu') && (situationFamille.id != null) }">
			<unireg:consulterLog entityNature="SituationFamille" entityId="${situationFamille.id}"/>
		</c:if>
		<c:if test="${page == 'edit' }">
			<c:if test="${!situationFamille.annule && situationFamille.editable}">
				<unireg:raccourciAnnuler onClick="javascript:annulerSituationFamille(${situationFamille.id});" tooltip="Annulation de situation de famille"/>
			</c:if>
		</c:if>
	</display:column>
	<display:setProperty name="paging.banner.all_items_found" value=""/>
	<display:setProperty name="paging.banner.one_item_found" value=""/>
</display:table>
</c:if>