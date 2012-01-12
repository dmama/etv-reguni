<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<table>
	<tr class="<unireg:nextRowClass/>" >
		<td width="25%"><fmt:message key="label.type.evenement" />&nbsp;:</td>
		<td width="75%" colspan ="3">
			<form:select path="typeEvenement">
				<form:option value="TOUS" ><fmt:message key="option.TOUS" /></form:option>
				<form:options items="${typesEvenement}"/>
			</form:select>	
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td width="25%"><fmt:message key="label.etat.evenement" />&nbsp;:</td>
		<td width="25%">
			<form:select path="etatEvenement" >
				<form:option value="TOUS" ><fmt:message key="option.TOUS" /></form:option>
				<form:options items="${etatsEvenement}" />
			</form:select>
		</td>
		<td width="25%">&nbsp;</td>
		<td width="25%">&nbsp;</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
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
	<tr class="<unireg:nextRowClass/>" >
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
		<td><fmt:message key="label.numero.individu" />&nbsp;:</td>
		<td>
			<form:input  path="numeroIndividuFormatte" id="numeroIndividuFormatte" cssClass="number"/>
		</td>
		<td><fmt:message key="label.numero.contribuable" />&nbsp;:</td>
		<td>
			<form:input  path="numeroCTBFormatte" id="numeroCTBFormatte" cssClass="number"/>
			<form:errors path="numeroCTBFormatte" cssClass="error"/>
		</td>
	</tr>

</table>
<!-- Debut Boutons -->
<table border="0">
	<tr class="<unireg:nextRowClass/>" >
		<td width="25%">&nbsp;</td>
		<td width="25%">
			<div class="navigation-action"><input type="submit" value="<fmt:message key="label.bouton.rechercher"/>" name="rechercher" /></div>
		</td>
		<td width="25%">
			<div class="navigation-action"><input type="submit" value="<fmt:message key="label.bouton.effacer" />" name="effacer"  /></div>
		</td>
		<td width="25%">&nbsp;</td>
	</tr>
</table>
<!-- Fin Boutons -->