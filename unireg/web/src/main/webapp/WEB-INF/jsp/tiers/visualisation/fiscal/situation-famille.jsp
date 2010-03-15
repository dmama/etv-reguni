<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<c:if test="${not empty command.situationsFamille}">
	<fieldset><legend><span>
	<fmt:message
		key="label.situation.famille.fiscale" /></span></legend>
		
		<input name="adresse_histo" type="checkbox" onClick="toggleRowsIsHisto('situationFamille','isSFHisto', 5);" id="isSFHisto" />
		<label for="isSFHisto"><fmt:message key="label.historique" /></label>
		
		<jsp:include page="../../common/fiscal/situation-famille.jsp">
			<jsp:param name="page" value="visu"/>
		</jsp:include>
		
	</fieldset>
</c:if>