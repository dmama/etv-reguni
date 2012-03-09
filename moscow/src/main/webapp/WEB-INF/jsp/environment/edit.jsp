<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title">Edition d'un environnement</tiles:put>

  	<tiles:put name="body">

  		<h1>Edition du répertoire n°<c:out value="${environment.id}"/></h1>

		<form:form commandName="environment" action="edit.do" method="POST">
			<fieldset class="ui-widget-content">
				<form:hidden path="id"/>
				<label for="name">Nom :</label><form:input path="name" id="name"/><br/>
			</fieldset><br/>
			<input type="submit" value="Sauver"/> ou <a href="<c:url value="/environment/list.do"/>">annuler</a>
		</form:form>

  	</tiles:put>

</tiles:insert>