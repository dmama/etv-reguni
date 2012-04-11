<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
    <tiles:put name="head">
        <script type="text/javascript">
             $(document).ready(function () {
                 $('#modeLotEvenement').change( function () {
                    if (this.checked) {
                        $('#tableForm tr.toggle').hide()
                    } else {
                        $('#tableForm tr.toggle').show()
                    }
                 }).change();

                 $('#rechercher').click( function () {
                 	$('#formRechercheEvenements').attr('action','rechercher.do');
                 })

                 $('#effacer').click( function () {
                 	window.location.href = 'effacer.do';
                 	return false;
                 })

             });
        </script>
    </tiles:put>

  	<tiles:put name="title"><fmt:message key="title.recherche.evenements.ech" /></tiles:put>
  	<tiles:put name="fichierAide">
		<a href="#" onClick="ouvrirAide('<c:url value='/docs/recherche.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
	    <form:form method="get" id="formRechercheEvenements" commandName="evenementEchCriteria">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors  cssClass="error"/>
				<jsp:include page="form.jsp"/>
			</fieldset>
		</form:form>

        <c:set var="sortable" value='${not evenementEchCriteria.modeLotEvenement}' scope="page" />
		<display:table 	name="listEvenementsEch" id="tableEvtsEch" pagesize="25" requestURI="/evenement/ech/nav-list.do" defaultsort="1" defaultorder="descending" sort="external" class="display_table" partialList="true" size="listEvenementsEchSize">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncun.evenement.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner"><fmt:message key="banner.un.evenement.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.evenements.trouves" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.evenements.trouves" /></span></display:setProperty>

			<!-- ID -->
			<display:column property="id" sortable ="${sortable}" titleKey="label.evenement" href="visu.do" paramId="id" paramProperty="id" sortName="id" />
			<!-- NO Individu + Conjoint -->
			<display:column sortable ="${sortable}" titleKey="label.individu" sortProperty="numeroIndividu" sortName="numeroIndividu">
				${tableEvtsEch.numeroIndividu}
			</display:column>
			<!-- NO CTB -->
			<display:column titleKey="label.numero.contribuable">
				<c:if test="${tableEvtsEch.numeroCTB != null}">
					<unireg:numCTB numero="${tableEvtsEch.numeroCTB}" />
				</c:if>
			</display:column>
			<!-- Nom  /Prénom -->
			<display:column titleKey="label.prenom.nom">
				<c:out value="${tableEvtsEch.nom}" />
			</display:column>
			<!-- Type evt -->
			<display:column sortable ="${sortable}" titleKey="label.type.evenement" sortName="type">
				<fmt:message key="option.type.evenement.ech.${tableEvtsEch.type}" />
			</display:column>
			<!-- Type evt -->
			<display:column sortable ="${sortable}" titleKey="label.action.evenement" sortName="action">
				<fmt:message key="option.action.evenement.ech.${tableEvtsEch.action}" />
			</display:column>
			<!-- Date evenement -->
			<display:column sortable ="${sortable}" titleKey="label.date.evenement" sortName="dateEvenement">
				<unireg:regdate regdate="${tableEvtsEch.dateEvenement}" />
			</display:column>
			<!-- Date traitement -->
			<display:column property="dateTraitement" sortable ="${sortable}" titleKey="label.date.traitement" format="{0,date,dd.MM.yyyy}" sortName="dateTraitement" />
			<!-- Status evt -->
			<display:column sortable ="${sortable}" titleKey="label.etat.evenement" sortName="etat" >
				<fmt:message key="option.etat.evenement.${tableEvtsEch.etat}" />
			</display:column>
			<display:column titleKey="label.commentaire.traitement">
				<i><c:out value="${tableEvtsEch.commentaireTraitement}"/></i>
			</display:column>
			<display:column style="action">
				<c:if test="${tableEvtsEch.id != null}">
					<unireg:consulterLog entityNature="Evenement" entityId="${tableEvtsEch.id}"/>
				</c:if>
		</display:column>
		</display:table>
	</tiles:put>
</tiles:insert>
