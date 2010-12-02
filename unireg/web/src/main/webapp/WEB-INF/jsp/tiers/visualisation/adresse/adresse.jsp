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
	</tr>
</table>
</c:if>
<fieldset>
	<legend><span><fmt:message key="label.adresse.fiscales" /></span></legend>

	<input name="adrHistoFiscales" type="checkbox" <c:if test="${command.adressesHisto}">checked</c:if> onclick="toggleRowsIsHisto('adresse','isAdrHisto',2);" id="isAdrHisto" />
	<label for="isAdrHisto"><fmt:message key="label.historique" /></label>

	<jsp:include page="../../common/adresse/adresseFiscale.jsp">
		<jsp:param name="page" value="visu"/>
	</jsp:include>
</fieldset>
<fieldset>
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

	<input name="adrHistoCiviles" type="checkbox" <c:if test="${command.adressesHistoCiviles}">checked</c:if> onclick="toggleRowsIsHisto('adresseCivile','isAdrHistoCiviles',2);" id="isAdrHistoCiviles" />
	<label for="isAdrHistoCiviles"><fmt:message key="label.historique" /></label>
	<jsp:include page="../../common/adresse/adresseCivile.jsp">
		<jsp:param name="page" value="visu"/>
		<jsp:param name="membre" value="principal"/>
	</jsp:include>
</fieldset>

<fieldset <c:if test="${command.tiersConjoint == null}">style="display:none"</c:if> >
	<legend><span><fmt:message key="label.adresse.civiles.membre.couple" />${command.nomPrenomConjoint}</span></legend>

	<input name="adrHistoCivilesConjoint" type="checkbox" <c:if test="${command.adressesHistoCivilesConjoint}">checked</c:if> onclick="toggleRowsIsHisto('adresseCivileConjoint','isAdrHistoCivilesConjoint',2);" id="isAdrHistoCivilesConjoint" />
	<label for="isAdrHistoCivilesConjoint"><fmt:message key="label.historique" /></label>

	<jsp:include page="../../common/adresse/adresseCivile.jsp">
		<jsp:param name="page" value="visu"/>
		<jsp:param name="membre" value="conjoint"/>
	</jsp:include>
</fieldset>





<!-- Fin Adresse -->
