<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="index" value="${param.index}" />
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">
		<fmt:message key="title.ajout.modele.document">
			<fmt:param>${command.anneePeriodeFiscale}</fmt:param>
		</fmt:message>
	</tiles:put>

	<tiles:put name="body">
	<form:form name="form" id="formModeleAdd">
		<form:hidden path="idPeriode"/>
		<fieldset>
			<legend><fmt:message key="label.param.modele-add" /></legend>
			<table>
				<spring:bind path="typeDocument">
				<tr>
					<th><fmt:message key="title.param.type"/></th>
					<td>
						<form:select path="typeDocument" items="${typeDocuments}"/>
						<c:if test="${status.error}">
			 				&nbsp;<span class="erreur">${status.errorMessage}</span>
			 			</c:if>
					</td>
				</tr>
				</spring:bind>
			</table>
		</fieldset>
		<div>
			<input type="submit" id="ajout" value="<fmt:message key="label.bouton.ajouter" />">
			<input type="button" id="annuler" value="<fmt:message key="label.bouton.annuler" />" onclick="document.location.href='list.do?pf=${command.idPeriode}'">
		</div>		
	</form:form>	
	</tiles:put>
</tiles:insert>
