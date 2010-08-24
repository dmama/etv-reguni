<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<c:if test="${!empty superGraSession.deltas}" >
<div id="actions_list">
	<form:form id="deltaForm" method="post">

		<input id="delDelta" type="hidden" name="delDelta" />

		<table class="sync_actions" classname="sync_actions" cellspacing="0" border="0">
		  <tbody>
			<tr class="header" classname="header">
			  <td colspan="2">Les actions suivantes seront exécutées si vous confirmez les changements </td>
			</tr>

			<c:forEach items="${superGraSession.deltas}" var="d" varStatus="i">
				<tr class="action" classname="action">
				  <td class="rowheader" classname="rowheader">»</td>
				  <td class="action" classname="action"><c:out value="${d}"/> (<a href="#" onclick="E$('delDelta').value = '${i.index}'; F$('deltaForm').submit(); return false;">supprimer</a>)</td>
				</tr>
			</c:forEach>
		  </tbody>
		</table>

	</form:form>
</div>
</c:if>
