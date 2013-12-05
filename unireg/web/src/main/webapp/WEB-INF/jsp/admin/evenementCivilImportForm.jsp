<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title"><fmt:message key="title.import.evenements.civils" /></tiles:put>
  	<tiles:put name="body">
	    <h2><fmt:message key="label.import.evenements.civils.selectionner"/></h2>
	    <fieldset>
		<legend><span><fmt:message key="label.import.evenements.civils" /></span></legend>
		    <form:form method="post" enctype="multipart/form-data">
			    <table>
					<tr class="odd" >
						<td><fmt:message key="label.admin.evenements.civils.filename"/></td>
						<td>
						<input type="file" id="multipartFile" name="multipartFile"/><form:errors path="multipartFile" cssClass="erreur"/></td>
					</tr>
					<tr class="even" >
						<td><input type="button" value="<fmt:message key="label.bouton.annuler"/>" onclick="document.location='index.do';"/></td>
						<td><input type="submit" value="<fmt:message key="label.bouton.charger"/>" /></td>
					</tr>
				</table>
			</form:form>
		</fieldset>
		<br/>
		<br/>
		<br/>
		<br/>
  	</tiles:put>
</tiles:insert>
