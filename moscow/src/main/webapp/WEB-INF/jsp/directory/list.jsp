<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">Liste des répertoires de log</tiles:put>

	<tiles:put name="body">

		<div id="addDialog">
			<form id="addForm" action="add.do" method="POST">
				<label>Environnement :</label><select name="env_id">
				<c:forEach var="entry" items="${environments}">
					<option value="${entry.id}"><c:out value="${entry.name}"/></option>
				</c:forEach>
			</select><br/>
				<label for="path">Chemin :</label><input type="text" name="path" id="path"/><br/>
				<label for="pattern">Pattern :</label><input type="text" name="pattern" id="pattern"/>
			</form>
		</div>

		<display:table name="${directories}" id="dir" class="table table-striped" requestURI="/directory/list.do">
			<display:caption>Les répertoires suivants sont configurés</display:caption>
			<display:column sortable="true" title="Id">
				<c:out value="${dir.id}"/>
			</display:column>
			<display:column sortable="true" title="Environnment">
				<c:out value="${dir.envName}"/>
			</display:column>
			<display:column sortable="true" title="Chemin">
				<c:out value="${dir.directoryPath}"/>
			</display:column>
			<display:column sortable="true" title="Pattern">
				<c:out value="${dir.pattern}"/>
			</display:column>
			<display:column sortable="false">
				<button class="btn btn-mini" onclick="window.location='edit.do?id=${dir.id}';">editer</button>
				<form action="del.do" method="POST" style="display:inline;">
					<button class="btn btn-mini btn-danger" name="id" type="submit" value="${dir.id}">supprimer</button>
				</form>
			</display:column>
		</display:table>

		<a href="#" id="addLink" class="btn btn-primary">Ajouter un répertoire</a>

		<script type="text/javascript">
			$(function () {
				$('#addDialog').dialog({
					title:"Ajout d'un nouveau répertoire",
					autoOpen:false,
					modal:true,
					buttons:{
						"Ajouter":function () {
							$('#addForm').submit();
						},
						"Annuler":function () {
							$(this).dialog('close');
						}
					}
				});
				$('#addLink').click(function () {
					$('#addDialog').dialog('open');
					return false;
				});
			});
		</script>
	</tiles:put>

</tiles:insert>