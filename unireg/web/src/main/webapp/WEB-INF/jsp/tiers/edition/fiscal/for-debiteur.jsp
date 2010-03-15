<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<fieldset>
<legend><span><fmt:message key="label.fors.fiscaux" /></span></legend>
		
	<table border="0">
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td>
				<a href="../fiscal/for.do?numero=<c:out value="${command.tiers.numero}"></c:out>&nature=DPI&height=150&width=900&index=&TB_iframe=true&modal=true" 
				class="add thickbox" title="Ajouter for">&nbsp;<fmt:message key="label.bouton.ajouter" /></a>
			</td>
		</tr>
	</table>
	
	<jsp:include page="../../common/fiscal/for-debiteur.jsp">
		<jsp:param name="page" value="edit"/>
	</jsp:include>
</fieldset>
