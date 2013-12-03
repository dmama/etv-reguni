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

		<display:table name="taches" id="tache" pagesize="25" requestURI="/tache/list.do" class="display_table"  sort="external" partialList="true" size="resultSize" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncune.tache.trouvee" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.tache.trouvee" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.taches.trouvees" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.taches.trouvees" /></span></display:setProperty>
	
			<display:column sortable ="true" titleKey="label.type.tache" sortName="class">
				<fmt:message key="option.type.tache.${tache.typeTache}"  />
			</display:column>
			<display:column sortable ="true" titleKey="label.numero.contribuable" sortProperty="numero" sortName="contribuable.numero">
				<c:choose>
					<c:when test="${tache.etatTache == 'TRAITE' || tache.annule}">
						<unireg:numCTB numero="${tache.numero}" />
					</c:when>
					<c:when test="${tache.etatTache == 'EN_INSTANCE'}">
						<c:choose>
							<c:when test="${tache.typeTache == 'TacheControleDossier'}">
								<a href="../tiers/visu.do?id=${tache.numero}&idTacheTraite=${tache.id}"><unireg:numCTB numero="${tache.numero}" /></a>
							</c:when>
							<c:when test="${tache.typeTache == 'TacheTransmissionDossier'}">
								<a href="../mouvement/edit.do?numero=${tache.numero}&depuisTache=true&idTacheTraite=${tache.id}"><unireg:numCTB numero="${tache.numero}" /></a>
							</c:when>
							<c:when test="${tache.typeTache == 'TacheEnvoiDeclarationImpot'}">
								<a href="../di/imprimer.do?depuisTache=true&tiersId=${tache.numero}&debut=<unireg:regdate regdate="${tache.dateDebutImposition}" format="yyyyMMdd"/>&fin=<unireg:regdate regdate="${tache.dateFinImposition}" format="yyyyMMdd"/>&typeDocument=${tache.typeDocument}&delaiRetour=${tache.delaiRetourEnJours}" ><unireg:numCTB numero="${tache.numero}" /></a>
							</c:when>
							<c:when test="${tache.typeTache == 'TacheAnnulationDeclarationImpot'}">
								<a href="../di/editer.do?id=${tache.idDI}&tacheId=${tache.id}"><unireg:numCTB numero="${tache.numero}" /></a>
							</c:when>
						</c:choose>
					</c:when>
				</c:choose>
			</display:column>
			<display:column titleKey="label.nom.raison" >
				<c:if test="${tache.nomCourrier1 != null }">
					<c:out value="${tache.nomCourrier1}"/>
				</c:if>
				<c:if test="${tache.nomCourrier2 != null }">
					<br /><c:out value="${tache.nomCourrier2}"/>
				</c:if>
			</display:column>
			<display:column titleKey="label.for.gestion" >
				<unireg:commune ofs="${tache.numeroForGestion}" displayProperty="nomOfficiel"/>
			</display:column>
			<display:column titleKey="label.office.impot" >
				${tache.officeImpot}
			</display:column>
			
			<!-- Spécifique à l'envoi des DIs -->
			<display:column titleKey="label.date.periodeImposition" >
				<c:if test="${tache.impositionSurAnneeComplete}">
					<fmt:message key="label.date.periodeImpositionComplete">
						<fmt:param>${tache.impositionAnneeComplete}</fmt:param>
					</fmt:message>
				</c:if>
				<c:if test="${!tache.impositionSurAnneeComplete && tache.dateDebutImposition != null}">
					<fmt:message key="label.date.periodeImpositionPartielle">
						<fmt:param><unireg:date date="${tache.dateDebutImposition}"/></fmt:param>
						<fmt:param><unireg:date date="${tache.dateFinImposition}"/></fmt:param>
					</fmt:message>
				</c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.etat.tache" sortName="etat">
				<fmt:message key="option.etat.tache.${tache.etatTache}"  />
			</display:column>
			<display:column>
				<unireg:consulterLog entityNature="Tache" entityId="${tache.id}"/>
			</display:column>
			<!-- Fin -->
			
		</display:table>
		
	</tiles:put>
</tiles:insert>
