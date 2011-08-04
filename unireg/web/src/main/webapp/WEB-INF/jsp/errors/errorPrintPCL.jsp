<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="menu" type="String">
	</tiles:put>

	<tiles:put name="title" type="String">Erreur de d'impression du flux PCL</tiles:put>
	<tiles:put name="connected" type="String">
	</tiles:put>
	<tiles:put name="body" type="String">

		<p><c:out value="${exception.message}"/></p>
		<br>
		<unireg:callstack exception="${exception}"
			headerMessage="Veuillez contacter l'administrateur en lui communiquant le message d'erreur ci-dessous "></unireg:callstack>
	</tiles:put>
</tiles:insert>
