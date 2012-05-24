<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title">Edition d'un répertoire de log</tiles:put>

  	<tiles:put name="body">

  		<h1>Edition du répertoire n°<c:out value="${directory.id}"/></h1>

		<form:form commandName="directory" action="edit.do" method="POST">
			<fieldset class="ui-widget-content">
				<form:hidden path="id"/>
				<label for="env">Environnement :</label><form:select id="env" path="envId"><form:options items="${environments}" itemValue="id" itemLabel="name"/></form:select><br/>
				<label for="path">Chemin :</label><form:input path="directoryPath" id="path"/><br/>
				<label for="pattenr">Pattern :</label><form:input path="pattern" id="pattern"/>
			</fieldset><br/>
			<input type="submit" value="Sauver"/> ou <a href="<c:url value="/directory/list.do"/>">annuler</a>
		</form:form>

  	</tiles:put>

</tiles:insert>