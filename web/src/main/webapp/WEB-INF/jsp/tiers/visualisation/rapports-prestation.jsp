<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut CTB associé + Rapports prestation -->
<unireg:setAuth var="autorisations" tiersId="${command.tiers.numero}"/>
<c:if test="${autorisations.rapportsDePrestations}">
	<table border="0">
		<tr>
			<td>
				<unireg:raccourciModifier link="../rapports-prestation/edit.do?id=${command.tiers.numero}" tooltip="Modifier les rapports de prestation" display="label.bouton.modifier"/>
			</td>
		</tr>
	</table>
</c:if>

<fieldset>
	<legend><span><fmt:message key="label.contribuable.associe" /></span></legend>
	<input class="noprint" name="ctbAssocieHisto" type="checkbox" id="ctbAssocieHisto" <c:if test="${command.ctbAssocieHisto}">checked</c:if> onClick="window.location = App.toggleBooleanParam(window.location, 'ctbAssocieHisto', true)" />
	<label class="noprint" for="ctbAssocieHisto"><fmt:message key="label.historique" /></label>
		
	<jsp:include page="../common/contribuable-associe.jsp">
		<jsp:param name="page" value="visu"/>
	</jsp:include>
</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.rapports.prestation" /></span></legend>

	<table border="0">
		<tr>
			<td>
				<input class="noprint" name="rapportsPrestationHisto" type="checkbox" id="rapportsPrestationHisto" <c:if test="${command.rapportsPrestationHisto}">checked</c:if> onClick="window.location = App.toggleBooleanParam(window.location, 'rapportsPrestationHisto', true)" />
				<label class="noprint" for="rapportsPrestationHisto"><fmt:message key="label.historique" /></label>
			</td>

			<td id="timeline" align="right">
				<a href="<c:url value="/rapports-prestation/list.do?idDpi=" /><c:out value="${command.tiers.numero}" />" >Liste complète</a>
			</td>
		</tr>
	</table>

	<jsp:include page="../common/rapports-prestation.jsp">
		<jsp:param name="page" value="visu"/>
	</jsp:include>
</fieldset>

<!-- Fin CTB associé + Rapports prestation -->
