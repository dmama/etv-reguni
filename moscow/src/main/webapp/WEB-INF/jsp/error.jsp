<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/common.jsp" %>

<%
Object jspException = pageContext.findAttribute("javax.servlet.error.exception");
if (jspException != null) {
	pageContext.setAttribute("exception", jspException, PageContext.REQUEST_SCOPE);
}
%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title" type="String">Erreur inattendue</tiles:put>
	<tiles:put name="connected" type="String"></tiles:put>
	<tiles:put name="body" type="String">

		<p>Oops ! We are sorry, something went wrong: <b><c:out value="${exception.message}" /></b></p>

		<p>
		Full call stack follows:<br/>
		<font color="red"><pre><c:out value="${exception}"/><c:forEach items="${exception.stackTrace}" var="stack">
		<c:out value="${stack}" /></c:forEach></pre></font>
		</p>

	</tiles:put>
</tiles:insert>
