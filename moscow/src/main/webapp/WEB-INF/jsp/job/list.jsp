<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">Liste des jobs</tiles:put>

	<tiles:put name="body">

		<display:table name="${jobs}" id="job" class="table table-striped" requestURI="/job/list.do">
			<display:caption>Les jobs suivants sont configurés</display:caption>
			<display:column sortable="true" title="Id">
				<c:out value="${job.id}"/>
			</display:column>
			<display:column sortable="true" title="Status">
				<c:if test="${job.status == null}">not running</c:if>
				<c:if test="${job.status != null}">running
					<c:if test="${job.status.percentCompletion != null}"><c:out value="${job.status.percentCompletion}"/>%</c:if>
					<c:if test="${job.status.message != null}">- <c:out value="${job.status.message}"/></c:if>
				</c:if>
			</display:column>
			<display:column sortable="true" title="Nom">
				<c:out value="${job.name}"/>
			</display:column>
			<display:column sortable="true" title="Cron">
				<c:out value="${job.cronExpression}"/>
				<form action="run.do" method="POST" style="display:inline;">
					<input type="hidden" name="id" value="${job.id}"/>
					(<a href="run.do" onclick="$(this).parent().submit(); return false;">déclencher maintenant</a>)
				</form>
			</display:column>
			<display:column sortable="true" title="Type">
				<c:out value="${job.type}"/>
			</display:column>
			<display:column sortable="true" title="Répertoire">
				<c:out value="${job.dirPath}"/>
			</display:column>
			<display:column sortable="false">
				<button class="btn btn-mini" onclick="window.location='edit.do?id=${job.id}';">editer</button>
				<form action="del.do" method="POST" style="display:inline;">
					<button class="btn btn-mini btn-danger" name="id" type="submit" value="${job.id}">supprimer</button>
				</form>
			</display:column>
		</display:table>

		<a href="add.do" id="addLink" class="btn btn-primary">Ajouter un job</a>

	</tiles:put>

</tiles:insert>