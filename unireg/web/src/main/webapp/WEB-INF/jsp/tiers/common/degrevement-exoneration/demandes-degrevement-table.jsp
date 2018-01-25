<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="demandesDegrevement" type="java.util.List<ch.vd.uniregctb.registrefoncier.allegement.DemandeDegrevementICIView>"--%>

<c:set value="${param.mode}" var="mode"/>       <%-- 'visu' ou 'edit' --%>

<c:if test="${mode == 'visu'}">
	<input class="noprint" id="histoDemandesDegrevement" type="checkbox" onclick="Histo.toggleRowsIsHistoFromClass('demandesDegrevement', 'histoDemandesDegrevement', 'histo-only');"/>
	<label class="noprint" for="histoDemandesDegrevement"><fmt:message key="label.historique"/></label>
</c:if>

<display:table name="${demandesDegrevement}" id="demande" class="display" decorator="ch.vd.uniregctb.decorator.TableAnnulableDateRangeDecorator" htmlId="demandesDegrevement">
	<display:column titleKey="label.periode.fiscale">
		<c:out value="${demande.periodeFiscale}"/>
	</display:column>
	<display:column titleKey="label.date.envoi">
		<unireg:regdate regdate="${demande.dateEnvoi}"/>
		<c:choose>
			<c:when test="${demande.urlVisualisationExterneDocument != null}">
				&nbsp;<a href="#" class="pdf" title="Visualisation du courrier" onclick="VisuExterneDoc.openWindow('${demande.urlVisualisationExterneDocument}');">&nbsp;</a>
			</c:when>
			<c:when test="${demande.avecCopieConformeEnvoi}">
				&nbsp;<a href="../autresdocs/copie-conforme-envoi.do?idDoc=${demande.id}&url_memorize=false" class="pdf" id="print-envoi-${demande.id}" title="Courrier envoyé" onclick="Link.tempSwap(this, '#disabled-print-envoi-${demande.id}');">&nbsp;</a>
				<span class="pdf-grayed" id="disabled-print-envoi-${demande.id}" style="display: none;">&nbsp;</span>
			</c:when>
		</c:choose>
	</display:column>
	<display:column titleKey="label.date.delai.accorde">
		<unireg:regdate regdate="${demande.delaiRetour}"/>
	</display:column>
	<display:column titleKey="label.date.rappel">
		<unireg:regdate regdate="${demande.dateRappel}"/>
		<c:choose>
			<c:when test="${demande.urlVisualisationExterneRappel != null}">
				&nbsp;<a href="#" class="pdf" title="Visualisation du courrier de rappel" onclick="VisuExterneDoc.openWindow('${demande.urlVisualisationExterneRappel}');">&nbsp;</a>
			</c:when>
			<c:when test="${demande.avecCopieConformeRappel}">
				&nbsp;<a href="../autresdocs/copie-conforme-rappel.do?idDoc=${demande.id}&url_memorize=false" class="pdf" id="print-rappel-${demande.id}" title="Rappel envoyé" onclick="Link.tempSwap(this, '#disabled-print-rappel-${docFiscal.id}');">&nbsp;</a>
				<span class="pdf-grayed" id="disabled-print-rappel-${demande.id}" style="display: none;">&nbsp;</span>
			</c:when>
		</c:choose>
	</display:column>
	<display:column titleKey="label.date.retour">
		<unireg:regdate regdate="${demande.dateRetour}"/>
	</display:column>
	<display:column titleKey="label.etat.avancement">
		<fmt:message key="option.etat.avancement.f.${demande.etat}"/>
	</display:column>
	<display:column titleKey="label.code.controle">
		<c:out value="${demande.codeControle}"/>
	</display:column>
	<display:column class="action" style="width: 5%;">
		<c:choose>
			<c:when test="${mode == 'visu'}">
				<c:if test="${!demande.annule}">
					<a href="#" class="detail" title="Détail de la demande de dégrèvement ICI" onclick="Decl.open_details_ddici(<c:out value="${demande.id}"/>); return false;">&nbsp;</a>
				</c:if>
				<unireg:consulterLog entityNature="AutreDocumentFiscal" entityId="${demande.id}"/>
			</c:when>
			<c:when test="${mode == 'edit' && !demande.annule}">
				<unireg:raccourciModifier link='edit-demande-degrevement.do?id=${demande.id}' tooltip="Modifier la demande de dégrèvement"/>
				<unireg:raccourciAnnuler onClick="EditDemandeDegrevement.cancel(${demande.id});" tooltip="Annuler"/>
			</c:when>
			<%--@elvariable id="periodesActives" type="java.util.Set"--%>
			<c:when test="${mode == 'edit' && demande.annule && !periodesActives.contains(demande.periodeFiscale)}">
				<unireg:linkTo name="" title="Désannuler la demande de dégrèvement" action="/degrevement-exoneration/desannuler.do" method="post" params="{id:${demande.id}}"
				               confirm="Voulez-vous vraiment désannuler ce document fiscal ?" link_class="undelete" />
			</c:when>
			<c:otherwise>
				&nbsp;
			</c:otherwise>
		</c:choose>
	</display:column>
</display:table>

<c:if test="${mode == 'edit'}">
	<script type="application/javascript">
		const EditDemandeDegrevement = {
			cancel: function(idDemande) {
				if (confirm('Voulez-vous réellement procéder à l\'annulation de ce formulaire de demande de dégrèvement ?')) {
					App.executeAction('post:/degrevement-exoneration/cancel-demande-degrevement.do?id=' + idDemande);
				}
			}
		};
	</script>
</c:if>
