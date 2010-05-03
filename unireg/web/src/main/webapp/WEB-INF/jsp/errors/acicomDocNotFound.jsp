<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="menu" type="String">
	<tiles:put name="title" type="String"><font color="red"><c:out value="${exception.libelleErreur}" /></font> </tiles:put>
	<tiles:put name="connected" type="String">
	</tiles:put>

	<tiles:put name="body" type="String">
		<unireg:closeOverlayButton/>

		<p>Unireg n'a pas pu effectuer l'opération demandée car le message  d'origine n'a pu être trouvé par le service de récupération de message ACICOM.</p>
		<p><input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="javascript:history.go(-1);" />

		<br><hr><br>

	</tiles:put>
	</tiles:put>
</tiles:insert>