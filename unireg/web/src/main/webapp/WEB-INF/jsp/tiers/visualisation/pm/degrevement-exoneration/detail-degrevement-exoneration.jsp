<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="idContribuable" type="java.lang.Long"--%>
<%--@elvariable id="demandesDegrevement" type="java.util.List<ch.vd.uniregctb.registrefoncier.allegement.DemandeDegrevementICIView>"--%>
<%--@elvariable id="degrevements" type="java.util.List<ch.vd.uniregctb.registrefoncier.allegement.DegrevementICIView>"--%>
<%--@elvariable id="exonerations" type="java.util.List<ch.vd.uniregctb.registrefoncier.allegement.ExonerationIFONCView>"--%>
<%--@elvariable id="immeuble" type="ch.vd.uniregctb.registrefoncier.allegement.ResumeImmeubleView"--%>
<%--@elvariable id="droits" type="java.util.List<ch.vd.uniregctb.registrefoncier.allegement.DroitView>"--%>

<unireg:setAuth var="autorisations" tiersId="${idContribuable}"/>
<%--@elvariable id="autorisations" type="ch.vd.uniregctb.tiers.manager.Autorisations"--%>

<jsp:include page="../../../common/degrevement-exoneration/resume-immeuble-fieldset.jsp"/>

<jsp:include page="../../../common/degrevement-exoneration/droits-fieldset.jsp"/>

<fieldset>
	<legend><span><fmt:message key="label.demandes.degrevement.ici"/></span></legend>
	<fmt:setLocale value="fr_CH" scope="session"/>
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
			<jsp:include page="../../../common/degrevement-exoneration/exonerations-table.jsp">
				<jsp:param name="mode" value="visu"/>
			</jsp:include>
		</c:when>
		<c:otherwise>
			<span style="font-style: italic"><fmt:message key="label.aucune.donnee.exoneration"/></span>
		</c:otherwise>
	</c:choose>
</fieldset>
