<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<html>
<head>
	<meta http-equiv="X-UA-Compatible" content="IE=edge"/>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<%@ include file="../templates/jsInclude.jsp" %>
</head>
<body>
<%--@elvariable id="defaultPageUrl" type="java.lang.String"--%>
<%--@elvariable id="defaultParams" type="java.lang.String"--%>
<script type="text/javascript">
	App.init('<c:url value="/"/>');
	Navigation.back('${defaultPageUrl}', '${defaultParams}');
</script>
</body>