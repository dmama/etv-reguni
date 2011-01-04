<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="menu" type="String">
	</tiles:put>

	<tiles:put name="title" type="String">Fonctionnalit&eacute; non impl&eacute;ment&eacute;e (notImplemented.jsp)</tiles:put>
	<tiles:put name="connected" type="String">
	</tiles:put>
	<tiles:put name="body" type="String">

		<p>Cette fonctionnalité n'est pas encore implémentée. Elle sera livrée dans une prochaine version d'Unireg.</p>
		
		<br><hr><br>
		<unireg:callstack exception="${exception}"
			headerMessage="Si vous pensez que cette fonctionnalité devrait être implémentée, merci d'ouvrir un incident et d'inclure le message ci-dessous "></unireg:callstack>
	</tiles:put>
</tiles:insert>
