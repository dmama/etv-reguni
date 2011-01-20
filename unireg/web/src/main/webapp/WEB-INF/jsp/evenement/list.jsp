<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.recherche.evenements" /></tiles:put>
  	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/recherche.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
	    <form:form method="post" id="formRechercheEvenements">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors  cssClass="error"/>
				<jsp:include page="form.jsp"/>
			</fieldset>
		</form:form>

		<display:table 	name="listEvenements" id="row" pagesize="25" requestURI="/evenement/list.do" defaultsort="1" defaultorder="descending" sort="external" class="display_table" partialList="true" size="listEvenementsSize">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncun.evenement.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner"><fmt:message key="banner.un.evenement.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.evenements.trouves" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.evenements.trouves" /></span></display:setProperty>

			<!-- ID -->
			<display:column property="id" sortable ="true" titleKey="label.evenement" href="visu.do" paramId="id" paramProperty="id" sortName="id" />
			<!-- NO Individu + Conjoint -->
			<display:column sortable ="true" titleKey="label.individu" sortProperty="numeroIndividuPrincipal" sortName="numeroIndividuPrincipal">
				${row.numeroIndividuPrincipal}
				<c:if test="${row.numeroIndividuConjoint != null }">
					<br>${row.numeroIndividuConjoint}
				</c:if>
			</display:column>
			<!-- NO CTB -->
			<display:column titleKey="label.numero.contribuable">
				<c:if test="${row.numeroCTB != null}">
					<unireg:numCTB numero="${row.numeroCTB}" />
				</c:if>
			</display:column>
			<!-- Nom  /Prénom -->
			<display:column titleKey="label.prenom.nom">
				<c:out value="${row.nom1}" />
				<c:if test="${row.nom2 != null}">
					<br><c:out value="${row.nom2}" />
				</c:if>
			</display:column>
			<!-- Type evt -->
			<display:column sortable ="true" titleKey="label.type.evenement" sortName="type">
				<fmt:message key="option.type.evenement.${row.type}" />
			</display:column>
			<!-- Date evenement -->
			<display:column sortable ="true" titleKey="label.date.evenement" sortName="dateEvenement">
				<unireg:regdate regdate="${row.dateEvenement}" />
			</display:column>
			<!-- Date traitement -->
			<display:column property="dateTraitement" sortable ="true" titleKey="label.date.traitement" format="{0,date,dd.MM.yyyy}" sortName="dateTraitement" />
			<!-- Status evt -->
			<display:column sortable ="true" titleKey="label.etat.evenement" sortName="etat" >
				<fmt:message key="option.etat.evenement.${row.etat}" />
			</display:column>
			<display:column style="action">
				<c:if test="${row.id != null}">
					<unireg:consulterLog entityNature="Evenement" entityId="${row.id}"/>
				</c:if>
		</display:column>
			
		</display:table>

	</tiles:put>
</tiles:insert>
