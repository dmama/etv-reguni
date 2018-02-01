<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="degrevements" type="java.util.List<ch.vd.unireg.registrefoncier.allegement.DegrevementICIView>"--%>

<c:set value="${param.mode}" var="mode"/>       <%-- 'visu' ou 'edit' --%>

<c:if test="${mode == 'visu'}">
	<input class="noprint" id="histoDegrevements" type="checkbox" onclick="Histo.toggleRowsIsHistoFromClass('degrevements', 'histoDegrevements', 'histo-only');"/>
	<label class="noprint" for="histoDegrevements"><fmt:message key="label.historique"/></label>
</c:if>

<unireg:nextRowClass reset="0"/>
<table class="display display_table degrevement_list" id="degrevements">
	<thead>
	<tr>
		<th rowspan="2"><fmt:message key="label.periode.fiscale.debut"/></th>
		<th rowspan="2"><fmt:message key="label.periode.fiscale.fin"/></th>
		<th rowspan="2" class="separator"></th>
		<th colspan="5" style="text-align: center;"><fmt:message key="label.donnees.location"/></th>
		<th rowspan="2" class="separator"></th>
		<th colspan="5" style="text-align: center;"><fmt:message key="label.donnees.propre.usage"/></th>
		<th rowspan="2" class="separator"></th>
		<th colspan="4" style="text-align: center;"><fmt:message key="label.donnees.loi.logement"/></th>
		<th rowspan="2" class="separator"></th>
		<th rowspan="2">&nbsp;</th>
	</tr>
	<tr>
		<th><fmt:message key="label.revenu.encaisse"/></th>
		<th><fmt:message key="label.volume"/></th>
		<th><fmt:message key="label.surface"/></th>
		<th><fmt:message key="label.portion.declaree"/></th>
		<th><fmt:message key="label.portion.arretee"/></th>
		<th><fmt:message key="label.revenu.estime"/></th>
		<th><fmt:message key="label.volume"/></th>
		<th><fmt:message key="label.surface"/></th>
		<th><fmt:message key="label.portion.declaree"/></th>
		<th><fmt:message key="label.portion.arretee"/></th>
		<th>&nbsp;</th>
		<th><fmt:message key="label.date.octroi"/></th>
		<th><fmt:message key="label.date.echeance.octroi"/></th>
		<th><fmt:message key="label.pourcentage.caractere.social.arrete"/></th>
	</tr>
	</thead>
	<c:forEach items="${degrevements}" var="degrevement">
		<tr class='<unireg:nextRowClass/><c:if test="${degrevement.annule}"> strike</c:if><c:if test="${degrevement.annule || degrevement.past}"> histo-only</c:if>'>
			<td style="width: 10ex;">
				<unireg:regdate regdate="${degrevement.dateDebut}" format="yyyy"/>
				<c:if test="${degrevement.nonIntegrable}">
					<div style="float: right;" class="warning_icon" title='<fmt:message key="label.degrevement.non.integrable.tooltip"/>'>&nbsp;</div>
				</c:if>
			</td>
			<td style="width: 10ex;"><unireg:regdate regdate="${degrevement.dateFin}" format="yyyy"/></td>
			<td class="separator"></td>
			<td style="text-align: right;">
				<c:choose>
					<c:when test="${degrevement.location != null && degrevement.location.revenu != null}">
						<fmt:formatNumber value="${degrevement.location.revenu}"/>&nbsp;<fmt:message key="label.chf"/>
					</c:when>
					<c:otherwise>
						&nbsp;
					</c:otherwise>
				</c:choose>
			</td>
			<td style="text-align: right;">
				<c:choose>
					<c:when test="${degrevement.location != null && degrevement.location.volume != null}">
						<fmt:formatNumber value="${degrevement.location.volume}"/>&nbsp;m&sup3;
					</c:when>
					<c:otherwise>
						&nbsp;
					</c:otherwise>
				</c:choose>
			</td>
			<td style="text-align: right;">
				<c:choose>
					<c:when test="${degrevement.location != null && degrevement.location.surface != null}">
						<fmt:formatNumber value="${degrevement.location.surface}"/>&nbsp;m&sup2;
					</c:when>
					<c:otherwise>
						&nbsp;
					</c:otherwise>
				</c:choose>
			</td>
			<td style="text-align: right;">
				<c:choose>
					<c:when test="${degrevement.location != null && degrevement.location.pourcentage != null}">
						<fmt:formatNumber maxIntegerDigits="4" minFractionDigits="2" value="${degrevement.location.pourcentage}"/>&nbsp;&percnt;
					</c:when>
					<c:otherwise>
						&nbsp;
					</c:otherwise>
				</c:choose>
			</td>
			<td style="text-align: right; font-weight: bold;">
				<c:choose>
					<c:when test="${degrevement.location != null && degrevement.location.pourcentageArrete != null}">
						<fmt:formatNumber maxIntegerDigits="4" minFractionDigits="2" value="${degrevement.location.pourcentageArrete}"/>&nbsp;&percnt;
					</c:when>
					<c:otherwise>
						&nbsp;
					</c:otherwise>
				</c:choose>
			</td>
			<td class="separator"></td>
			<td style="text-align: right;">
				<c:choose>
					<c:when test="${degrevement.propreUsage != null && degrevement.propreUsage.revenu != null}">
						<fmt:formatNumber value="${degrevement.propreUsage.revenu}"/>&nbsp;<fmt:message key="label.chf"/>
					</c:when>
					<c:otherwise>
						&nbsp;
					</c:otherwise>
				</c:choose>
			</td>
			<td style="text-align: right;">
				<c:choose>
					<c:when test="${degrevement.propreUsage != null && degrevement.propreUsage.volume != null}">
						<fmt:formatNumber value="${degrevement.propreUsage.volume}"/>&nbsp;m&sup3;
					</c:when>
					<c:otherwise>
						&nbsp;
					</c:otherwise>
				</c:choose>
			</td>
			<td style="text-align: right;">
				<c:choose>
					<c:when test="${degrevement.propreUsage != null && degrevement.propreUsage.surface != null}">
						<fmt:formatNumber value="${degrevement.propreUsage.surface}"/>&nbsp;m&sup2;
					</c:when>
					<c:otherwise>
						&nbsp;
					</c:otherwise>
				</c:choose>
			</td>
			<td style="text-align: right;">
				<c:choose>
					<c:when test="${degrevement.propreUsage != null && degrevement.propreUsage.pourcentage != null}">
						<fmt:formatNumber maxIntegerDigits="4" minFractionDigits="2" value="${degrevement.propreUsage.pourcentage}"/>&nbsp;&percnt;
					</c:when>
					<c:otherwise>
						&nbsp;
					</c:otherwise>
				</c:choose>
			</td>
			<td style="text-align: right; font-weight: bold;">
				<c:choose>
					<c:when test="${degrevement.propreUsage != null && degrevement.propreUsage.pourcentageArrete != null}">
						<fmt:formatNumber maxIntegerDigits="4" minFractionDigits="2" value="${degrevement.propreUsage.pourcentageArrete}"/>&nbsp;&percnt;
					</c:when>
					<c:otherwise>
						&nbsp;
					</c:otherwise>
				</c:choose>
			</td>
			<td class="separator"></td>
			<td style="text-align: center;">
				<c:choose>
					<c:when test="${degrevement.loiLogement != null && degrevement.loiLogement.controleOfficeLogement}">
						<input type="checkbox" disabled checked/>
					</c:when>
					<c:otherwise>
						<input type="checkbox" disabled/>
					</c:otherwise>
				</c:choose>
			</td>
			<td style="width: 10ex;">
				<c:choose>
					<c:when test="${degrevement.loiLogement != null}">
						<unireg:regdate regdate="${degrevement.loiLogement.dateOctroi}"/>
					</c:when>
					<c:otherwise>
						&nbsp;
					</c:otherwise>
				</c:choose>
			</td>
			<td style="width: 10ex;">
				<c:choose>
					<c:when test="${degrevement.loiLogement != null}">
						<unireg:regdate regdate="${degrevement.loiLogement.dateEcheance}"/>
					</c:when>                                                                           
					<c:otherwise>
						&nbsp;
					</c:otherwise>
				</c:choose>
			</td>
			<td style="text-align: right; font-weight: bold;">
				<c:choose>
					<c:when test="${degrevement.loiLogement != null && degrevement.loiLogement.pourcentageCaractereSocial != null}">
						<fmt:formatNumber maxIntegerDigits="4" minFractionDigits="2" value="${degrevement.loiLogement.pourcentageCaractereSocial}"/>&nbsp;&percnt;
					</c:when>
					<c:otherwise>
						&nbsp;
					</c:otherwise>
				</c:choose>
			</td>
			<td class="separator"></td>
			<td style="width: 5%;">
				<c:choose>
					<c:when test="${mode == 'visu'}">
						<unireg:consulterLog entityNature="AllegementFoncier" entityId="${degrevement.idDegrevement}"/>
					</c:when>
					<c:when test="${mode == 'edit' && !degrevement.annule}">
						<unireg:raccourciModifier link="edit-degrevement.do?id=${degrevement.idDegrevement}" tooltip="Modifier les données de dégrèvement"/>
						<unireg:raccourciAnnuler onClick="EditDegrevement.cancel(${degrevement.idDegrevement});" tooltip="Annuler"/>
					</c:when>
					<c:otherwise>
						&nbsp;
					</c:otherwise>
				</c:choose>
			</td>
		</tr>
	</c:forEach>

	<c:if test="${mode == 'edit'}">
		<script type="application/javascript">
			const EditDegrevement = {
				cancel: function(idDegrevement) {
					if (confirm('Voulez-vous réellement procéder à l\'annulation de ces informations de dégrèvement ?')) {
						App.executeAction('post:/degrevement-exoneration/cancel-degrevement.do?id=' + idDegrevement);
					}
				}
			};
		</script>
	</c:if>
</table>
