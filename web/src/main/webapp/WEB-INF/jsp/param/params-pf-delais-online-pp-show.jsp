<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<%--@elvariable id="paramsDelaisAccordablesOnlinePP" type="java.util.List"--%>
<c:if test="${paramsDelaisAccordablesOnlinePP != null}">
	<fieldset style="margin: 10px" class="information">
		<legend>
			<fmt:message key="label.param.params.delais.online.pp"/>
		</legend>

		<table>
			<tr>
				<th rowspan="2"><fmt:message key="label.date.debut"/></th>
				<th colspan="2"><fmt:message key="label.param.demande.unitaire"/></th>
				<th colspan="2"><fmt:message key="label.param.demande.groupee"/></th>
			</tr>
			<tr>
				<th><fmt:message key="label.param.delai.1"/></th>
				<th><fmt:message key="label.param.delai.2"/></th>
				<th><fmt:message key="label.param.delai.1"/></th>
				<th><fmt:message key="label.param.delai.2"/></th>
			</tr>
			<unireg:nextRowClass reset="0"/>
			<c:forEach var="periode" items="${paramsDelaisAccordablesOnlinePP}">
			<tr class="<unireg:nextRowClass/>">
				<td><unireg:regdate regdate="${periode.dateDebut}" format="dd.MM"/></td>
				<td><unireg:daymonth value="${periode.delai1DemandeUnitaire}"/></td>
				<td><unireg:daymonth value="${periode.delai2DemandeUnitaire}"/></td>
				<td><unireg:daymonth value="${periode.delai1DemandeGroupee}"/></td>
				<td><unireg:daymonth value="${periode.delai2DemandeGroupee}"/></td>
			</tr>
			</c:forEach>
		</table>

	</fieldset>
</c:if>
