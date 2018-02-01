<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<html>
	<head>
		<!-- demandons à IE d'utiliser le dernier moteur -->
		<meta http-equiv="X-UA-Compatible" content="IE=edge" />

		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<title><tiles:getAsString name='title' ignore='true'/></title>

		<%@ include file="cssInclude.jsp" %>
		<%@ include file="jsInclude.jsp" %>

		<tiles:getAsString name='head' ignore='true'/>
	</head>
	<body>

		<fmt:setLocale value="fr_CH" scope="session"/>

		<script type="text/javascript">
			App.init('<c:url value="/"/>');
		</script>

		<%-- Message flash --%>
		<%--@elvariable id="flash" type="ch.vd.unireg.common.FlashMessage"--%>
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
