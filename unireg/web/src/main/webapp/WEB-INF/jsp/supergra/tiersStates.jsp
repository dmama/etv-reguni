<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<div id="states_list">
	<table class="sync_states" classname="sync_actions" cellspacing="0" border="0"><tbody>

		<tr class="header" classname="header">
			<c:if test="${empty superGraSession.tiersStates}" >
				<td>Aucun tiers n'a été modifié</td>
			</c:if>
			<c:if test="${!empty superGraSession.tiersStates}" >
				<td colspan="2">Les tiers suivants ont été modifiés</td>
			</c:if>
		</tr>

		<c:forEach items="${superGraSession.tiersStates}" var="s">
			<tr class="state">
				<td class="state">

					<table cellspacing="0" border="0">
						<tr>
							<c:if test="${s.valid}">
								<c:set var="header_class" value="header_valid"/>
							</c:if>
							<c:if test="${s.inError}">
								    <c:set var="header_class" value="header_error"/>
							</c:if>
							<c:if test="${!s.valid && !s.inError}">
								    <c:set var="header_class" value="header_warning"/>
							</c:if>
							<td class="${header_class}" colspan="2"><a href="<c:url value="/supergra/entity.do?id=${s.key.id}&class=${s.key.type}"/>"><c:out value="${s.key}"/></a></td>
						</tr>
						<c:forEach items="${s.validationResults.warnings}" var="w" >
						<tr>
							<td class="bullet">»</td>
							<td class="warning">Warning: <c:out value="${w}"/></td>
						</tr>
						</c:forEach>
						<c:forEach items="${s.validationResults.errors}" var="e" >
						<tr>
							<td class="bullet">»</td>
							<td class="error">Erreur: <c:out value="${e}"/></td>
						</tr>
						</c:forEach>
					</table>
					
				</td>
			  </td>
			</tr>
		</c:forEach>

	</tbody></table>
</div>

