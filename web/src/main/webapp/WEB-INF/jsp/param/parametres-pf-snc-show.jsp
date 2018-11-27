<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<fieldset style="margin: 10px" class="information">
	<%--@elvariable id="parametrePeriodeFiscaleSNC" type="ch.vd.unireg.parametrage.ParametrePeriodeFiscaleSNC"--%>
	<legend>
		<fmt:message key="label.param.parametres.questionnaires.snc"/>
	</legend>
	<a href="pf-edit-snc.do?pf=${periodeSelectionnee.id}" class="edit" title="${titleParametres}"><fmt:message key="label.param.edit"/>&nbsp;</a>
	<div class="checkbox">
		<input type="checkbox" disabled="disabled" <%--@elvariable id="codeControleSurRappelQSNC" type="java.lang.Boolean"--%>
		       <c:if test="${codeControleSurRappelQSNC}">checked</c:if>/>
		<fmt:message key="label.param.code.controle.sur.rappel.snc"/>
	</div>
	<table>
		<tr>
			<th class="colonneTitreParametres"><fmt:message key="label.param.rappel.reg"/></th>
			<td>
				<unireg:date date="${parametrePeriodeFiscaleSNC.termeGeneralRappelImprime}"/>
			</td>
		</tr>
		<tr>
			<th><fmt:message key="label.param.rappel.eff"/></th>
			<td>
				<unireg:date date="${parametrePeriodeFiscaleSNC.termeGeneralRappelEffectif}"/>
			</td>
		</tr>
	</table>

</fieldset>