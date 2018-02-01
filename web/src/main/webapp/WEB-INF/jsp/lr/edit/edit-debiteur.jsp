<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="idDebiteur" type="java.lang.Long"--%>
<%--@elvariable id="listesRecapitulatives" type="java.util.List<ch.vd.unireg.lr.view.ListeRecapitulativeView>"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.edition.lrs.debiteur" /></tiles:put>
	<tiles:put name="fichierAide">
		<li>
			<a href="#" onClick="ouvrirAide('<c:url value='/docs/listes-recapitulatives.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
		</li>
	</tiles:put>
	
	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>

		<!-- Debut Caracteristiques generales -->
		<jsp:include page="../../general/debiteur.jsp">
			<jsp:param name="idDebiteur" value="${idDebiteur}" />
		</jsp:include>
		<!-- Fin Caracteristiques generales -->

		<!-- Debut Liste de LRs -->
		<fieldset>
			<legend><span><fmt:message key="caracteristiques.lr" /></span></legend>
			<table border="0">
				<tr>
					<td>
						<unireg:raccourciAjouter link="add-lr.do?idDebiteur=${idDebiteur}" tooltip="label.bouton.ajouter" display="label.bouton.ajouter"/>
						<form:errors cssClass="error"/>
					</td>
				</tr>
			</table>

			<c:if test="${not empty listesRecapitulatives}">
				<display:table name="listesRecapitulatives" id="lr" pagesize="10" requestURI="edit-debiteur.do" class="display" sort="list" decorator="ch.vd.unireg.decorator.TableEntityDecorator">
					<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncune.lr.trouvee" /></span></display:setProperty>
					<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.lr.trouvee" /></span></display:setProperty>
					<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.lrs.trouvees" /></span></display:setProperty>
					<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.lrs.trouvees" /></span></display:setProperty>

					<display:column sortable ="true" titleKey="label.periode" sortProperty="dateDebut">
						<unireg:regdate regdate="${lr.dateDebut}"/>&nbsp;-&nbsp;<unireg:regdate regdate="${lr.dateFin}"/>
					</display:column>
					<display:column sortable ="true" titleKey="label.date.retour" sortProperty="dateRetour">
						<unireg:regdate regdate="${lr.dateRetour}"/>
					</display:column>
					<display:column sortable ="true" titleKey="label.date.delai.accorde" sortProperty="delaiAccorde" >
						<unireg:regdate regdate="${lr.delaiAccorde}"/>
					</display:column>
					<display:column sortable ="true" titleKey="label.etat.avancement" >
						<fmt:message key="option.etat.avancement.f.${lr.etat}" />
					</display:column>
					<display:column style="action">
						<c:if test="${!lr.annule}">
							<unireg:raccourciModifier link="edit-lr.do?id=${lr.id}" tooltip="LR"/>
						</c:if>
					</display:column>
					<display:setProperty name="paging.banner.all_items_found" value=""/>
					<display:setProperty name="paging.banner.one_item_found" value=""/>
				</display:table>
			</c:if>
		</fieldset>
		<!-- Fin Liste de LRs -->

		<!-- Debut Bouton -->
		<c:set var="RetourLabel"><fmt:message key="label.bouton.retour" /></c:set>
		<unireg:buttonTo name="${RetourLabel}" action="/tiers/visu.do?id=${idDebiteur}" method="get"/>
		<!-- Fin Bouton -->
	</tiles:put>
</tiles:insert>
