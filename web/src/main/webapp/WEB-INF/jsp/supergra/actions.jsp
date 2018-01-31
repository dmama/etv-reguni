<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="superGraSession" type="ch.vd.uniregctb.supergra.SuperGraSession"--%>

<c:if test="${!empty superGraSession.deltas}" >
<div id="actions_list">
	<form:form id="deltaForm" method="post">

		<table class="sync_actions" cellspacing="0" border="0">
		  <tbody>
			<tr class="header">
			  <td colspan="2">Les modifications suivantes sont mémorisées :</td>
			</tr>

			<c:forEach items="${superGraSession.deltas}" var="d" varStatus="i">
				<tr class="action">
				  <td class="rowheader">»</td>
				  <td class="action"><c:out value="${d.html}" escapeXml="false"/> (<unireg:linkTo name="supprimer" action="/supergra/actions/delete.do" params="{index:${i.index}}" method="POST"/>)</td>
				</tr>
			</c:forEach>
		  </tbody>
		</table>

	</form:form>
</div>
</c:if>
