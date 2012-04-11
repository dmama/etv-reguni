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

  		<p>Les environnements suivants sont configurés <a href="#" id="addLink">(ajouter)</a> :</p>

		<display:table name="${environments}" id="env" class="ui-widget ui-widget-content" requestURI="/environment/list.do">
			<display:column sortable="true" title="Id">
				<c:out value="${env.id}" />
			</display:column>
			<display:column sortable="true" title="Environnment">
				<c:out value="${env.name}" />
			</display:column>
			<display:column sortable="false">
				<a href="edit.do?id=${env.id}">editer</a> |
				<form action="del.do" method="POST" style="display:inline;">
					<input type="hidden" name="id" value="${env.id}"/>
					<a href="del.do" onclick="$(this).parent().submit(); return false;">supprimer</a>
				</form>
			</display:column>
		</display:table>

		<script>
			$(function() {
				$('table.ui-widget thead tr').addClass('ui-widget-header');
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