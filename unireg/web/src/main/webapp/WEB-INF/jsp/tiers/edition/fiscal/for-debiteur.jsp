<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<fieldset>
<legend><span><fmt:message key="label.fors.fiscaux" /></span></legend>
		
	<table border="0">
		<tr class="<unireg:nextRowClass/>" >
			<td>
				<unireg:linkTo name="Ajouter" title="Ajouter for" action="/fors/addDebiteur.do" params="{tiersId:${command.tiers.numero}}" link_class="add"/>
			</td>
		</tr>
	</table>
	
	<jsp:include page="../../common/fiscal/for-debiteur.jsp">
		<jsp:param name="page" value="edit"/>
	</jsp:include>
</fieldset>
