<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.recherche.debiteur" /></tiles:put>
	<tiles:put name="fichierAide">
		<li>
			<a href="#" onClick="ouvrirAide('<c:url value='/docs/gestion-debiteurIS.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
		</li>
	</tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>

	    <%--@elvariable id="debiteurCriteriaView" type="ch.vd.unireg.rt.view.DebiteurListView"--%>
	    <unireg:bandeauTiers numero="${debiteurCriteriaView.numeroSourcier}" titre="label.caracteristiques.sourcier" showValidation="false" showEvenementsCivils="false" showLinks="false" showAvatar="false" showComplements="true"/>

	    <form:form method="get" id="formRechercheTiers" commandName="debiteurCriteriaView">
		    <form:hidden path="numeroSourcier"/>
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors  cssClass="error"/>
				<jsp:include page="../../../recherche/form.jsp">
					<jsp:param name="typeRecherche" value="rt-debiteur" />
					<jsp:param name="prefixeEffacer" value="/rt/debiteur" />
					<jsp:param name="paramsEffacer" value="numeroSrc:${debiteurCriteriaView.numeroSourcier}"/>
				</jsp:include>
			</fieldset>
		</form:form>

		<display:table name="list" id="debiteur" pagesize="25" requestURI="/rapports-prestation/search-debiteur.do" class="display" sort="list">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncun.debiteur.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.debiteur.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.debiteurs.trouves" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.debiteurs.trouves" /></span></display:setProperty>

			<display:column sortable="true" titleKey="label.numero.debiteur" sortProperty="numero" >
				<c:set var="idDpi">
					<unireg:numCTB numero="${debiteur.numero}"/>
				</c:set>
				<unireg:linkTo name="${idDpi}" action="/rapports-prestation/add.do" params="{numeroSrc:${debiteurCriteriaView.numeroSourcier},numeroDpi:${debiteur.numero}}" title="Sélectionner ce débiteur"/>
			</display:column>
			<display:column sortable="true" titleKey="label.nom.raison" >
				<c:out value="${debiteur.nom1}" />
				<c:if test="${debiteur.nom2 != null}">
					<br><c:out value="${debiteur.nom2}" />
				</c:if>
			</display:column>
			<display:column sortable="true" titleKey="label.debiteur.is" >
				<c:if test="${debiteur.roleLigne2 != null}">
					<c:out value="${debiteur.roleLigne2}" />
				</c:if>
			</display:column>
			<display:column sortable="true" titleKey="label.date.naissance" sortProperty="dateNaissanceInscriptionRC">
				<unireg:date date="${debiteur.dateNaissanceInscriptionRC}"/>
			</display:column>
			<display:column property="localiteOuPays" sortable="true" titleKey="label.localitePays"  />
		</display:table>

		<table border="0">
			<tr>
				<td>
					<unireg:buttonTo name="Retour" action="/rapport/list.do" params="{id:${debiteurCriteriaView.numeroSourcier}}" method="GET"/>
				</td>
			</tr>
		</table>
			
	</tiles:put>
</tiles:insert>
