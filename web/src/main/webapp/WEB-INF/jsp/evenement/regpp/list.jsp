<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.recherche.evenements.regpp" /></tiles:put>

  	    <tiles:put name="head">
            <script type="text/javascript">
                 $(document).ready(function () {

                     $('#rechercher').click(function () {
	                     $('#formRechercheEvenements').attr('action', 'rechercher.do');
                     });

                     $('#effacer').click( function () {
                     	window.location.href = 'effacer.do';
                     	return false;
                     });

                 });
            </script>
        </tiles:put>

  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onClick="ouvrirAide('<c:url value='/docs/evenements.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
	    <form:form method="get" id="formRechercheEvenements" modelAttribute="evenementCriteria">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors  cssClass="error"/>
				<jsp:include page="form.jsp"/>
			</fieldset>
		</form:form>

		<display:table 	name="listEvenements" id="tableEvts" pagesize="25" requestURI="/evenement/regpp/nav-list.do" defaultsort="1" defaultorder="descending" sort="external" class="display_table" partialList="true" size="listEvenementsSize">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncun.evenement.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner"><fmt:message key="banner.un.evenement.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.evenements.trouves" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.evenements.trouves" /></span></display:setProperty>

			<!-- ID -->
			<display:column property="id" sortable ="true" titleKey="label.evenement" href="visu.do" paramId="id" paramProperty="id" sortName="id" />
			<!-- NO Individu + Conjoint -->
			<display:column sortable ="true" titleKey="label.individu" sortProperty="numeroIndividuPrincipal" sortName="numeroIndividuPrincipal">
				${tableEvts.numeroIndividuPrincipal}
				<c:if test="${tableEvts.numeroIndividuConjoint != null }">
					<br>${tableEvts.numeroIndividuConjoint}
				</c:if>
			</display:column>
			<!-- NO CTB -->
			<display:column titleKey="label.numero.contribuable">
				<c:if test="${tableEvts.numeroCTB != null}">
					<unireg:numCTB numero="${tableEvts.numeroCTB}" />
				</c:if>
			</display:column>
			<!-- Nom  /Prénom -->
			<display:column titleKey="label.prenom.nom">
				<c:out value="${tableEvts.nom1}" />
				<c:if test="${tableEvts.nom2 != null}">
					<br><c:out value="${tableEvts.nom2}" />
				</c:if>
			</display:column>
			<!-- Type evt -->
			<display:column sortable ="true" titleKey="label.type.evenement" sortName="type">
				<fmt:message key="option.type.evenement.${tableEvts.type}" />
			</display:column>
			<!-- Date evenement -->
			<display:column sortable ="true" titleKey="label.date.evenement" sortName="dateEvenement">
				<unireg:regdate regdate="${tableEvts.dateEvenement}" />
			</display:column>
			<!-- Date traitement -->
			<display:column property="dateTraitement" sortable ="true" titleKey="label.date.traitement" format="{0,date,dd.MM.yyyy}" sortName="dateTraitement" />
			<!-- Status evt -->
			<display:column sortable ="true" titleKey="label.etat.evenement" sortName="etat" >
				<fmt:message key="option.etat.evenement.${tableEvts.etat}" />
			</display:column>
			<display:column titleKey="label.commentaire.traitement">
				<i><c:out value="${tableEvts.commentaireTraitement}"/></i>
			</display:column>
			<display:column style="action">
				<c:if test="${tableEvts.id != null}">
					<unireg:consulterLog entityNature="Evenement" entityId="${tableEvts.id}"/>
				</c:if>
		</display:column>
			
		</display:table>

	</tiles:put>
</tiles:insert>
