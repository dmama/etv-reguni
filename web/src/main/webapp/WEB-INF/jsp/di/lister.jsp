<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.edition.di.contribuable" /></tiles:put>
	<tiles:put name="fichierAide"><li><a href="#" onClick="ouvrirAide('<c:url value='/docs/maj-di.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a></li></tiles:put>
	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
		<unireg:bandeauTiers numero="${command.ctbId}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" titre="Caractéristiques du contribuable" />

		<!-- Debut Caracteristiques generales -->
		<fieldset>
			<legend><span><fmt:message key="caracteristiques.di" /></span></legend>
			<c:if test="${command.ctbPP}">
				<authz:authorize access="hasAnyRole('DI_EMIS_PP')">
					<table border="0">
						<tr><td>
							<unireg:linkTo name="Ajouter" action="/di/choisir-pp.do" method="get" params="{tiersId:${command.ctbId},url_memorize:false}" title="Ajouter une déclaration" link_class="add noprint"/>
						</td></tr>
					</table>
				</authz:authorize>
			</c:if>
			<c:if test="${command.ctbPM}">
				<authz:authorize access="hasAnyRole('DI_EMIS_PM')">
					<table border="0">
						<tr><td>
							<unireg:linkTo name="Ajouter" action="/di/choisir-pm.do" method="get" params="{tiersId:${command.ctbId},url_memorize:false}" title="Ajouter une déclaration" link_class="add noprint"/>
						</td></tr>
					</table>
				</authz:authorize>
			</c:if>

			<c:if test="${not empty command.dis}">
				<display:table name="command.dis" id="di" class="display" decorator="ch.vd.unireg.decorator.TableEntityDecorator" requestURI="/di/list.do" sort="list">
					<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncune.di.trouvee" /></span></display:setProperty>
					<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.di.trouvee" /></span></display:setProperty>
					<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.dis.trouvees" /></span></display:setProperty>
					<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.dis.trouvees" /></span></display:setProperty>

					<display:column sortable="true" titleKey="label.periode.fiscale">
						${di.periodeFiscale}
					</display:column>
					<c:if test="${di.diPM}">
						<display:column sortable ="true" titleKey="label.exercice.commercial" sortProperty="dateDebut">
							<unireg:regdate regdate="${di.dateDebutExercice}"/>&nbsp;-&nbsp;<unireg:regdate regdate="${di.dateFinExercice}"/>
						</display:column>
					</c:if>
					<display:column sortable ="true" titleKey="label.periode.imposition" sortProperty="dateDebut">
						<unireg:regdate regdate="${di.dateDebut}"/>&nbsp;-&nbsp;<unireg:regdate regdate="${di.dateFin}"/>
					</display:column>
					<display:column sortable ="true" titleKey="label.date.delai.accorde" sortProperty="delaiAccorde">
						<unireg:regdate regdate="${di.delaiAccorde}"/>
					</display:column>
					<display:column sortable ="true" titleKey="label.date.retour" sortProperty="dateRetour">
						<unireg:regdate regdate="${di.dateRetour}"/>
					</display:column>
					<display:column sortable ="true" titleKey="label.etat.avancement" >
						<fmt:message key="option.etat.avancement.f.${di.etat}" />
						<c:if test="${di.dateRetour != null}">
							<c:if test="${di.sourceRetour == null}">
								(<fmt:message key="option.source.quittancement.UNKNOWN" />)
							</c:if>
							<c:if test="${di.sourceRetour != null}">
								(<fmt:message key="option.source.quittancement.${di.sourceRetour}" />)
							</c:if>
						</c:if>
					</display:column>
					<display:column style="action">
						<c:if test="${di.diPP}">
							<authz:authorize access="hasAnyRole('DI_QUIT_PP', 'DI_DELAI_PP', 'DI_SOM_PP', 'DI_DUPLIC_PP')">
								<c:if test="${!di.annule}">
									<unireg:linkTo name="" action="/di/editer.do" method="get" params="{id:${di.id}}" title="Editer la déclaration" link_class="edit"/>
								</c:if>
							</authz:authorize>
							<authz:authorize access="hasAnyRole('DI_DESANNUL_PP')">
								<c:if test="${di.annule}">
									<unireg:linkTo name="" title="Désannuler la déclaration" action="/di/desannuler-pp.do" method="post" params="{id:${di.id}}"
									               confirm="Voulez-vous vraiment désannuler cette déclaration d'impôt ?" link_class="undelete" />
								</c:if>
							</authz:authorize>
						</c:if>
						<c:if test="${di.diPM}">
							<authz:authorize access="hasAnyRole('DI_QUIT_PM', 'DI_DELAI_PM', 'DI_SOM_PM', 'DI_DUPLIC_PM', 'DI_SUSPENDRE_PM', 'DI_DESUSPENDRE_PM')">
								<c:if test="${!di.annule}">
									<unireg:linkTo name="" action="/di/editer.do" method="get" params="{id:${di.id}}" title="Editer la déclaration" link_class="edit"/>
								</c:if>
							</authz:authorize>
							<authz:authorize access="hasAnyRole('DI_DESANNUL_PM')">
								<c:if test="${di.annule}">
									<unireg:linkTo name="" title="Désannuler la déclaration" action="/di/desannuler-pm.do" method="post" params="{id:${di.id}}"
									               confirm="Voulez-vous vraiment désannuler cette déclaration d'impôt ?" link_class="undelete" />
								</c:if>
							</authz:authorize>
						</c:if>
					</display:column>
					<display:setProperty name="paging.banner.all_items_found" value=""/>
					<display:setProperty name="paging.banner.one_item_found" value=""/>
				</display:table>
			</c:if>

		</fieldset>
		<!-- Fin Caracteristiques generales -->

		<!-- Debut Bouton -->
		<table>
			<tr><td>
				<unireg:buttonTo name="Retour" action="/tiers/visu.do" method="get" params="{id:${command.ctbId}}" />
			</td></tr>
		</table>
		<!-- Fin Bouton -->
	</tiles:put>

</tiles:insert>