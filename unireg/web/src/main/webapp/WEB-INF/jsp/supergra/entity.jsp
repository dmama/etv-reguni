<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/templateSuperGra.jsp">
	<tiles:put name="menu" type="String"></tiles:put> 

	<tiles:put name="title" type="String">*** Mode Supergra ***</tiles:put>
	<tiles:put name="body" type="String">

		<table border="0"><tr valign="top">
		<td>

			<h3>Edition du ${entity.key.type.displayName} n°${entity.key.id}</h3>
			<br/>

			<form:form method="post">
				<display:table name="${entity.attributes}" id="a" class="display_table">
						<display:column title="Attribute">
							<c:out value="${a.name}"/>
						</display:column>
						<display:column title="Type">
							<c:out value="${a.type.simpleName}"/>
						</display:column>
						<display:column title="Valeur">
							<c:if test="${a.collection}">
								<a href="coll.do?id=${entity.key.id}&class=${entity.key.type}&name=${a.name}"><c:out value="${a.value}"/></a>
							</c:if>
							<c:if test="${a.parentForeignKey}">
								<a href="<c:url value="/supergra/entity.do?id=${a.value.id}&class=${a.parentEntityType}"/>"><c:out value="${a.value}"/></a>
							</c:if>
							<c:if test="${!a.collection && !a.parentForeignKey}">
								<unireg:formField id="attributes_${a_rowNum - 1}" path="attributes[${a_rowNum - 1}].value" clazz="${a.type}" value="${a.value}" readonly="${a.readonly}"/>
							</c:if>
						</display:column>
				</display:table>
				<input type="submit" name="save" value="Mettre-à-jour" />
			</form:form>

		</td>
		<td id="actions_column">
		
			<div id="actions_list">
				<form:form id="deltaForm" method="post">

					<input id="delDelta" type="hidden" name="delDelta" />

					<table class="sync_actions" classname="sync_actions" cellspacing="0" border="0">
					  <tbody>
						<tr class="header" classname="header">
						  <td colspan="2">Les actions suivantes seront exécutées si vous confirmez les changements </td>
						</tr>

						<c:forEach items="${superGraSession.deltas}" var="d" varStatus="i">
							<tr class="action" classname="action">
							  <td class="rowheader" classname="rowheader">»</td>
							  <td class="action" classname="action"><c:out value="${d}"/> (<a href="#" onclick="E$('delDelta').value = '${i.index}'; F$('deltaForm').submit(); return false;">supprimer</a>)</td>
							</tr>
						</c:forEach>
					  </tbody>
					</table>

				</form:form>
			</div>
			
		</td>
		</tr></table>

	</tiles:put>
</tiles:insert>
