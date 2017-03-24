<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="idContribuable" type="java.lang.Long"--%>
<%--@elvariable id="demandesDegrevement" type="java.util.List<ch.vd.uniregctb.registrefoncier.DemandeDegrevementICIView>"--%>
<%--@elvariable id="degrevements" type="java.util.List<ch.vd.uniregctb.registrefoncier.DegrevementICIView>"--%>
<%--@elvariable id="exonerations" type="java.util.List<ch.vd.uniregctb.registrefoncier.ExonerationIFONCView>"--%>
<%--@elvariable id="immeuble" type="ch.vd.uniregctb.registrefoncier.ResumeImmeubleView"--%>

<unireg:setAuth var="autorisations" tiersId="${idContribuable}"/>
<%--@elvariable id="autorisations" type="ch.vd.uniregctb.tiers.manager.Autorisations"--%>

<jsp:include page="../../../common/degrevement-exoneration/resume-immeuble-fieldset.jsp"/>

<fieldset>
	<legend><span><fmt:message key="label.demandes.degrevement.ici"/></span></legend>
	<c:if test="${autorisations.demandesDegrevementIci}">
		<table border="0">
			<tr><td>
				<c:if test="${empty param['message'] && empty param['retour']}">
					<unireg:raccourciModifier link="../degrevement-exoneration/edit-demandes-degrevement.do?idContribuable=${idContribuable}&idImmeuble=${immeuble.idImmeuble}" tooltip="Modifier" display="label.bouton.modifier"/>
				</c:if>
			</td></tr>
		</table>
	</c:if>
	<c:choose>
		<c:when test="${not empty demandesDegrevement}">
			<jsp:include page="../../../common/degrevement-exoneration/demandes-degrevement-table.jsp">
				<jsp:param name="mode" value="visu"/>
			</jsp:include>
		</c:when>
		<c:otherwise>
			<span style="font-style: italic"><fmt:message key="label.aucun.formulaire.demande.degrevement"/></span>
		</c:otherwise>
	</c:choose>
</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.degrevements.ici"/></span></legend>
	<c:if test="${autorisations.degrevementsIci}">
		<table border="0">
			<tr><td>
				<c:if test="${empty param['message'] && empty param['retour']}">
					<unireg:raccourciModifier link="../degrevement-exoneration/edit-degrevements.do?idContribuable=${idContribuable}&idImmeuble=${immeuble.idImmeuble}" tooltip="Modifier" display="label.bouton.modifier"/>
				</c:if>
			</td></tr>
		</table>
	</c:if>
	<c:choose>
		<c:when test="${not empty degrevements}">
			<jsp:include page="../../../common/degrevement-exoneration/degrevements-table.jsp">
				<jsp:param name="mode" value="visu"/>
			</jsp:include>
		</c:when>
		<c:otherwise>
			<span style="font-style: italic"><fmt:message key="label.aucune.donnee.degrevement"/></span>
		</c:otherwise>
	</c:choose>

</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.exonerations.ifonc"/></span></legend>
	<c:if test="${autorisations.exonerationsIfonc}">
		<table border="0">
			<tr><td>
				<c:if test="${empty param['message'] && empty param['retour']}">
					<unireg:raccourciModifier link="../degrevement-exoneration/edit-exonerations.do?idContribuable=${idContribuable}&idImmeuble=${immeuble.idImmeuble}" tooltip="Modifier" display="label.bouton.modifier"/>
				</c:if>
			</td></tr>
		</table>
	</c:if>
	<c:choose>
		<c:when test="${not empty exonerations}">
			<unireg:nextRowClass reset="1"/>
			<table class="display display_table">
				<thead>
				<tr>
					<th><fmt:message key="label.date.debut"/></th>
					<th><fmt:message key="label.date.fin"/></th>
					<th><fmt:message key="label.pourcentage.exoneration"/></th>
					<th>&nbsp;</th>
				</tr>
				</thead>
				<tbody>
				<c:forEach items="${exonerations}" var="exo">
					<tr class='<unireg:nextRowClass/><c:if test="${exo.annule}"> strike</c:if>'>
						<td><unireg:regdate regdate="${exo.dateDebut}"/></td>
						<td><unireg:regdate regdate="${exo.dateFin}"/></td>
						<td>
							<fmt:formatNumber maxIntegerDigits="4" minFractionDigits="2" value="${exo.pourcentageExoneration}"/><c:if test="${exo.pourcentageExoneration != null}">&nbsp;&percnt;</c:if>
						</td>
						<td>
							<unireg:consulterLog entityNature="AllegementFoncier" entityId="${exo.idExoneration}"/>
						</td>
					</tr>
				</c:forEach>
				</tbody>
			</table>
		</c:when>
		<c:otherwise>
			<span style="font-style: italic"><fmt:message key="label.aucune.donnee.exoneration"/></span>
		</c:otherwise>
	</c:choose>
</fieldset>
