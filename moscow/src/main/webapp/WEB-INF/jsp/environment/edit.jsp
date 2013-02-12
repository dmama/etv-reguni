<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">Edition d'un environnement</tiles:put>

	<tiles:put name="body">

		<form:form commandName="environment" action="edit.do" method="POST" cssClass="form-horizontal">
			<fieldset>
			<legend>Edition du répertoire n°<c:out value="${environment.id}"/></legend>

			<form:hidden path="id"/>
			<div class="control-group">
				<label class="control-label" for="name">Nom</label>

				<div class="controls">
					<form:input path="name" id="name"/>
				</div>
			</div>

			<div class="form-actions">
			<input type="submit" value="Sauver" class="btn btn-primary"/> ou <a href="<c:url value="/environment/list.do"/>">annuler</a>
			</div>
			</fieldset>
		</form:form>

	</tiles:put>

</tiles:insert>