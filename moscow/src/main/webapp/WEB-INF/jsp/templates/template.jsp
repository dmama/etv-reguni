<!DOCTYPE html>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common.jsp" %>

<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<tiles:getAsString name='refresh' ignore='true'/>

	<link rel="SHORTCUT ICON" href="<c:url value="/images/favicon.ico"/>">
	<link href="<c:url value="/css/bootstrap.css"/>" rel="stylesheet" type="text/css">
	<style>
		body {
			padding-top: 60px; /* 60px to make the container go all the way to the bottom of the topbar */
		}
	</style>
	<link href="<c:url value="/css/bootstrap-responsive.css"/>" rel="stylesheet" type="text/css">
	<link href="<c:url value="/css/jquery-ui.css"/>" rel="stylesheet" type="text/css">

	<script type="text/javascript" language="Javascript" src="<c:url value="/js/jquery.js"/>"></script>
	<script type="text/javascript" language="Javascript" src="<c:url value="/js/bootstrap.js"/>"></script>
	<script type="text/javascript" language="Javascript" src="<c:url value="/js/jquery-ui.js"/>"></script>
	<script type="text/javascript" language="Javascript" src="<c:url value="/js/jquery.ui.datepicker-fr-CH.js"/>"></script>

	<!-- HTML5 shim, for IE6-8 support of HTML5 elements -->
	<!--[if lt IE 9]>
	<script src="http://html5shim.googlecode.com/svn/trunk/html5.js" type="text/javascript"></script>
	<![endif]-->

	<title><tiles:getAsString name='title' ignore='false'/></title>
	<tiles:getAsString name='head' ignore='true'/>
</head>

<body>

<div class="navbar navbar-inverse navbar-fixed-top">
	<div class="navbar-inner">
		<div class="container">
			<a class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
				<span class="icon-bar"></span>
				<span class="icon-bar"></span>
				<span class="icon-bar"></span>
			</a>
			<a class="brand" href="<c:url value="/"/>">L'Oeil de Moscow</a>

			<div class="nav-collapse collapse">
				<ul class="nav">
					<li><a href="<c:url value="/graph/custom.do"/>">Graphique personnalisé</a></li>
					<li class="dropdown">
						<a href="#" class="dropdown-toggle" data-toggle="dropdown">Configuration <b class="caret"></b></a>
						<ul class="dropdown-menu">
							<li><a href="<c:url value="/environment/list.do"/>">Environnements</a></li>
							<li><a href="<c:url value="/directory/list.do"/>">Répertoires</a></li>
							<li><a href="<c:url value="/job/list.do"/>">Jobs</a></li>
						</ul>
					</li>
				</ul>
			</div>
			<!--/.nav-collapse -->
		</div>
	</div>
</div>

<div class="container">

	<%-- Message flash --%>
	<c:if test="${flash != null && flash.active}">
		<div id="flashdisplay" class="<c:out value='${flash.displayClass}'/>"><c:out value="${flash.messageForDisplay}"/></div>
		<c:if test="${flash.timeout > 0}">
			<script type="text/javascript">
				$('#flashdisplay').delay(<c:out value="${flash.timeout}"/>).fadeOut('slow');
			</script>
		</c:if>
	</c:if>

	<tiles:getAsString name='body'/>
</div>

</body>

</html>
