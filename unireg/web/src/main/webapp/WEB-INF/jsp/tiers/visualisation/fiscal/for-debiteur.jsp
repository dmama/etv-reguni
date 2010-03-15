<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<fieldset>
	<legend><span><fmt:message key="label.fors.fiscaux" /></span></legend>
	<c:if test="${not empty command.forsFiscaux}">
		<input name="fors_debiteurs_histo"
			type="checkbox" onClick="toggleRowsIsHisto('forFiscal','isForDebHisto', 2);" id="isForDebHisto" />
		<label for="isForDebHisto"><fmt:message key="label.historique" /></label>
		
		<jsp:include page="../../common/fiscal/for-debiteur.jsp">
			<jsp:param name="page" value="visu"/>
		</jsp:include>
	</c:if>		
</fieldset>
