<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title">
		<fmt:message key="title.acces.dossier.recherche.pp" />
  	</tiles:put>
  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/acces-par-dossier.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
  	<tiles:put name="body">
	    <form:form method="post" id="formRecherchePP">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors  cssClass="error"/>
				<c:if test="${errorMessage != null}">
					<span class="error">
						<fmt:message key="${errorMessage}"/>
					</span>
				</c:if>
				<form:hidden path="typeTiers"/>
				<unireg:nextRowClass reset="0"/>
				<jsp:include page="../../tiers/recherche/form.jsp">
					<jsp:param name="typeRecherche" value="acces" />
					<jsp:param name="prefixeEffacer" value="/acces/par-dossier"/>
				</jsp:include>
			</fieldset>
		</form:form>

		<display:table 	name="list" id="row" pagesize="25" requestURI="/acces/par-dossier.do" class="display" sort="list">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncune.personne.trouvee" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.personne.trouvee" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.personnes.trouvees" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.personnes.trouvees" /></span></display:setProperty>

			<display:column sortable ="true" titleKey="label.numero.contribuable" sortProperty="numero" >
				<a href="par-dossier/restrictions.do?numero=${row.numero}"><unireg:numCTB numero="${row.numero}" /></a>
			</display:column>
			<display:column sortable ="true" titleKey="label.prenom.nom" property="nom1"/>
			<display:column sortable ="true" titleKey="label.date.naissance" sortProperty="dateNaissanceInscriptionRC">
				<unireg:date date="${row.dateNaissanceInscriptionRC}"/>
			</display:column>
			<display:column property="localiteOuPays" sortable ="true" titleKey="label.localitePays"  />
		</display:table>
	</tiles:put>
</tiles:insert>