<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title">Liste des jobs</tiles:put>

  	<tiles:put name="body">

  		<p>Les jobs suivants sont configurés <a href="add.do" id="addLink">(ajouter)</a> :</p>
		<display:table name="${jobs}" id="job" class="ui-widget ui-widget-content" requestURI="/job/list.do">
			<display:column sortable="true" title="Id">
				<c:out value="${job.id}" />
			</display:column>
			<display:column sortable="true" title="Status">
				<c:if test="${job.status == null}">not running</c:if>
				<c:if test="${job.status != null}">running
					<c:if test="${job.status.percentCompletion != null}"><c:out value="${job.status.percentCompletion}"/>%</c:if>
					<c:if test="${job.status.message != null}">- <c:out value="${job.status.message}"/></c:if>
				</c:if>
			</display:column>
			<display:column sortable="true" title="Nom">
				<c:out value="${job.name}" />
			</display:column>
			<display:column sortable="true" title="Cron">
				<c:out value="${job.cronExpression}" />
				<form action="run.do" method="POST" style="display:inline;">
					<input type="hidden" name="id" value="${job.id}"/>
					(<a href="run.do" onclick="$(this).parent().submit(); return false;">déclencher maintenant</a>)
				</form>
			</display:column>
			<display:column sortable="true" title="Type">
				<c:out value="${job.type}" />
			</display:column>
			<display:column sortable="true" title="Répertoire">
				<c:out value="${job.dirPath}" />
			</display:column>
			<display:column sortable="false">
				<a href="edit.do?id=${job.id}">editer</a> |
				<form action="del.do" method="POST" style="display:inline;">
					<input type="hidden" name="id" value="${job.id}"/>
					<a href="del.do" onclick="$(this).parent().submit(); return false;">supprimer</a>
				</form>
			</display:column>
		</display:table>

		<script>
			$(function() {
				$('table.ui-widget thead tr').addClass('ui-widget-header');
			});
		</script>
  	</tiles:put>

</tiles:insert>