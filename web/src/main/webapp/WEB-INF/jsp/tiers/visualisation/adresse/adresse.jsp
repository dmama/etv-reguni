<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Adresse -->
<unireg:setAuth var="autorisations" tiersId="${command.tiers.numero}"/>

<c:set var="adressesHisto" value="${command.adressesHisto != null ? command.adressesHisto : false}" />
<c:set var="adressesHistoCiviles" value="${command.adressesHistoCiviles != null ? command.adressesHistoCiviles : false}" />
<c:set var="adressesHistoCivilesConjoint" value="${command.adressesHistoCivilesConjoint != null ? command.adressesHistoCivilesConjoint : false}" />

<c:if test="${autorisations.adresses}">
<table border="0">
	<tr>
		<td>
		<c:if test="${empty param['message'] && empty param['retour']}">
			<unireg:raccourciModifier link="../adresses/edit.do?id=${command.tiers.numero}" tooltip="Modifier les adresses" display="label.bouton.modifier"/>
		</c:if>
		</td>

		<authz:authorize access="hasAnyRole('ROLE_SUPERGRA')">
			<td id="timeline" align="right">
				<a href='<c:url value="/adresses/timeline.do?id=" /><c:out value="${command.tiers.numero}" />'><fmt:message key="title.vue.chronologique"/></a>
			</td>
		</authz:authorize>
	</tr>
</table>
</c:if>

<%-- Les adresses fiscales --%>
<fieldset>
	<legend><span><fmt:message key="label.adresse.fiscales" /></span></legend>

	<input class="noprint" name="adrHistoFiscale" type="checkbox" <c:if test="${adressesHisto}">checked</c:if> onClick="window.location = App.toggleBooleanParam(window.location, 'adressesHisto', true);" id="isAdrHisto" />
	<label class="noprint" for="isAdrHisto"><fmt:message key="label.historique" /></label>

	<jsp:include page="../../common/adresse/adresseFiscale.jsp">
		<jsp:param name="page" value="visu"/>
	</jsp:include>
</fieldset>

<%-- Les adresses civiles du principal --%>
<fieldset id="adrCivPrincipalFieldset">
	<legend>
		<span>
			<c:choose>
				<c:when test="${command.natureTiers == 'MenageCommun'}" >
					<fmt:message key="label.adresse.civiles.membre.couple" /><c:out value=" ${command.nomPrenomPrincipal}"/>
				</c:when>
				<c:otherwise>
					<fmt:message key="label.adresse.civiles" />
				</c:otherwise>
			</c:choose>
		</span>
	</legend>

	<input class="noprint" name="adrHistoCiviles" type="checkbox" <c:if test="${adressesHistoCiviles}">checked</c:if> onclick="window.location = App.toggleBooleanParam(window.location, 'adressesHistoCiviles', true);" id="isAdrHistoCiviles" />
	<label class="noprint" for="isAdrHistoCiviles"><fmt:message key="label.historique" /></label>
	<jsp:include page="../../common/adresse/adresseCivile.jsp">
		<jsp:param name="page" value="visu"/>
		<jsp:param name="membre" value="principal"/>
	</jsp:include>
</fieldset>

<%-- Les adresses civiles du conjoint --%>
<fieldset id="adrCivConjointFieldset" <c:if test="${command.tiersConjoint == null}">style="display:none"</c:if> >
	<legend><span><fmt:message key="label.adresse.civiles.membre.couple" /><c:out value=" ${command.nomPrenomConjoint}"/></span></legend>

	<input class="noprint" name="adrHistoCivilesConjoint" type="checkbox" <c:if test="${adressesHistoCivilesConjoint}">checked</c:if> onclick="window.location = App.toggleBooleanParam(window.location, 'adressesHistoCivilesConjoint', true);" id="isAdrHistoCivilesConjoint" />
	<label class="noprint" for="isAdrHistoCivilesConjoint"><fmt:message key="label.historique" /></label>

	<jsp:include page="../../common/adresse/adresseCivile.jsp">
		<jsp:param name="page" value="visu"/>
		<jsp:param name="membre" value="conjoint"/>
	</jsp:include>
</fieldset>

<!-- Fin Adresse -->
