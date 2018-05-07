<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">
		<fmt:message key="title.traitement.scission.entreprise">
			<fmt:param>
				<unireg:numCTB numero="${scission.idEntrepriseScindee}"/>
			</fmt:param>
		</fmt:message>
	</tiles:put>

  	<tiles:put name="body">

	    <unireg:bandeauTiers numero="${scission.idEntrepriseScindee}" titre="label.caracteristiques.scission.entreprise.scindee" showAvatar="true" showValidation="false" showEvenementsCivils="false" showLinks="false" showComplements="true"/>

	    <fieldset>
		    <unireg:nextRowClass reset="0"/>
		    <legend><span><fmt:message key="label.caracteristiques.scission.entreprise" /></span></legend>
		    <table>
			    <tr class="<unireg:nextRowClass/>" >
				    <td width="25%"><fmt:message key="label.date.contrat.scission" />&nbsp;:</td>
				    <td width="75%"><unireg:regdate regdate="${scission.dateContratScission}"/></td>
			    </tr>
		    </table>
	    </fieldset>

	    <fieldset>
		    <unireg:nextRowClass reset="0"/>
		    <legend><span><fmt:message key="label.caracteristiques.entreprises.resultantes" /></span></legend>
		    <c:if test="${not empty scission.entreprisesResultantes}">
			    <c:choose>
				    <c:when test="${fn:length(scission.entreprisesResultantes) == 1}">
					    <span style="font-style: italic;">1 <fmt:message key="label.entreprise.selectionnee"/></span>
				    </c:when>
				    <c:otherwise>
					    <span style="font-style: italic;">${fn:length(scission.entreprisesResultantes)} <fmt:message key="label.entreprises.selectionnees"/></span>
				    </c:otherwise>
			    </c:choose>
		    </c:if>

		    <display:table name="scission.entreprisesResultantes" id="resultante">
			    <display:column titleKey="label.numero.contribuable">
				    <unireg:numCTB numero="${resultante.id}"/>
			    </display:column>
			    <display:column titleKey="label.numero.ide">
				    <unireg:numIDE numeroIDE="${resultante.numeroIDE}"/>
			    </display:column>
			    <display:column titleKey="label.date.inscription.rc">
				    <unireg:regdate regdate="${resultante.dateInscription}"/>
				    <c:if test="${resultante.dateInscription != scission.dateContratScission}">
					    <span class="warning_icon" title="Date différente de la date de contrat de scission">&nbsp;</span>
				    </c:if>
			    </display:column>
			    <display:column titleKey="label.raison.sociale" property="raisonSociale"/>
			    <display:column titleKey="label.siege" property="nomSiege"/>
			    <display:column titleKey="label.forme.juridique" property="formeJuridique"/>
			    <display:column titleKey="label.etat.entreprise.actuel">
				    <c:if test="${resultante.etatActuel != null}">
					    <fmt:message key="option.etat.entreprise.${resultante.etatActuel}"/>
				    </c:if>
			    </display:column>
			    <display:column titleKey="label.action">
				    <unireg:raccourciAnnuler tooltip="Retirer de la liste" onClick="ScissionEntreprise.retirerEntrepriseResultante(${resultante.id});"/>
			    </display:column>
		    </display:table>

		    <span id="raccourciAjouter">
			    <unireg:raccourciAjouter display="label.bouton.ajouter" tooltip="label.bouton.ajouter" onClick="ScissionEntreprise.showRechercheResultante();"/>
		    </span>

		    <script type="application/javascript">
			    var ScissionEntreprise = {
				    showRechercheResultante: function() {
					    $('#rechercheResultante').show();
					    $('#raccourciAjouter').hide();
				    },
				    retirerEntrepriseResultante: function(id) {
					    Form.dynamicSubmit('post', App.curl('/processuscomplexe/scission/resultantes/remove.do'), {id:id});
				    }
			    }
		    </script>

		    <c:if test="${not empty scission.entreprisesResultantes}">
			    <c:choose>
				    <c:when test="${scission.toutesDatesInscriptionEnPhaseAvecDateContratScission}">
					    <c:set var="questionValidation">
						    <fmt:message key="label.scission.validation.question.dates.alignees"/>
					    </c:set>
				    </c:when>
				    <c:otherwise>
					    <c:set var="questionValidation">
						    <fmt:message key="label.scission.validation.question.dates.non.alignees"/>
					    </c:set>
				    </c:otherwise>
			    </c:choose>
			    <unireg:buttonTo name="Valider" action="/processuscomplexe/scission/scinder.do" confirm="${questionValidation}"/>
		    </c:if>

		    <div style="display: none;" id="rechercheResultante">
			    <c:set var="searchUrl">
				    <c:url value="/processuscomplexe/scission/resultantes/list.do?searched=true"/>
			    </c:set>
			    <form:form method="post" id="formRecherche" action="${searchUrl}">
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
						    <jsp:param name="typeRecherche" value="scissionEntreprise" />
						    <jsp:param name="prefixeEffacer" value="/processuscomplexe/scission/resultantes"/>
					    </jsp:include>
				    </fieldset>
			    </form:form>

			    <display:table name="list" id="row" pagesize="25" requestURI="/processuscomplexe/scission/resultantes/list.do" class="display" sort="list">
				    <display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncune.entreprise.trouvee" /></span></display:setProperty>
				    <display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.entreprise.trouvee" /></span></display:setProperty>
				    <display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.entreprises.trouvees" /></span></display:setProperty>
				    <display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.entreprises.trouvees" /></span></display:setProperty>

				    <display:column sortable="true" titleKey="label.numero.contribuable" sortProperty="numero" >
					    <c:choose>
						    <c:when test="${row.selectionnable}">
							    <c:set var="noctb">
								    <unireg:numCTB numero="${row.numero}"/>
							    </c:set>
							    <unireg:linkTo name="${noctb}" action="/processuscomplexe/scission/resultantes/add.do" method="post" params="{id:${row.numero}}" title="Ajouter à la liste des entreprises résultantes"/>
						    </c:when>
						    <c:when test="${row.explicationNonSelectionnable != null}">
							    <span title="${row.explicationNonSelectionnable}">
								    <unireg:numCTB numero="${row.numero}"/>
							    </span>
						    </c:when>
						    <c:otherwise>
							    <unireg:numCTB numero="${row.numero}"/>
						    </c:otherwise>
					    </c:choose>
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
		    </div>

		    <c:if test="${param.searched}">
			    <script type="application/javascript">
			    	$(function() {
					    ScissionEntreprise.showRechercheResultante();
				    });
		        </script>
		    </c:if>

	    </fieldset>

	    <unireg:RetourButton link="../retour-choix-date.do" checkIfModified="true"/>

	</tiles:put>
</tiles:insert>