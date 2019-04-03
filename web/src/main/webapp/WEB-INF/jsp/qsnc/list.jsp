<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.edition.qsnc.entreprise" /></tiles:put>
	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
		<unireg:bandeauTiers numero="${command.ctbId}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" titre="Caractéristiques de l'entreprise" />

		<!-- Debut Caracteristiques generales -->
		<fieldset>
			<legend><span><fmt:message key="label.caracteristiques.questionnaires.snc" /></span></legend>
			<authz:authorize access="hasAnyRole('QSNC_EMISSION')">
				<table border="0">
					<tr><td>
						<unireg:linkTo name="Ajouter" action="/qsnc/choisir.do" method="get" params="{tiersId:${command.ctbId},url_memorize:false}" title="Ajouter un questionnaire" link_class="add noprint"/>
					</td></tr>
				</table>
			</authz:authorize>

			<c:if test="${not empty command.questionnaires}">
				<display:table name="command.questionnaires" id="qsnc" class="display" decorator="ch.vd.unireg.decorator.TableEntityDecorator" requestURI="/qsnc/list.do" sort="list">
					<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncune.di.trouvee" /></span></display:setProperty>
					<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.di.trouvee" /></span></display:setProperty>
					<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.dis.trouvees" /></span></display:setProperty>
					<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.dis.trouvees" /></span></display:setProperty>

					<display:column sortable="true" titleKey="label.periode.fiscale">
						${qsnc.periodeFiscale}
					</display:column>
					<display:column sortable ="true" titleKey="label.date.delai.accorde" sortProperty="delaiAccorde">
						<unireg:regdate regdate="${qsnc.delaiAccorde}"/>
					</display:column>
					<display:column sortable ="true" titleKey="label.date.retour" sortProperty="dateRetour">
						<unireg:regdate regdate="${qsnc.dateRetour}"/>
					</display:column>
					<display:column sortable ="true" titleKey="label.etat.avancement" >
						<fmt:message key="option.etat.avancement.f.${qsnc.etat}" />
						<c:if test="${qsnc.dateRetour != null}">
							<c:if test="${qsnc.sourceRetour == null}">
								(<fmt:message key="option.source.quittancement.UNKNOWN" />)
							</c:if>
							<c:if test="${qsnc.sourceRetour != null}">
								(<fmt:message key="option.source.quittancement.${qsnc.sourceRetour}" />)
							</c:if>
						</c:if>
					</display:column>
					<display:column style="action">
						<authz:authorize access="hasAnyRole('QSNC_RAPPEL', 'QSNC_DUPLICATA', 'QSNC_QUITTANCEMENT')">
							<c:if test="${!qsnc.annule}">
								<unireg:linkTo name="" action="/qsnc/editer.do" method="get" params="{id:${qsnc.id}}" title="Editer le questionnaire" link_class="edit"/>
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