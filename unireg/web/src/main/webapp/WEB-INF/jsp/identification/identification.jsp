<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.identification.recherche.personne" /></tiles:put>
  	<tiles:put name="fichierAide">
	</tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
	    <form:form method="post" id="formRecherchePersonne" name="theForm" action="edit.do">
	    	<input type="hidden"  name="__TARGET__" value="">
			<input type="hidden"  name="__EVENT_ARGUMENT__" value="">
	    	<jsp:include page="demande-identification.jsp" >
		    	<jsp:param name="path" value="demandeIdentificationView" />
	    	</jsp:include>
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors cssClass="error"/>
				<jsp:include page="../tiers/recherche/form.jsp" >
					<jsp:param name="typeRecherche" value="identification" />
				</jsp:include>
			</fieldset>
		
			<display:table name="list" id="personne" pagesize="${parametresApp.nbMaxParPage}" requestURI="edit.do" class="display_table" sort="list" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
				<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncune.personne.trouvee" /></span></display:setProperty>
				<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.personne.trouvee" /></span></display:setProperty>
				<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.personnes.trouvees" /></span></display:setProperty>
				<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.personnes.trouvees" /></span></display:setProperty>
				
				<display:column sortable ="true" titleKey="label.numero.contribuable" sortProperty="numero" >
						<a href="../../tiers/visu.do?id=${personne.numero}&message=${command.demandeIdentificationView.id}"><unireg:numCTB numero="${personne.numero}" /></a>
				</display:column>
				<display:column sortable ="true" titleKey="label.role" >
						<c:out value="${personne.roleLigne1}" />
						<c:if test="${personne.roleLigne2 != null}">
							<br><c:out value="${personne.roleLigne2}" />
						</c:if>
				</display:column>
				<display:column sortable ="true" titleKey="label.nom.prenom" >
					 <a href="${personne.numero}" class="civTip" id="civildata" name="${personne.numero}">
						<c:out value="${personne.nom1}" />
						<c:if test="${personne.nom2 != null}">
							<br><c:out value="${personne.nom2}" />
						</c:if>
						</a>
				</display:column>
				<display:column titleKey="label.date.naissance" sortable="true" sortName="dateNaissance" sortProperty="dateNaissance">
						<unireg:date date="${personne.dateNaissance}"></unireg:date>
				</display:column>
				<display:column sortable ="true" titleKey="label.npa" >
						<c:out value="${personne.npa}" />
				</display:column>
				<display:column sortable ="true" titleKey="label.localitePays" >
					 <a href="${personne.numero}" class="adrTip" id="adressedata" name="${personne.numero}">
						<c:out value="${personne.localiteOuPays}" />
					</a>
				</display:column>
				<display:column sortable ="true" titleKey="label.for.principal" >
						<c:out value="${personne.forPrincipal}" />
				</display:column>
				<display:column sortable ="true" titleKey="label.date.ouverture.for" sortProperty="dateOuvertureFor">
						<fmt:formatDate value="${personne.dateOuvertureFor}" pattern="dd.MM.yyyy"/>
				</display:column>
				<display:column sortable ="true" titleKey="label.date.fermeture.for" sortProperty="dateFermetureFor">
						<fmt:formatDate value="${personne.dateFermetureFor}" pattern="dd.MM.yyyy"/>
				</display:column>
				<display:column>
					<unireg:raccourciIdentifier onClick="javascript:IdentificationCtb.Page_Identifier(${personne.numero});" tooltip="Identifier" />
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

		
		<!-- Debut Boutons -->
		<unireg:RetourButton link="edit.do?unlock=true" message="Voulez-vous vraiment quitter cette page sans sauver ?"/>
		
		<authz:authorize ifAnyGranted="ROLE_MW_IDENT_CTB_CELLULE_BO,ROLE_MW_IDENT_CTB_ADMIN,ROLE_NCS_IDENT_CTB_CELLULE_BO,ROLE_LISTE_IS_IDENT_CTB_CELLULE_BO">
            <c:set var="expertiserButtonName">
                <fmt:message key="label.bouton.expertiser" />
            </c:set>
            &nbsp;<unireg:buttonTo action="/identification/gestion-messages/edit.do" method="post" name="${expertiserButtonName}" confirm="Voulez-vous soumettre à expertise le message ?" params="{id:${command.demandeIdentificationView.id},expertiser:expertiser}"/>
		</authz:authorize>
		<authz:authorize ifAnyGranted="ROLE_MW_IDENT_CTB_GEST_BO,ROLE_MW_IDENT_CTB_ADMIN">
            <c:set var="nonIdentifiableButtonName">
                <fmt:message key="label.bouton.identification.impossible" />
            </c:set>
            &nbsp;<unireg:buttonTo action="/identification/gestion-messages/nonIdentifie.do" name="${nonIdentifiableButtonName}" method="get" params="{id:${command.demandeIdentificationView.id}}"/>
		</authz:authorize>
		
		<!-- Fin Boutons -->
		</form:form>
		
		<script type="text/javascript" language="javascript" src="<c:url value="/js/identification.js"/>"></script>
	</tiles:put>
</tiles:insert>
