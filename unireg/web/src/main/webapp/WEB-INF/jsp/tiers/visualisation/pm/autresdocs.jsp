<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="command" type="ch.vd.uniregctb.entreprise.TiersVisuView"--%>

<unireg:setAuth var="autorisations" tiersId="${command.entreprise.id}"/>

<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>

<c:if test="${autorisations.autresDocumentsFiscaux}">
	<table border="0">
		<tr><td>
			<c:if test="${empty param['message'] && empty param['retour']}">
				<unireg:raccourciModifier link="../autresdocs/edit-list.do?pmId=${command.tiers.numero}" tooltip="Modifier" display="label.bouton.modifier"/>
			</c:if>
		</td></tr>
	</table>
</c:if>

<fieldset>
	<legend><span><fmt:message key="label.autres.documents.fiscaux.suivis"/></span></legend>

	<c:if test="${not empty command.autresDocumentsFiscauxSuivis}">
		<display:table name="${command.autresDocumentsFiscauxSuivis}" id="docFiscal" htmlId="docFiscalAvecSuivi" requestURI="visu.do" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator" sort="list">
			<display:column sortable="true" titleKey="label.autre.document.fiscal.type.document">
				${docFiscal.libelleTypeDocument}
			</display:column>
			<display:column sortable="true" titleKey="label.autre.document.fiscal.soustype.document">
				${docFiscal.libelleSousType}
			</display:column>
			<display:column sortable ="true" titleKey="label.date.envoi" sortProperty="dateEnvoi">
				<unireg:regdate regdate="${docFiscal.dateEnvoi}"/>
				<c:choose>
					<c:when test="${docFiscal.urlVisualisationExterneDocument != null}">
						&nbsp;<a href="#" class="pdf" title="Visualisation du courrier" onclick="VisuExterneDoc.openWindow('${docFiscal.urlVisualisationExterneDocument}');">&nbsp;</a>
					</c:when>
					<c:when test="${docFiscal.avecCopieConformeEnvoi}">
						&nbsp;<a href="../autresdocs/copie-conforme-envoi.do?idDoc=${docFiscal.id}&url_memorize=false" class="pdf" id="print-envoi-${docFiscal.id}" title="Courrier envoyé" onclick="Link.tempSwap(this, '#disabled-print-envoi-${docFiscal.id}');">&nbsp;</a>
						<span class="pdf-grayed" id="disabled-print-envoi-${docFiscal.id}" style="display: none;">&nbsp;</span>
					</c:when>
				</c:choose>
			</display:column>
			<display:column sortable ="true" titleKey="label.date.delai.accorde" sortProperty="delaiRetour">
				<unireg:regdate regdate="${docFiscal.delaiRetour}"/>
			</display:column>
			<display:column sortable ="true" titleKey="label.date.rappel" sortProperty="dateRetour">
				<unireg:regdate regdate="${docFiscal.dateRappel}"/>
				<c:choose>
					<c:when test="${docFiscal.urlVisualisationExterneRappel != null}">
						&nbsp;<a href="#" class="pdf" title="Visualisation du courrier de rappel" onclick="VisuExterneDoc.openWindow('${docFiscal.urlVisualisationExterneRappel}');">&nbsp;</a>
					</c:when>
					<c:when test="${docFiscal.avecCopieConformeRappel}">
						&nbsp;<a href="../autresdocs/copie-conforme-rappel.do?idDoc=${docFiscal.id}&url_memorize=false" class="pdf" id="print-rappel-${docFiscal.id}" title="Rappel envoyé" onclick="Link.tempSwap(this, '#disabled-print-rappel-${docFiscal.id}');">&nbsp;</a>
						<span class="pdf-grayed" id="disabled-print-rappel-${docFiscal.id}" style="display: none;">&nbsp;</span>
					</c:when>
				</c:choose>
			</display:column>
			<display:column sortable ="true" titleKey="label.date.retour" sortProperty="dateRetour">
				<unireg:regdate regdate="${docFiscal.dateRetour}"/>
			</display:column>
			<display:column sortable ="true" titleKey="label.etat.avancement">
				<fmt:message key="option.etat.avancement.m.${docFiscal.etat}" />
			</display:column>
			<display:column class="action">
				<c:if test="${!docFiscal.annule}">
					<a href="#" class="detail" title="DocFiscal" onclick="Decl.open_details_lb(<c:out value="${docFiscal.id}"/>); return false;">&nbsp;</a>
				</c:if>
				<unireg:consulterLog entityNature="AutreDocumentFiscal" entityId="${docFiscal.id}"/>
			</display:column>
		</display:table>

	</c:if>

</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.autres.documents.fiscaux.non.suivis"/></span></legend>

	<c:if test="${not empty command.autresDocumentsFiscauxNonSuivis}">
		<display:table name="${command.autresDocumentsFiscauxNonSuivis}" id="docFiscal" htmlId="docFiscalSansSuivi" requestURI="visu.do" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator" sort="list">
			<display:column sortable="true" titleKey="label.autre.document.fiscal.type.document">
				${docFiscal.libelleTypeDocument}
			</display:column>
			<display:column sortable="true" titleKey="label.autre.document.fiscal.soustype.document">
				${docFiscal.libelleSousType}
			</display:column>
			<display:column sortable ="true" titleKey="label.date.envoi" sortProperty="dateEnvoi">
				<unireg:regdate regdate="${docFiscal.dateEnvoi}"/>
				<c:choose>
					<c:when test="${docFiscal.urlVisualisationExterneDocument != null}">
						&nbsp;<a href="#" class="pdf" title="Visualisation du courrier" onclick="VisuExterneDoc.openWindow('${docFiscal.urlVisualisationExterneDocument}');">&nbsp;</a>
					</c:when>
					<c:when test="${docFiscal.avecCopieConformeEnvoi}">
						&nbsp;<a href="../autresdocs/copie-conforme-envoi.do?idDoc=${docFiscal.id}&url_memorize=false" class="pdf" id="print-envoi-${docFiscal.id}" title="Courrier envoyé" onclick="Link.tempSwap(this, '#disabled-print-envoi-${docFiscal.id}');">&nbsp;</a>
						<span class="pdf-grayed" id="disabled-print-envoi-${docFiscal.id}" style="display: none;">&nbsp;</span>
					</c:when>
				</c:choose>
			</display:column>
			<display:column class="action">
				<c:if test="${!docFiscal.annule}">
					<a href="#" class="detail" title="DocFiscal" onclick="Decl.open_details_doc_sans_suivi(<c:out value="${docFiscal.id}"/>); return false;">&nbsp;</a>
				</c:if>
				<unireg:consulterLog entityNature="AutreDocumentFiscal" entityId="${docFiscal.id}"/>
			</display:column>
		</display:table>
	</c:if>

</fieldset>
