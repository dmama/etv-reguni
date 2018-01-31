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
			input.duree {
				width: 30px;
			}
			input[type=checkbox] {
				vertical-align: bottom;
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

			<div class="checkbox">
				<c:set var="labelCheckbox">
					<fmt:message key="label.param.code.controle.sur.sommation.pm"/>
				</c:set>
				<form:checkbox path="codeControleSurSommationDI" label=" ${labelCheckbox}"/>
			</div>

			<table>
			<tr>
				<th>&nbsp;</th>
				<th colspan="2" style="text-align: center;"><fmt:message key="label.param.entete.VD"/></th>
				<th colspan="2" style="text-align: center;"><fmt:message key="label.param.entete.HC"/></th>
				<th colspan="2" style="text-align: center;"><fmt:message key="label.param.entete.HS"/></th>
				<th colspan="2" style="text-align: center;"><fmt:message key="label.param.entete.utilite.publique"/></th>
			</tr>
			<tr>
				<th rowspan="2"><fmt:message key="label.param.pm.delai.imprime"/></th>

				<td rowspan="2">
					<form:input path="delaiImprimeMoisVaud" cssClass="duree"/>
					<fmt:message key="label.param.pm.delai.mois"/>
					<span class="mandatory">*</span>
					<form:errors path="delaiImprimeMoisVaud" cssClass="error"/>
				</td>
				<td>
					<form:select path="refDelaiVaud">
						<form:options items="${referencesPourDelais}"/>
					</form:select>
				</td>

				<td rowspan="2">
					<form:input path="delaiImprimeMoisHorsCanton" cssClass="duree"/>
					<fmt:message key="label.param.pm.delai.mois"/>
					<span class="mandatory">*</span>
					<form:errors path="delaiImprimeMoisHorsCanton" cssClass="error"/>
				</td>
				<td>
					<form:select path="refDelaiHorsCanton">
						<form:options items="${referencesPourDelais}"/>
					</form:select>
				</td>

				<td rowspan="2">
					<form:input path="delaiImprimeMoisHorsSuisse" cssClass="duree"/>
					<fmt:message key="label.param.pm.delai.mois"/>
					<span class="mandatory">*</span>
					<form:errors path="delaiImprimeMoisHorsSuisse" cssClass="error"/>
				</td>
				<td>
					<form:select path="refDelaiHorsSuisse">
						<form:options items="${referencesPourDelais}"/>
					</form:select>
				</td>

				<td rowspan="2">
					<form:input path="delaiImprimeMoisUtilitePublique" cssClass="duree"/>
					<fmt:message key="label.param.pm.delai.mois"/>
					<span class="mandatory">*</span>
					<form:errors path="delaiImprimeMoisUtilitePublique" cssClass="error"/>
				</td>
				<td>
					<form:select path="refDelaiUtilitePublique">
						<form:options items="${referencesPourDelais}"/>
					</form:select>
				</td>
			</tr>
			<tr>
				<td>
					<form:checkbox path="delaiImprimeRepousseFinDeMoisVaud"/>
					<fmt:message key="label.param.pm.report.fin.mois"/>
				</td>
				<td>
					<form:checkbox path="delaiImprimeRepousseFinDeMoisHorsCanton"/>
					<fmt:message key="label.param.pm.report.fin.mois"/>
				</td>
				<td>
					<form:checkbox path="delaiImprimeRepousseFinDeMoisHorsSuisse"/>
					<fmt:message key="label.param.pm.report.fin.mois"/>
				</td>
				<td>
					<form:checkbox path="delaiImprimeRepousseFinDeMoisUtilitePublique"/>
					<fmt:message key="label.param.pm.report.fin.mois"/>
				</td>
			</tr>
			<tr>
				<th><fmt:message key="label.param.pm.delai.tolerance"/></th>
				<td>
					<form:input path="toleranceJoursVaud" cssClass="duree"/>
					<fmt:message key="label.param.pm.delai.jours"/>
					<span class="mandatory">*</span>
					<form:errors path="toleranceJoursVaud" cssClass="error"/>
				</td>
				<td>
					<form:checkbox path="toleranceRepousseeFinDeMoisVaud"/>
					<fmt:message key="label.param.pm.report.fin.mois"/>
				</td>
				<td>
					<form:input path="toleranceJoursHorsCanton" cssClass="duree"/>
					<fmt:message key="label.param.pm.delai.jours"/>
					<span class="mandatory">*</span>
					<form:errors path="toleranceJoursHorsCanton" cssClass="error"/>
				</td>
				<td>
					<form:checkbox path="toleranceRepousseeFinDeMoisHorsCanton"/>
					<fmt:message key="label.param.pm.report.fin.mois"/>
				</td>
				<td>
					<form:input path="toleranceJoursHorsSuisse" cssClass="duree"/>
					<fmt:message key="label.param.pm.delai.jours"/>
					<span class="mandatory">*</span>
					<form:errors path="toleranceJoursHorsSuisse" cssClass="error"/>
				</td>
				<td>
					<form:checkbox path="toleranceRepousseeFinDeMoisHorsSuisse"/>
					<fmt:message key="label.param.pm.report.fin.mois"/>
				</td>
				<td>
					<form:input path="toleranceJoursUtilitePublique" cssClass="duree"/>
					<fmt:message key="label.param.pm.delai.jours"/>
					<span class="mandatory">*</span>
					<form:errors path="toleranceJoursUtilitePublique" cssClass="error"/>
				</td>
				<td>
					<form:checkbox path="toleranceRepousseeFinDeMoisUtilitePublique"/>
					<fmt:message key="label.param.pm.report.fin.mois"/>
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
