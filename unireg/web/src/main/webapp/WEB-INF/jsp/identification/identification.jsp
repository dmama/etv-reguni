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
		
			<display:table name="list" id="personne" pagesize="${parametresApp.nbMaxParPage}" requestURI="edit.do" class="display_table" sort="list">
				<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncune.personne.trouvee" /></span></display:setProperty>
				<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.personne.trouvee" /></span></display:setProperty>
				<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.personnes.trouvees" /></span></display:setProperty>
				<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.personnes.trouvees" /></span></display:setProperty>
				
				<display:column sortable ="true" titleKey="label.numero.contribuable" sortProperty="numero" >
					<c:if test="${personne.annule}"><strike></c:if>
						<a href="../../tiers/visu.do?id=${personne.numero}&message=${command.demandeIdentificationView.id}"><unireg:numCTB numero="${personne.numero}" /></a>
					<c:if test="${personne.annule}"></strike></c:if>
				</display:column>
				<display:column sortable ="true" titleKey="label.role" >
					<c:if test="${personne.annule}"><strike></c:if>
						<c:out value="${personne.roleLigne1}" />
						<c:if test="${personne.roleLigne2 != null}">
							<br><c:out value="${personne.roleLigne2}" />
						</c:if>
					<c:if test="${personne.annule}"></strike></c:if>
				</display:column>
				<display:column sortable ="true" titleKey="label.nom.prenom" >
					<c:if test="${personne.annule}"><strike></c:if>
					 
					 <a href="${personne.numero}" class="civTip" id="civildata" name="${personne.numero}">
						<c:out value="${personne.nom1}" />
						<c:if test="${personne.nom2 != null}">
							<br><c:out value="${personne.nom2}" />
						</c:if>
						</a>
					<c:if test="${personne.annule}"></strike></c:if>
				</display:column>
				<display:column titleKey="label.date.naissance" sortable="true" sortName="dateNaissance" sortProperty="dateNaissance">
					<c:if test="${personne.annule}"><strike></c:if>
						<unireg:date date="${personne.dateNaissance}"></unireg:date>
					<c:if test="${personne.annule}"></strike></c:if>
				</display:column>
				<display:column sortable ="true" titleKey="label.npa" >
					<c:if test="${personne.annule}"><strike></c:if>
						<c:out value="${personne.npa}" />
					<c:if test="${personne.annule}"></strike></c:if>
				</display:column>
				<display:column sortable ="true" titleKey="label.localitePays" >
					<c:if test="${personne.annule}"><strike></c:if>
					 <a href="${personne.numero}" class="adrTip" id="adressedata" name="${personne.numero}">
						<c:out value="${personne.localiteOuPays}" />
					</a>	
					<c:if test="${personne.annule}"></strike></c:if>
				</display:column>
				<display:column sortable ="true" titleKey="label.for.principal" >
					<c:if test="${personne.annule}"><strike></c:if>
						<c:out value="${personne.forPrincipal}" />
					<c:if test="${personne.annule}"></strike></c:if>
				</display:column>
				<display:column sortable ="true" titleKey="label.date.ouverture.for" sortProperty="dateOuvertureFor">
					<c:if test="${personne.annule}"><strike></c:if>
						<fmt:formatDate value="${personne.dateOuvertureFor}" pattern="dd.MM.yyyy"/>
					<c:if test="${personne.annule}"></strike></c:if>
				</display:column>
				<display:column sortable ="true" titleKey="label.date.fermeture.for" sortProperty="dateFermetureFor">
					<c:if test="${personne.annule}"><strike></c:if>
						<fmt:formatDate value="${personne.dateFermetureFor}" pattern="dd.MM.yyyy"/>
					<c:if test="${personne.annule}"></strike></c:if>
				</display:column>
				<display:column>
					<unireg:raccourciIdentifier onClick="javascript:Page_Identifier(${personne.numero});" tooltip="Identifier" />
				</display:column>
			</display:table>
		
		<!-- Debut Boutons -->
		<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="javascript:Page_RetourIdentification();" />
		
		<authz:authorize ifAnyGranted="ROLE_MW_IDENT_CTB_CELLULE_BO,ROLE_MW_IDENT_CTB_ADMIN">
			&nbsp;<input type="button" name="expertiser" value="<fmt:message key="label.bouton.expertiser" />" onClick="javascript:confirmeExpertise();" />		
		</authz:authorize>				
		<authz:authorize ifAnyGranted="ROLE_MW_IDENT_CTB_GEST_BO,ROLE_MW_IDENT_CTB_ADMIN">
		&nbsp;<input type="button" name="nonIdentifiable" value="<fmt:message key="label.bouton.identification.impossible" />" onClick="javascript:page_NonIdentification(${command.demandeIdentificationView.id});" />
		
		</authz:authorize>
		
		<!-- Fin Boutons -->
		
			
		</form:form>
		
		<script type="text/javascript">
		
		function Page_RetourIdentification() {
				if(confirm('Voulez-vous vraiment quitter cette page sans sauver ?')) {
					
						document.location.href='edit.do?unlock=true';
					
					
				}
			}


		function page_NonIdentification( id ) {		
			
				document.location.href='nonIdentifie.do?id='+id;
			
				
		}

		function VoirMessage() {
			var form = F$("theForm");
			form.action = 'edit.do?fichier_acicom=true';
			form.submit();
		}
		
		
			function Page_Identifier(idCtb) {
				if(confirm('Voulez-vous vraiment identifier ce message avec ce contribuable ?')) {					
					var form = F$("theForm");
					form.doPostBack("identifier", idCtb);
				}
			}
			function AppSelect_OnChange(select) {
				var value = select.options[select.selectedIndex].value;
				if ( value && value !== '') {
					//window.open(value, '_blank') ;
					window.location.href = value;
				}
			}

			function getParamValue( name )
			{
			  name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
			  var regexS = "[\\?&]"+name+"=([^&#]*)";
			  var regex = new RegExp( regexS );
			  var results = regex.exec( window.location.href );
			  if( results == null )
			    return "";
			  else
			    return results[1];
			}
		</script>
	</tiles:put>
</tiles:insert>
