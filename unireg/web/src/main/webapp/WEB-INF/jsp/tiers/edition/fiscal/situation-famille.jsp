<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<!-- Debut Situation famille -->

<fieldset><legend><span><fmt:message key="label.situation.famille.fiscale" /></span></legend>

	<c:if test="${command.situationsFamilleEnErreurMessage != null}">
		<div class="flash-warning"><c:out value="${command.situationsFamilleEnErreurMessage}"/></div>
	</c:if>
	<c:if test="${command.situationsFamilleEnErreurMessage == null}">
		<table border="0">
			<tr><td>
				<unireg:linkTo name="Ajouter" action="/situationfamille/add.do" method="get" params="{tiersId:${command.tiers.numero}}" title="Ajouter situation famille" link_class="add noprint"/>
			</td></tr>
		</table>
		<jsp:include page="../../common/fiscal/situation-famille.jsp">
			<jsp:param name="page" value="edit"/>
		</jsp:include>
	</c:if>

</fieldset>
<!-- Fin Situation famille -->