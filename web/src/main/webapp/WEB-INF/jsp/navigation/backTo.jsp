<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html>
<head>
	<meta http-equiv="X-UA-Compatible" content="IE=edge"/>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<script type="text/javascript" language="Javascript" src="<c:url value="/js/navigation.js"/>"></script>
</head>
<body>
<%--@elvariable id="pageUrls" type="java.lang.String[]"--%>
<%--@elvariable id="defaultPageUrl" type="java.lang.String"--%>
<%--@elvariable id="defaultParams" type="java.lang.String"--%>
<script type="text/javascript">
	Navigation.init('<c:url value="/"/>');
	Navigation.backTo([<c:forEach items="${pageUrls}" var="url">'${url}', </c:forEach>], '${defaultPageUrl}', '${defaultParams}');
</script>
</body>