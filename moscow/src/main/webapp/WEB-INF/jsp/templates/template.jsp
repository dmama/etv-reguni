<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/common.jsp" %>

<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<tiles:getAsString name='refresh' ignore='true'/>

		<link rel="SHORTCUT ICON" href="<c:url value="/images/favicon.ico"/>">
		<link href="<c:url value="/css/displaytag.css"/>" rel="stylesheet" type="text/css">
        <link href="<c:url value="/css/layout.css"/>" rel="stylesheet" type="text/css">
        <link href="<c:url value="/css/jquery-ui.css"/>" rel="stylesheet" type="text/css">
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/jquery.js"/>"></script>
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/jquery-ui.js"/>"></script>
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/jquery.ui.datepicker-fr-CH.js"/>"></script>

		<title><tiles:getAsString name='title' ignore='false'/></title>
		<tiles:getAsString name='head' ignore='true'/>
	</head>

	<body>
		<div id="header">
			<span>L'oeil de Moscou</span>
		</div>
		<div id="menu">
			<h2>Graphiques</h2>
			<a href="<c:url value="/graph/custom.do"/>"/>Personnalisé</a><br/>
			<h2>Config</h2>
			<a href="<c:url value="/environment/list.do"/>"/>Environnements</a><br/>
			<a href="<c:url value="/directory/list.do"/>"/>Répertoires</a><br/>
			<a href="<c:url value="/job/list.do"/>"/>Jobs</a><br/>
		</div>
		<div id="content">
        	<tiles:getAsString name='body' />
		</div>
	</body>

</html>
