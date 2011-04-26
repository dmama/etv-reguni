<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">Détails IFOSec de l'utilisateur <authz:authentication property="principal.username"/></tiles:put>

	<tiles:put name="body">

		<fieldset class="info">
			<table>
				<thead style="font-weight: bold">
					<tr>
						<td/>
						<td>Business</td>
						<td>Web</td>
					</tr>
				</thead>
				<tbody>
					<tr><td style="font-weight: bold">Visa</td><td><c:out value="${visa}" /></td><td><authz:authentication property="principal.username"/></td></tr>
					<tr><td style="font-weight: bold">Oid</td><td><c:out value="${oid}" /></td><td>n/a</td></tr>
					<tr><td style="font-weight: bold">Procédures Unireg</td><td>
						<c:forEach items="${proceduresUnireg}" var="p">
							<c:out value="${p.code}"></c:out>
						</c:forEach>
						<c:if test="${not empty rolesIfoSecByPass}">
							<span style="color: red;">(plus les procédures bypassées suivantes:
								<c:forEach items="${rolesIfoSecByPass}" var="r">
									<c:out value="${r.ifosecCode}"></c:out>
								</c:forEach>
							)</span>
						</c:if>
					</td><td>
						<c:forEach items="${roles}" var="r">
							<authz:authorize ifAnyGranted="${r.code}">
								<c:if test="${not empty r.ifosecCode}">
									<c:out value="${r.ifosecCode}"></c:out>
								</c:if>
							</authz:authorize>
						</c:forEach>
					</td></tr>
					<tr><td style="font-weight: bold">Procédures autres applications</td><td>
						<c:forEach items="${proceduresAutres}" var="p">
							<c:out value="${p.code}"></c:out>
						</c:forEach>
					</td><td>n/a</td></tr>
				</tbody>
			</table>
		</fieldset>

	</tiles:put>
</tiles:insert>
