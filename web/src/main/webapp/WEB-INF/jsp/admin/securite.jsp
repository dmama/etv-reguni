<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">Détails des procédures de l'utilisateur <authz:authentication property="principal.username"/></tiles:put>

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
					<tr><td style="font-weight: bold">Visa</td><td><%--@elvariable id="visa" type="java.lang.String"--%>
					<c:out value="${visa}" /></td><td><authz:authentication property="principal.username"/></td></tr>
					<tr><td style="font-weight: bold">Oid</td><td><%--@elvariable id="oid" type="java.lang.Integer"--%>
					<c:out value="${oid}" /></td><td>n/a</td></tr>
					<tr><td style="font-weight: bold">Procédures Unireg</td><td>
							<%--@elvariable id="proceduresUnireg" type="java.util.List<ch.vd.unireg.security.ProcedureSecurite>"--%>
						<ul>
						<c:forEach items="${proceduresUnireg}" var="p">
							<li><c:out value="${p.code}"/> (<c:out value="${p.designation}"/>)</li>
						</c:forEach>
						</ul>
						<%--@elvariable id="rolesSecuriteByPass" type="java.util.List<ch.vd.unireg.security.Role>"--%>
						<c:if test="${not empty rolesSecuriteByPass}">
							<span style="color: red;">(plus les procédures bypassées suivantes:
								<ul>
									<c:forEach items="${rolesSecuriteByPass}" var="r">
										<li><c:out value="${r.codeProcedure}"/> (<c:out value="${r}"/>)</li>
									</c:forEach>
								</ul>
							)</span>
						</c:if>
					</td><td>
						<%--@elvariable id="roles" type="ch.vd.unireg.security.Role[]"--%>
						<ul>
						<c:forEach items="${roles}" var="r">
							<authz:authorize access="hasAnyRole('${r}')">
								<c:if test="${not empty r.codeProcedure}">
									<li><c:out value="${r.codeProcedure}"/> (<c:out value="${r}"/>)</li>
								</c:if>
							</authz:authorize>
						</c:forEach>
						</ul>
					</td></tr>
					<tr><td style="font-weight: bold">Procédures autres applications</td><td>
							<%--@elvariable id="proceduresAutres" type="java.util.List<ch.vd.unireg.security.ProcedureSecurite>"--%>
						<ul>
						<c:forEach items="${proceduresAutres}" var="p">
							<li><c:out value="${p.code}"/> (<c:out value="${p.designation}"/>)</li>
						</c:forEach>
						</ul>
					</td><td>n/a</td></tr>
				</tbody>
			</table>
		</fieldset>

	</tiles:put>
</tiles:insert>
