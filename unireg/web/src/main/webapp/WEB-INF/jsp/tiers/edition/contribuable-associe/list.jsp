<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title">
		  	<fmt:message key="title.recherche.contribuable.associe" />
  	</tiles:put>
  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onClick="ouvrirAide('<c:url value='/docs/gestion-debiteurIS.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>

	    <%--@elvariable id="command" type="ch.vd.uniregctb.contribuableAssocie.view.ContribuableAssocieListView"--%>
	    <%--@elvariable id="parametresApp" type="ch.vd.uniregctb.param.view.ParamApplicationView"--%>

	    <!-- Caractéristiques générales -->
	    <c:set var="titre"><fmt:message key="caracteristiques.debiteur.is"/></c:set>
	    <unireg:bandeauTiers numero="${command.numeroDpi}" titre="${titre}" showValidation="false" showEvenementsCivils="false" showLinks="false" showAvatar="false" showComplements="true"/>

	    <!-- Formulaire de recherche -->
	    <form:form method="get" id="formRechercheTiers" action="list.do" >
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:hidden path="numeroDpi"/>
				<form:errors  cssClass="error"/>
				<jsp:include page="../../recherche/form.jsp">
					<jsp:param name="typeRecherche" value="rapport" />
					<jsp:param name="prefixeEffacer" value="/contribuable-associe" />
					<jsp:param name="paramsEffacer" value="numeroDpi:${command.numeroDpi}" />
				</jsp:include>
			</fieldset>
		</form:form>

	    <!-- Résultats -->
		<display:table 	name="list" id="tiers" pagesize="${parametresApp.nbMaxParPage}" requestURI="list.do" class="display" sort="list">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncun.tiers.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.tiers.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.tiers.trouves" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.tiers.trouves" /></span></display:setProperty>

			<display:column sortable ="true" titleKey="label.numero.tiers" sortProperty="numero" >
				<c:if test="${tiers.annule}"><strike></c:if>
					<a href="edit.do?numeroDpi=${command.numeroDpi}&numeroContribuable=${tiers.numero}"><unireg:numCTB numero="${tiers.numero}" /></a>
				<c:if test="${tiers.annule}"></strike></c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.role" >
				<c:if test="${tiers.annule}"><strike></c:if>
					<c:out value="${tiers.roleLigne1}" />
					<c:if test="${tiers.roleLigne2 != null}">
						<br><c:out value="${tiers.roleLigne2}" />
					</c:if>
				<c:if test="${tiers.annule}"></strike></c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.nom.raison" >
				<c:if test="${tiers.annule}"><strike></c:if>
					<c:out value="${tiers.nom1}" />
					<c:if test="${tiers.nom2 != null}">
						<br><c:out value="${tiers.nom2}" />
					</c:if>
				<c:if test="${tiers.annule}"></strike></c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.date.naissance.ou.rc" sortProperty="dateNaissanceInscriptionRC">
				<c:if test="${tiers.annule}"><strike></c:if>
					<unireg:date date="${tiers.dateNaissanceInscriptionRC}"/>
				<c:if test="${tiers.annule}"></strike></c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.localitePays" >
				<c:if test="${tiers.annule}"><strike></c:if>
					<c:out value="${tiers.localiteOuPays}" />
				<c:if test="${tiers.annule}"></strike></c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.for.principal" >
				<c:if test="${tiers.annule}"><strike></c:if>
					<c:out value="${tiers.forPrincipal}" />
				<c:if test="${tiers.annule}"></strike></c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.date.ouverture.for" sortProperty="dateOuvertureFor">
				<c:if test="${tiers.annule}"><strike></c:if>
					<fmt:formatDate value="${tiers.dateOuvertureFor}" pattern="dd.MM.yyyy"/>
				<c:if test="${tiers.annule}"></strike></c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.date.fermeture.for" sortProperty="dateFermetureFor">
				<c:if test="${tiers.annule}"><strike></c:if>
					<fmt:formatDate value="${tiers.dateFermetureFor}" pattern="dd.MM.yyyy"/>
				<c:if test="${tiers.annule}"></strike></c:if>
			</display:column>
		</display:table>
		
		<table border="0">
			<tr>
				<td>
					<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="document.location.href='../rapports-prestation/edit.do?id=${command.numeroDpi}';" />
				</td>
			</tr>
		</table>
		
	</tiles:put>
</tiles:insert>
