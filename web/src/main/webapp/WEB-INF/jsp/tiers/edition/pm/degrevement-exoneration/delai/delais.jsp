<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="page" value="${param.page}" />

<%--@elvariable id="editDemandeDegrevementCommand" type="ch.vd.uniregctb.registrefoncier.allegement.EditDemandeDegrevementView"--%>

<c:if test="${not empty editDemandeDegrevementCommand.delais}">

	<display:table 	name="editDemandeDegrevementCommand.delais" id="delai" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
		<display:column titleKey="label.date.demande">
			<unireg:regdate regdate="${delai.dateDemande}" />
		</display:column>
		<display:column titleKey="label.date.traitement">
			<unireg:regdate regdate="${delai.dateTraitement}" />
		</display:column>
		<c:if test="${page == 'visu' }">
			<display:column titleKey="label.confirmation.ecrite">
				<c:if test="${delai.etat != 'DEMANDE'}">
					<c:if test="${delai.urlVisualisationExterneDocument != null}">
						<a href="#" class="pdf" onclick="VisuExterneDoc.openWindow('${delai.urlVisualisationExterneDocument}');" title="Visualisation du courrier émis">&nbsp;</a>
					</c:if>
				</c:if>
			</display:column>
		</c:if>
		<display:column titleKey="label.date.delai.accorde">
			<unireg:regdate regdate="${delai.delaiAccordeAu}" />
			<c:if test="${delai.sursis && delai.delaiAccordeAu != null}">
				(<fmt:message key="label.sursis"/>)
			</c:if>
		</display:column>
		<display:column style="action">
			<unireg:consulterLog entityNature="DelaiDocumentFiscal" entityId="${delai.id}"/>
			<c:if test="${page == 'edit'}">
				<c:if test="${(!delai.annule) && (!delai.first)}">
					<unireg:linkTo name="" title="Annuler le délai"  confirm="Voulez-vous vraiment annuler ce delai ?"
					               action="/degrevement-exoneration/delai/annuler.do" method="post" params="{id:${delai.id}}" link_class="delete"/>
				</c:if>
			</c:if>
		</display:column>

		<display:setProperty name="paging.banner.all_items_found" value=""/>
		<display:setProperty name="paging.banner.one_item_found" value=""/>
	</display:table>

</c:if>