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
                 });

                 $('#effacer').click( function () {
                 	window.location.href = 'effacer.do';
                 	return false;
                 });

             });
        </script>
    </tiles:put>

  	<tiles:put name="title"><fmt:message key="title.recherche.evenements.organisation" /></tiles:put>
  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onClick="ouvrirAide('<c:url value='/docs/evenements.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
  	<tiles:put name="body">

	    <c:choose>
		    <c:when test="${capping == 'EN_ERREUR'}">
			    <div class="flash-error"><fmt:message key="label.traitement.systematique.erreur"/></div>
		    </c:when>
		    <c:when test="${capping == 'A_VERIFIER'}">
			    <div class="flash-warning"><fmt:message key="label.traitement.systematique.aVerifier"/></div>
		    </c:when>
		    <c:otherwise>
			    <!-- rien à afficher -->
		    </c:otherwise>
	    </c:choose>

		<unireg:nextRowClass reset="1"/>
	    <form:form method="get" id="formRechercheEvenements" commandName="evenementOrganisationCriteria">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors  cssClass="error"/>
				<jsp:include page="form.jsp"/>
			</fieldset>
		</form:form>

        <c:set var="sortable" value='${not evenementOrganisationCriteria.modeLotEvenement}' scope="page" />
		<display:table 	name="listEvenementsOrganisation" id="tableEvtsOrganisation" pagesize="25" requestURI="/evenement/organisation/nav-list.do" defaultsort="1" defaultorder="descending" sort="external" class="display_table" partialList="true" size="listEvenementsOrganisationSize">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncun.evenement.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner"><fmt:message key="banner.un.evenement.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.evenements.trouves" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.evenements.trouves" /></span></display:setProperty>

			<display:column >
				<a href="#" class="jTip-evtinfo infotraitement" title="<c:url value="/evenement/organisation/summary.do?id=${tableEvtsOrganisation.id}"/>" id="messages-${tableEvtsOrganisation.id}" ></a>
			</display:column>

			<!-- No Evenement -->
			<display:column property="noEvenement" sortable ="${sortable}" titleKey="label.no.evenement" href="visu.do" paramId="id" paramProperty="id" sortName="noEvenement" />
			<!-- NO Organisation -->
			<display:column titleKey="label.no.cantonal">
				${tableEvtsOrganisation.numeroOrganisation}
			</display:column>
			<!-- NO CTB -->
			<display:column titleKey="label.numero.contribuable">
				<c:if test="${tableEvtsOrganisation.numeroCTB != null}">
					<a href="<c:url value="../../tiers/visu.do"/>?id=${tableEvtsOrganisation.numeroCTB}"><unireg:numCTB numero="${tableEvtsOrganisation.numeroCTB}" /></a>
				</c:if>
			</display:column>
			<!-- Raison sociale -->
			<display:column titleKey="label.raison.sociale">
				<c:out value="${tableEvtsOrganisation.nom}" />
			</display:column>
			<!-- Siège -->
			<display:column titleKey="label.siege">
				<c:choose>
					<c:when test="${tableEvtsOrganisation.organisation.typeSiege == 'COMMUNE_OU_FRACTION_VD'}">
						<unireg:commune ofs="${tableEvtsOrganisation.organisation.noOFSSiege}" date="${tableEvtsOrganisation.dateEvenement}" displayProperty="nomOfficiel" titleProperty="noOFS"/>
					</c:when>
					<c:when test="${tableEvtsOrganisation.organisation.typeSiege == 'COMMUNE_HC'}">
						<unireg:commune ofs="${tableEvtsOrganisation.organisation.noOFSSiege}" date="${tableEvtsOrganisation.dateEvenement}" displayProperty="nomOfficielAvecCanton" titleProperty="noOFS"/>
					</c:when>
					<c:when test="${tableEvtsOrganisation.organisation.typeSiege == 'PAYS_HS'}">
						<unireg:pays ofs="${tableEvtsOrganisation.organisation.noOFSSiege}" date="${tableEvtsOrganisation.dateEvenement}" displayProperty="nomOfficiel" titleProperty="noOFS"/>
					</c:when>
				</c:choose>
			</display:column>
			<!-- Type evt -->
			<display:column sortable ="${sortable}" titleKey="label.type.evenement" sortName="type">
				<fmt:message key="option.type.evenement.organisation.${tableEvtsOrganisation.type}" />
			</display:column>
			<!-- Date evenement -->
			<display:column sortable ="${sortable}" titleKey="label.date" sortName="dateEvenement">
				<unireg:regdate regdate="${tableEvtsOrganisation.dateEvenement}" />
			</display:column>
			<!-- Date traitement -->
			<display:column property="dateTraitement" sortable ="${sortable}" titleKey="label.date.traitement" format="{0,date,dd.MM.yyyy}" sortName="dateTraitement" />
			<!-- Status evt -->
			<display:column sortable ="${sortable}" titleKey="label.etat.evenement" sortName="etat">
				<fmt:message key="option.etat.evenement.${tableEvtsOrganisation.etat}" />
			</display:column>
			<display:column>
				<c:if test="${tableEvtsOrganisation.correctionDansLePasse == true}">
					<a href="#" class="alert" title="<fmt:message key="label.correction.passe"/>"></a>
				</c:if>
			</display:column>
			<display:column style="action">
				<c:if test="${tableEvtsOrganisation.id != null}">
					<unireg:consulterLog entityNature="EvenementOrganisation" entityId="${tableEvtsOrganisation.id}"/>
				</c:if>
		</display:column>
		</display:table>
	</tiles:put>
</tiles:insert>
<script language="javascript">
	/**
	 * Variante de la version jTip globale avec un positionnement en dessous de la ligne de l'événement concerné.
	 */
	function activate_evtinfo_tooltips() {
		$(".jTip-evtinfo").tooltip({
			                   items: "[title]",
			                   position: { my: "right top", at: "right bottom" },
			                   content: function(response) {
				                   var url = $(this).attr("title");
				                   $.get(url, response);
				                   return "Chargement...";
			                   }
		                   });
	}

	activate_evtinfo_tooltips();
</script>
