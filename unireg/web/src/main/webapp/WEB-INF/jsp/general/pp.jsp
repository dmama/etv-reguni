<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="page" value="${param.page}" />
<c:set var="path" value="${param.path}" />
<c:set var="className" value="information" />
<c:if test="${not empty param.className}">
	<c:set var="className" value="${param.className}" />
</c:if>

<c:set var="bind" value="command.${path}" scope="request"/>
<spring:bind path="${bind}" >
	<c:set var="tiersGeneral" value="${status.value}"  scope="request"/>
</spring:bind>

<c:set var="titre"><fmt:message key="label.caracteristiques.${param.path}"/></c:set>

<!-- Debut Caracteristiques generales -->
<unireg:bandeauTiers numero="${tiersGeneral.numero}" titre="${titre}" cssClass="${className}"
	showValidation="false" showEvenementsCivils="false" showLinks="false" showAvatar="false" showComplements="true"/>

<c:set var="premierePersonneErrors"><form:errors path="premierePersonne"/></c:set>
<c:set var="secondePersonneErrors"><form:errors path="secondePersonne"/></c:set>

<c:if test="${not empty premierePersonneErrors || not empty secondePersonneErrors}">
	<table cellspacing="0" cellpadding="5">
		<tr class="<unireg:nextRowClass/>" >
			<td colspan="3">
				<c:if test="${path == 'premierePersonne'}">${premierePersonneErrors}</c:if>
				<c:if test="${path == 'secondePersonne'}">${secondePersonneErrors}</c:if>
			</td>
		</tr>
	</table>
</c:if>

<!-- Fin Caracteristiques generales -->