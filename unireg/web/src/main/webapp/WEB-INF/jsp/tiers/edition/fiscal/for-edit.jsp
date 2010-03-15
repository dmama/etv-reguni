<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="index" value="${param.index}" />

<c:if test="${index == ''}">
	<c:choose>
		<c:when test="${command.natureForFiscal == 'ForDebiteurPrestationImposable'}">
			<jsp:include page="for-ajout-debiteur.jsp"/>
		</c:when>
		<c:when test="${command.natureForFiscal != 'ForDebiteurPrestationImposable'}">
			<jsp:include page="for-ajout.jsp"/>
		</c:when>
	</c:choose>
</c:if>
<c:if test="${index != ''}">
	<c:choose>
		<c:when test="${command.natureForFiscal == 'ForDebiteurPrestationImposable'}">
			<jsp:include page="for-modif-debiteur.jsp"/>
		</c:when>
		<c:when test="${command.natureForFiscal != 'ForDebiteurPrestationImposable'}">
			<jsp:include page="for-modif.jsp"/>
		</c:when>
	</c:choose>
</c:if>

<c:if test="${command.natureForFiscal != 'ForDebiteurPrestationImposable'}">
	<c:if test="${index == ''}">
		<script type="text/javascript">
			selectGenreImpot('${command.genreImpot}');
		</script>
	</c:if>
	<script type="text/javascript">
		selectForFiscal('${command.typeAutoriteFiscale}');
	</script>
</c:if>