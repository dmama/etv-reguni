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
	<tr class="<unireg:nextRowClass/>" >
		<td>
			<form:select path="erreurMessage">								
						<form:options items="${erreursMessage}" />
			</form:select>
			<span class="mandatory">*</span>
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td width="25%">&nbsp;</td>
		<td width="25%">&nbsp;</td>		
	</tr>
		
   </table>
</fieldset>

<c:set var="retourButtonName">
    <fmt:message key="label.bouton.retour" />
</c:set>
<unireg:buttonTo name="${retourButtonName}" action="/identification/gestion-messages/edit.do" confirm="Voulez-vous vraiment quitter cette page sans sauver ?" method="get"/>
&nbsp;
<input type="button" name="nonIdentifier" value="<fmt:message key="label.bouton.identification.valider" />" onClick="IdentificationCtb.confirmerImpossibleAIdentifier(${command.demandeIdentificationView.id});" />

	

<!-- Fin Boutons -->

<script type="text/javascript" language="javascript" src="<c:url value="/js/identification.js"/>"></script>


<!-- Fin Caracteristiques identification -->