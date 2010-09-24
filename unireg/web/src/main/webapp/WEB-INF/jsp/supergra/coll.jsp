<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/templateSuperGra.jsp">
	<tiles:put name="menu" type="String"></tiles:put>

	<tiles:put name="title" type="String">*** Mode Supergra ***</tiles:put>

	<tiles:put name="actions" type="String"></tiles:put>

	<tiles:put name="body" type="String">

		<table border="0"><tr valign="top">
		<td>
			<h3>Edition de la collection ${coll.name} du <unireg:superGraLink key="${coll.key}"/></h3>
			<br/>

			<div id="optionsDiv">
				<form:form id="optionsForm" method="post">
					<input id="showDetails" name="showDetails" type="checkbox" onclick="F$('optionsForm').submit()" <c:if test="${superGraSession.options.showDetails}">checked="checked"</c:if>/>
					<label for="showDetails">Afficher les détails</label>
					<input name="_showDetails" type="hidden" value="${!superGraSession.options.showDetails}"/>
				</form:form>
			</div>

			<form:form method="post">

				<table class="display_table" id="a">
					<thead><tr>
					<c:forEach items="${coll.attributeNames}" var="name">
						<th><c:out value="${name}"></c:out></th>
					</c:forEach>
					</tr></thead>

					<c:forEach items="${coll.entities}" var="entity">
						<tr<c:if test="${entity.annule}"> class="strike"</c:if>>
						<c:forEach items="${coll.attributeNames}" var="name">
							<c:set var="a" value="${entity.attributesMap[name]}"/>
							<td>
							<c:if test="${a != null}">
								<c:if test="${a.name == coll.primaryKeyAtt}">
									<a href="<c:url value="/supergra/entity.do?id=${a.value}&class=${coll.primaryKeyType}"/>"><c:out value="${a.value}"/></a>
								</c:if>
								<c:if test="${a.name != coll.primaryKeyAtt}">
									<unireg:formField id="attributes_${a_rowNum - 1}_${name}" clazz="${a.type}" value="${a.value}" readonly="true"/>
								</c:if>
							</c:if>
							</td>
						</c:forEach>
						<c:out value="${name}"></c:out>
						</tr>
					</c:forEach>
				</table>

				<select id="newClass" name="newClass">
					<option>-- Veuillez sélectionner --</option>
					<c:forEach items="${coll.concreteEntityClasses}" var="c">
					<option value="${c.name}">${c.simpleName}</option>
					</c:forEach>
				</select>
				<input type="submit" name="add" value="Ajouter" />

			</form:form>

		</td>
		<td id="actions_column">
			<jsp:include page="/WEB-INF/jsp/supergra/tiersStates.jsp"/>
			<jsp:include page="/WEB-INF/jsp/supergra/actions.jsp"/>
		</td>
		</tr></table>

	</tiles:put>
</tiles:insert>
