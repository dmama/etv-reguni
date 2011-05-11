<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Adresse -->
<c:if test="${command.allowedOnglet.ADR}">
<table border="0">
	<tr>
		<td>
		<c:if test="${empty param['message'] && empty param['retour']}">
			<unireg:raccourciModifier link="../adresses/edit.do?id=${command.tiers.numero}" tooltip="Modifier les adresses" display="label.bouton.modifier"/>
		</c:if>	
		</td>

		<authz:authorize ifAnyGranted="ROLE_SUPERGRA">
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

	<input class="noprint" name="adrHistoFiscales" type="checkbox" <c:if test="${command.adressesHisto}">checked</c:if> onclick="toggleRowsIsHisto('adresse','isAdrHisto',2);" id="isAdrHisto" />
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
				<c:when test="${command.natureTiers != 'MenageCommun'}" >
					<fmt:message key="label.adresse.civiles" />
				</c:when>
				<c:when test="${command.natureTiers == 'MenageCommun'}" >
					<fmt:message key="label.adresse.civiles.membre.couple" /> ${command.nomPrenomPrincipal}
				</c:when>
			</c:choose>
		</span>
	</legend>

	<input class="noprint" name="adrHistoCiviles" type="checkbox" <c:if test="${command.adressesHistoCiviles}">checked</c:if> onclick="toggleRowsIsHisto('adresseCivile','isAdrHistoCiviles',2);" id="isAdrHistoCiviles" />
	<label class="noprint" for="isAdrHistoCiviles"><fmt:message key="label.historique" /></label>
	<jsp:include page="../../common/adresse/adresseCivile.jsp">
		<jsp:param name="page" value="visu"/>
		<jsp:param name="membre" value="principal"/>
	</jsp:include>
</fieldset>

<%-- Les adresses civiles du conjoint --%>
<fieldset id="adrCivConjointFieldset" <c:if test="${command.tiersConjoint == null}">style="display:none"</c:if> >
	<legend><span><fmt:message key="label.adresse.civiles.membre.couple" />${command.nomPrenomConjoint}</span></legend>

	<input class="noprint" name="adrHistoCivilesConjoint" type="checkbox" <c:if test="${command.adressesHistoCivilesConjoint}">checked</c:if> onclick="toggleRowsIsHisto('adresseCivileConjoint','isAdrHistoCivilesConjoint',2);" id="isAdrHistoCivilesConjoint" />
	<label class="noprint" for="isAdrHistoCivilesConjoint"><fmt:message key="label.historique" /></label>

	<jsp:include page="../../common/adresse/adresseCivile.jsp">
		<jsp:param name="page" value="visu"/>
		<jsp:param name="membre" value="conjoint"/>
	</jsp:include>
</fieldset>


<!--[if IE 6]>
<script>
	$(function() {
		$('#isAdrHisto').click(function() {
			// [SIFISC-787] on force le recalcul des widgets parce que IE6 ne détecte pas le changement autrement.
			$('#adrCivPrincipalFieldset').addClass('toresize');
			$('#adrCivPrincipalFieldset').removeClass('toresize');
		});

		$('#isAdrHistoCiviles').click(function() {
			// [SIFISC-787] on force le recalcul des widgets parce que IE6 ne détecte pas le changement autrement.
			$('#adrCivConjointFieldset').addClass('toresize');
			$('#adrCivConjointFieldset').removeClass('toresize');
		});
	});
</script>
<![endif]-->


<!-- Fin Adresse -->
