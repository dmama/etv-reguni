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
			</jsp:include></td>
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
			<div class="navigation-action"><input type="submit" value="<fmt:message key="label.bouton.rechercher"/>"/></div>
		</td>
		<td width="25%">
			<div class="navigation-action"><input type="button" value="<fmt:message key="label.bouton.effacer" />" name="effacer" onClick="effacerCriteresLR();" /></div>
		</td>
		<td width="25%">&nbsp;</td>
	</tr>
</table>
<script type="text/javascript">
	function effacerCriteresLR() {
  		top.location.replace('list.do?action=effacer');
	}
</script>
<!-- Fin Boutons -->