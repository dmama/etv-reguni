<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<table>

	<tr class="<unireg:nextRowClass/>" >
		<td width="25%"><fmt:message key="label.reqdes.unite.traitement.etat" />&nbsp;:</td>
		<td width="75%" colspan ="3">
			<form:select path="etat">
				<form:option value=""><fmt:message key="option.TOUS" /></form:option>
				<form:options items="${etats}"/>
			</form:select>	
		</td>
	</tr>

	<tr class="<unireg:nextRowClass/>" >
		<td width="25%"><fmt:message key="label.reqdes.unite.traitement.date.acte.min" />&nbsp;:</td>
		<td width="25%">
			<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
				<jsp:param name="path" value="dateActeMin" />
				<jsp:param name="id" value="dateActeMin" />
			</jsp:include>
		</td>
		<td width="25%"><fmt:message key="label.reqdes.unite.traitement.date.acte.max" />&nbsp;:</td>
		<td width="25%">
			<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
				<jsp:param name="path" value="dateActeMax" />
				<jsp:param name="id" value="dateActeMax" />
			</jsp:include>
		</td>
	</tr>

	<tr class="<unireg:nextRowClass/>" >
		<td width="25%"><fmt:message key="label.reqdes.unite.traitement.date.traitement.min" />&nbsp;:</td>
		<td width="25%">
			<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
				<jsp:param name="path" value="dateTraitementMin" />
				<jsp:param name="id" value="dateTraitementMin" />
			</jsp:include>
		</td>
		<td width="25%"><fmt:message key="label.reqdes.unite.traitement.date.traitement.max" />&nbsp;:</td>
		<td width="25%">
			<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
				<jsp:param name="path" value="dateTraitementMax" />
				<jsp:param name="id" value="dateTraitementMax" />
			</jsp:include>
		</td>
	</tr>

	<tr class="<unireg:nextRowClass/>" >
		<td width="25%"><fmt:message key="label.reqdes.numero.minute" />&nbsp;:</td>
		<td width="25%">
			<form:input path="numeroMinute" id="numeroMinute"/>
			<form:errors path="numeroMinute" cssClass="error"/>
		</td>
		<td width="25%"><fmt:message key="label.reqdes.notaire.visa" />&nbsp;:</td>
		<td width="25%">
			<form:input path="visaNotaire" id="visaNotaire"/>
			<form:errors path="visaNotaire" cssClass="error"/>
		</td>
	</tr>

</table>

<!-- Debut Boutons -->
<table border="0">
	<tr class="<unireg:nextRowClass/>" >
		<td width="25%">&nbsp;</td>
		<td width="25%">
			<fmt:message key="label.bouton.rechercher" var="labelBoutonRechercher"/>
			<div class="navigation-action"><input type="submit" value="${labelBoutonRechercher}" name="rechercher" id="rechercher"/></div>
		</td>
		<td width="25%">
			<fmt:message key="label.bouton.effacer" var="labelBoutonEffacer"/>
			<div class="navigation-action"><input type="submit" value="${labelBoutonEffacer}" name="effacer"  id="effacer"/></div>
		</td>
		<td width="25%">&nbsp;</td>
	</tr>
</table>
<!-- Fin Boutons -->