<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Fiscal -->
	<c:choose>
		<c:when test="${command.natureTiers == 'DebiteurPrestationImposable'}">
			<jsp:include page="debiteur-old.jsp"/>
			<c:if test="${command.numeroCtbAssocie == null}">
		 		<jsp:include page="for-debiteur.jsp"/>
			</c:if>
		</c:when>
		<c:when test="${command.natureTiers != 'DebiteurPrestationImposable'}">
			<c:if test="${command.autorisations.forsPrincipaux ||
						  command.autorisations.forsSecondaires ||
						  command.autorisations.forsAutresImpots ||
						  command.autorisations.forsAutresElementsImposables}">
				<jsp:include page="for.jsp"/>
			</c:if>
			<c:if test="${command.autorisations.situationsFamille}">
				<jsp:include page="situation-famille.jsp"/>
			</c:if>
		</c:when>
	</c:choose>
<!-- Fin Fiscal -->
