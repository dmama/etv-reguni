<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="page" value="${param.page}" />
<c:set var="path" value="${param.path}" />
<!-- Debut Caracteristiques generales -->
<fieldset>
	<legend><span><fmt:message key="caracteristiques.debiteur.is" /></span></legend>
	<table>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.numero.debiteur" />&nbsp;:</td>
			<jsp:include page="numero.jsp">
				<jsp:param name="page" value="${page}" />
				<jsp:param name="path" value="${path}" />	
			</jsp:include>
			<td width="25%">&nbsp;</td>
		</tr>
			
		<jsp:include page="adresse-envoi.jsp">
			<jsp:param name="path" value="${path}" />
		</jsp:include>
		
		<jsp:include page="complement-debiteur.jsp">
			<jsp:param name="path" value="${path}" />
		</jsp:include>
		
	</table>
	
</fieldset>
<!-- Fin Caracteristiques generales -->