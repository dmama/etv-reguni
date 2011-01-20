<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<c:choose>		
		<c:when test="${messageEnCours}">
			<c:set var="myAction" value="listEnCours.do" />
		</c:when>
		
		<c:when test="${messageTraite}">
			<c:set var="myAction" value="listTraite.do" />
		</c:when>
</c:choose>

<c:set var="titrePage" value="title.messages.en.cours" />

<c:if test="${messageTraite}">
	<c:set var="titrePage" value="title.messages.archive" />
</c:if>




<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="${titrePage}" /></tiles:put>
  	<tiles:put name="fichierAide">
	</tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
	    <form:form method="post" id="formRechercheMessage" name="theForm" action="${myAction}">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche.messages"/></span></legend>
				<form:errors cssClass="error"/>
				<jsp:include page="form.jsp" />
			</fieldset>
			
			<div id="desynchro" style="display:none;">
				<FONT COLOR="#FF0000">Attention la recherche est désynchronisée après la suspension ou la soumission de messages</FONT>
			</div>
		
			<display:table name="identifications" id="message" pagesize="25" size="identificationsSize" requestURI="/identification/gestion-messages/${myAction}"
			class="display_table" sort="external" partialList="true" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator" >
				<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncun.message.trouve" /></span></display:setProperty>
				<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.message.trouve" /></span></display:setProperty>
				<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.messages.trouves" /></span></display:setProperty>
				<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.messages.trouves" /></span></display:setProperty>
				
				<c:if test="${!messageTraite}">
				<authz:authorize ifAnyGranted="ROLE_MW_IDENT_CTB_ADMIN">
					<display:column title="<input type='checkbox'  name='selectAll' onclick='javascript:selectAllIdentifications(this);' />">
						<c:if test="${!message.annule}">
							<input type="checkbox" checked  name="tabIdsMessages" id="tabIdsMessages_${message_rowNum}" value="${message.id}" >
						</c:if>
					</display:column>
				</authz:authorize>
				</c:if>
				<display:column sortable ="true" titleKey="label.type.message" sortName="demande.typeMessage">
						<fmt:message key="option.type.message.${message.typeMessage}" />
				</display:column>
				<display:column sortable ="true" titleKey="label.periode.fiscale" sortName="demande.periodeFiscale" >
						${message.periodeFiscale}
				</display:column>
				<display:column sortable ="true" titleKey="label.emetteur" sortName="demande.emetteurId">
						${message.emetteurId}
				</display:column>
				<display:column sortable ="true" titleKey="label.date.message" sortName="demande.date">
						<fmt:formatDate value="${message.dateMessage}" pattern="dd.MM.yyyy"/>
				</display:column>
				<display:column sortable ="true" titleKey="label.etat.message" style="white-space:nowrap">
						<fmt:message key="option.etat.message.${message.etatMessage}"  />
				</display:column>
				<display:column sortable ="true" titleKey="label.nom" sortName="demande.personne.nom">
						<c:out value="${message.nom}" />
				</display:column>
				<display:column sortable ="true" titleKey="label.prenom"  sortName="demande.personne.prenoms">
						<c:out value="${message.prenoms}" />
				</display:column>
				<c:if test="${messageTraite}">
					<display:column sortable ="true" titleKey="label.numero.contribuable"  sortName="reponse.noContribuable">
						 <c:if test="${(message.etatMessage != 'NON_IDENTIFIE')}">
								<a href="../../tiers/visu.do?id=${message.numeroContribuable}&retour=traite"><unireg:numCTB numero="${message.numeroContribuable}" /></a>
						</c:if>	
					</display:column>
				</c:if>
				  <display:column sortable ="true" titleKey="label.navs11" sortName="demande.personne.NAVS11">
						<c:out value="${message.navs11}" />
				</display:column>
				<display:column sortable ="true" titleKey="label.date.naissance" sortName="demande.personne.dateNaissance">
						<unireg:date date="${message.dateNaissance}" />
				</display:column>
				<display:column sortable ="true" titleKey="label.navs13" sortName="demande.personne.NAVS13">
						<c:out value="${message.navs13}" />
				</display:column>
				<c:if test="${message.traitementUser != null }">
					<display:column  titleKey="label.identification.traitement.user" style="text-align:center">
						<unireg:consulterInfoTraitement dateTraitement="${message.traitementDate}" userTraitement="${message.traitementUser}"/>
					</display:column>
				</c:if>
				<authz:authorize ifAnyGranted="ROLE_MW_IDENT_CTB_GEST_BO,ROLE_MW_IDENT_CTB_ADMIN,ROLE_MW_IDENT_CTB_CELLULE_BO">
				<display:column>
				 <c:if test="${(message.etatMessage == 'A_TRAITER_MANUELLEMENT') || (message.etatMessage == 'EXCEPTION') ||
					 (message.etatMessage == 'A_EXPERTISER') || (message.etatMessage == 'SUSPENDU') ||
					  (message.etatMessage == 'A_EXPERTISER_SUSPENDU') || (message.etatMessage == 'A_TRAITER_MAN_SUSPENDU')}">				  
					  	
					  	<c:choose>
					  		<c:when test="${(message.utilisateurTraitant==null) || (message.utilisateurTraitant==command.userCourant)}">
					  		<c:if test="${!messageTraite}">
								<unireg:raccourciModifier link="edit.do?id=${message.id}" />
							</c:if>					
					  		</c:when>
					  		<c:when test="${(message.utilisateurTraitant!=null)}">
					  		<img src="<c:url value="/css/x/running.png"/>" title="En cours de Traitement par ${message.utilisateurTraitant}" />
					  		</c:when>
					  	</c:choose>					  	
				</c:if>
				</display:column>
				</authz:authorize>
				<display:column style="action">
                    <c:if test="${message.id != null}">
						 <unireg:consulterLog entityNature="identification" entityId="${message.id}"/>
                    </c:if>
               </display:column>
			</display:table>
		
		<!-- Debut Boutons -->
		
		<!-- On affiche les bouton d'action sur les messages que si on a le rôle administrateur-->
		<c:if test="${!messageTraite}">
			<authz:authorize ifAnyGranted="ROLE_MW_IDENT_CTB_ADMIN">
				<table border="0">
					<tr>
						<td width="25%">&nbsp;</td>
						<td width="50%">
							<div class="navigation-action">
									<input type="button" name="suspendre" value="<fmt:message key="label.bouton.suspendre" />" onClick="javascript:confirmeSuspensionMessage();" />&nbsp;<input type="button" name="soumettre" value="<fmt:message key="label.bouton.soumettre"/>" onClick="javascript:confirmeSoumissionMessage();"/>
							</div>
						</td>
						<td width="25%">&nbsp;</td>
					</tr>
				</table>
			</authz:authorize>
		</c:if>
		<!-- Fin Boutons -->
			
		</form:form>
		
		<script type="text/javascript">
			function AppSelect_OnChange(select) {
				var value = select.options[select.selectedIndex].value;
				if ( value && value !== '') {
					//window.open(value, '_blank') ;
					window.location.href = value;
				}
			}
		</script>
	</tiles:put>
</tiles:insert>
