<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="menu" type="String">
	</tiles:put>

	<tiles:put name="title" type="String"><font color="red"><c:out value="${exception.libelleErreur}" /></font></tiles:put>
	<tiles:put name="connected" type="String">
	</tiles:put>

	<tiles:put name="body" type="String">
		<unireg:closeOverlayButton/>
		
		<p>Unireg n'a pas pu effectuer l'opération demandée car une erreur s'est produite lors de la communication avec le service de récupération de message ACICOM.</p>
		<p>Cette erreur peut survenir si l'application ACICOM ne répond pas, ou si un problème d'infrastructure empêche la communication entre Unireg et ACICOM.</p>
		<p><input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="javascript:history.go(-1);" />
		<br><hr><br>

		<unireg:callstack exception="${exception}"
			headerMessage="Si le problème persiste, veuillez contacter l'administrateur en lui communiquant le message d'erreur ci-dessous ">
		</unireg:callstack>

	</tiles:put>
</tiles:insert>