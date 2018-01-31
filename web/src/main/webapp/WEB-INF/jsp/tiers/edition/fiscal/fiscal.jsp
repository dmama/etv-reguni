<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Fiscal -->
<unireg:setAuth var="autorisations" tiersId="${command.tiersGeneral.numero}"/>
<c:if test="${autorisations.decisionsAci}">
    <jsp:include page="decision-aci.jsp"/>
</c:if>
<c:if test="${autorisations.forsPrincipaux ||
			  autorisations.forsSecondaires ||
			  autorisations.forsAutresImpots ||
			  autorisations.forsAutresElementsImposables}">
	<jsp:include page="../../common/fiscal/for.jsp">
		<jsp:param name="page" value="edit"/>
	</jsp:include>
</c:if>

<c:if test="${autorisations.situationsFamille}">
	<jsp:include page="situation-famille.jsp"/>
</c:if>
<!-- Fin Fiscal -->
