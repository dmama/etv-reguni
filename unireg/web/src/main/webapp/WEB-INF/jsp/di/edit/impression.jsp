<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="index" value="${param.index}" />
<tiles:insert template="/WEB-INF/jsp/templates/templateIFrame.jsp">
	<tiles:put name="head"></tiles:put>

	<tiles:put name="title"></tiles:put>
	<tiles:put name="body">
		<form:form name="formImpression" id="formImpression">
		<fieldset><legend><span><fmt:message key="label.impression.di" /></span></legend>
		<unireg:nextRowClass reset="0"/>
		<table border="0">

			<c:forEach items="${command.modelesDocumentView}" var="modele" varStatus="statusModele">
				<tr class="<unireg:nextRowClass/>" >
					<td width="50%">	
						<form:radiobutton path="selectedTypeDocument" value="${modele.typeDocument}" onclick="javascript:changeTypeDocument(${command.idDI}, '${modele.typeDocument}');" />
						<b><fmt:message key="option.type.document.${modele.typeDocument}"/>&nbsp;:</b>
					</td>
					<td width="50%">
						&nbsp;
					</td>
				</tr>

				<c:forEach items="${modele.modelesFeuilles}" var="feuille" varStatus="statusFeuille">
				<tr class="<unireg:nextRowClass/>" >
					<td width="50%">${feuille.intituleFeuille}&nbsp;:</td>
					<td width="50%">	
					<c:if test="${command.selectedTypeDocument != modele.typeDocument}">
						<form:input path="modelesDocumentView[${statusModele.index}].modelesFeuilles[${statusFeuille.index}].nbreIntituleFeuille" size="2" disabled="true"/>
					</c:if>
					<c:if test="${command.selectedTypeDocument == modele.typeDocument}">
						<form:input path="modelesDocumentView[${statusModele.index}].modelesFeuilles[${statusFeuille.index}].nbreIntituleFeuille" size="2" maxlength="1" />
					</c:if>
					
					</td>
				</tr>
				</c:forEach>

			</c:forEach>

		</table>
		</fieldset>
		<table border="0">
			<tr>
				<td colspan="2" align="center">
					<input type="button" id="imprimer" value="<fmt:message key="label.bouton.imprimer" />" onclick="javascript:duplicataDI();">
					<input type="button" id="effacer" value="<fmt:message key="label.bouton.effacer" />" onclick="javascript:effacerImpressionDI(${command.idDI});">
					<input type="button" id="annuler" value="<fmt:message key="label.bouton.fermer" />" onclick="self.parent.tb_remove()">
				</td>
			</tr>
			
		</table>
		</form:form>

	</tiles:put>
</tiles:insert>