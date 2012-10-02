<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ include file="/WEB-INF/jsp/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title">Liste des environnements</tiles:put>

  	<tiles:put name="body">

		<div id="addDialog">
			<form id="addForm" action="add.do" method="POST">
				<label for="name">Nom :</label><input type="text" name="name" id="name"/><br/>
			</form>
		</div>

		<display:table name="${environments}" id="env" class="table table-striped" requestURI="/environment/list.do">
			<display:caption>Les environnements suivants sont configurés</display:caption>
			<display:column sortable="true" title="Id">
				<c:out value="${env.id}" />
			</display:column>
			<display:column sortable="true" title="Environnment">
				<c:out value="${env.name}" />
			</display:column>
			<display:column sortable="false">
				<button class="btn btn-mini" onclick="window.location='edit.do?id=${env.id}';">editer</button>
				<form action="del.do" method="POST" style="display:inline;">
					<button class="btn btn-mini btn-danger" name="id" type="submit" value="${env.id}">supprimer</button>
				</form>
			</display:column>
		</display:table>

		<a href="#" id="addLink" class="btn btn-primary">Ajouter un environnement</a>

		<script type="text/javascript">
			$(function() {
				$('#addDialog').dialog({
						title: "Ajout d'un nouvel environnement",
						autoOpen: false,
						modal: true,
						buttons : {
							"Ajouter" : function () {$('#addForm').submit();},
							"Annuler" : function () {$(this).dialog('close');}
						}
				 	});
				$('#addLink').click(function() {
					$('#addDialog').dialog('open');
					return false;
				});
			});
		</script>
  	</tiles:put>

</tiles:insert>