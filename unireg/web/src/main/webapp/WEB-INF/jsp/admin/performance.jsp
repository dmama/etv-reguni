<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<% String requestURI = (String) request.getAttribute("requestURI");
	   String labelCode  = (String) request.getAttribute("labelCode");
	%>

	<tiles:put name="title">
		<fmt:message key="title.statistique.performances" />
	</tiles:put>
	
	<tiles:put name="body">
		<h2>Available:</h2><br>
		<a href="performance.do?layer=all">Tous</a>&nbsp;
		<a href="performance.do?layer=dao">DAO</a>&nbsp;
		<a href="performance.do?layer=controller">Controller</a>&nbsp;
		<a href="performance.do?layer=service">Service</a>&nbsp;
		<p>
		<h3>Current layer: <c:out value="layer" /></h3>
		
		<display:table name="performanceLogs" defaultsort="1" defaultorder="ascending" requestURI="<%=requestURI %>" class="display_table" export="true">
			<display:column property="name" title="URL du contrôleur" sortable="true"/>
			<display:column property="average" title="Temps de réponse moyen (µs)" sortable="true"/>
			<display:column property="min" title="Temps de réponse minimal (µs)" sortable="true"/>
			<display:column property="max" title="Temps de réponse maximal (µs)" sortable="true"/>
			<display:column property="total" title="Temps de réponse total cumulé (µs)" sortable="true"/>
			<display:column property="hits" sortable="true"/>
		</display:table>
	</tiles:put>

</tiles:insert>
