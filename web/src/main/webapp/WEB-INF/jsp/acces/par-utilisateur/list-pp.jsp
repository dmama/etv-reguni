<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title">
		<fmt:message key="title.acces.utilisateur.recherche.pp.ou.entreprise" />
  	</tiles:put>
  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onClick="ouvrirAide('<c:url value='/docs/acces-par-utilisateur.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
  	<tiles:put name="body">
	    <form:form method="post" id="formRecherchePP">
		    <jsp:include page="../../general/utilisateur.jsp">
				<jsp:param name="path" value="utilisateurView" />
				<jsp:param name="titleKey" value="title.droits.operateur" />
			</jsp:include>
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors  cssClass="error"/>
				<c:if test="${errorMessage != null}">
					<span class="error">
						<fmt:message key="${errorMessage}"/>
					</span>
				</c:if>
				<form:hidden path="typeTiers"/>
				<jsp:include page="../../tiers/recherche/form.jsp">
					<jsp:param name="typeRecherche" value="acces" />
					<jsp:param name="prefixeEffacer" value="/acces/par-utilisateur/restriction"/>
					<jsp:param name="paramsEffacer" value="noIndividuOperateur:${command.noIndividuOperateur}"/>
				</jsp:include>
			</fieldset>
		</form:form>

		<display:table 	name="list" id="row" pagesize="25" requestURI="/acces/par-utilisateur/ajouter-restriction.do" class="display" sort="list">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.aucun.dossier.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.dossier.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.dossiers.trouves" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.dossiers.trouves" /></span></display:setProperty>

			<display:column sortable ="true" titleKey="label.numero.contribuable" sortProperty="numero" >
				<a href="recap.do?numero=${row.numero}&noIndividuOperateur=${command.noIndividuOperateur}"><unireg:numCTB numero="${row.numero}" /></a>
			</display:column>
			<display:column sortable ="true" titleKey="label.nom.raison" >
				<c:out value="${row.nom1}" />
			</display:column>
			<display:column sortable ="true" titleKey="label.date.naissance.ou.rc" sortProperty="dateNaissanceInscriptionRC">
				<unireg:date date="${row.dateNaissanceInscriptionRC}"/>
			</display:column>
			<display:column property="localiteOuPays" sortable ="true" titleKey="label.localitePays"  />
		</display:table>
		
		<!-- Debut Bouton -->
		<table border="0">
		<tr><td>
			<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="document.location.href='restrictions.do?noIndividuOperateur=${command.utilisateurView.numeroIndividu}';" />
		</td></tr>
		</table>
		<!-- Fin Bouton -->
		
	</tiles:put>
</tiles:insert>