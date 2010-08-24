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
			<jsp:include page="/WEB-INF/jsp/supergra/tiersStates.jsp"/>
			<jsp:include page="/WEB-INF/jsp/supergra/actions.jsp"/>
		</td>
		</tr></table>

	</tiles:put>
</tiles:insert>
