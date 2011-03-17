<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:if test="${command.allowedOnglet.CIVIL}">
	<table border="0">
		<tr>
			<td>
				<c:if test="${empty param['message'] && empty param['retour']}">
					<unireg:raccourciModifier link="../civil/edit.do?id=${command.tiers.numero}" tooltip="Modifier la partie fiscale" display="label.bouton.modifier"/>
				</c:if>	
			</td>
		</tr>
	</table>
</c:if>	

<c:choose>
	<c:when test="${command.natureTiers == 'Habitant'}"><jsp:include page="individu.jsp"/></c:when>
	<c:when test="${command.natureTiers == 'NonHabitant'}"><jsp:include page="non-habitant.jsp"/></c:when>
	<c:when test="${command.natureTiers == 'MenageCommun'}"><jsp:include page="menage-commun.jsp"/></c:when>
	<c:when test="${command.natureTiers == 'AutreCommunaute'}"><jsp:include page="organisation.jsp"/></c:when>
	<c:when test="${command.natureTiers == 'Entreprise'}"><jsp:include page="entreprise.jsp"/></c:when>
	<c:when test="${command.natureTiers == 'Etablissement'}"><jsp:include page="etablissement.jsp"/></c:when>
</c:choose>
