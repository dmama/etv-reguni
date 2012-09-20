<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<fieldset>
	<legend><span><fmt:message key="label.fors.fiscaux" /></span></legend>
	<c:if test="${not empty command.forsFiscaux}">
		<unireg:raccourciToggleAffichage tableId="forFiscal" numeroColonne="2" nombreLignes="${fn:length(command.forsFiscaux)}"/>

		<jsp:include page="../../common/fiscal/for-debiteur.jsp">
			<jsp:param name="page" value="visu"/>
		</jsp:include>
	</c:if>		
</fieldset>
