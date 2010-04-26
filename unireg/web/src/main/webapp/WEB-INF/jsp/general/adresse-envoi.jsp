<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="path" value="${param.path}" />
<c:set var="bind" value="command.${path}" scope="request"/>
<spring:bind path="${bind}" >
	<c:set var="tiersGeneral" value="${status.value}"  scope="request"/>
</spring:bind>
<c:if test="${tiersGeneral.adresseEnvoi != null}">
	<c:if test="${tiersGeneral.adresseEnvoi.ligne1 != null}">
	<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
		<td width="25%"><fmt:message key="label.adresse" />&nbsp;:</td>
		<td width="75%" colspan="2">${tiersGeneral.adresseEnvoi.ligne1}</td>
	</tr>
	</c:if>
	<c:if test="${tiersGeneral.adresseEnvoi.ligne2 != null }">
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />	
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td width="25%">&nbsp;</td>
			<td width="75%" colspan="2">${tiersGeneral.adresseEnvoi.ligne2}</td>
		</tr>
	</c:if>
	<c:if test="${tiersGeneral.adresseEnvoi.ligne3 != null }">
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td width="25%">&nbsp;</td>
			<td width="75%" colspan="2">${tiersGeneral.adresseEnvoi.ligne3}</td>
		</tr>
	</c:if>
	<c:if test="${tiersGeneral.adresseEnvoi.ligne4 != null }">
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td width="25%">&nbsp;</td>
			<td width="75%" colspan="2">${tiersGeneral.adresseEnvoi.ligne4}</td>
		</tr>
	</c:if>
	<c:if test="${tiersGeneral.adresseEnvoi.ligne5 != null}" >
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td width="25%">&nbsp;</td>
			<td width="75%" colspan="2">${tiersGeneral.adresseEnvoi.ligne5}</td>
		</tr>
	</c:if>
	<c:if test="${tiersGeneral.adresseEnvoi.ligne6 != null}" >
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td width="25%">&nbsp;</td>
			<td width="75%" colspan="2">${tiersGeneral.adresseEnvoi.ligne6}</td>
		</tr>
	</c:if>
	<c:if test="${tiersGeneral.adresseEnvoiException != null}">
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td width="25%"><fmt:message key="label.adresse" />&nbsp;:</td>
			<td  width="75%"  colspan="2" class="error"><fmt:message key="error.adresse.envoi.entete" /></td>
		</tr>
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />	
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td width="25%">&nbsp;</td>
			<td  width="75%"  colspan="2" class="error">=&gt;&nbsp;${tiersGeneral.adresseEnvoiException.message}</td>
		</tr>
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td width="25%">&nbsp;</td>
			<td  width="75%"  colspan="2" class="error"><fmt:message key="error.adresse.envoi.remarque" /></td>
		</tr>
	</c:if>
	
</c:if>