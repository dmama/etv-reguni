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
				<th><fmt:message key="label.param.entete.VD"/></th>
				<th><fmt:message key="label.param.entete.HC"/></th>
				<th><fmt:message key="label.param.entete.HS"/></th>
			</tr>
			<tr>
				<th><fmt:message key="label.param.pm.delai.imprime.sans.mandataire"/></th>
				<td>
					<form:input path="delaiImprimeSansMandataireVaud"/>
					<fmt:message key="label.param.pm.delai.unite.pluriel"/>
					<form:errors path="delaiImprimeSansMandataireVaud" cssClass="error"/>
				</td>
				<td>
					<form:input path="delaiImprimeSansMandataireHorsCanton"/>
					<fmt:message key="label.param.pm.delai.unite.pluriel"/>
					<form:errors path="delaiImprimeSansMandataireHorsCanton" cssClass="error"/>
				</td>
				<td>
					<form:input path="delaiImprimeSansMandataireHorsSuisse"/>
					<fmt:message key="label.param.pm.delai.unite.pluriel"/>
					<form:errors path="delaiImprimeSansMandataireHorsSuisse" cssClass="error"/>
				</td>
			</tr>
			<tr>
				<th><fmt:message key="label.param.pm.delai.imprime.avec.mandataire"/></th>
				<td>
					<form:input path="delaiImprimeAvecMandataireVaud"/>
					<fmt:message key="label.param.pm.delai.unite.pluriel"/>
					<form:errors path="delaiImprimeAvecMandataireVaud" cssClass="error"/>
				</td>
				<td>
					<form:input path="delaiImprimeAvecMandataireHorsCanton"/>
					<fmt:message key="label.param.pm.delai.unite.pluriel"/>
					<form:errors path="delaiImprimeAvecMandataireHorsCanton" cssClass="error"/>
				</td>
				<td>
					<form:input path="delaiImprimeAvecMandataireHorsSuisse"/>
					<fmt:message key="label.param.pm.delai.unite.pluriel"/>
					<form:errors path="delaiImprimeAvecMandataireHorsSuisse" cssClass="error"/>
				</td>
			</tr>
			<tr>
				<th><fmt:message key="label.param.pm.delai.effectif.sans.mandataire"/></th>
				<td>
					<form:input path="delaiEffectifSansMandataireVaud"/>
					<fmt:message key="label.param.pm.delai.unite.pluriel"/>
					<form:errors path="delaiEffectifSansMandataireVaud" cssClass="error"/>
				</td>
				<td>
					<form:input path="delaiEffectifSansMandataireHorsCanton"/>
					<fmt:message key="label.param.pm.delai.unite.pluriel"/>
					<form:errors path="delaiEffectifSansMandataireHorsCanton" cssClass="error"/>
				</td>
				<td>
					<form:input path="delaiEffectifSansMandataireHorsSuisse"/>
					<fmt:message key="label.param.pm.delai.unite.pluriel"/>
					<form:errors path="delaiEffectifSansMandataireHorsSuisse" cssClass="error"/>
				</td>
			</tr>
			<tr>
				<th><fmt:message key="label.param.pm.delai.effectif.avec.mandataire"/></th>
				<td>
					<form:input path="delaiEffectifAvecMandataireVaud"/>
					<fmt:message key="label.param.pm.delai.unite.pluriel"/>
					<form:errors path="delaiEffectifAvecMandataireVaud" cssClass="error"/>
				</td>
				<td>
					<form:input path="delaiEffectifAvecMandataireHorsCanton"/>
					<fmt:message key="label.param.pm.delai.unite.pluriel"/>
					<form:errors path="delaiEffectifAvecMandataireHorsCanton" cssClass="error"/>
				</td>
				<td>
					<form:input path="delaiEffectifAvecMandataireHorsSuisse"/>
					<fmt:message key="label.param.pm.delai.unite.pluriel"/>
					<form:errors path="delaiEffectifAvecMandataireHorsSuisse" cssClass="error"/>
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
