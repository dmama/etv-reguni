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
	<legend><span><fmt:message key="label.adresse.fiscal" /></span></legend>
	<input name="adrFiscaleHisto" type="checkbox" <c:if test="${command.adressesFiscalesHisto}">checked</c:if> onclick="afficheAdressesFiscalesHisto('isAdrFiscaleHisto', ${command.tiersGeneral.numero});" id="isAdrFiscaleHisto" />
	<label for="isAdrFiscaleeHisto"><fmt:message key="label.historique" /></label>

	<jsp:include page="../../common/adresse/adresseFiscale.jsp">
		<jsp:param name="page" value="visu"/>
	</jsp:include>
</fieldset>
<c:if test="${not empty command.historiqueAdressesCiviles}">
	<fieldset>
		<legend><span><fmt:message key="label.adresse.civil" /></span></legend>
		<input name="adrCivileHisto" type="checkbox" <c:if test="${command.adressesCivilesHisto}">checked</c:if> onclick="afficheAdressesCivilesHisto('isAdrCivileHisto', ${command.tiersGeneral.numero});" id="isAdrCivileHisto" />
		<label for="isAdrCivileHisto"><fmt:message key="label.historique" /></label>

		<jsp:include page="../../common/adresse/adresseCivile.jsp">
			<jsp:param name="page" value="visu"/>
		</jsp:include>		
	</fieldset>
</c:if>
<!-- Fin Adresse -->
