<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="periodesDebut" type="java.util.List<ch.vd.uniregctb.registrefoncier.allegement.PeriodeFiscaleView>"--%>

<c:set var="commandName" value="${param.commandName}"/>
<c:set var="allowPeriodeDebutEdit" value="${param.allowPeriodeDebutEdit}"/>

<table border="0" class="exoneration">
	<tr class="even">
		<td style="width: 15%;"><fmt:message key="label.periode.fiscale.debut"/>&nbsp;:</td>
		<td style="width: 35%;">
			<c:choose>
				<c:when test="${allowPeriodeDebutEdit}">
					<form:select path="anneeDebut">
						<form:option value=""/>
						<c:forEach items="${periodesDebut}" var="periode">
							<form:option value="${periode.annee}" disabled="${periode.interdite}"/>
						</c:forEach>
					</form:select>
					<span class="mandatory">*</span>
					<form:errors path="anneeDebut" cssClass="error"/>
				</c:when>
				<c:otherwise>
					<form:hidden path="anneeDebut"/>
					<c:set var="pfName" value="${commandName}.anneeDebut"/>
					<spring:bind path="${pfName}">
						<span style="padding-left: 1em;"><c:out value="${status.value}"/></span>
					</spring:bind>
				</c:otherwise>
			</c:choose>
		</td>
		<td style="width: 15%;"><fmt:message key="label.periode.fiscale.fin"/>&nbsp;:</td>
		<td style="width: 35%;">
			<c:set var="pfFinName" value="${commandName}.anneeFin"/>
			<spring:bind path="${pfFinName}">
				<span style="font-style: italic; color: gray; padding: 0 0 0 1em;"><c:out value="${status.value}"/></span>
			</spring:bind>
			<form:hidden path="anneeFin"/>
		</td>
	</tr>
	<tr class="odd">
		<td><fmt:message key="label.pourcentage.exoneration"/>&nbsp;:</td>
		<td>
			<form:input path="pourcentageExoneration" cssClass="nombre"/>
			<span class="mandatory">*</span>
			<form:errors path="pourcentageExoneration" cssClass="error"/>
		</td>
		<td colspan="2">&nbsp;</td>
	</tr>
</table>
