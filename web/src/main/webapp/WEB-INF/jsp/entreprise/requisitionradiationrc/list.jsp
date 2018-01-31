<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title">
		<fmt:message key="title.requisition.radiation.rc.recherche" />
  	</tiles:put>
  	<tiles:put name="body">
	    <form:form method="post" id="formRecherche">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors  cssClass="error"/>
				<c:if test="${errorMessage != null}">
					<span class="error">
						<c:out value="${errorMessage}"/>
					</span>
				</c:if>
				<form:hidden path="typeTiers"/>
				<unireg:nextRowClass reset="0"/>
				<jsp:include page="../../tiers/recherche/form.jsp">
					<jsp:param name="typeRecherche" value="reqRadiationRC" />
					<jsp:param name="prefixeEffacer" value="/processuscomplexe/requisitionradiationrc"/>
				</jsp:include>
			</fieldset>
		</form:form>

		<display:table name="list" id="row" pagesize="25" requestURI="/processuscomplexe/requisitionradiationrc/list.do" class="display" sort="list">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncune.entreprise.trouvee" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.entreprise.trouvee" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.entreprises.trouvees" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.entreprises.trouvees" /></span></display:setProperty>

			<display:column sortable="true" titleKey="label.numero.contribuable" sortProperty="numero" >
				<a href="<c:url value="/processuscomplexe/requisitionradiationrc/start.do"/>?id=${row.numero}"><unireg:numCTB numero="${row.numero}" /></a>
			</display:column>
			<display:column sortable="true" titleKey="label.numero.ide" sortProperty="numeroIDE">
				<unireg:numIDE numeroIDE="${row.numeroIDE}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.date.inscription.rc" sortProperty="dateNaissanceInscriptionRC">
				<unireg:date date="${row.dateNaissanceInscriptionRC}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.raison.sociale" property="nom1"/>
			<display:column sortable="true" titleKey="label.siege" property="domicileEtablissementPrincipal"/>
			<display:column sortable="true" titleKey="label.forme.juridique" sortProperty="formeJuridique.code">
				<c:if test="${row.formeJuridique != null}">
					<c:out value="${row.formeJuridique}"/>
				</c:if>
			</display:column>
			<display:column sortable="true" titleKey="label.etat.entreprise.actuel">
				<c:if test="${row.etatEntreprise != null}">
					<fmt:message key="option.etat.entreprise.${row.etatEntreprise}"/>
				</c:if>
			</display:column>
		</display:table>
	</tiles:put>
</tiles:insert>