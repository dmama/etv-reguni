<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.edition.questionnaire.snc" /></tiles:put>
	<tiles:put name="body">

		<unireg:nextRowClass reset="1"/>
		<unireg:bandeauTiers numero="${questionnaire.tiersId}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" />

		<!-- Début questionnaire SNC -->
		<fieldset class="information">
			<legend><span><fmt:message key="label.caracteristiques.questionnaire.snc" /></span></legend>

			<table border="0">
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.periode.fiscale" />&nbsp;:</td>
					<td width="25%">${questionnaire.periodeFiscale}</td>
					<td width="25%"><fmt:message key="label.etat.courant" />&nbsp;:</td>
					<td width="25%"><fmt:message key="option.etat.avancement.f.${questionnaire.etat}"/></td>
				</tr>
			</table>
		</fieldset>
		<!-- Fin questionnaire SNC -->

		<!-- Debut délais -->
		<fieldset class="information">
			<legend><span><fmt:message key="label.delais"/></span></legend>
			<display:table 	name="questionnaire.delais" id="delai" pagesize="10" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
				<display:column titleKey="label.date.demande" style="width: 30%;">
					<unireg:regdate regdate="${delai.dateDemande}" />
				</display:column>
				<display:column titleKey="label.date.delai.accorde" style="width: 30%;">
					<unireg:regdate regdate="${delai.delaiAccordeAu}" />
				</display:column>
				<display:column titleKey="label.date.traitement" style="width: 30%;">
					<unireg:regdate regdate="${delai.dateTraitement}" />
				</display:column>
				<display:column style="action">
					<unireg:consulterLog entityNature="DelaiDeclaration" entityId="${delai.id}"/>
				</display:column>

				<display:setProperty name="paging.banner.all_items_found" value=""/>
				<display:setProperty name="paging.banner.one_item_found" value=""/>
			</display:table>
		</fieldset>
		<!-- Fin délais -->

		<!-- Debut états -->
		<fieldset>
			<legend><span><fmt:message key="label.etats"/></span></legend>

			<c:if test="${!depuisTache}">
				<authz:authorize ifAnyGranted="ROLE_QSNC_QUITTANCEMENT">
					<table id="quittancerBouton" border="0">
						<tr>
							<td>
								<unireg:linkTo name="Quittancer" title="Quittancer le questionnaire" action="/qsnc/ajouter-quittance.do" params="{id:${questionnaire.id}}" link_class="add margin_right_10"/>
							</td>
						</tr>
					</table>
				</authz:authorize>
			</c:if>

			<c:if test="${not empty questionnaire.etats}">
				<display:table name="questionnaire.etats" id="etat" pagesize="10" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
					<display:column titleKey="label.date.obtention" style="width: 30%;">
						<unireg:regdate regdate="${etat.dateObtention}"/>
						<c:if test="${!etat.annule && etat.etat == 'RAPPELE'}">
							&nbsp;
							(<c:out value="${etat.dateEnvoiCourrierMessage}"/>)
						</c:if>
					</display:column>
					<display:column titleKey="label.etat" style="width: 30%;">
						<fmt:message key="option.etat.avancement.f.${etat.etat}"/>
					</display:column>
					<display:column titleKey="label.source" style="width: 30%;">
						<c:if test="${etat.etat == 'RETOURNE'}">
							<c:if test="${etat.source == null}">
								<fmt:message key="option.source.quittancement.UNKNOWN"/>
							</c:if>
							<c:if test="${etat.source != null}">
								<fmt:message key="option.source.quittancement.${etat.source}"/>
							</c:if>
						</c:if>
					</display:column>
					<display:column style="action">
						<unireg:consulterLog entityNature="EtatDeclaration" entityId="${etat.id}"/>
						<authz:authorize ifAnyGranted="ROLE_QSNC_QUITTANCEMENT">
							<c:if test="${!etat.annule && etat.etat == 'RETOURNE'}">
								<unireg:linkTo name="" title="Annuler le quittancement" confirm="Voulez-vous vraiment annuler ce quittancement ?"
								               action="/qsnc/annuler-quittance.do" method="post" params="{id:${etat.id}}" link_class="delete"/>
							</c:if>
						</authz:authorize>
					</display:column>
					<display:setProperty name="paging.banner.all_items_found" value=""/>
					<display:setProperty name="paging.banner.one_item_found" value=""/>
				</display:table>
			</c:if>
		</fieldset>
		<!-- Fin états -->

		<div style="margin-top:1em;">
			<!-- Debut Boutons -->

			<!-- Bouton Retour -->
			<c:choose>
				<c:when test="${depuisTache}">
					<unireg:buttonTo name="Retour" action="/tache/list.do" id="boutonRetour" method="get"/>
				</c:when>
				<c:otherwise>
					<unireg:buttonTo name="Retour" action="/qsnc/list.do" id="boutonRetour" method="get" params="{tiersId:${questionnaire.tiersId}}"/>
				</c:otherwise>
			</c:choose>

			<!-- Bouton de rappel -->
			<authz:authorize ifAnyGranted="ROLE_QSNC_RAPPEL">
				<c:if test="${!depuisTache && !questionnaire.annule && questionnaire.rappelable}">
					<input type="button" value="<fmt:message key="label.bouton.rappeler"/>" class="button_to" onclick="return EnvoiRappel.execute(${questionnaire.id});"/>
					<script type="application/javascript">
						var EnvoiRappel = {
							execute: function(questionnaireId) {
								if (!confirm('Voulez-vous réellement générer le courrier de rappel ?')) {
									return false;
								}
								$(":button:not('#boutonRetour')").attr('disabled', true);
								Form.dynamicSubmit('post', App.curl('/qsnc/rappel.do'), {id:questionnaireId});
								return true;
							}
						};
					</script>
				</c:if>
			</authz:authorize>

			<!-- Bouton d'impression de duplicata -->
			<authz:authorize ifAnyGranted="ROLE_QSNC_DUPLICATA">
				<c:if test="${!depuisTache && !questionnaire.annule && questionnaire.duplicable}">
					<unireg:buttonTo name="Duplicata" action="/qsnc/duplicata.do" method="post" params='{id:${questionnaire.id}}'/>
				</c:if>
			</authz:authorize>

			<!-- Bouton annulation de questionnaire -->
			<authz:authorize ifAnyGranted="ROLE_QSNC_EMISSION">
				<c:if test="${!questionnaire.annule}">
					<unireg:buttonTo name="Annuler questionnaire" confirm="Voulez-vous vraiment annuler ce questionnaire SNC ?"
					                 action="/qsnc/annuler.do" method="post" params='{id:${questionnaire.id},depuisTache:${depuisTache}}'/>
				</c:if>
			</authz:authorize>

		</div>

	</tiles:put>
</tiles:insert>
