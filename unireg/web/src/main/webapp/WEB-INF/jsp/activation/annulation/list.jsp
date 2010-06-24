<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<c:set var="activation" value="${param.activation}" />

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title">
  		<fmt:message key="title.annulation.recherche.tiers.remplacant" />
  	</tiles:put>
    
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
	    <form:form method="post" id="formRecherche">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors  cssClass="error"/>
				<jsp:include page="../../tiers/recherche/form.jsp">
					<jsp:param name="typeRecherche" value="activation" />
				</jsp:include>
			</fieldset>
		</form:form>
	
	<script type="text/javascript" language="Javascript">
		function selectTiers(numero) {
			var params = tb_parseQuery(top.location.search.replace(/^[^\?]+\??/,''));
			var refreshURL = top.document.URL;
			if (params['numeroRemplacant']) {
				refreshURL=refreshURL.replace(new RegExp(params['numeroRemplacant'], ""), numero);
			}
			else {
				refreshURL+="&numeroRemplacant="+numero;
			}
			top.location.href=refreshURL;
		}
	</script>
			
		<display:table 	name="list" id="row" pagesize="25" requestURI="/activation/list.do" class="display" sort="list">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncun.tiers.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.tiers.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.tiers.trouves" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.tiers.trouves" /></span></display:setProperty>

			<display:column sortable ="true" titleKey="label.numero.tiers" sortProperty="numero" >
				<a href="#" onclick="selectTiers(${row.numero})"><unireg:numCTB numero="${row.numero}" /></a>
			</display:column>
			<display:column sortable ="true" titleKey="label.role" >
				<c:out value="${row.roleLigne1}" />
				<c:if test="${row.roleLigne2 != null}">
					<br><c:out value="${row.roleLigne2}" />
				</c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.nom.raison" >
				<c:out value="${row.nom1}" />
				<c:if test="${row.nom2 != null}">
					<br><c:out value="${row.nom2}" />
				</c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.date.naissance" sortProperty="dateNaissance">
				<unireg:date date="${row.dateNaissance}"></unireg:date>
			</display:column>
			<display:column property="localiteOuPays" sortable ="true" titleKey="label.localitePays"  />
			<display:column sortable ="true" titleKey="label.for.principal" >
				<c:out value="${row.forPrincipal}" />
			</display:column>
			<display:column sortable ="true" titleKey="label.date.ouverture.for" sortProperty="dateOuvertureFor">
				<fmt:formatDate value="${row.dateOuvertureFor}" pattern="dd.MM.yyyy"/>
			</display:column>
			<display:column sortable ="true" titleKey="label.date.fermeture.for" sortProperty="dateFermetureFor">
				<fmt:formatDate value="${row.dateFermetureFor}" pattern="dd.MM.yyyy"/>
			</display:column>
		</display:table>
	</tiles:put>
</tiles:insert>