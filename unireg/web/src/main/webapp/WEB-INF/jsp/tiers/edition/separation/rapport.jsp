<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Rapport Menage Commun -->
<fieldset>
	<legend><span><fmt:message key="label.rapport.separation" /></span></legend>
	<table>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.date.separation" />&nbsp;:</td>
			<td width="75%">
				<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
					<jsp:param name="path" value="dateSeparation" />
					<jsp:param name="id" value="dateSeparation" />
				</jsp:include>
				<FONT COLOR="#FF0000">*</FONT>
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%" style="vertical-align: top"><fmt:message key="label.type.separation" />&nbsp;:</td>
			<td width="75%">
				<form:radiobutton path="etatCivil" id="type-divorce" value="DIVORCE"/>
				<label for="type-divorce"><fmt:message key="label.type.separation.divorce"/></label>
				<br/>
				<form:radiobutton path="etatCivil" id="type-separation" value="SEPARE"/>
				<label for="type-separation"><fmt:message key="label.type.separation.separation"/></label>
				<!--  
				L'annulation légale de mariage est désactivée
				<br/>
				<form:radiobutton path="etatCivil" id="type-annulation-legale" value="NON_MARIE"/>
				<label for="type-annulation-legale"><fmt:message key="label.type.separation.annulation.legale"/></label>
				-->
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>">
			<td width="25%"><fmt:message key="label.commentaire" />&nbsp;:</td>
			<td width="75%">
				<form:textarea path="remarque" id="remarque" cols="80" rows="5"/>
			</td>
		</tr>
	</table>
	
</fieldset>
<!-- Fin Rapport Menage Commun -->