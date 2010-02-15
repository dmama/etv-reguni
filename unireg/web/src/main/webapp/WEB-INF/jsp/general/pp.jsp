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
	<legend><span><fmt:message key="label.caracteristiques.${param.path}" /></span></legend>
	<table cellspacing="0" cellpadding="5">
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td width="25%"><fmt:message key="label.numero.contribuable" />&nbsp;:</td>
			<jsp:include page="numero.jsp">
				<jsp:param name="page" value="${page}" />	
				<jsp:param name="path" value="${path}" />
			</jsp:include>
			<td width="25%">&nbsp;</td>
		</tr>
		
		<jsp:include page="adresse-envoi.jsp">
			<jsp:param name="path" value="${path}" />
		</jsp:include>
		
		<jsp:include page="complement-pp.jsp">
			<jsp:param name="path" value="${path}" />
		</jsp:include>

	</table>
	
</fieldset>
<!-- Fin Caracteristiques generales -->