<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.recherche.nouveaux.dossiers" /></tiles:put>
  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onClick="ouvrirAide('<c:url value='/docs/nouveaux-dossiers.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
	    <form:form method="post" id="formRechNouvDossier" name="theForm" action="list-nouveau-dossier.do">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors  cssClass="error"/>
				<jsp:include page="form.jsp"/>
			</fieldset>
		</form:form>

	    <form:form method="post" id="formImprimerNouvDossiers" name="imprimerNouvDossier" action="imprimer-nouveaux-dossiers.do">

		<div id="desynchro" style="display:none;">
			<span style="color: red;">Attention la recherche est désynchronisée après l'impression</span>
		</div>
		<display:table 	name="nouveauxDossiers" id="nouveauDossier" pagesize="25" requestURI="/tache/list-nouveau-dossier.do" sort="external" class="display_table" partialList="true" size="resultSize" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncun.nouveau.dossier.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.nouveau.dossier.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.nouveaux.dossiers.trouves" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.nouveaux.dossiers.trouves" /></span></display:setProperty>

			<display:column title="<input type='checkbox' checked name='selectAll' onclick='javascript:selectAllDossiers(this);' />">
				<c:if test="${!nouveauDossier.annule}">
					<input type="checkbox" checked name="tabIdsDossiers" id="tabIdsDossiers_${nouveauDossier_rowNum}" value="${nouveauDossier.id}"/>
				</c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.numero.contribuable" sortProperty="numero" sortName="contribuable.numero">
				<c:choose>
					<c:when test="${nouveauDossier.annule}">
						<unireg:numCTB numero="${nouveauDossier.numero}"/>
					</c:when>
					<c:otherwise>
						<a href="../tiers/visu.do?id=${nouveauDossier.numero}"><unireg:numCTB numero="${nouveauDossier.numero}"/></a>
					</c:otherwise>
				</c:choose>
			</display:column>
			<display:column titleKey="label.nom.raison" >
				<unireg:multiline lines="${nouveauDossier.nomCourrier}"/>
			</display:column>
			<display:column titleKey="label.for.gestion" >
				<unireg:commune ofs="${nouveauDossier.numeroForGestion}" displayProperty="nomOfficiel"/>
			</display:column>
			<display:column sortable ="true" titleKey="label.etat.tache" sortName="etat">
				<fmt:message key="option.etat.tache.${nouveauDossier.etatTache}"  />
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
					<div class="navigation-action"><input type="submit" name="imprimer" value="<fmt:message key="label.bouton.imprimer"/>" onClick="return confirmeImpression();" />
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
