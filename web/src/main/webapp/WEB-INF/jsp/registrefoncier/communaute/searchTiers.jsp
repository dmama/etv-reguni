<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title">
  		<fmt:message key="title.selection.principal.recherche" />
  	</tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
	    <%--@elvariable id="criteria" type="ch.vd.uniregctb.tiers.view.TiersCriteriaView"--%>
	    <form:form method="get" id="formRecherchePP" commandName="criteria">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors  cssClass="error"/>
				<jsp:include page="../../tiers/recherche/form.jsp">
					<jsp:param name="typeRecherche" value="communaute" />
					<jsp:param name="prefixeEffacer" value="/registrefoncier/communaute" />
				</jsp:include>
			</fieldset>
		</form:form>

		<display:table name="list" id="row" pagesize="25" requestURI="/registrefoncier/communaute/searchTiers.do" class="display" sort="list" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncune.personne.trouvee" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.personne.trouvee" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.personnes.trouvees" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.personnes.trouvees" /></span></display:setProperty>

			<display:column sortable="true" titleKey="label.numero.contribuable" sortProperty="numero" >
				<a href="showTiers.do?id=${row.numero}"><unireg:numCTB numero="${row.numero}" /></a>
			</display:column>
			<display:column sortable="true" titleKey="label.prenom.nom" >
				<c:out value="${row.nom1}" />
				<c:if test="${row.nom2 != null}">
					<br><c:out value="${row.nom2}" />
				</c:if>
			</display:column>
			<display:column sortable="true" titleKey="label.date.naissance" sortProperty="dateNaissanceInscriptionRC">
				<unireg:date date="${row.dateNaissanceInscriptionRC}"/>
			</display:column>
			<display:column property="localiteOuPays" sortable="true" titleKey="label.localitePays"  />
			<display:column sortable="false" titleKey="label.nombre.communautes">
				<c:out value="${row.modelesCount}"/>
			</display:column>
		</display:table>
	</tiles:put>
</tiles:insert>
