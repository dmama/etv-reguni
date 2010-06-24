<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="page" value="${param.page}" />
<c:set var="path" value="${param.path}" />
<c:set var="className" value="information" />
<c:if test="${not empty param.className}">
	<c:set var="className" value="${param.className}" />
</c:if>
<!-- Debut Caracteristiques generales -->
<fieldset class="${className}">
	<legend><span><fmt:message key="caracteristiques.contribuable" /></span></legend>
	<table cellspacing="0" cellpadding="5">
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.numero.contribuable" />&nbsp;:</td>
			<jsp:include page="numero.jsp">
				<jsp:param name="page" value="${page}" />
				<jsp:param name="path" value="${path}" />
			</jsp:include>
			<td width="25%">&nbsp;</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.role" />&nbsp;:</td>
			<td width="50%">
				<jsp:include page="role.jsp">
					<jsp:param name="path" value="${path}" />
				</jsp:include>
			</td>
			<td width="25%">&nbsp;</td>
		</tr>
		<jsp:include page="adresse-envoi.jsp">
			<jsp:param name="path" value="${path}" />
		</jsp:include>

	</table>
	
</fieldset>
<!-- Fin Caracteristiques generales -->