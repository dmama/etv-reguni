<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<html>
	<head>
		<title><tiles:getAsString name='title' ignore='true'/></title>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

		<link media="screen" href="<c:url value="/css/x/layout.css"/>" rel="stylesheet" type="text/css">
		<link media="screen" href="<c:url value="/css/x/unireg.css"/>" rel="stylesheet" type="text/css">

		<script type="text/javascript" language="Javascript" src="<c:url value="/js/springxt.js"/>"></script>
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/jquery.js"/>"></script>
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/jquery.bgiframe.js"/>"></script>
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/jquery-ui.js"/>"></script>
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/jquery.ui.datepicker-fr-CH.js"/>"></script>
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/jquery.cookie.js"/>"></script>
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/dialog.js"/>"></script>
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/autocomplete.js"/>"></script>
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/jtip.js"/>"></script>

		<tiles:getAsString name='head' ignore='true'/>
	</head>
	<body>

		<%-- Message flash --%>
		<c:if test="${flash != null && flash.active}">
			<div id="flashdisplay" class="<c:out value='${flash.displayClass}'/>"><c:out value="${flash.messageForDisplay}"/></div>
			<c:if test="${flash.timeout > 0}">
				<script type="text/javascript">
					$('#flashdisplay').delay(<c:out value="${flash.timeout}"/>).fadeOut('slow');
				</script>
			</c:if>
		</c:if>

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
