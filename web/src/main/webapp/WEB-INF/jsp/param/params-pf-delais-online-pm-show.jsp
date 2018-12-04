<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<%--@elvariable id="paramsDelaisAccordablesOnlinePM" type="java.util.List"--%>
<c:if test="${paramsDelaisAccordablesOnlinePM != null}">
	<fieldset style="margin: 10px" class="information">
		<legend>
			<fmt:message key="label.param.params.delais.online.pm"/>
		</legend>
		<unireg:linkTo name="label.param.edit" link_class="edit" action="/param/periode/online/pm/edit.do" params="{'pf':${periodeSelectionnee.id}}"/>

		<table>
			<tr>
				<th rowspan="2"><fmt:message key="label.param.periode.demande.delai"/></th>
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
			<c:forEach var="periode" items="${paramsDelaisAccordablesOnlinePM}">
			<tr class="<unireg:nextRowClass/>">
				<td><unireg:delai value="${periode.delaiDebut}"/></td>
				<td><unireg:delai value="${periode.delai1DemandeUnitaire}"/></td>
				<td><unireg:delai value="${periode.delai2DemandeUnitaire}"/></td>
				<td><unireg:delai value="${periode.delai1DemandeGroupee}"/></td>
				<td><unireg:delai value="${periode.delai2DemandeGroupee}"/></td>
			</tr>
			</c:forEach>
		</table>

	</fieldset>
</c:if>
