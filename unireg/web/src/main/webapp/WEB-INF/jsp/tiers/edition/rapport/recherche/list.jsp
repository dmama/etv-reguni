<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="numeroTiers" value="${param.numero}" />

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title">
		  	<fmt:message key="title.recherche.tiers.lie" />
  	</tiles:put>
  	<tiles:put name="body">
	    <%--@elvariable id="command" type="ch.vd.uniregctb.rapport.view.RapportListView"--%>
	    <unireg:bandeauTiers numero="${command.tiersId}" showAvatar="true" showValidation="false" showEvenementsCivils="false" showLinks="false" showComplements="false"/>

		<unireg:nextRowClass reset="1"/>
	    <form:form method="get" id="formRechercheTiers">
		    <form:hidden path="tiersId"/>
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors  cssClass="error"/>
				<jsp:include page="../../../recherche/form.jsp">
					<jsp:param name="typeRecherche" value="rapport" />
					<jsp:param name="prefixeEffacer" value="/rapport" />
					<jsp:param name="paramsEffacer" value="tiersId:${command.tiersId}"/>
				</jsp:include>		
			</fieldset>
		</form:form>

		<display:table 	name="list" id="tiers" pagesize="25" requestURI="/rapport/search.do" class="display" sort="list" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncun.tiers.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.tiers.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.tiers.trouves" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.tiers.trouves" /></span></display:setProperty>

			<display:column sortable ="true" titleKey="label.numero.tiers" sortProperty="numero" >
				<c:set var="noctb"><unireg:numCTB numero="${tiers.numero}"/></c:set>
				<unireg:linkTo name="${noctb}" action="/rapport/edit.do" params="{numeroTiers:${command.tiersId},numeroTiersLie:${tiers.numero}}"/>
			</display:column>
			<display:column sortable ="true" titleKey="label.role" >
				<c:out value="${tiers.roleLigne1}" />
				<c:if test="${tiers.roleLigne2 != null}">
					<br><c:out value="${tiers.roleLigne2}" />
				</c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.nom.raison" >
				<c:out value="${tiers.nom1}" />
				<c:if test="${tiers.nom2 != null}">
					<br><c:out value="${tiers.nom2}" />
				</c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.date.naissance" sortProperty="dateNaissanceInscriptionRC">
				<unireg:date date="${tiers.dateNaissanceInscriptionRC}"/>
			</display:column>
			<display:column sortable ="true" titleKey="label.localitePays" >
				<c:out value="${tiers.localiteOuPays}" />
			</display:column>
			<display:column sortable ="true" titleKey="label.for.principal" >
				<c:out value="${tiers.forPrincipal}" />
			</display:column>
			<display:column sortable ="true" titleKey="label.date.ouverture.for" sortProperty="dateOuvertureFor">
				<fmt:formatDate value="${tiers.dateOuvertureFor}" pattern="dd.MM.yyyy"/>
			</display:column>
			<display:column sortable ="true" titleKey="label.date.fermeture.for" sortProperty="dateFermetureFor">
				<fmt:formatDate value="${tiers.dateFermetureFor}" pattern="dd.MM.yyyy"/>
			</display:column>
		</display:table>

	    <!-- Debut Bouton -->
	    <table border="0">
		    <tr><td>
			    <unireg:buttonTo name="Retour" action="/dossiers-apparentes/edit.do" params="{id:${command.tiersId}}" method="GET"/>
		    </td></tr>
	    </table>
	    <!-- Fin Bouton -->

	</tiles:put>
</tiles:insert>
