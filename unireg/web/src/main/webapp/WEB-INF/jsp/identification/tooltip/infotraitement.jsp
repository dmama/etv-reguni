<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<unireg:nextRowClass reset="1"/>

<tiles:insert template="/WEB-INF/jsp/templates/templateDialog.jsp">
	<tiles:put name="body">
	<table>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.identification.traitement.user" />&nbsp;:</td>
			<td width="25%"></td>
			<td width="25%"><fmt:message key="label.identification.traitement.date" />&nbsp;:</td>
			<td width="25%"></td>
		</tr>
	</table>

	</tiles:put>
</tiles:insert>