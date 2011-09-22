<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="menu" type="String">
	</tiles:put>

	<tiles:put name="title" type="String"><fmt:message key="title.erreur" /></tiles:put>
	<tiles:put name="connected" type="String">
	</tiles:put>
	<tiles:put name="body" type="String">

		<pre><font color="red"><c:out value="${exception.message}" /></font><br></pre>
		<br>
		<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="javascript:history.go(-1);" />
	</tiles:put>
</tiles:insert>
