<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<c:choose>		
	<c:when test="${messageEnCours}">
		<c:set var="myAction" value="nav-listEnCours.do" />
		<c:set var="mySource" value="enCours" />
		<c:set var="titrePage" value="title.messages.en.cours" />
	</c:when>
	<c:when test="${messageTraite}">
		<c:set var="myAction" value="nav-listTraite.do" />
		<c:set var="mySource" value="traite" />
		<c:set var="titrePage" value="title.messages.archive" />
	</c:when>
    <c:when test="${messageSuspendu}">
        <c:set var="myAction" value="nav-listSuspendu.do" />
	    <c:set var="mySource" value="suspendu"/>
	    <c:set var="titrePage" value="title.messages.suspendu" />
    </c:when>
</c:choose>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="${titrePage}" /></tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
	    <form:form method="post" id="formRechercheMessage" name="theForm" action="${myAction}" modelAttribute="identificationCriteria">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche.messages"/></span></legend>
				<form:errors cssClass="error"/>
				<jsp:include page="form.jsp" >
					<jsp:param name="source" value="${mySource}" />
				</jsp:include>
			</fieldset>
			
			<div id="desynchro" style="display:none;">
				<span style="color: red;">Attention la recherche est désynchronisée après la suspension ou la soumission de messages</span>
			</div>
            <script type="text/javascript">
                function traiterMessage(id,source) {
                    var form = $('<form method="POST" action="' + App.curl('identification/gestion-messages/demandeEdit.do?id=' + id+'&source='+source) +'"/>');
                    form.appendTo('body');
                    form.submit();
                }
            </script>

			<display:table name="identifications" id="message" pagesize="25" defaultsort="1" size="tailleTableau" requestURI="/identification/gestion-messages/${myAction}"
			class="display_table" sort="external"  excludedParams="*" partialList="true" decorator="ch.vd.unireg.decorator.TableEntityDecorator">
				<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncun.message.trouve" /></span></display:setProperty>
				<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.message.trouve" /></span></display:setProperty>
				<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">
                    <c:choose>
                        <c:when test="${identificationsSize > tailleTableau}">
                            <fmt:message key="banner.limitation.messages.trouves">
                                <fmt:param>${identificationsSize}</fmt:param>
                                <fmt:param>${tailleTableau}</fmt:param>
                            </fmt:message>
                        </c:when>
                        <c:otherwise>
                            {0} <fmt:message key="banner.messages.trouves">
                        </fmt:message>
                        </c:otherwise>
                    </c:choose>
                </span></display:setProperty>
				<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.messages.trouves" /></span></display:setProperty>

				<c:if test="${!messageTraite}">
				<authz:authorize access="hasAnyRole('MW_IDENT_CTB_ADMIN', 'MW_IDENT_CTB_GEST_BO', 'SUPERGRA')">
					<display:column title="<input type='checkbox'  name='selectAll' onclick='javascript:IdentificationCtb.selectAllIdentifications(this);' />">
						<c:if test="${!message.annule}">
							<input type="checkbox" name="tabIdsMessages" id="tabIdsMessages_${message_rowNum}" value="${message.id}" >
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
				<display:column sortable ="true" titleKey="label.etat.message"  sortName="etat" style="white-space:nowrap">
						<fmt:message key="option.etat.message.${message.etatMessage}"  />
				</display:column>
				<display:column sortable ="true" titleKey="label.montant.message" sortName="demande.montant" class="number" style="white-space:nowrap">
					<unireg:currency value="${message.montant}"/>
				</display:column>
				<display:column sortable ="true" titleKey="label.nom.raison" sortName="demande.personne.nom">
						<c:out value="${message.nom}" />
				</display:column>
				<display:column sortable ="true" titleKey="label.prenoms"  sortName="demande.personne.prenoms">
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
					<c:if test="${message.navs13Upi != null}">
						<span id="avs13upi-${message.id}" class="staticTip upiAutreNavs13">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>
						<div id="avs13upi-${message.id}-tooltip" style="display:none;">
							<fmt:message key="warning.identification.navs13.upi">
								<fmt:param value="${message.navs13Upi}"/>
							</fmt:message>
						</div>
					</c:if>
				</display:column>
                <authz:authorize access="hasAnyRole('MW_IDENT_CTB_GEST_BO', 'MW_IDENT_CTB_ADMIN', 'MW_IDENT_CTB_CELLULE_BO', 'NCS_IDENT_CTB_CELLULE_BO', 'LISTE_IS_IDENT_CTB_CELLULE_BO', 'RAPPROCHEMENT_RF_IDENTIFICATION_CTB')">
                    <display:column>
                        <c:if test="${(message.etatMessage == 'A_TRAITER_MANUELLEMENT') || (message.etatMessage == 'EXCEPTION') ||
							 (message.etatMessage == 'A_EXPERTISER') || (message.etatMessage == 'SUSPENDU') ||
							  (message.etatMessage == 'A_EXPERTISER_SUSPENDU') || (message.etatMessage == 'A_TRAITER_MAN_SUSPENDU')}">

								<c:choose>
									<c:when test="${(message.utilisateurTraitant==null) || (message.utilisateurTraitant == identificationCriteria.userCourant)}">
										<c:if test="${!messageTraite}">
											<unireg:raccourciModifier onClick="traiterMessage(${message.id},'${mySource}');" tooltip="Traiter le message" />
										</c:if>
									</c:when>
									<c:when test="${(message.utilisateurTraitant!=null)}">
										<img src="<c:url value="/css/x/unlock_off.png"/>" title="En cours de Traitement par ${message.utilisateurTraitant}" />
									</c:when>
								</c:choose>
						</c:if>
					</display:column>
				</authz:authorize>
				<display:column style="action">
                    <c:if test="${message.id != null}">
						 <unireg:consulterLog entityNature="Identification" entityId="${message.id}"/>
                    </c:if>
					<c:if test="${message.traitementUser != null }">
						<unireg:consulterInfoTraitement dateTraitement="${message.traitementDate}" userTraitement="${message.traitementUser}" messageRetour="${message.messageRetour}"/>
					</c:if>
               </display:column>
			</display:table>
		
		<!-- Debut Boutons -->
		
		<!-- On affiche les bouton d'action sur les messages que si on a le rôle administrateur ou gestionnaire back office pour les déblocage-->
            <c:if test="${!messageTraite}">
                <authz:authorize access="hasAnyRole('MW_IDENT_CTB_ADMIN', 'MW_IDENT_CTB_GEST_BO', 'SUPERGRA')">
                    <table border="0">
                        <tr>
                            <td width="25%">&nbsp;</td>
                            <td width="50%">
                                <div class="navigation-action" id="actions-masse-messages">
	                                <input type="hidden" name="source" value="${mySource}"/>
                                    <authz:authorize access="hasAnyRole('MW_IDENT_CTB_ADMIN')">
                                        <c:if test="${messageEnCours}">
                                            <input type="button" name="suspendre" value="<fmt:message key="label.bouton.suspendre" />" onClick="IdentificationCtb.confirmeSuspensionMessage();"/>&nbsp;
                                        </c:if>
                                        <c:if test="${messageSuspendu}">
                                            <input type="button" name="soumettre" value="<fmt:message key="label.bouton.soumettre"/>" onClick="IdentificationCtb.confirmeSoumissionMessage();"/>&nbsp;
                                        </c:if>
                                    </authz:authorize>
                                    <input type="button" name="debloquer" value="<fmt:message key="label.bouton.debloquer"/>" onClick="IdentificationCtb.confirmeDeblocageMessage();"/>&nbsp;
                                    <unireg:testMode>
                                        <input type="button" name="bloquer" value="Bloquer" onClick="IdentificationCtb.confirmeBlocageMessage();"/>
                                    </unireg:testMode>
                                </div>
                            </td>
                            <td width="25%">&nbsp;</td>
                        </tr>
                    </table>
                </authz:authorize>
            </c:if>
		<!-- Fin Boutons -->
			
		</form:form>

		<script type="text/javascript" language="javascript" src="<c:url value="/js/identification.js"/>"></script>
	    <script type="text/javascript" language="javascript">
		    $(function() {
			    Tooltips.activate_static_tooltips($('#message'));
		    });
	    </script>

	</tiles:put>
</tiles:insert>
