<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.recherche.nouveaux.dossiers" /></tiles:put>
  	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/nouveaux-dossiers.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
	    <form:form method="post" id="formRechNouvDossier" name="theForm" >
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors  cssClass="error"/>
				<jsp:include page="form.jsp"/>
			</fieldset>

		<div id="desynchro" style="display:none;">
			<FONT COLOR="#FF0000">Attention la recherche est désynchronisée après l'impression</FONT>
		</div>
		<display:table 	name="nouveauxDossiers" id="nouveauDossier" pagesize="25" requestURI="/tache/list-nouveau-dossier.do" sort="external" class="display_table" partialList="true" size="resultSize">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncun.nouveau.dossier.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.nouveau.dossier.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.nouveaux.dossiers.trouves" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.nouveaux.dossiers.trouves" /></span></display:setProperty>

			<display:column title="<input type='checkbox' checked name='selectAll' onclick='javascript:selectAllDossiers(this);' />">
				<c:if test="${!nouveauDossier.annule}">
					<input type="checkbox" checked name="tabIdsDossiers" id="tabIdsDossiers_${nouveauDossier_rowNum}" value="${nouveauDossier.id}" >
				</c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.numero.contribuable" sortProperty="numero" sortName="contribuable.numero">
				<c:if test="${!nouveauDossier.annule}">
					<a href="../tiers/visu.do?id=${nouveauDossier.numero}"><unireg:numCTB numero="${nouveauDossier.numero}"></unireg:numCTB></a>
				</c:if>
				<c:if test="${nouveauDossier.annule}">
					<strike><unireg:numCTB numero="${nouveauDossier.numero}"></unireg:numCTB></strike>
				</c:if>
			</display:column>
			<display:column titleKey="label.nom.raison" >
				<c:if test="${nouveauDossier.annule}"><strike></c:if>
					<c:if test="${nouveauDossier.nomCourrier1 != null }">
						${nouveauDossier.nomCourrier1}
					</c:if>
					<c:if test="${nouveauDossier.nomCourrier2 != null }">
						<br />${nouveauDossier.nomCourrier2}
					</c:if>
				<c:if test="${nouveauDossier.annule}"></strike></c:if>
			</display:column>
			<display:column titleKey="label.for.gestion" >
				<c:if test="${nouveauDossier.annule}"><strike></c:if>
					<unireg:commune ofs="${nouveauDossier.numeroForGestion}" displayProperty="nomMinuscule"/>
				<c:if test="${nouveauDossier.annule}"></strike></c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.etat.tache" sortName="etat">
				<c:if test="${nouveauDossier.annule}"><strike></c:if>
					<fmt:message key="option.etat.tache.${nouveauDossier.etatTache}"  />
				<c:if test="${nouveauDossier.annule}"></strike></c:if>
			</display:column>
			<display:column>
				<unireg:consulterLog entityNature="Tache" entityId="${nouveauDossier.id}"/>
			</display:column>
		</display:table>
		
		<!-- Debut Boutons -->
		<table border="0">
			<tr>
				<td width="25%">&nbsp;</td>
				<td width="50%">
					<div class="navigation-action"><input type="button" name="imprimer" value="<fmt:message key="label.bouton.imprimer"/>" onClick="javascript:confirmeImpression();" />
					</div>
				</td>
				<td width="25%">&nbsp;</td>
			</tr>
		</table>
		<!-- Fin Boutons -->
		
		</form:form>		

		<script type="text/javascript" language="Javascript" src="<c:url value="/js/tache.js"/>"></script>

	</tiles:put>
</tiles:insert>
