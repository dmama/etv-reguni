<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/templateDialog.jsp">
  	<tiles:put name="title"></tiles:put>
  	<tiles:put name="body">
  			<c:if test="${command.source == null && command.errorMessage == null}">
				<fmt:message key="label.nonHabitant.adresse.vaudoise.inconnue"/>
  			</c:if>
  			<c:if test="${command.errorMessage != null}">
				Erreur&nbsp;:&nbsp;<c:out value="${command.errorMessage}"/>
  			</c:if>
  			<c:if test="${command.source != null}">
  				<b>Dernière adresse vaudoise</b><br/>
				Source&nbsp;:&nbsp;<b><c:out value="${command.source}"/></b><br/>
	  			<c:if test="${command.complements != null}">
					<c:out value="${command.complements}"/><br/>
	  			</c:if>
				<c:if test="${command.rue != null}">
					<c:out value="${command.rue}"/><br/>
	  			</c:if>
	  			<c:if test="${command.localite != null}">
					<c:out value="${command.localite}"/><br/>
	  			</c:if>
	  			<c:if test="${command.pays != null}">
					<c:out value="${command.pays}"/><br/>
	  			</c:if>
  			</c:if>
	</tiles:put>
</tiles:insert>
