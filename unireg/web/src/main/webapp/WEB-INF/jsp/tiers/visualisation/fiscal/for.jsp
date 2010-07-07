<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<c:if test="${not empty command.forsFiscaux}">
	<fieldset>
	<legend><span><fmt:message key="label.fors.fiscaux" /></span></legend>
	<input name="fors_histo"
	type="checkbox" onClick="toggleRowsIsHisto('forFiscal', 'isForHisto', 6);" id="isForHisto" />
	<label for="isForHisto"><fmt:message key="label.historique" /></label>
		
	<jsp:include page="../../common/fiscal/for.jsp">
		<jsp:param name="page" value="visu"/>
	</jsp:include>
		
	</fieldset>
</c:if>