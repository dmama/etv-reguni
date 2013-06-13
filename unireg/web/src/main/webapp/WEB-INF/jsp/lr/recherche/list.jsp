<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.recherche.lr" /></tiles:put>
  	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/listes-recapitulatives.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
	    <form:form method="post" id="formRechercheLR">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors  cssClass="error"/>
				<jsp:include page="form.jsp"/>
			</fieldset>
		</form:form>

		<display:table 	name="lrs" id="lr" pagesize="25" requestURI="/lr/list.do" class="display_table" sort="external" partialList="true" size="resultSize">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncune.lr.trouvee" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.lr.trouvee" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.lrs.trouvees" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.lrs.trouvees" /></span></display:setProperty>

			<display:column sortable ="true" titleKey="label.numero.debiteur" sortProperty="numero" sortName="tiers.numero" >
				<c:if test="${!lr.annule}"><a href="../tiers/visu.do?id=${lr.numero}"><unireg:numCTB numero="${lr.numero}"></unireg:numCTB></a></c:if>
				<c:if test="${lr.annule}"><strike><unireg:numCTB numero="${lr.numero}" /></strike></c:if>
			</display:column>
			<display:column titleKey="label.nom.raison">
				<c:if test="${lr.annule}"><strike></c:if>
					<c:if test="${lr.nomCourrier1 != null }">
						${lr.nomCourrier1}
					</c:if>
					<c:if test="${lr.nomCourrier2 != null }">
						<br />${lr.nomCourrier2}
					</c:if>
				<c:if test="${lr.annule}"></strike></c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.debiteur.is" sortName="tiers.categorieImpotSource">
				<c:if test="${lr.annule}"><strike></c:if>
					<fmt:message key="option.categorie.impot.source.${lr.categorie}" />
				<c:if test="${lr.annule}"></strike></c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.mode.communication" sortName="modeCommunication">
				<c:if test="${lr.annule}"><strike></c:if>
					<fmt:message key="option.mode.communication.${lr.modeCommunication}" />
				<c:if test="${lr.annule}"></strike></c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.periode" sortName="dateDebut">
				<c:if test="${lr.annule}"><strike></c:if>
					<fmt:formatDate value="${lr.dateDebutPeriode}" pattern="dd.MM.yyyy"/>&nbsp;-&nbsp;<fmt:formatDate value="${lr.dateFinPeriode}" pattern="dd.MM.yyyy"/>
				<c:if test="${lr.annule}"></strike></c:if>
			</display:column>
			<display:column titleKey="label.date.retour">
				<c:if test="${lr.annule}"><strike></c:if>
					<fmt:formatDate value="${lr.dateRetour}" pattern="dd.MM.yyyy"/>
				<c:if test="${lr.annule}"></strike></c:if>
			</display:column>
			<display:column titleKey="label.date.delai.accorde">
				<c:if test="${lr.annule}"><strike></c:if>
					<fmt:formatDate value="${lr.delaiAccorde}" pattern="dd.MM.yyyy"/>
				<c:if test="${lr.annule}"></strike></c:if>
			</display:column>
			<display:column titleKey="label.etat.avancement" >
				<c:if test="${lr.annule}"><strike></c:if>
					<fmt:message key="option.etat.avancement.${lr.etat}" />
				<c:if test="${lr.annule}"></strike></c:if>
			</display:column>
			<display:column>
				<c:if test="${!lr.annule}">
					<unireg:raccourciModifier link="edit.do?id=${lr.id}" tooltip="LR"/>
				</c:if>
			</display:column>
		</display:table>
		
	</tiles:put>
</tiles:insert>
