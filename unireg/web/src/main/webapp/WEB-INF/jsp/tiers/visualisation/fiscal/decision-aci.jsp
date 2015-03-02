<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<c:if test="${not empty command.decisionsAci}">
    <fieldset>
        <legend><span><fmt:message key="label.decision.aci" /></span></legend>
        <unireg:raccourciToggleAffichage tableId="decisionAci" numeroColonne="3" nombreLignes="${fn:length(command.decisionsAci)}" />

        <jsp:include page="../../common/fiscal/decision-aci.jsp">
            <jsp:param name="page" value="visu"/>
        </jsp:include>

    </fieldset>
</c:if>
