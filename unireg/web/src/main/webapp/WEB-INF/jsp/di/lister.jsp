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
			<authz:authorize ifAnyGranted="ROLE_DI_EMIS_PP">
				<table border="0">
					<tr><td>
						<unireg:linkTo name="Ajouter" action="/di/choisir.do" method="get" params="{tiersId:${command.ctbId}}" title="Ajouter une déclaration" link_class="add noprint"/>
					</td></tr>
				</table>
			</authz:authorize>

			<c:if test="${not empty command.dis}">
				<display:table name="command.dis" id="di" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator" requestURI="/di/list.do" sort="list">
					<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncune.di.trouvee" /></span></display:setProperty>
					<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.di.trouvee" /></span></display:setProperty>
					<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.dis.trouvees" /></span></display:setProperty>
					<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.dis.trouvees" /></span></display:setProperty>

					<display:column sortable="true" titleKey="label.periode.fiscale">
						${di.periodeFiscale}
					</display:column>
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
						<fmt:message key="option.etat.avancement.${di.etat}" />
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
						<c:if test="${!di.annule}">
							<unireg:linkTo name="" action="/di/editer.do" method="get" params="{id:${di.id}}" title="Editer la déclaration" link_class="edit"/>
						</c:if>
						<authz:authorize ifAnyGranted="ROLE_DI_DESANNUL_PP">
							<c:if test="${di.annule}">
								<unireg:linkTo name="" title="Désannuler la déclaration" action="/di/desannuler.do" method="post" params="{id:${di.id}}"
								               confirm="Voulez-vous vraiment désannuler cette déclaration d'impôt ?" link_class="undelete" />
							</c:if>
						</authz:authorize>
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