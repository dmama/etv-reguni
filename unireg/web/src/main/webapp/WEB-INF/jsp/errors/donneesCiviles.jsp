<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="menu" type="String">
	</tiles:put>

	<tiles:put name="title" type="String">Erreur dans les données du registre civil</tiles:put>
	<tiles:put name="connected" type="String">
	</tiles:put>

	<tiles:put name="body" type="String">

		<p>Unireg a détecté une erreur dans les données fournies par le
		registre
		<c:if test="${exception.numeroCtb != null}">
			civil pour le tiers <c:out value="${exception.numeroCtb}"/>.				
		</c:if>
		<c:if test="${exception.numeroCtb == null}">
			civil.				
		</c:if>
		</p>

		<c:if test="${exception.descriptionContexte != null}">
			<p><b><c:out value="${exception.descriptionContexte}"/></b></p>
		</c:if>
		
		<ul>
			<c:forEach items="${exception.errors}" var="error">
				<li style="margin-left:2em;"><c:out value="${error}"/></li>
			</c:forEach>
		</ul>

		<p>Cette erreur peut survenir lorsque des données sont manquantes
		(i.e. un individu sans état-civil), incomplète ou incohérente (i.e.
		une adresse courrier qui finit avant de commencer).</p>
		<h3>Essayer de corriger les données dans le registre civil.</h3>

		<br><hr><br>
		<unireg:callstack exception="${exception}"
			headerMessage="Si vous ne pouvez pas corriger les données vous-mêmes, veuillez contacter l'administrateur en lui communiquant le message d'erreur ci-dessous ">
		</unireg:callstack>

	</tiles:put>
</tiles:insert>