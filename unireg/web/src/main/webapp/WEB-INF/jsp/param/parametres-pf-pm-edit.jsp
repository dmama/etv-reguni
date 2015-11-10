<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="index" value="${param.index}" />
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="head">
		<style>
			span.bouton {
				width: 50%;
				text-align: center;
			}
			div.checkbox {
				margin: 10px;
			}
		</style>
	</tiles:put>

	<tiles:put name="title">
		<fmt:message key="title.edit.param.periode.fiscale.pm">
			<fmt:param>${command.anneePeriodeFiscale}</fmt:param>
		</fmt:message>
	</tiles:put>

	<tiles:put name="body">
	<form:form name="form" id="formTermes">
		<form:hidden path="idPeriodeFiscale"/>
		<fieldset>
			<legend><fmt:message key="label.param.parametres-pf-edit" /></legend>

			<%--<div class="checkbox">--%>
				<%--<c:set var="labelCheckbox">--%>
					<%--<fmt:message key="label.param.code.controle.sur.sommation"/>--%>
				<%--</c:set>--%>
				<%--<form:checkbox path="codeControleSurSommationDI" label=" ${labelCheckbox}"/>--%>
			<%--</div>--%>

			<table>
			<tr>
				<th></th>
				<th><fmt:message key="label.param.entete.VD"/> / <fmt:message key="label.param.entete.report.fin.mois"/></th>
				<th><fmt:message key="label.param.entete.HC"/> / <fmt:message key="label.param.entete.report.fin.mois"/></th>
				<th><fmt:message key="label.param.entete.HS"/> / <fmt:message key="label.param.entete.report.fin.mois"/></th>
			</tr>
			<tr>
				<th><fmt:message key="label.param.pm.delai.imprime"/></th>
				<td>
					<form:input path="delaiImprimeMoisVaud"/>
					<fmt:message key="label.param.pm.delai.mois"/>
					&nbsp;/&nbsp;<form:checkbox path="delaiImprimeRepousseFinDeMoisVaud"/>
					<form:errors path="delaiImprimeMoisVaud" cssClass="error"/>
				</td>
				<td>
					<form:input path="delaiImprimeMoisHorsCanton"/>
					<fmt:message key="label.param.pm.delai.mois"/>
					&nbsp;/&nbsp;<form:checkbox path="delaiImprimeRepousseFinDeMoisHorsCanton"/>
					<form:errors path="delaiImprimeMoisHorsCanton" cssClass="error"/>
				</td>
				<td>
					<form:input path="delaiImprimeMoisHorsSuisse"/>
					<fmt:message key="label.param.pm.delai.mois"/>
					&nbsp;/&nbsp;<form:checkbox path="delaiImprimeRepousseFinDeMoisHorsSuisse"/>
					<form:errors path="delaiImprimeMoisHorsSuisse" cssClass="error"/>
				</td>
			</tr>
			<tr>
				<th><fmt:message key="label.param.pm.delai.tolerance"/></th>
				<td>
					<form:input path="toleranceJoursVaud"/>
					<fmt:message key="label.param.pm.delai.jours"/>
					&nbsp;/&nbsp;<form:checkbox path="toleranceRepousseeFinDeMoisVaud"/>
					<form:errors path="toleranceJoursVaud" cssClass="error"/>
				</td>
				<td>
					<form:input path="toleranceJoursHorsCanton"/>
					<fmt:message key="label.param.pm.delai.jours"/>
					&nbsp;/&nbsp;<form:checkbox path="toleranceRepousseeFinDeMoisHorsCanton"/>
					<form:errors path="toleranceJoursHorsCanton" cssClass="error"/>
				</td>
				<td>
					<form:input path="toleranceJoursHorsSuisse"/>
					<fmt:message key="label.param.pm.delai.jours"/>
					&nbsp;/&nbsp;<form:checkbox path="toleranceRepousseeFinDeMoisHorsSuisse"/>
					<form:errors path="toleranceJoursHorsSuisse" cssClass="error"/>
				</td>
			</tr>
		</table>
		</fieldset>
		<div>
			<span class="bouton">
				<input type="submit" id="maj" value="<fmt:message key="label.bouton.mettre.a.jour" />">
			</span>
			<span class="bouton">
				<input type="button" id="annuler" value="<fmt:message key="label.bouton.annuler" />" onclick="document.location.href='list.do?pf=${command.idPeriodeFiscale}'">
			</span>
		</div>
	</form:form>	
	</tiles:put>
</tiles:insert>
