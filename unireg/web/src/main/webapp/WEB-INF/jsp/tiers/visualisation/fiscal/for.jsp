<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<c:if test="${not empty command.forsFiscaux}">
	<fieldset>
		<legend><span><fmt:message key="label.fors.fiscaux" /></span></legend>
		<unireg:raccourciToggleAffichage tableId="forFiscal" numeroColonne="6" nombreLignes="${fn:length(command.forsFiscaux)}" />

		<jsp:include page="../../common/fiscal/for.jsp">
			<jsp:param name="page" value="visu"/>
		</jsp:include>
		
	</fieldset>
</c:if>