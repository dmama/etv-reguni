<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="page" value="${param.page}" />
<c:set var="depuisTache" value="${param.depuisTache}" />
<c:if test="${not empty command.delais}">

	<display:table 	name="command.delais" id="delai" pagesize="10" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
		<display:column titleKey="label.date.demande">
			<unireg:regdate regdate="${delai.dateDemande}" />
		</display:column>
		<display:column titleKey="label.date.delai.accorde">
			<unireg:regdate regdate="${delai.delaiAccordeAu}" />
		</display:column>
		<display:column titleKey="label.confirmation.ecrite">
			<input type="checkbox" name="confirme" <c:if test="${delai.confirmationEcrite}">checked </c:if> disabled="disabled" />
			<c:if test="${page == 'visu' }">
				<c:choose>
					<c:when test="${delai.urlVisualisationExterneDocument != null}">
						<a href="#" class="pdf" title="Visualisation du courrier émis" onclick="VisuExterneDoc.openWindow('${delai.urlVisualisationExterneDocument}');">&nbsp;</a>
					</c:when>
					<c:when test="${delai.confirmationEcrite}">
						<a href="../declaration/copie-conforme-delai.do?idDelai=${delai.id}&url_memorize=false" class="pdf" id="print-delai-${delai.id}" onclick="Link.tempSwap(this, '#disabled-print-delai-${delai.id}');">&nbsp;</a>
						<span class="pdf-grayed" id="disabled-print-delai-${delai.id}" style="display: none;">&nbsp;</span>
					</c:when>
				</c:choose>
			</c:if>
		</display:column>
		<display:column titleKey="label.date.traitement">
			<unireg:regdate regdate="${delai.dateTraitement}" />
		</display:column>
		<display:column style="action">
			<c:choose>                                                                                        
				<c:when test="${page == 'visu'}">
					<unireg:consulterLog entityNature="DelaiDeclaration" entityId="${delai.id}"/>
				</c:when>
				<c:when test="${page == 'edit' && depuisTache == null && command.allowedDelai}">
					<c:if test="${!delai.annule && delai.lastOfState && delai.etat != 'DEMANDE' && !delai.confirmationEcrite && delai.urlVisualisationExterneDocument == null}">
						<unireg:linkTo name="" title="Imprimer un document de confirmation" confirm="Voulez-vous vraiment imprimer une confirmation ?"
						               action="/di/delai/print-confirmation.do" method="post" params="{idDelai:${delai.id}}" link_class="printer"/>
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