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
	<legend><span><fmt:message key="label.adresse" /></span></legend>

	<input name="adrHisto" type="checkbox" <c:if test="${command.adressesHisto}">checked</c:if> onclick="afficheAdressesHisto('isAdrHisto', ${command.tiersGeneral.numero});" id="isAdrHisto" />
	<label for="isAdrHisto"><fmt:message key="label.historique" /></label>

	<jsp:include page="../../common/adresse/adresse.jsp">
		<jsp:param name="page" value="visu"/>
	</jsp:include>
</fieldset>
<!-- Fin Adresse -->
