<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Rapport Menage Commun -->
<fieldset>
	<legend><span><fmt:message key="label.caracteristiques.annulation" /></span></legend>
	<table>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.date.annulation" />&nbsp;:</td>
			<td width="75%">
				<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
					<jsp:param name="path" value="dateAnnulation" />
					<jsp:param name="id" value="dateAnnulation" />
				</jsp:include>
				<FONT COLOR="#FF0000">*</FONT>
			</td>
		</tr>
	</table>
	
</fieldset>
<!-- Fin Rapport Menage Commun -->