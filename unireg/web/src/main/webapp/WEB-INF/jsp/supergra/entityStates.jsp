<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="coll" type="ch.vd.uniregctb.supergra.view.CollectionView"--%>
<%--@elvariable id="superGraSession" type="ch.vd.uniregctb.supergra.SuperGraSession"--%>

<div id="states_list">
	<table class="sync_states" cellspacing="0" border="0"><tbody>

		<tr class="header">
			<c:if test="${empty superGraSession.entityStates}" >
				<td>Aucune modification n'est en cours</td>
			</c:if>
			<c:if test="${!empty superGraSession.entityStates}" >
				<td colspan="2">Les entités suivantes ont été modifiées ou sont impactées par les modifications :</td>
			</c:if>
		</tr>

		<c:if test="${!empty superGraSession.entityStates}" >
			<c:set var="hasError" value="false"/>

			<c:forEach items="${superGraSession.entityStates}" var="s">
				<tr class="state">
					<td class="state">

						<table cellspacing="0" border="0">
							<tr>
								<c:if test="${s.valid}">
									<c:set var="header_class" value="header_valid iepngfix"/>
								</c:if>
								<c:if test="${s.inError}">
										<c:set var="header_class" value="header_error"/>
										<c:set var="hasError" value="true"/>
								</c:if>
								<c:if test="${!s.valid && !s.inError}">
										<c:set var="header_class" value="header_warning iepngfix"/>
								</c:if>
								<td class="${header_class}" colspan="2"><a href="<c:url value="/supergra/entity/show.do?id=${s.key.id}&class=${s.key.type}"/>"><c:out value="${s.key}"/></a></td>
							</tr>
							<c:forEach items="${s.validationResults.warnings}" var="w" >
							<tr>
								<td class="bullet">»</td>
								<td class="warn"><fmt:message key="label.validation.warning"/>: <c:out value="${w}"/></td>
							</tr>
							</c:forEach>
							<c:forEach items="${s.validationResults.errors}" var="e" >
							<tr>
								<td class="bullet">»</td>
								<td class="error"><fmt:message key="label.validation.erreur"/>: <c:out value="${e}"/></td>
							</tr>
							</c:forEach>
						</table>

					</td>
				</tr>
			</c:forEach>

			<tr class="state">
				<td class="footer">
					<c:if test="${entity != null}">
						<c:set var="key" value="${entity.key}"/>
					</c:if>
					<c:if test="${coll != null}">
						<c:set var="key" value="${coll.key}"/>
					</c:if>
					<unireg:buttonTo name="Sauvegarder" action="/supergra/actions/commit.do" params="{id:${key.id},class:'${key.type}'}" method="POST"
					                 confirm="Toutes les modifications seront sauvées dans la base de données.\n\nVoulez-vous continuer ?" disabled="${hasError}" />
					ou
					<unireg:linkTo name="tout annuler" action="/supergra/actions/rollback.do" method="POST" />
				</td>
			</tr>
		</c:if>

	</tbody></table>
</div>

