<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<table>
	<tr class="<unireg:nextRowClass/>" >
		<td width="25%"><fmt:message key="label.periodicite" />&nbsp;:</td>
		<td width="25%">
			<form:select path="periodicite">
				<form:option value="" ><fmt:message key="option.TOUTES" /></form:option>
				<form:options items="${periodicitesDecompte}" />
			</form:select>	
		</td>
		<td width="25%"><fmt:message key="label.date.debut.periode" />&nbsp;:</td>
		<td width="25%">
			<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
				<jsp:param name="path" value="periode" />
				<jsp:param name="id" value="periode" />
			</jsp:include>
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td><fmt:message key="label.categorie.impot.source" />&nbsp;:</td>
		<td>
			<form:select path="categorie">
				<form:option value="" ><fmt:message key="option.TOUTES" /></form:option>
				<form:options items="${categoriesImpotSource}" />
			</form:select>	
		</td>
		<td><fmt:message key="label.etat.avancement" />&nbsp;:</td>
		<td>
			<form:select path="etat">
				<form:option value="" ><fmt:message key="option.TOUS" /></form:option>
				<form:options items="${etatsDocument}" />
			</form:select>	
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td><fmt:message key="label.mode.communication" />&nbsp;:</td>
		<td>
			<form:select path="modeCommunication">
				<form:option value="" ><fmt:message key="option.TOUS" /></form:option>
				<form:options items="${modesCommunication}" />
			</form:select>	
		</td>
		<td>&nbsp;</td>
		<td>&nbsp;</td>
	</tr>
</table>
<!-- Debut Boutons -->
<table border="0">
	<tr class="<unireg:nextRowClass/>" >
		<td width="25%">&nbsp;</td>
		<td width="25%">
			<!-- Comme ça, quand on soumet réellement le formulaire, on le sait et on peut interdire de récupérer les critères en session... -->
			<input type="hidden" name="realSearch" value="true"/>
			<div class="navigation-action"><input type="submit" value="<fmt:message key="label.bouton.rechercher"/>"/></div>
		</td>
		<td width="25%">
			<div class="navigation-action">
				<c:set var="effacerName"><fmt:message key="label.bouton.effacer"/></c:set>
				<unireg:buttonTo name="${effacerName}" action="/lr/list.do?effacer=true" method="get"/>
			</div>
		</td>
		<td width="25%">&nbsp;</td>
	</tr>
</table>
<!-- Fin Boutons -->