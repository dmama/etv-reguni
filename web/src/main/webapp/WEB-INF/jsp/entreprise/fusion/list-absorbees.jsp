<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">
		<fmt:message key="title.traitement.fusion.entreprises"/>
	</tiles:put>

  	<tiles:put name="body">

	    <c:set var="titreAbsorbante">
		    <fmt:message key="label.caracteristiques.fusion.entreprise.absorbante"/>
	    </c:set>
	    <unireg:bandeauTiers numero="${fusion.idEntrepriseAbsorbante}" showAvatar="true" showValidation="false" showEvenementsCivils="false" showLinks="false" showComplements="true" titre="${titreAbsorbante}"/>

	    <fieldset>
		    <unireg:nextRowClass reset="0"/>
		    <legend><span><fmt:message key="label.caracteristiques.fusion.entreprises" /></span></legend>
		    <table>
			    <tr class="<unireg:nextRowClass/>" >
				    <td width="25%"><fmt:message key="label.date.bilan.fusion" />&nbsp;:</td>
				    <td width="75%"><unireg:regdate regdate="${fusion.dateBilanFusion}"/></td>
			    </tr>
			    <tr class="<unireg:nextRowClass/>" >
				    <td width="25%"><fmt:message key="label.date.contrat.fusion" />&nbsp;:</td>
				    <td width="75%"><unireg:regdate regdate="${fusion.dateContratFusion}"/></td>
			    </tr>
		    </table>
	    </fieldset>

	    <fieldset>
		    <unireg:nextRowClass reset="0"/>
		    <legend><span><fmt:message key="label.caracteristiques.entreprises.absorbees" /></span></legend>
		    <c:if test="${not empty fusion.entreprisesAbsorbees}">
			    <c:choose>
				    <c:when test="${fn:length(fusion.entreprisesAbsorbees) == 1}">
					    <span style="font-style: italic;">1 <fmt:message key="label.entreprise.selectionnee"/></span>
				    </c:when>
				    <c:otherwise>
					    <span style="font-style: italic;">${fn:length(fusion.entreprisesAbsorbees)} <fmt:message key="label.entreprises.selectionnees"/></span>
				    </c:otherwise>
			    </c:choose>
		    </c:if>

		    <display:table name="fusion.entreprisesAbsorbees" id="absorbee">
			    <display:column titleKey="label.numero.contribuable">
				    <unireg:numCTB numero="${absorbee.id}"/>
			    </display:column>
			    <display:column titleKey="label.numero.ide">
				    <unireg:numIDE numeroIDE="${absorbee.numeroIDE}"/>
			    </display:column>
			    <display:column titleKey="label.date.inscription.rc">
				    <unireg:regdate regdate="${absorbee.dateInscription}"/>
			    </display:column>
			    <display:column titleKey="label.raison.sociale" property="raisonSociale"/>
			    <display:column titleKey="label.siege" property="nomSiege"/>
			    <display:column titleKey="label.forme.juridique" property="formeJuridique"/>
			    <display:column titleKey="label.etat.entreprise.actuel">
				    <c:if test="${absorbee.etatActuel != null}">
					    <fmt:message key="option.etat.entreprise.${absorbee.etatActuel}"/>
				    </c:if>
			    </display:column>
			    <display:column titleKey="label.action">
				    <unireg:raccourciAnnuler tooltip="Retirer de la liste" onClick="FusionEntreprises.retirerEntrepriseAbsorbee(${absorbee.id});"/>
			    </display:column>
		    </display:table>

		    <span id="raccourciAjouter">
			    <unireg:raccourciAjouter display="label.bouton.ajouter" tooltip="label.bouton.ajouter" onClick="FusionEntreprises.showRechercheAbsorbee();"/>
		    </span>

		    <script type="application/javascript">
			    var FusionEntreprises = {
				    showRechercheAbsorbee: function() {
					    $('#rechercheAbsorbee').show();
					    $('#raccourciAjouter').hide();
				    },
				    retirerEntrepriseAbsorbee: function(id) {
					    Form.dynamicSubmit('post', App.curl('/processuscomplexe/fusion/absorbees/remove.do'), {id:id});
				    }
			    }
		    </script>

		    <c:if test="${not empty fusion.entreprisesAbsorbees}">
			    <unireg:buttonTo name="Valider" action="/processuscomplexe/fusion/fusionner.do" confirm="Voulez-vous réellement finaliser cette fusion d'entreprises ?"/>
		    </c:if>

		    <div style="display: none;" id="rechercheAbsorbee">
			    <c:set var="searchUrl">
				    <c:url value="/processuscomplexe/fusion/absorbees/list.do?searched=true"/>
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
						    <jsp:param name="typeRecherche" value="fusionEntreprises" />
						    <jsp:param name="prefixeEffacer" value="/processuscomplexe/fusion/absorbees"/>
					    </jsp:include>
				    </fieldset>
			    </form:form>

			    <display:table name="list" id="row" pagesize="25" requestURI="/processuscomplexe/fusion/absorbees/list.do" class="display" sort="list">
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
							    <unireg:linkTo name="${noctb}" action="/processuscomplexe/fusion/absorbees/add.do" method="post" params="{id:${row.numero}}" title="Ajouter à la liste des entreprises absorbées"/>
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
					    FusionEntreprises.showRechercheAbsorbee();
				    });
		        </script>
		    </c:if>

	    </fieldset>

	    <unireg:RetourButton link="../retour-choix-dates.do" checkIfModified="true"/>

	</tiles:put>
</tiles:insert>