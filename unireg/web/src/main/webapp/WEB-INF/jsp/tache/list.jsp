<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.recherche.taches" /></tiles:put>
  	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/taches.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
	    <form:form method="post" id="formRechercheTache">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors  cssClass="error"/>
				<jsp:include page="form.jsp"/>
			</fieldset>
		</form:form>

		<display:table name="taches" id="tache" pagesize="25" requestURI="/tache/list.do" class="display_table"  sort="external" partialList="true" size="resultSize">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncune.tache.trouvee" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.tache.trouvee" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.taches.trouvees" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.taches.trouvees" /></span></display:setProperty>
	
			<display:column sortable ="true" titleKey="label.type.tache" sortName="class">
				<c:if test="${tache.annulee}"><strike></c:if>
					<fmt:message key="option.type.tache.${tache.typeTache}"  />
				<c:if test="${tache.annulee}"></strike></c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.numero.contribuable" sortProperty="numero" sortName="contribuable.numero">
					<c:if test="${tache.etatTache == 'TRAITE' || tache.annulee}">
						<c:if test="${tache.annulee}"><strike></c:if>
							<unireg:numCTB numero="${tache.numero}" />
						<c:if test="${tache.annulee}"></strike></c:if>
					</c:if>
					<c:if test="${tache.etatTache == 'EN_INSTANCE'}">
					<c:choose>
						<c:when test="${tache.typeTache == 'TacheControleDossier' && !tache.annulee}">
							<a href="../tiers/visu.do?id=${tache.numero}&idTacheTraite=${tache.id}"><unireg:numCTB numero="${tache.numero}" /></a>
						</c:when>
						<c:when test="${tache.typeTache == 'TacheTransmissionDossier' && !tache.annulee}">
							<a href="../mouvement/edit.do?numero=${tache.numero}&depuisTache=true&idTacheTraite=${tache.id}"><unireg:numCTB numero="${tache.numero}" /></a>
						</c:when>
						<c:when test="${tache.typeTache == 'TacheEnvoiDeclarationImpot' && !tache.annulee}">
							<a href="../di/edit.do?depuisTache=true&action=newdi&numero=${tache.numero}&debut=<unireg:regdate regdate="${tache.dateDebutImposition}" format="INDEX"/>&fin=<unireg:regdate regdate="${tache.dateFinImposition}" format="INDEX"/>&typeDeclaration=${tache.typeDocument}&delaiRetour=${tache.delaiRetourEnJours}" ><unireg:numCTB numero="${tache.numero}" /></a>
						</c:when>
						<c:when test="${tache.typeTache == 'TacheAnnulationDeclarationImpot' && !tache.annulee}">
							<a href="../di/edit.do?depuisTache=true&action=editdi&id=${tache.idDI}&idTache=${tache.id}"><unireg:numCTB numero="${tache.numero}" /></a>
						</c:when>
					</c:choose>
					</c:if>
			</display:column>
			<display:column titleKey="label.nom.raison" >
				<c:if test="${tache.annulee}"><strike></c:if>
					<c:if test="${tache.nomCourrier1 != null }">
							<c:out value="${tache.nomCourrier1}"/>
					</c:if>
					<c:if test="${tache.nomCourrier2 != null }">
							<br /><c:out value="${tache.nomCourrier2}"/>
					</c:if>
				<c:if test="${tache.annulee}"></strike></c:if>
			</display:column>
			<display:column titleKey="label.for.gestion" >
				<c:if test="${tache.annulee}"><strike></c:if>
					<unireg:infra entityId="${tache.numeroForGestion}" entityType="commune" entityPropertyName="nomMinuscule"></unireg:infra>
				<c:if test="${tache.annulee}"></strike></c:if>
			</display:column>
			<display:column titleKey="label.office.impot" >
				<c:if test="${tache.annulee}"><strike></c:if>
					${tache.officeImpot}
				<c:if test="${tache.annulee}"></strike></c:if>
			</display:column>
			
			<!-- Spécifique à l'envoi des DIs -->
			<display:column titleKey="label.date.periodeImposition" >
				<c:if test="${tache.annulee}"><strike></c:if>
					<c:if test="${tache.impositionSurAnneeComplete}">
						<fmt:message key="label.date.periodeImpositionComplete">
							<fmt:param>${tache.impositionAnneeComplete}</fmt:param>
						</fmt:message>
					</c:if>
					<c:if test="${!tache.impositionSurAnneeComplete && tache.dateDebutImposition != null}">
						<fmt:message key="label.date.periodeImpositionPartielle">
							<fmt:param><unireg:date date="${tache.dateDebutImposition}"></unireg:date></fmt:param>
							<fmt:param><unireg:date date="${tache.dateFinImposition}"></unireg:date></fmt:param>
						</fmt:message>
					</c:if>
				<c:if test="${tache.annulee}"></strike></c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.etat.tache" sortName="etat">
				<c:if test="${tache.annulee}"><strike></c:if>
					<fmt:message key="option.etat.tache.${tache.etatTache}"  />
				<c:if test="${tache.annulee}"></strike></c:if>
			</display:column>
			<display:column>
				<unireg:consulterLog entityNature="Tache" entityId="${tache.id}"/>
			</display:column>
			<!-- Fin -->
			
		</display:table>
		
	</tiles:put>
</tiles:insert>
