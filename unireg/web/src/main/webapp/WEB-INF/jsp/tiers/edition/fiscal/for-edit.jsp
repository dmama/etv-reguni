<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="index" value="${param.index}" />

<c:if test="${index == ''}">
	<%-- Ajout d'un nouveau for --%>
	<c:choose>
		<c:when test="${command.natureForFiscal == 'ForDebiteurPrestationImposable'}">
			<jsp:include page="for-ajout-debiteur.jsp"/>
		</c:when>
		<c:when test="${command.natureForFiscal != 'ForDebiteurPrestationImposable'}">
			<jsp:include page="for-ajout.jsp"/>
		</c:when>
	</c:choose>
</c:if>

<c:if test="${index == 'modeimposition'}">
	<%-- Modification du mode d'imposition du for principal courant --%>
	<jsp:include page="for-modif-mode-imposition.jsp"/>
</c:if>

<c:if test="${index != '' && index != 'modeimposition'}">
	<%-- Modification d'un for fiscal existant --%>
	<c:choose>
		<c:when test="${command.natureForFiscal == 'ForDebiteurPrestationImposable'}">
			<jsp:include page="for-modif-debiteur.jsp"/>
		</c:when>
		<c:when test="${command.natureForFiscal != 'ForDebiteurPrestationImposable'}">
			<jsp:include page="for-modif.jsp"/>
		</c:when>
	</c:choose>
</c:if>
