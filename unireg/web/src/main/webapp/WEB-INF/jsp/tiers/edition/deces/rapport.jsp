<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Rapport Menage Commun -->
<fieldset>
	<legend><span><fmt:message key="label.caracteristiques.deces" /></span></legend>
	<table>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.date.deces" />&nbsp;:</td>
			<td width="75%">
				<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
					<jsp:param name="path" value="dateDeces" />
					<jsp:param name="id" value="dateDeces" />
				</jsp:include>
				<FONT COLOR="#FF0000">*</FONT>
			</td>
		</tr>
		<c:if test="${command.marieSeul}">
			<tr class="<unireg:nextRowClass/>" >
				<td width="100%" colspan="2"><fmt:message key="label.deces.nature.marie.seul"/>&nbsp;:</td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				<td width="25%">&nbsp;</td>
				<td width="75%">
					<form:radiobutton path="veuf" id="nature-decede" value="false"/>
					<label for="nature-decede"><fmt:message key="label.deces.decede"/></label>
					<br>
					<form:radiobutton path="veuf" id="nature-veuf" value="true"/>
					<label for="nature-veuf"><fmt:message key="label.deces.veuf"/></label>
				</td>
			</tr>
		</c:if>
		<tr class="<unireg:nextRowClass/>">
			<td width="25%"><fmt:message key="label.commentaire" />&nbsp;:</td>
			<td width="75%">
				<form:textarea path="remarque" id="remarque" cols="80" rows="5"/>
			</td>
		</tr>
	</table>
	
</fieldset>
<!-- Fin Rapport Menage Commun -->