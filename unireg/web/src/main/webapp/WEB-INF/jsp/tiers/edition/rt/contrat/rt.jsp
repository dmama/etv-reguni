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
				</jsp:include>
				<FONT COLOR="#FF0000">*</FONT>
			</td>
			<td width="25%">&nbsp;</td>
			<td width="25%">&nbsp;</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.type.activite" />&nbsp;:</td>
			<td width="25%">
				<form:select path="typeActivite"  items="${typesActivite}"
					id="typeActivite" onchange="selectTypeActivite(this.options[this.selectedIndex].value);" />		
			</td>
			<td width="25%"><div id="tauxActiviteLabel" ><fmt:message key="label.taux.activite" />&nbsp;:</div></td>
			<td width="25%">
				<div id="tauxActiviteInput" style="display:block;">
					<form:input path="tauxActivite" cssErrorClass="input-with-errors" size ="3" maxlength="3" />%
					<form:errors path="tauxActivite" cssClass="error"/>
				</div>
			</td>
		</tr>
	</table>
	
</fieldset>
<!-- Fin Caracteristiques rapport de travail -->