<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%
Object jspException = pageContext.findAttribute("javax.servlet.error.exception");
if (jspException != null) {
	pageContext.setAttribute("exception", jspException, PageContext.REQUEST_SCOPE);
}
%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="menu" type="String">
	</tiles:put>

	<tiles:put name="title" type="String">Erreur inattendue</tiles:put>
	<tiles:put name="connected" type="String">
	</tiles:put>
	<tiles:put name="body" type="String">
		<unireg:closeOverlayButton/>
		
		<p>Une erreur inattendue a eu lieu.</p>
		<br>
		<unireg:callstack exception="${exception}"
			headerMessage="Veuillez contacter l'administrateur en lui communiquant le message d'erreur ci-dessous "></unireg:callstack>
	</tiles:put>
</tiles:insert>
