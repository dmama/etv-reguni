<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="menu" type="String"></tiles:put>

	<tiles:put name="title" type="String">*** FIN DE SESSION *** </tiles:put>

	<tiles:put name="connected" type="String">
	</tiles:put>

	<tiles:put name="body" type="String">
		<% request.getSession().invalidate(); %>
	
		<p>Vous terminez votre session dans le domaine prot&eacute;g&eacute;. Votre session va &ecirc;tre ferm&eacute;e.</p>
		<p>Pour votre propre s&eacute;curit&eacute;, nous vous recommandons de vider la m&eacute;moire cache de votre navigateur.</p>
		<IMG src="/siteminderagent/application.logoff" width="1" height="1"/>
	</tiles:put>
</tiles:insert>
