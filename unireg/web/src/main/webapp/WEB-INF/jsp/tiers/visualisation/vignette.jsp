<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/templateDialog.jsp">
  	<tiles:put name="title"></tiles:put>
  	<tiles:put name="body">
		<unireg:bandeauTiers
			numero="${command.numero}"
			titre="${command.titre}"
			showValidation="${command.showValidation}"
			showEvenementsCivils="${command.showEvenementsCivils}"
			showLinks="${command.showLinks}"
			showAvatar="${command.showAvatar}"
			showComplements="${command.showComplements}"/>
	</tiles:put>
</tiles:insert>