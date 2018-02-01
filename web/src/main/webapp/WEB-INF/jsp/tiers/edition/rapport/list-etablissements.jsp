<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Etablissements -->
<fieldset>
	<legend><span><fmt:message key="label.etablissements" /></span></legend>

	<unireg:setAuth var="autorisations" tiersId="${command.tiers.numero}"/>

	<table border="0">
		<tr class="<unireg:nextRowClass/>" >
			<td>
				<unireg:raccourciAjouter link="../tiers/etablissement/create.do?numeroCtbAss=${command.tiers.numero}" tooltip="Ajouter &eacute;tablissement" display="label.bouton.ajouter"/>
			</td>
		</tr>
	</table>

	<c:if test="${not empty command.rapportsEtablissements}">
		<display:table name="command.rapportsEtablissements" id="rapportEtablissement" pagesize="10" requestURI="list.do" sort="list" class="display" decorator="ch.vd.unireg.decorator.TableEntityDecorator">
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
				<unireg:regdate regdate="${rapportEtablissement.dateDebut}"/>
			</display:column>

			<display:column sortable ="true" titleKey="label.date.fin" sortProperty="dateFin">
				<unireg:regdate regdate="${rapportEtablissement.dateFin}"/>
			</display:column>

			<display:column sortable ="true" titleKey="label.numero.tiers" sortProperty="numero" >
				<c:if test="${rapportEtablissement.numero != null}">
					<a href="../tiers/visu.do?id=${rapportEtablissement.numero}&rid=${tiersGeneral.numero}"><unireg:numCTB numero="${rapportEtablissement.numero}"/></a>
				</c:if>
				<c:if test="${rapportEtablissement.numero == null && rapportEtablissement.messageNumeroAbsent != null}">
					<div class="flash-warning"><c:out value="${rapportEtablissement.messageNumeroAbsent}"/></div>
				</c:if>
			</display:column>

			<display:column sortable ="true" titleKey="label.nom.raison">
				<unireg:multiline lines="${rapportEtablissement.nomCourrier}"/>
			</display:column>

			<display:column style="action">
				<c:if test="${!rapportEtablissement.activiteEconomiquePrincipale && !rapportEtablissement.annule}">
					<c:choose>
						<c:when test="${rapportEtablissement.dateFin == null}">
							<unireg:raccourciModifier tooltip="Fermer le rapport" link="activite-economique/close.do?id=${rapportEtablissement.id}"/>
						</c:when>
						<c:otherwise>
							<unireg:linkTo name="" action="/dossiers-apparentes/activite-economique/reopen.do" method="POST" params="{idRapport:${rapportEtablissement.id}}" link_class="reOpen"
							               title="Ré-ouvrir le rapport" confirm="Voulez-vous vraiment ré-ouvrir ce rapport entre tiers ?" />
						</c:otherwise>
					</c:choose>
				</c:if>
			</display:column>
			<display:setProperty name="paging.banner.all_items_found" value=""/>
			<display:setProperty name="paging.banner.one_item_found" value=""/>
		</display:table>

		<script type="application/javascript">
			$(function() {
				Tooltips.activate_static_tooltips();
			});
		</script>

	</c:if>

</fieldset>
<!-- Fin Etablissements -->
