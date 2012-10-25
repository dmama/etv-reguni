<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/templateDialog.jsp">
  	<tiles:put name="title"></tiles:put>
  	<tiles:put name="body">
  			<c:if test="${command.numeroIndividu == null}">
				<fmt:message key="label.nonHabitant"/>
  			</c:if>
  			<c:if test="${command.numeroIndividu != null}">
				<fmt:message key="label.numero.individu"/>&nbsp;:&nbsp;<b><c:out value="${command.numeroIndividu}"/></b><br/>
				<fmt:message key="label.nom"/>&nbsp;:&nbsp;<b><c:out value="${command.nom}"/></b><br/>
				<fmt:message key="label.nom.naissance"/>&nbsp;:&nbsp;<b><c:out value="${command.nomNaissance}"/></b><br/>
				<fmt:message key="label.prenom"/>&nbsp;:&nbsp;<b><c:out value="${command.prenom}"/></b><br/>
				<fmt:message key="label.autres.prenoms"/>&nbsp;:&nbsp;<b><c:out value="${command.autresPrenoms}"/></b><br/>
				<fmt:message key="label.sexe"/>&nbsp;:&nbsp;<b><c:out value="${command.sexe != null ? command.sexe : ''}"/></b><br/>
				<fmt:message key="label.date.naissance"/>&nbsp;:&nbsp;<b><unireg:regdate regdate="${command.dateNaissance}"/></b><br/>
				<fmt:message key="label.etat.civil"/>&nbsp;:&nbsp;<b><c:out value="${command.etatCivil}"/></b><br/>
				<fmt:message key="label.nouveau.numero.avs"/>&nbsp;:&nbsp;<b><c:out value="${command.numeroAssureSocial}"/></b><br/>
				<fmt:message key="label.ancien.numero.avs"/>&nbsp;:&nbsp;<b><c:out value="${command.ancienNumeroAVS}"/></b><br/>
				<fmt:message key="label.nationalite"/>&nbsp;:&nbsp;<b><c:out value="${command.nationalite}"/></b><br/>
  			</c:if>
	</tiles:put>
</tiles:insert>
