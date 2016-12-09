<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
<tiles:put name="title"><fmt:message key="label.caracteristiques.evenement.ech"/></tiles:put>
<tiles:put name="fichierAide">
	<li>
		<c:url var="doc" value="/docs/evenements.pdf"/>
		<a href="#" onClick="ouvrirAide('${doc}');" title="AccessKey: a" accesskey="e">Aide</a>
	</li>
</tiles:put>
<tiles:put name="body">

	<%@ include file="detail.jsp" %>

</tiles:put>
</tiles:insert>
