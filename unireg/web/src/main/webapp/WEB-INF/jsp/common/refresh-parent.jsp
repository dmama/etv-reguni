<%-- Rafraichissement de la page parent --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" 		uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head><title>Page bidon JSP pour rafraichir la page parent</title>
<script type="text/javascript" language="Javascript" src="<c:url value="/js/unireg.js"/>"></script>
</head>
<body>

<script type="text/javascript" language="Javascript1.3">

var form = window.parent.document.forms["theForm"];
if ( form) {
	Form.doPostBack("theForm", "refresh", "refresh");
}
else {
	window.parent.location.reload(true);
}

</script>
</body>
</html>
