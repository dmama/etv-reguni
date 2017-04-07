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
		<fmt:message key="title.edit.param.periode.fiscale.snc">
			<fmt:param>${command.anneePeriodeFiscale}</fmt:param>
		</fmt:message>
	</tiles:put>

	<tiles:put name="body">
	<form:form name="form" id="formTermes" commandName="command">
		<form:hidden path="idPeriodeFiscale"/>
		<form:hidden path="anneePeriodeFiscale"/>
		<fieldset>
			<legend><fmt:message key="label.param.parametres-pf-edit" /></legend>

			<table>
			<tr>
				<th width="30%"><fmt:message key="label.param.rappel.reg"/></th>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="rappelReglementaire" />
						<jsp:param name="id" value="rappelReglementaire" />
					</jsp:include>
					<span style="color: red;">*</span>
				</td>
			</tr>
			<tr>
				<th><fmt:message key="label.param.rappel.eff"/></th>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="rappelEffectif" />
						<jsp:param name="id" value="rappelEffectif" />
					</jsp:include>
					<span style="color: red;">*</span>
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
