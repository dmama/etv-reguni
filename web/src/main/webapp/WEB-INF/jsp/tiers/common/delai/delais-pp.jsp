<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="page" value="${param.page}" />
<c:set var="depuisTache" value="${param.depuisTache}" />
<%--@elvariable id="command" type="ch.vd.unireg.di.view.EditerDeclarationImpotView"--%>
<c:if test="${not empty command.delais}">

	<display:table name="command.delais" id="delai" pagesize="10" requestURI="editer.do" class="display" decorator="ch.vd.unireg.decorator.TableEntityDecorator">
		<display:column titleKey="label.date.demande">
			<unireg:regdate regdate="${delai.dateDemande}" />
		</display:column>
		<display:column titleKey="label.type.delai">
			<span title="<fmt:message key="option.type.delai.tooltip.${delai.typeDelai}"/>"><fmt:message key="option.type.delai.${delai.typeDelai}"/></span>
			<c:if test="${delai.demandeDelaisMandataireId != null}">
				<unireg:consulterDemandeMandataire demandeId="${delai.demandeDelaisMandataireId}"/>
			</c:if>
		</display:column>
		<display:column titleKey="label.date.traitement">
			<unireg:regdate regdate="${delai.dateTraitement}" />
		</display:column>
		<display:column titleKey="label.decision">
			<fmt:message key="option.etat.delai.${delai.etat}"/>
		</display:column>
		<display:column titleKey="label.date.delai.accorde">
			<unireg:regdate regdate="${delai.delaiAccordeAu}" />
		</display:column>
		<display:column style="action">
			<c:choose>                                                                                        
				<c:when test="${page == 'visu'}">
					<unireg:consulterLog entityNature="DelaiDeclaration" entityId="${delai.id}"/>
				</c:when>
				<c:when test="${page == 'edit' && depuisTache == null && command.allowedDelai}">
					<c:if test="${!delai.annule && delai.lastOfState && delai.etat == 'ACCORDE' && !delai.confirmationEcrite && delai.urlVisualisationExterneDocument == null}">
						<unireg:linkTo name="" title="Imprimer un document de confirmation" confirm="Voulez-vous vraiment imprimer une confirmation ?"
						               action="/di/delai/print-confirmation.do" method="post" params="{idDelai:${delai.id}}" link_class="printer"/>
					</c:if>
					<c:if test="${delai.etat == 'DEMANDE' && !delai.annule}">
						<unireg:linkTo name="" title="Accorder/refuser le délai" action="/di/delai/editer-pp.do" params="{id:${delai.id}}" link_class="edit"/>
					</c:if>
					<c:if test="${!delai.annule && !delai.first}">
						<unireg:linkTo name="" title="Annuler le délai"  confirm="Voulez-vous vraiment annuler ce delai ?"
						               action="/declaration/delai/annuler.do" method="post" params="{id:${delai.id}}" link_class="delete"/>
					</c:if>
				</c:when>
			</c:choose>
		</display:column>

		<display:setProperty name="paging.banner.all_items_found" value=""/>
		<display:setProperty name="paging.banner.one_item_found" value=""/>
	</display:table>

</c:if>