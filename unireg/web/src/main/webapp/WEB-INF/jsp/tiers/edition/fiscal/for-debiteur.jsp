<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<span><%-- span vide pour que IE8 calcule correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
<fieldset>
<legend><span><fmt:message key="label.fors.fiscaux" /></span></legend>
		
	<table border="0">
		<tr class="<unireg:nextRowClass/>" >
			<td>
				<unireg:linkTo name="Ajouter" title="Ajouter for" action="/fors/debiteur/add.do" params="{tiersId:${command.tiers.numero}}" link_class="add"/>
			</td>
		</tr>
	</table>
	
	<jsp:include page="../../common/fiscal/for-debiteur.jsp">
		<jsp:param name="page" value="edit"/>
	</jsp:include>
</fieldset>
