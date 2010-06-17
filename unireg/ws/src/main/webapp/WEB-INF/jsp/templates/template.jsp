<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
	<head>
		<title><tiles:getAsString name='title' ignore='true'/></title>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

		<link media="screen" href="<c:url value="/css/layout.css"/>" rel="stylesheet" type="text/css">
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/custom.js"/>"></script>

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
