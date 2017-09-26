<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="exonerations" type="java.util.List<ch.vd.uniregctb.registrefoncier.allegement.ExonerationIFONCView>"--%>

<c:set value="${param.mode}" var="mode"/>       <%-- 'visu' ou 'edit' --%>

<c:if test="${mode == 'visu'}">
	<input class="noprint" id="histoExonerations" type="checkbox" onclick="Histo.toggleRowsIsHistoFromClass('exonerations', 'histoExonerations', 'histo-only');"/>
	<label class="noprint" for="histoExonerations"><fmt:message key="label.historique"/></label>
</c:if>

<unireg:nextRowClass reset="1"/>
<table class="display display_table" id="exonerations">
	<thead>
	<tr>
		<th><fmt:message key="label.periode.fiscale.debut"/></th>
		<th><fmt:message key="label.periode.fiscale.fin"/></th>
		<th><fmt:message key="label.pourcentage.exoneration"/></th>
		<th>&nbsp;</th>
	</tr>
	</thead>
	<c:forEach items="${exonerations}" var="exoneration">
		<tr class='<unireg:nextRowClass/><c:if test="${exoneration.annule}"> strike</c:if><c:if test="${exoneration.annule || exoneration.past}"> histo-only</c:if>'>
			<td><unireg:regdate regdate="${exoneration.dateDebut}" format="yyyy"/></td>
			<td><unireg:regdate regdate="${exoneration.dateFin}" format="yyyy"/></td>
			<td style="text-align: right;">
				<c:choose>
					<c:when test="${exoneration.pourcentageExoneration != null}">
						<fmt:formatNumber maxIntegerDigits="4" minFractionDigits="2" value="${exoneration.pourcentageExoneration}"/>&nbsp;&percnt;
					</c:when>
					<c:otherwise>
						&nbsp;
					</c:otherwise>
				</c:choose>
			</td>
			<td style="width: 5%;">
				<c:choose>
					<c:when test="${mode == 'visu'}">
						<unireg:consulterLog entityNature="AllegementFoncier" entityId="${exoneration.idExoneration}"/>
					</c:when>
					<c:when test="${mode == 'edit' && !exoneration.annule}">
						<unireg:raccourciModifier link="edit-exoneration.do?id=${exoneration.idExoneration}" tooltip="Modifier les données d'exonération"/>
						<unireg:raccourciAnnuler onClick="EditExoneration.cancel(${exoneration.idExoneration});" tooltip="Annuler"/>
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
			const EditExoneration = {
				cancel: function(idExoneration) {
					if (confirm('Voulez-vous réellement procéder à l\'annulation de ces informations d\'exonération ?')) {
						App.executeAction('post:/degrevement-exoneration/cancel-exoneration.do?id=' + idExoneration);
					}
				}
			};
		</script>
	</c:if>
</table>
