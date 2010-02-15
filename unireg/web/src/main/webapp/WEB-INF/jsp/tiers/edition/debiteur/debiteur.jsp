<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Debiteur Prestation Imposable -->

<fieldset>
	<legend><span><fmt:message key="label.debiteur.is" /></span></legend>
	
	<table border="0">
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td>
				<a href="../tiers/edit.do?nature=DebiteurPrestationImposable&amp;numeroCtbAss=<c:out value="${command.tiers.numero}"></c:out>" 
				class="add" title="Ajouter debiteur">&nbsp;<fmt:message key="label.bouton.ajouter" /></a>
			</td>
		</tr>
	</table>
	<jsp:include page="../../common/debiteur.jsp">
		<jsp:param name="page" value="edit"/>
	</jsp:include>
</fieldset>
<!-- Fin Debiteur Prestation Imposable -->