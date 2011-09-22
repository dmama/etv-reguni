<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<fieldset>
	<legend><span><fmt:message key="label.delais" /></span></legend>
	
	<table border="0">
		<tr>
			<td>
				<a href="delai.do?idLR=${command.id}" class="add" title="Ajouter">&nbsp;<fmt:message key="label.bouton.ajouter"/></a>
			</td>
		</tr>
	</table>
	
	<jsp:include page="../../tiers/common/delai/delais.jsp">
		<jsp:param name="page" value="edit"/>
	</jsp:include>

</fieldset>