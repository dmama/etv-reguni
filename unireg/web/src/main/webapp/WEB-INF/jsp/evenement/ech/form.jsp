<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<table id="tableForm">
	<tr class="<unireg:nextRowClass/> toggle" >
		<td width="25%"><fmt:message key="label.type.evenement" />&nbsp;:</td>
		<td width="75%" colspan ="3">
			<form:select path="typeEvenement">
				<form:option value="TOUS" ><fmt:message key="option.TOUS" /></form:option>
				<form:options items="${typesEvenementEch}"/>
			</form:select>	
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/> toggle" >
		<td width="25%"><fmt:message key="label.etat.evenement" />&nbsp;:</td>
		<td width="25%">
			<form:select path="etatEvenement" >
				<form:option value="TOUS" ><fmt:message key="option.TOUS" /></form:option>
				<form:options items="${etatsEvenement}" />
			</form:select>
		</td>
		<td width="25%"><fmt:message key="label.action.evenement" />&nbsp;:</td>
		<td width="25%">
			<form:select path="actionEvenement" >
				<form:option value="TOUS" ><fmt:message key="option.TOUS" /></form:option>
				<form:options items="${actionsEvenementEch}" />
			</form:select>
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/> toggle" >
		<td width="25%"><fmt:message key="label.date.evenement.debut" />&nbsp;:</td>
		<td width="25%">
			<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
				<jsp:param name="path" value="dateEvenementDebut" />
				<jsp:param name="id" value="dateEvenementDebut" />
			</jsp:include>
		</td>
		<td width="25%"><fmt:message key="label.date.evenement.fin" />&nbsp;:</td>
		<td width="25%">
			<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
				<jsp:param name="path" value="dateEvenementFin" />
				<jsp:param name="id" value="dateEvenementFin" />
			</jsp:include>
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/> toggle" >
		<td width="25%"><fmt:message key="label.date.traitement.debut" />&nbsp;:</td>
		<td width="25%">
			<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
				<jsp:param name="path" value="dateTraitementDebut" />
				<jsp:param name="id" value="dateTraitementDebut" />
			</jsp:include>
		</td>
		<td width="25%"><fmt:message key="label.date.traitement.fin" />&nbsp;:</td>
		<td width="25%">
			<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
				<jsp:param name="path" value="dateTraitementFin" />
				<jsp:param name="id" value="dateTraitementFin" />
			</jsp:include>
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td width="25%"><fmt:message key="label.numero.individu" />&nbsp;:</td>
		<td width="25%">
			<form:input  path="numeroIndividuFormatte" id="numeroIndividuFormatte" cssClass="number"/>
			<form:errors path="numeroIndividuFormatte" cssClass="error"/>
		</td>
		<td width="25%">Événements en attente pour l'individu <span id="num_indiv"></span></td>
		<td width="25%">
			<form:checkbox  path="rechercheEvenementEnAttente" id="rechercheEvenementEnAttente" cssClass="boolean"/>
			<form:errors path="rechercheEvenementEnAttente" cssClass="error"/>
		</td>
	</tr>
    <tr class="<unireg:nextRowClass/> toggle" >
        <td width="25%"><fmt:message key="label.numero.contribuable" />&nbsp;:</td>
        <td width="25%">
            <form:input  path="numeroCTBFormatte" id="numeroCTBFormatte" cssClass="number"/>
            <form:errors path="numeroCTBFormatte" cssClass="error"/>
        </td>
        <td width="25%">&nbsp;</td>
        <td width="25%">&nbsp;</td>
    </tr>


</table>
<!-- Debut Boutons -->
<table border="0">
	<tr class="<unireg:nextRowClass/>" >
		<td width="25%">&nbsp;</td>
		<td width="25%">
			<fmt:message key="label.bouton.rechercher" var="labelBoutonRechercher"/>
			<div class="navigation-action"><input type="submit" value="${labelBoutonRechercher}" name="rechercher" /></div>
		</td>
		<td width="25%">
			<fmt:message key="label.bouton.effacer" var="labelBoutonEffacer"/>
			<div class="navigation-action"><input type="submit" value="${labelBoutonEffacer}" name="effacer"  /></div>
		</td>
		<td width="25%">&nbsp;</td>
	</tr>
</table>
<!-- Fin Boutons -->