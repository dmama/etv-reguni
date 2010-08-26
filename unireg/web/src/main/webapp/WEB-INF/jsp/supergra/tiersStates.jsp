<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<div id="states_list">
	<table class="sync_states" cellspacing="0" border="0"><tbody>

		<tr class="header">
			<c:if test="${empty superGraSession.tiersStates}" >
				<td>Aucun tiers n'a été modifié</td>
			</c:if>
			<c:if test="${!empty superGraSession.tiersStates}" >
				<td colspan="2">Les tiers suivants ont été modifiés</td>
			</c:if>
		</tr>

		<c:if test="${!empty superGraSession.tiersStates}" >
			<c:set var="hasError" value="false"/>

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
										<c:set var="hasError" value="true"/>
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

			<tr class="state">
				<td class="footer">
					<form method="post" style="display:inline;">
						<input type="submit" name="commitAll" value="Sauvegarder" <c:if test="${hasError}">disabled="disabled"</c:if> />
					</form>
					ou
					<form id="delAllForm" method="post" style="display:inline;">
						<a href="#" onclick="F$('delAllForm').submit(); return false;">tout annuler</a>
						<input type="hidden" name="rollbackAll" value="rollbackAll" />
					</form>
				</td>
			</tr>
		</c:if>

	</tbody></table>
</div>

