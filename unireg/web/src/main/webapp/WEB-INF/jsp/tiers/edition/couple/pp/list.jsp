<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<c:set var="numeroPP1" value="${param.numeroPP1}" />

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title">
  		<c:if test="${numeroPP1 == null}">
  			<fmt:message key="title.recherche.premiere.pp" />
  		</c:if>
  		<c:if test="${numeroPP1 != null}">
		  	<fmt:message key="title.recherche.seconde.pp" />
		</c:if>
  	</tiles:put>
  	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/creation-couple.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
  	<tiles:put name="body">
	  	<c:if test="${!command.allowed}">
	  		<span class="error"><fmt:message key="error.tiers.droit.modification" /></span>
	  	</c:if>
	  	<c:if test="${numeroPP1 != null}">
			<jsp:include page="../../../../general/pp.jsp">
				<jsp:param name="page" value="couple" />
				<jsp:param name="path" value="premierePersonne" />
			</jsp:include>
		</c:if>
		
		<unireg:nextRowClass reset="1"/>
	    <form:form method="post" id="formRecherchePP">
		  	<c:if test="${numeroPP1 != null}">
		  		<form:checkbox path="conjointInconnu" id="conjointInconnu" value="true" onclick="conjointInconnuHandle()"/><label for="conjointInconnu">&nbsp;<fmt:message key="label.conjoint.inconnu"/></label>
		  		<script type="text/javascript" language="Javascript">
		  			function conjointInconnuHandle() {
		  				togglePanels('conjointInconnu', 'searchPanel', 'marieSeulPanel')
		  			}

		  			$(function() {
		  				conjointInconnuHandle();
		  			});
		  		</script>
		  	</c:if>
			<div id="searchPanel">
				<fieldset>
					<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
					<form:errors  cssClass="error"/>
					<jsp:include page="../../../recherche/form.jsp">
						<jsp:param name="typeRecherche" value="couple" />
					</jsp:include>
				</fieldset>
	
				<display:table 	name="list" id="row" pagesize="25" requestURI="/couple/list-pp.do" class="display" sort="list">
					<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncune.personne.trouvee" /></span></display:setProperty>
					<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.personne.trouvee" /></span></display:setProperty>
					<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.personnes.trouvees" /></span></display:setProperty>
					<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.personnes.trouvees" /></span></display:setProperty>
		
					<display:column sortable ="true" titleKey="label.numero.contribuable" sortProperty="numero" >
						<c:if test="${numeroPP1 == null}">
							<a href="list-pp.do?numeroPP1=${row.numero}"><unireg:numCTB numero="${row.numero}" /></a>
						</c:if>
						<c:if test="${numeroPP1 != null}">
							<a href="recap.do?numeroPP1=${numeroPP1}&numeroPP2=${row.numero}"><unireg:numCTB numero="${row.numero}" /></a>
						</c:if>				
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
			</div>
			<div id="marieSeulPanel" style="display: none;">
				<input type="submit" value="<fmt:message key="label.bouton.poursuivre"/>" name="poursuivre"/>
			</div>
		</form:form>
		
	</tiles:put>
</tiles:insert>
