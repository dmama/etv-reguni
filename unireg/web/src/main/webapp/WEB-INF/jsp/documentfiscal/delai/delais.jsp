<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="page" value="${param.page}" />

<%--@elvariable id="command" type="ch.vd.uniregctb.documentfiscal.AutreDocumentFiscalView"--%>

<c:if test="${not empty command.delais}">

	<display:table 	name="command.delais" id="delai" pagesize="10" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
		<display:column titleKey="label.date.demande">
			<unireg:regdate regdate="${delai.dateDemande}" />
		</display:column>
		<display:column titleKey="label.date.traitement">
			<unireg:regdate regdate="${delai.dateTraitement}" />
		</display:column>
		<c:if test="${page == 'visu' }">
			<display:column titleKey="label.confirmation.ecrite">
				<c:if test="${delai.etat != 'DEMANDE'}">
					<c:choose>
						<c:when test="${delai.urlVisualisationExterneDocument != null}">
							<a href="#" class="pdf" onclick="VisuExterneDoc.openWindow('${delai.urlVisualisationExterneDocument}');" title="Visualisation du courrier émis">&nbsp;</a>
						</c:when>
						<c:when test="${delai.confirmationEcrite}">
							<a href="../declaration/copie-conforme-delai.do?idDelai=${delai.id}&url_memorize=false" class="pdf" id="print-delai-${delai.id}" onclick="Link.tempSwap(this, '#disabled-print-delai-${delai.id}');">&nbsp;</a>
							<span class="pdf-grayed" id="disabled-print-delai-${delai.id}" style="display: none;">&nbsp;</span>
						</c:when>
					</c:choose>
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
			<c:if test="${page == 'visu' }">
				<unireg:consulterLog entityNature="DelaiDocumentFiscal" entityId="${delai.id}"/>
			</c:if>
			<c:if test="${page == 'edit'}">
				<c:if test="${delai.etat == 'DEMANDE' && !delai.annule}">
					<unireg:linkTo name="" title="Accorder/refuser le délai" action="/autresdocs/delai/editer.do" params="{id:${delai.id}}" link_class="edit"/>
				</c:if>
				<c:if test="${(!delai.annule) && (!delai.first)}">
					<unireg:linkTo name="" title="Annuler le délai"  confirm="Voulez-vous vraiment annuler ce delai ?"
					               action="/autresdocs/delai/annuler.do" method="post" params="{id:${delai.id}}" link_class="delete"/>
				</c:if>
			</c:if>
		</display:column>

		<display:setProperty name="paging.banner.all_items_found" value=""/>
		<display:setProperty name="paging.banner.one_item_found" value=""/>
	</display:table>

</c:if>