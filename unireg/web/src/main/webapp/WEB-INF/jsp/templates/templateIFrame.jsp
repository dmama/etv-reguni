<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
	<head>
		<title><tiles:getAsString name='title' ignore='true'/></title>
		<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
		<link rel="SHORTCUT ICON" href="<c:url value="/images/favicon.ico"/>">
		<link media="screen" href="<c:url value="/css/x/layout.css"/>" rel="stylesheet" type="text/css">
		<link media="screen" href="<c:url value="/css/x/tabs.css"/>" rel="stylesheet" type="text/css">
		<link media="screen" href="<c:url value="/css/x/tools.css"/>" rel="stylesheet" type="text/css">
		<link media="screen" href="<c:url value="/css/x/displaytag.css"/>" rel="stylesheet" type="text/css">
		<link media="screen" href="<c:url value="/css/x/suggest.css"/>" rel="stylesheet" type="text/css">
		<link media="screen" href="<c:url value="/css/x/unireg.css"/>" rel="stylesheet" type="text/css">
		<link media="screen" href="<c:url value="/css/x/thickbox.css"/>" rel="stylesheet" type="text/css">

		<link media="print" href="<c:url value="/css/print/layout.css"/>" rel="stylesheet" type="text/css">
		<link media="print" href="<c:url value="/css/print/tabs.css"/>" rel="stylesheet" type="text/css">
		<link media="print" href="<c:url value="/css/print/tools.css"/>" rel="stylesheet" type="text/css">
		<link media="print" href="<c:url value="/css/print/displaytag.css"/>" rel="stylesheet" type="text/css">
		<link media="print" href="<c:url value="/css/print/suggest.css"/>" rel="stylesheet" type="text/css">
		<link media="print" href="<c:url value="/css/print/unireg.css"/>" rel="stylesheet" type="text/css">
		<link media="print" href="<c:url value="/css/print/thickbox.css"/>" rel="stylesheet" type="text/css">
		
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/request.js"/>"></script>
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/springxt.js"/>"></script>
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/custom.js"/>"></script>		
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/calendar.js"/>"></script>
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/tab.js"/>"></script>
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/tiers.js"/>"></script>
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/for.js"/>"></script>
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/di.js"/>"></script>
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/rapport.js"/>"></script>
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/mouvement.js"/>"></script>
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/tache.js"/>"></script>
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/suggest.js"/>"></script>
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/jquery.js"/>"></script>
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/thickbox.js"/>"></script>
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/util.js"/>"></script>
		<tiles:getAsString name='head' ignore='true'/>
	</head>
	<body>
		<spring:hasBindErrors name="command">
				<div class="global-error"><fmt:message key="global.error.entete"/></div>		          	
         		<c:if test="${errors.globalErrorCount > 0}">
         		<div class="global-error"><ul>
			        <c:forEach var="error" items="${errors.globalErrors}">
						<li><spring:message message="${error}" /></li>
			        </c:forEach>
		        </ul></div>
	        </c:if>
        </spring:hasBindErrors>
		<tiles:getAsString name='body' ignore='true'/>
	</body>
</html>
