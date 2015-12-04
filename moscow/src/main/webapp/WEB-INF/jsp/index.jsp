<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">Charge des web-services d'Unireg</tiles:put>

	<tiles:put name="head">
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/sprintf-0.6.js"/>"></script>
	</tiles:put>

	<tiles:put name="body">

		<h1>Charge des web-services d'Unireg</h1>

		<%--@elvariable id="environments" type="java.util.List"--%>
		<c:forEach items="${environments}" var="env">
			<h2><c:out value="${env.name}"/></h2>

			<div>
				<img id="graph-${env.name}" src="" class="img-polaroid"/>
			</div>
		</c:forEach>

		<script type="text/javascript">
			$(function () {
				<c:forEach items="${environments}" var="env">
				$('#graph-${env.name}').attr('src', 'graph/load.do?filters=ENVIRONMENT:' + ${env.id} +
						'&criteria=' +
						'&from=' + today() +
						'&to=' +
						'&resolution=FIFTEEN_MINUTES' +
						'&width=' +
						'&height=');
				</c:forEach>
			});

			function today() {
				var date = new Date();
				return sprintf('%04d-%02d-%02dT00:00:00', date.getFullYear(), (date.getMonth() + 1), date.getDate());
			}

		</script>

	</tiles:put>

</tiles:insert>