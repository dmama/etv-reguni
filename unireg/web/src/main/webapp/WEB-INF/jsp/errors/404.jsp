<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="menu" type="String" />

	<tiles:put name="title" type="String">
		La page que vous demandez n'a pas &eacute;t&eacute; trouv&eacute;e. (404.jsp)
	</tiles:put>

	<tiles:put name="connected" type="String" />

	<tiles:put name="body" type="String" />

</tiles:insert>
