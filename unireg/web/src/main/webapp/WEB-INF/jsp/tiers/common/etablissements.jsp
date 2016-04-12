<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="page" value="${param.page}" />
<c:if test="${page == 'visu' }">
	<c:set var="url" value="visu.do" />
</c:if>
<c:if test="${page == 'edit' }">
	<c:set var="url" value="edit.do" />
	<unireg:setAuth var="autorisations" tiersId="${command.tiersGeneral.numero}"/>
</c:if>

<c:if test="${not empty command.rapportsEtablissements}">
	<display:table name="command.rapportsEtablissements" id="rapportEtablissement" pagesize="10" requestURI="${url}" sort="list" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
		<display:column sortable ="true" titleKey="label.rapport.tiers">
			<c:if test="${rapportEtablissement.activiteEconomiquePrincipale}">
				<fmt:message key="option.rapport.entre.tiers.${rapportEtablissement.sensRapportEntreTiers}.${rapportEtablissement.typeRapportEntreTiers}.principal" />
			</c:if>
			<c:if test="${!rapportEtablissement.activiteEconomiquePrincipale}">
				<fmt:message key="option.rapport.entre.tiers.${rapportEtablissement.sensRapportEntreTiers}.${rapportEtablissement.typeRapportEntreTiers}" />
			</c:if>

			<c:if test="${rapportEtablissement.toolTipMessage != null}">
				<a href="#tooltip" class="staticTip" id="ret-${rapportEtablissement_rowNum}">?</a>
				<div id="ret-${rapportEtablissement_rowNum}-tooltip" style="display:none;">
					<c:out value="${rapportEtablissement.toolTipMessage}"/>
				</div>
			</c:if>
		</display:column>

		<display:column sortable ="true" titleKey="label.date.debut" sortProperty="dateDebut">
			<fmt:formatDate value="${rapportEtablissement.dateDebut}" pattern="dd.MM.yyyy" />
		</display:column>

		<display:column sortable ="true" titleKey="label.date.fin" sortProperty="dateFin">
			<fmt:formatDate value="${rapportEtablissement.dateFin}" pattern="dd.MM.yyyy" />
		</display:column>

		<display:column sortable ="true" titleKey="label.numero.tiers" sortProperty="numero" >
			<c:if test="${rapportEtablissement.numero != null}">
				<a href="../tiers/visu.do?id=${rapportEtablissement.numero}&rid=${tiersGeneral.numero}"><unireg:numCTB numero="${rapportEtablissement.numero}"></unireg:numCTB></a>
			</c:if>
			<c:if test="${rapportEtablissement.numero == null && rapportEtablissement.messageNumeroAbsent != null}">
				<div class="flash-warning"><c:out value="${rapportEtablissement.messageNumeroAbsent}"/></div>
			</c:if>
		</display:column>

		<display:column sortable ="true" titleKey="label.nom.raison">
			<c:if test="${rapportEtablissement.nomCourrier1 != null }">
				<c:out value="${rapportEtablissement.nomCourrier1}"/>
			</c:if>
			<c:if test="${rapportEtablissement.nomCourrier2 != null }">
				<br/><c:out value="${rapportEtablissement.nomCourrier2}"/>
			</c:if>
		</display:column>

		<display:column style="action">
			<c:if test="${page == 'visu' }">
				<unireg:consulterLog entityNature="RapportEntreTiers" entityId="${rapportEtablissement.id}"/>
			</c:if>
			<c:if test="${page == 'edit' }">
				<c:if test="${rapportEtablissement.etablissementAnnulable}">
					<unireg:raccourciAnnuler onClick="javascript:Rapport.annulerRapport(${rapportEtablissement.id})" tooltip="Annuler"/>
				</c:if>
			</c:if>
		</display:column>
		<display:setProperty name="paging.banner.all_items_found" value=""/>
		<display:setProperty name="paging.banner.one_item_found" value=""/>
	</display:table>

	<script>
		$(function() {
			Tooltips.activate_static_tooltips();
		});
	</script>

</c:if>
