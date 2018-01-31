<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<fieldset><legend><span><fmt:message key="label.decision.aci" /></span></legend>

	<table border="0">
		<tr>
			<td>
				<unireg:setAuth var="autorisations" tiersId="${command.tiers.numero}"/>
                <c:if test="${autorisations.decisionsAci}">
                    <unireg:raccourciAjouter link="../decision-aci/add.do?tiersId=${command.tiers.numero}" tooltip="Ajouter une décision ACI" display="label.bouton.ajouter"/>
				</c:if>
			</td>
		</tr>
	</table>
	<jsp:include page="../../common/fiscal/decision-aci.jsp">
		<jsp:param name="page" value="edit"/>
	</jsp:include>



</fieldset>


