<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="path" value="${param.path}" />
<c:set var="bind" value="command.${path}" scope="request"/>
<spring:bind path="${bind}" >
	<c:set var="message" value="${status.value}"  scope="request"/>
</spring:bind>
<!-- Debut Caracteristiques identification -->
<fieldset class="information">
	<legend><span>
		<fmt:message key="caracteristiques.message.Retour" />
	</span></legend>

	<table>			
	<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
		<td>
			<form:select path="erreurMessage">								
						<form:options items="${erreursMessage}" />
			</form:select>
		</td>			
	</tr>
	<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
		<td width="25%">&nbsp;</td>
		<td width="25%">&nbsp;</td>		
	</tr>
		
   </table>
   </fieldset>
<input type="button" id="annuler" value="<fmt:message key="label.bouton.retour" />" onclick="javascript:Page_RetourNonIdentification()">
&nbsp;
<input type="button" name="nonIdentifier" value="<fmt:message key="label.bouton.identification.valider" />" onClick="javascript:confirmerImpossibleAIdentifier(${command.demandeIdentificationView.id});" />

	

<!-- Fin Boutons -->

	

<!-- Fin Caracteristiques identification -->