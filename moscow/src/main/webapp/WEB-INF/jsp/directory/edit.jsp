<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">Edition d'un répertoire de log</tiles:put>

	<tiles:put name="body">

		<h1>Edition du répertoire n°<c:out value="${directory.id}"/></h1>

		<form:form commandName="directory" action="edit.do" method="POST" cssClass="form-horizontal">
			<legend>Edition du répertoire n°<c:out value="${directory.id}"/></legend>

			<form:hidden path="id"/>
			<div class="control-group">
				<label class="control-label" for="env">Environnement :</label>

				<div class="controls">
					<form:select id="env" path="envId"><form:options items="${environments}" itemValue="id" itemLabel="name"/></form:select>
				</div>
			</div>
			<div class="control-group">
				<label class="control-label" for="path">Chemin :</label>

				<div class="controls">
					<form:input path="directoryPath" id="path"/>
				</div>
			</div>
			<div class="control-group">
				<label class="control-label" for="pattern">Pattern :</label>

				<div class="controls">
					<form:input path="pattern" id="pattern"/>
				</div>
			</div>
			<div class="form-actions">
				<input type="submit" value="Sauver" class="btn btn-primary"/> ou <a href="<c:url value="/directory/list.do"/>">annuler</a>
			</div>
		</form:form>

	</tiles:put>

</tiles:insert>