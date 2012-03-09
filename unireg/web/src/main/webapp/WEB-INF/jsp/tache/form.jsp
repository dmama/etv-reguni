<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<table>
	<tr class="<unireg:nextRowClass/>" >
		<td width="25%"><fmt:message key="label.type.tache" />&nbsp;:</td>
		<td width="25%">
			<form:select path="typeTache" id="type_tache" onchange="selectTypeTache(this.options[this.selectedIndex].value);" >
				<form:option value="TOUS" ><fmt:message key="option.TOUTES" /></form:option>
				<form:options items="${typesTache}" />
			</form:select>	
		</td>
		<td width="25%" id="periode_fiscale_label" style="display:none;"><fmt:message key="label.periode.fiscale" />&nbsp;:</td>
		<td width="25%" id="periode_fiscale_input" style="display:none;">
			<form:select path="annee">
				<form:option value="TOUS" ><fmt:message key="option.TOUTES" /></form:option>
				<form:options items="${periodesFiscales}" />
			</form:select>
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td width="25%"><fmt:message key="label.etat.tache" />&nbsp;:</td>
		<td width="25%">
			<form:select path="etatTache">
				<form:option value="TOUS" ><fmt:message key="option.TOUS" /></form:option>
				<form:option value="EN_INSTANCE" ><fmt:message key="option.etat.tache.EN_INSTANCE" /></form:option>
				<form:option value="TRAITE" ><fmt:message key="option.etat.tache.TRAITE" /></form:option>
			</form:select>
		</td>
		<td width="25%"><fmt:message key="label.office.impot" />&nbsp;:</td>
		<td width="25%">
			<form:select path="officeImpot">
				<form:option value="TOUS" ><fmt:message key="option.TOUS" /></form:option>
				<form:options items="${officesImpotUtilisateur}" />
			</form:select>	
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td width="25%"><fmt:message key="label.date.depuis" />&nbsp;:</td>
		<td width="25%">
			<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
				<jsp:param name="path" value="dateCreationDepuis" />
				<jsp:param name="id" value="dateCreationDepuis" />
			</jsp:include>
		</td>
		<td width="25%"><fmt:message key="label.date.jusqua" />&nbsp;:</td>
		<td width="25%">
			<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
				<jsp:param name="path" value="dateCreationJusqua" />
				<jsp:param name="id" value="dateCreationJusqua" />
			</jsp:include>
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td width="25%"><fmt:message key="label.numero.contribuable" />&nbsp;:</td>
		<td width="25%">
			<form:input  path="numeroFormate" id="numeroFormate" cssClass="number"/>
			<form:errors path="numeroFormate" cssClass="error"/>
		</td>
		<td width="25%"><fmt:message key="label.type.voir.toutes.taches" />&nbsp;:</td>
		<td width="25%"><form:checkbox path="voirTachesAutomatiques" title="Inclure aussi les tâches qui seront traitées en masse par les jobs automatiques" /></td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td width="25%" />
		<td width="25%" />
		<td width="25%"><fmt:message key="label.type.voir.taches.annulees" />&nbsp;:</td>
		<td width="25%"><form:checkbox path="voirTachesAnnulees" title="Inclure aussi les tâches annulées" /></td>
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
			<div class="navigation-action"><input type="submit" value="<fmt:message key="label.bouton.effacer"/>" name="effacer" /></div>
		</td>
		<td width="25%">&nbsp;</td>
	</tr>
</table>
<!-- Fin Boutons -->
<script type="text/javascript" language="Javascript" src="<c:url value="/js/tache.js"/>"></script>
<script type="text/javascript">
	selectTypeTache('${command.typeTache}');
</script>