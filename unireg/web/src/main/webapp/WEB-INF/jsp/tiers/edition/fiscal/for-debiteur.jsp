<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<fieldset>
<legend><span><fmt:message key="label.fors.fiscaux" /></span></legend>
		
	<table border="0">
		<tr class="<unireg:nextRowClass/>" >
			<td>
				<a href="../fiscal/for.do?numero=<c:out value="${command.tiers.numero}"></c:out>&nature=DPI&index=" class="add" title="Ajouter for">&nbsp;<fmt:message key="label.bouton.ajouter"/></a>
			</td>
		</tr>
	</table>
	
	<jsp:include page="../../common/fiscal/for-debiteur.jsp">
		<jsp:param name="page" value="edit"/>
	</jsp:include>
</fieldset>
