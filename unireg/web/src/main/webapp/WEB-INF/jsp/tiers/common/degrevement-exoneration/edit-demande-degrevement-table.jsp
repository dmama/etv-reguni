<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="periodes" type="java.util.List<ch.vd.uniregctb.registrefoncier.allegement.PeriodeFiscaleView>"--%>

<c:set var="commandName" value="${param.commandName}"/>
<c:set var="allowPeriodeEdit" value="${param.allowPeriodeEdit}"/>       <%-- true / false --%>
<c:set var="allowDelaiEdit" value="${param.allowDelaiEdit}"/>           <%-- true / false --%>
<c:set var="allowRetourEdit" value="${param.allowRetourEdit}"/>         <%-- true / false / not-shown --%>

<unireg:nextRowClass reset="0"/>
<table border="0">
	<tr class="<unireg:nextRowClass/>">
		<td style="width: 15%;"><fmt:message key="label.periode.fiscale"/>&nbsp;:</td>
		<td>
			<c:choose>
				<c:when test="${allowPeriodeEdit}">
					<form:select path="periodeFiscale">
						<form:option value=""/>
						<c:forEach items="${periodes}" var="periode">
							<form:option value="${periode.annee}" disabled="${periode.interdite}"/>
						</c:forEach>
					</form:select>
					<span class="mandatory">*</span>
					<form:errors path="periodeFiscale" cssClass="error"/>
				</c:when>
				<c:otherwise>
					<form:hidden path="periodeFiscale"/>
					<c:set var="pfName" value="${commandName}.periodeFiscale"/>
					<spring:bind path="${pfName}">
						<span><c:out value="${status.value}"/></span>
					</spring:bind>
				</c:otherwise>
			</c:choose>
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/>">
		<td><fmt:message key="label.date.delai.accorde"/>&nbsp;:</td>
		<td>
			<c:choose>
				<c:when test="${allowDelaiEdit}">
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="delaiRetour"/>
						<jsp:param name="id" value="delaiRetour"/>
						<jsp:param name="mandatory" value="true" />
					</jsp:include>
				</c:when>
				<c:otherwise>
					<form:hidden path="delaiRetour"/>
					<c:set var="delaiRetourName" value="${commandName}.delaiRetour"/>
					<spring:bind path="${delaiRetourName}">
						<span><c:out value="${status.value}"/></span>
					</spring:bind>
				</c:otherwise>
			</c:choose>
		</td>
	</tr>
	<c:if test="${allowRetourEdit != 'not-shown'}">
		<tr class="<unireg:nextRowClass/>">
			<td><fmt:message key="label.date.retour"/>&nbsp;:</td>
			<td>
				<c:choose>
					<c:when test="${allowRetourEdit}">
						<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
							<jsp:param name="path" value="dateRetour"/>
							<jsp:param name="id" value="dateRetour"/>
							<jsp:param name="mandatory" value="true" />
						</jsp:include>
					</c:when>
					<c:otherwise>
						<form:hidden path="dateRetour"/>
						<c:set var="dateRetourName" value="${commandName}.dateRetour"/>
						<spring:bind path="${dateRetourName}">
							<span><c:out value="${status.value}"/></span>
						</spring:bind>
					</c:otherwise>
				</c:choose>
			</td>
		</tr>
	</c:if>
</table>