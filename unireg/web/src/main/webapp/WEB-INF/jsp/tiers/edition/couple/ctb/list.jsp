<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<tiles:insert template="/WEB-INF/jsp/templates/templateIFrame.jsp">
	<tiles:put name="head"></tiles:put>
  	<tiles:put name="title"></tiles:put>
  	
  	<tiles:put name="body">
		<h1><fmt:message key="title.recherche.contribuable.existant" /></h1>
	  	<c:if test="${numeroPP1 != null}">
			<jsp:include page="../../../../general/pp.jsp">
				<jsp:param name="page" value="couple" />
				<jsp:param name="path" value="premierePersonne" />
			</jsp:include>
		</c:if>
		
		<c:set var="ligneTableau" value="${1}" scope="request" />
	    <c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	    <form:form method="post" id="formRechercheCTB">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors  cssClass="error"/>
				<jsp:include page="../../../recherche/form.jsp">
					<jsp:param name="typeRecherche" value="couple" />
				</jsp:include>
			</fieldset>

			<script type="text/javascript" language="Javascript">
				function selectCtb(numero) {
					var params = tb_parseQuery(top.location.search.replace(/^[^\?]+\??/,''));
					var refreshURL = top.document.URL;
					if (params['numeroCTB']) {
						refreshURL=refreshURL.replace(new RegExp(params['numeroCTB'], ""), numero);
					}
					else {
						refreshURL+="&numeroCTB="+numero;
					}
					top.location.href=refreshURL;
				}
			</script>
			
			<display:table name="list" id="row" pagesize="5" requestURI="/couple/list-ctb.do" class="display" sort="list">
				<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncun.contribuable.trouvee" /></span></display:setProperty>
				<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.contribuable.trouve" /></span></display:setProperty>
				<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.contribuables.trouves" /></span></display:setProperty>
				<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.contribuables.trouves" /></span></display:setProperty>
	
				<display:column sortable ="true" titleKey="label.numero.contribuable" sortProperty="numero" >
					<a href="#" onclick="selectCtb(${row.numero})"><unireg:numCTB numero="${row.numero}" /></a>
				</display:column>
				<display:column sortable ="true" titleKey="label.prenom.nom" >
					<c:out value="${row.nom1}" />
					<c:if test="${row.nom2 != null}">
						<br><c:out value="${row.nom2}" />
					</c:if>
				</display:column>
				<display:column sortable ="true" titleKey="label.date.naissance" sortProperty="dateNaissance">
					<unireg:date date="${row.dateNaissance}"></unireg:date>
				</display:column>
				<display:column property="localiteOuPays" sortable ="true" titleKey="label.localitePays"  />
			</display:table>
		</form:form>
		<br>
		<div style="text-align: center;">
			<input type="button" id="annuler" value="<fmt:message key="label.bouton.annuler" />" onclick="self.parent.tb_remove()">
		</div>
				
	</tiles:put>
</tiles:insert>
