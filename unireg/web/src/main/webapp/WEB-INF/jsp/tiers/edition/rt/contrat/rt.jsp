<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Caracteristiques rapport de travail -->
<fieldset>
	<legend><span><fmt:message key="label.caracteristiques.rt" /></span></legend>
	<table>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.date.debut" />&nbsp;:</td>
			<td width="25%">
				<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
					<jsp:param name="path" value="dateDebut"  />
					<jsp:param name="id" value="dateDebut" />
					<jsp:param name="mandatory" value="true" />
				</jsp:include>
			</td>
			<td width="25%">&nbsp;</td>
			<td width="25%">&nbsp;</td>
		</tr>
	</table>
	
</fieldset>
<!-- Fin Caracteristiques rapport de travail -->