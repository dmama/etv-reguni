<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="page" value="${param.page}" />
<c:if test="${page == 'visu' }">
	<c:set var="url" value="visu.do" />
</c:if>
<c:if test="${page == 'edit' }">
	<c:set var="url" value="edit.do" />
</c:if>

<c:if test="${not empty command.dossiersApparentes}">

<%-- détermine si un rapport est de type représentation conventionnelle --%>
<c:set var="hasExtensionExecutionForcee" value="${false}" />
<c:forEach items="${command.dossiersApparentes}" var="rapport">
	<c:if test="${rapport.extensionExecutionForcee != null}">
		<c:set var="hasExtensionExecutionForcee" value="${true}" />
	</c:if>
	<c:if test="${rapport.nomAutoriteTutelaire != null}">
		<c:set var="hasAutoriteTutelaire" value="${true}" />
	</c:if>
</c:forEach>

<display:table name="command.dossiersApparentes" id="dossierApparente" pagesize="10" requestURI="${url}" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
	<display:column sortable ="true" titleKey="label.rapport.tiers">
			<fmt:message key="option.rapport.entre.tiers.${dossierApparente.sensRapportEntreTiers}.${dossierApparente.typeRapportEntreTiers}" />
			<c:if test="${dossierApparente.toolTipMessage != null}">
				<a href="#tooltip" class="staticTip" id="ret-${dossierApparente_rowNum}">?</a>
				<div id="ret-${dossierApparente_rowNum}-tooltip" style="display:none;">
					<c:out value="${dossierApparente.toolTipMessage}"/>
				</div>
			</c:if>
	</display:column>

	<display:column sortable ="true" titleKey="label.date.debut" sortProperty="dateDebut">
		<fmt:formatDate value="${dossierApparente.dateDebut}" pattern="dd.MM.yyyy" />
	</display:column>

	<display:column sortable ="true" titleKey="label.date.fin" sortProperty="dateFin">
		<fmt:formatDate value="${dossierApparente.dateFin}" pattern="dd.MM.yyyy" />
	</display:column>

	<display:column sortable ="true" titleKey="label.numero.tiers" sortProperty="numero" >
		<c:if test="${dossierApparente.numero != null}">
			<a href="../tiers/visu.do?id=${dossierApparente.numero}&rid=${tiersGeneral.numero}"><unireg:numCTB numero="${dossierApparente.numero}"></unireg:numCTB></a>
		</c:if>
		<c:if test="${dossierApparente.numero == null && dossierApparente.messageNumeroAbsent != null}">
			<div class="flash-warning"><c:out value="${dossierApparente.messageNumeroAbsent}"/></div>
		</c:if>
	</display:column>
	
	<display:column sortable ="true" titleKey="label.nom.raison">
		<c:if test="${dossierApparente.nomCourrier1 != null }">
			<c:out value="${dossierApparente.nomCourrier1}"/>
		</c:if>
		<c:if test="${dossierApparente.nomCourrier2 != null }">
			<br/><c:out value="${dossierApparente.nomCourrier2}"/>
		</c:if>
	</display:column>
	<c:if test="${hasAutoriteTutelaire}">
		<display:column sortable ="true" titleKey="label.autorite.tutelaire">
			<c:if test="${dossierApparente.nomAutoriteTutelaire!=null}">
				<c:out value="${dossierApparente.nomAutoriteTutelaire}"/>
			</c:if>
		</display:column>
	</c:if>
	<c:if test="${hasExtensionExecutionForcee}">
		<display:column sortable ="true" titleKey="label.extension.execution.forcee">
				<c:if test="${dossierApparente.extensionExecutionForcee != null}">
					<input type="checkbox" <c:if test="${dossierApparente.extensionExecutionForcee}">checked="true"</c:if> disabled="true"/>
				</c:if>
		</display:column>
	</c:if>

	<display:column style="action">
		<c:if test="${dossierApparente.typeRapportEntreTiers != 'FILIATION'}">
			<c:if test="${page == 'visu' }">
				<unireg:consulterLog entityNature="RapportEntreTiers" entityId="${dossierApparente.id}"/>
			</c:if>
			<c:if test="${page == 'edit' }">
				<c:if test="${!dossierApparente.annule}">
				<c:if test="${((dossierApparente.typeRapportEntreTiers == 'PRESTATION_IMPOSABLE') && (command.allowedOnglet.DOS_TRA)) ||
					((dossierApparente.typeRapportEntreTiers != 'APPARTENANCE_MENAGE') && (dossierApparente.typeRapportEntreTiers != 'PRESTATION_IMPOSABLE') && (command.allowedOnglet.DOS_NO_TRA))  && (dossierApparente.id != null)}">
						<unireg:raccourciModifier link="../tiers/rapport.do?idRapport=${dossierApparente.id}&sens=${dossierApparente.sensRapportEntreTiers}&viewRetour=%2Fdossiers-apparentes%2Fedit.do%3Fid%3D${tiersGeneral.numero}" tooltip="Edition de rapport"/>
						<unireg:raccourciAnnuler onClick="javascript:annulerRapport(${dossierApparente.id});" tooltip="Annuler"/>
					</c:if>
				</c:if>
			</c:if>
		</c:if>
	</display:column>
	<display:setProperty name="paging.banner.all_items_found" value=""/>
	<display:setProperty name="paging.banner.one_item_found" value=""/>
</display:table>

<script>
	$(function() {
		activate_static_tooltips();
	});
</script>

</c:if>
