<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<fieldset class="<c:if test="${command.individuConjoint != null || command.tiersConjoint != null}">individu</c:if><c:if test="${command.individuConjoint == null && command.tiersConjoint == null}">information</c:if>">
	<legend><span><fmt:message key="label.habitant" /></span></legend>
	<c:choose>
		<c:when test="${command.natureMembrePrincipal == 'Habitant'}">
			<jsp:include page="individu-core.jsp">
				<jsp:param name="path" value="individu" />
			</jsp:include>
		</c:when>
		<c:when test="${command.natureMembrePrincipal == 'NonHabitant'}">
			<jsp:include page="non-habitant-core.jsp">
				<jsp:param name="path" value="tiersPrincipal" />
			</jsp:include>
		</c:when>
	</c:choose>
</fieldset>
<c:if test="${command.individuConjoint != null || command.tiersConjoint != null}">
<fieldset class="individu">
	<legend><span><fmt:message key="label.conjoint" /></span></legend>
	<c:choose>
		<c:when test="${command.natureMembreConjoint == 'Habitant'}">
			<jsp:include page="individu-core.jsp">
				<jsp:param name="path" value="individuConjoint" />
			</jsp:include>
		</c:when>
		<c:when test="${command.natureMembreConjoint == 'NonHabitant'}">
			<jsp:include page="non-habitant-core.jsp">
				<jsp:param name="path" value="tiersConjoint" />
			</jsp:include>
		</c:when>
	</c:choose>
</fieldset>	
</c:if>



