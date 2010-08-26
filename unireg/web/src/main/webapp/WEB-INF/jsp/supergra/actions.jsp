<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<c:if test="${!empty superGraSession.deltas}" >
<div id="actions_list">
	<form:form id="deltaForm" method="post">

		<input id="delDelta" type="hidden" name="delDelta" />

		<table class="sync_actions" cellspacing="0" border="0">
		  <tbody>
			<tr class="header">
			  <td colspan="2">Les modifications ci-dessous sont en attente :</td>
			</tr>

			<c:forEach items="${superGraSession.deltas}" var="d" varStatus="i">
				<tr class="action">
				  <td class="rowheader">»</td>
				  <td class="action"><c:out value="${d}"/> (<a href="#" onclick="E$('delDelta').value = '${i.index}'; F$('deltaForm').submit(); return false;">supprimer</a>)</td>
				</tr>
			</c:forEach>
		  </tbody>
		</table>

	</form:form>
</div>
</c:if>
