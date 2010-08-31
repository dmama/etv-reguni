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

			<%-- Affichage des erreurs de validation, si nécessaire --%>
			<c:if test="${entity.validationResults != null && (!empty entity.validationResults.errors || !empty entity.validationResults.warnings)}">
				<table class="validation_error" cellspacing="0" cellpadding="0" border="0">
					<tr><td class="heading">Un ou plusieurs problèmes ont été détectés sur cette entité</td></tr>
					<tr id="val_errors"><td class="details"><ul>
					<c:forEach var="err" items="${entity.validationResults.errors}">
						<li class="err"><fmt:message key="label.validation.erreur"/>: <c:out value="${err}"/></li>
					</c:forEach>
					<c:forEach var="warn" items="${entity.validationResults.warnings}">
						<li class="warn"><fmt:message key="label.validation.warning"/>: <c:out value="${warn}"/></li>
					</c:forEach>
					</ul></td></tr>
				</table>
			</c:if>

			<%-- Affichage des attributs de l'entité --%>
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
