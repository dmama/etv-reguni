<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="page" value="${param.page}" />
<c:set var="path" value="${param.path}" />
<c:set var="bind" value="command.${path}" scope="request"/>
<spring:bind path="${bind}" >
	<c:set var="tiersGeneral" value="${status.value}"  scope="request"/>
</spring:bind>

<td width="50%">
	<unireg:numCTB numero="${tiersGeneral.numero}"></unireg:numCTB>
	<c:if test="${page == 'visu' }">
		<unireg:raccourciConsulter link="../common/consult-log.do?height=200&width=800&nature=Tiers&id=${tiersGeneral.numero}&TB_iframe=true&modal=true" thickbox="true" tooltip="Edition des logs"/>
	</c:if>
</td>
