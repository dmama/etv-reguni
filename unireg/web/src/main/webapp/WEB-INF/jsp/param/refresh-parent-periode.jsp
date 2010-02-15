<%-- Rafraichissement de la page parent --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<html>
<head>
	<title>rafraichir la page parent période</title>
	<c:url value="/js/jquery.js" var="urlJQuery"/>
	<script type="text/javascript" language="Javascript" src="${urlJQuery}"></script>
	<script type="text/javascript">
	  $(document).ready(function() {$("form").submit()})
	</script>
</head>
<body>
	<form target="_parent" method="get" action="periode.do">
		<input type="hidden" name="pf" value="${pf}"/>
		<c:if test="${not empty md}">
			<input type="hidden" name="md" value="${md}"/>
		</c:if>
		<c:if test="${not empty md}">
			<input type="hidden" name="md" value="${md}"/>
		</c:if>		
	</form>
</body>
</html>
