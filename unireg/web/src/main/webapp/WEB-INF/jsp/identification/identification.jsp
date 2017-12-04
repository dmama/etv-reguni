<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<%--@elvariable id="messageData" type="ch.vd.uniregctb.identification.contribuable.view.DemandeIdentificationView"--%>
	<%--@elvariable id="message" type="ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesEditView"--%>
	<%--@elvariable id="source" type="ch.vd.uniregctb.identification.contribuable.IdentificationController.Source"--%>
	<%--@elvariable id="searchCtbErrorMessage" type="java.lang.String"--%>
	<%--@elvariable id="found" type="java.util.List<ch.vd.uniregctb.tiers.TiersIndexedDataView>"--%>
	<%--@elvariable id="hideSoumissionExpertise" type="java.lang.Boolean"--%>
	<%--@elvariable id="hideNonIdentifiable" type="java.lang.Boolean"--%>

  	<tiles:put name="title"><fmt:message key="title.identification.recherche.personne" /></tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>

        <jsp:include page="demande-identification.jsp"/>

	    <c:choose>
		    <c:when test="${message.noCtbIdentifie == null}">

			    <form:form method="post" id="formRecherchePersonne" name="theSearchForm" action="edit.do?source=${source}&id=${messageData.id}" commandName="identificationSearchCriteria">

				    <fieldset>
					    <legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
					    <form:errors cssClass="error"/>
					    <c:set var="typeRechercheIdentification">
						    <c:choose>
							    <c:when test="${messageData.typeContribuable == 'ENTREPRISE'}">identification-pm</c:when>
							    <c:when test="${messageData.typeContribuable == 'PERSONNE_PHYSIQUE'}">identification-pp</c:when>
							    <c:otherwise>identification</c:otherwise>
						    </c:choose>
					    </c:set>
					    <jsp:include page="../tiers/recherche/form.jsp" >
						    <jsp:param name="typeRecherche" value="${typeRechercheIdentification}" />
						    <jsp:param name="prefixeEffacer" value="/identification/gestion-messages" />
						    <jsp:param name="paramsEffacer" value="id:${messageData.id},source:'${source}'" />
					    </jsp:include>
				    </fieldset>

			    </form:form>

			    <form:form method="post" id="formIdentification" name="theIdentificationForm" action="identifie.do?source=${source}&id=${messageData.id}" commandName="identificationSelect">

				    <input type="hidden" name="contribuableIdentifie" id="contribuableIdentifie" value=""/>

				    <c:if test="${searchCtbErrorMessage != null}">
					    <span class="error"><c:out value="${searchCtbErrorMessage}"/></span>
				    </c:if>
				    <display:table name="found" id="personne" pagesize="25" requestURI="edit.do" class="display_table" sort="list" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
					    <display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncune.personne.trouvee" /></span></display:setProperty>
					    <display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.personne.trouvee" /></span></display:setProperty>
					    <display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.personnes.trouvees" /></span></display:setProperty>
					    <display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.personnes.trouvees" /></span></display:setProperty>

					    <display:column sortable ="true" titleKey="label.numero.contribuable" sortProperty="numero" >
						    <a href="../../tiers/visu.do?id=${personne.numero}&message=${messageData.id}&source=${source}"><unireg:numCTB numero="${personne.numero}" /></a>
					    </display:column>
					    <display:column sortable ="true" titleKey="label.role" >
						    <c:out value="${personne.roleLigne1}" />
						    <c:if test="${personne.roleLigne2 != null}">
							    <br><c:out value="${personne.roleLigne2}" />
						    </c:if>
					    </display:column>
					    <c:choose>
						    <c:when test="${messageData.typeContribuable == 'ENTREPRISE'}">
							    <display:column sortable ="true" titleKey="label.raison.sociale" >
									<c:out value="${personne.nom1}" />
							    </display:column>
							    <display:column sortable="true" titleKey="label.numero.ide">
								    <unireg:numIDE numeroIDE="${personne.numeroIDE}"/>
							    </display:column>
							    <display:column titleKey="label.date.naissance.ou.rc" sortable="true" sortName="dateNaissanceInscriptionRC" sortProperty="dateNaissanceInscriptionRC">
								    <unireg:date date="${personne.dateNaissanceInscriptionRC}"/>
							    </display:column>
							    <display:column sortable="true" titleKey="label.siege">
								    <c:out value="${personne.forPrincipal}"/>
							    </display:column>
							    <display:column sortable="true" titleKey="label.forme.juridique">
								    <c:if test="${personne.formeJuridique != null}">
									    <fmt:message key="option.forme.legale.${personne.formeJuridique}"/>
								    </c:if>
							    </display:column>
							    <display:column sortable="true" titleKey="label.etat.entreprise.actuel">
								    <c:if test="${personne.etatEntreprise != null}">
									    <fmt:message key="option.etat.entreprise.${personne.etatEntreprise}"/>
								    </c:if>
							    </display:column>
						    </c:when>
						    <c:otherwise>
							    <display:column sortable ="true" titleKey="label.nom.prenom" >
									<span class="civTip" id="civildata" name="${personne.numero}">
										<c:out value="${personne.nom1}" />
										<c:if test="${personne.nom2 != null}">
											<br><c:out value="${personne.nom2}" />
										</c:if>
									</span>
							    </display:column>
							    <display:column titleKey="label.date.naissance" sortable="true" sortName="dateNaissanceInscriptionRC" sortProperty="dateNaissanceInscriptionRC">
								    <unireg:date date="${personne.dateNaissanceInscriptionRC}"/>
							    </display:column>
							    <display:column sortable ="true" titleKey="label.npa" >
								    <c:out value="${personne.npa}" />
							    </display:column>
							    <display:column sortable ="true" titleKey="label.localitePays" >
									<span  class="adrTip" id="adressedata" name="${personne.numero}">
										<c:out value="${personne.localiteOuPays}" />
									</span>
							    </display:column>
							    <display:column sortable ="true" titleKey="label.for.principal" >
								    <c:out value="${personne.forPrincipal}" />
							    </display:column>
							    <display:column sortable ="true" titleKey="label.date.ouverture.for" sortProperty="dateOuvertureFor">
								    <unireg:regdate regdate="${personne.dateOuvertureFor}" format="dd.MM.yyyy"/>
							    </display:column>
							    <display:column sortable ="true" titleKey="label.date.fermeture.for" sortProperty="dateFermetureFor">
								    <unireg:regdate regdate="${personne.dateFermetureFor}" format="dd.MM.yyyy"/>
							    </display:column>
						    </c:otherwise>
					    </c:choose>
					    <display:column>
						    <unireg:raccourciIdentifier onClick="IdentificationCtb.Page_Identifier(${personne.numero});" tooltip="Identifier" />
					    </display:column>
				    </display:table>

				    <script>
					    $(function() {
						    $(".civTip").tooltip({
							                         items: "[name]",
							                         content: function(response) {
								                         var noCtb = $(this).attr("name");
								                         var url = "<c:url value='/identification/tooltip/individu.do?noCtb='/>" + noCtb + '&' + new Date().getTime();
								                         $.get(url, response);
								                         return "Chargement...";
							                         }
						                         });

						    $(".adrTip").tooltip({
							                         items: "[name]",
							                         content: function(response) {
								                         var noCtb = $(this).attr("name");
								                         var url = "<c:url value='/identification/tooltip/adresse.do?noCtb='/>" + noCtb + '&' + new Date().getTime();
								                         $.get(url, response);
								                         return "Chargement...";
							                         }
						                         });
					    });
				    </script>

			    </form:form>

			    <!-- Debut Boutons -->

			    <unireg:RetourButton link="back-from-edit.do?source=${source}&idToUnlock=${message.demandeIdentificationView.id}" message="Voulez-vous vraiment quitter cette page sans sauver ?"/>

			    <!-- Bouton de passage en expertise -->
			    <c:if test="${!hideSoumissionExpertise}">
				    <authz:authorize ifAnyGranted="ROLE_MW_IDENT_CTB_CELLULE_BO,ROLE_MW_IDENT_CTB_ADMIN,ROLE_NCS_IDENT_CTB_CELLULE_BO,ROLE_LISTE_IS_IDENT_CTB_CELLULE_BO">
					    <c:set var="expertiserButtonName">
						    <fmt:message key="label.bouton.expertiser" />
					    </c:set>
					    &nbsp;<unireg:buttonTo action="/identification/gestion-messages/soumettre-expertise.do" method="post" name="${expertiserButtonName}" confirm="Voulez-vous soumettre à expertise le message ?" params="{id:${messageData.id},source:'${source}'}"/>
				    </authz:authorize>
			    </c:if>

			    <!-- Bouton de non-identification -->
			    <c:if test="${!hideNonIdentifiable}">
				    <authz:authorize ifAnyGranted="ROLE_MW_IDENT_CTB_GEST_BO,ROLE_MW_IDENT_CTB_ADMIN">
					    <c:set var="nonIdentifiableButtonName">
						    <fmt:message key="label.bouton.identification.impossible" />
					    </c:set>
					    &nbsp;<unireg:buttonTo action="/identification/gestion-messages/non-identifie.do" name="${nonIdentifiableButtonName}" method="get" params="{id:${messageData.id},source:'${source}'}"/>
				    </authz:authorize>
			    </c:if>

			    <!-- Fin Boutons -->


		    </c:when>

		    <c:otherwise>

			    <!-- La demande a en fait déjà été traitée, il faut donc afficher les caractéristiques du tiers effectivement choisi -->
			    <unireg:bandeauTiers numero="${message.noCtbIdentifie}" titre="Caractéristiques du contribuable identifié" showAvatar="true" showEvenementsCivils="false" showLinks="false" showValidation="false" showComplements="true"/>

			    <!-- Debut Boutons -->
			    <unireg:RetourButton link="back-from-edit.do?source=${source}&idToUnlock=${messageData.id}"/>
			    <!-- Fin Boutons -->

		    </c:otherwise>

	    </c:choose>


		<script type="text/javascript" language="javascript" src="<c:url value="/js/identification.js"/>"></script>

	</tiles:put>
</tiles:insert>
