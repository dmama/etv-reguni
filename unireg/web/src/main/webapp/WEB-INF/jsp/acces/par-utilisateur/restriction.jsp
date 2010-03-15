<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Rapport Menage Commun -->
<fieldset>
	<legend><span><fmt:message key="label.caracteristiques.droit.acces" /></span></legend>
	<table>
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td width="25%"><fmt:message key="label.type.restriction" />&nbsp;:</td>
			<td width="25%">
				<form:select path="type" items="${typesDroitAcces}" />
			</td>
			<td width="25%"><fmt:message key="label.lecture.seule" />&nbsp;:</td>
			<td width="25%">
				<form:checkbox path="lectureSeule" />
			</td>
		</tr>
	</table>
	
</fieldset>
<!-- Fin Rapport Menage Commun -->