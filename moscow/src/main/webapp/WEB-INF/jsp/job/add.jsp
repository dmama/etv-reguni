<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title">Ajout d'un nouveau job</tiles:put>

  	<tiles:put name="body">

  		<h1>Ajout d'un nouveau job</h1>

		<form:form commandName="job" action="add.do" method="POST">
			<fieldset class="ui-widget-content">
				<label for="name">Nom :</label><form:input path="name" id="name"/><br/>
				<label for="name">Cron :</label><form:input path="cronExpression" id="cron"/><br/>
				<label for="env">Répertoire d''import :</label><form:select id="dir" path="dirId"><form:options items="${directories}" itemValue="id" itemLabel="name"/></form:select><br/>
			</fieldset><br/>
			<input type="submit" value="Ajouter"/> ou <a href="<c:url value="/job/list.do"/>">annuler</a>
		</form:form>

  	</tiles:put>

</tiles:insert>