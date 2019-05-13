<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/templateSuperGra.jsp">
	<tiles:put name="menu" type="String"/>

	<tiles:put name="title" type="String">*** Mode Supergra ***</tiles:put>

	<%--@elvariable id="superGraSession" type="ch.vd.unireg.supergra.SuperGraSession"--%>
	<%--@elvariable id="coll" type="ch.vd.unireg.supergra.view.CollectionView"--%>

	<tiles:put name="actions" type="String"/>

	<tiles:put name="body" type="String">

		<table border="0"><tr valign="top">
		<td>
			<h3>Edition de la collection ${coll.name} du <unireg:superGraLink key="${coll.key}"/></h3>
			<br/>

			<div id="optionsDiv">
				<form:form id="optionsForm" method="post">
					<input id="showDetails" type="checkbox" onclick="Form.dynamicSubmit('POST', App.curl('/supergra/option/details.do'), {show:${!superGraSession.options.showDetails}});" <c:if test="${superGraSession.options.showDetails}">checked="checked"</c:if>/>
					<label for="showDetails">Afficher les détails</label>
				</form:form>
			</div>

			<form:form modelAttribute="coll" method="post" action="add.do">

				<input type="hidden" name="id" value="${coll.key.id}"/>
				<input type="hidden" name="class" value="${coll.key.type}"/>
				<input type="hidden" name="name" value="${coll.name}"/>

				<table class="display_table" id="a">
					<thead><tr>
					<c:forEach items="${coll.attributeNames}" var="name">
						<th><c:out value="${name}"/>
					</c:forEach>
					</tr></thead>

					<c:forEach items="${coll.entities}" var="entity">
						<tr<c:if test="${entity.annule}"> class="strike"</c:if>>
						<c:forEach items="${coll.attributeNames}" var="name">
							<c:set var="a" value="${entity.attributesMap[name]}"/>
							<td>
							<c:if test="${a != null}">
								<c:choose>
									<c:when test="${a.name == coll.primaryKeyAtt}">
										<div style="float: right;">
											<a href="<c:url value="/supergra/entity/show.do?id=${a.value}&class=${coll.primaryKeyType}"/>"><c:out value="${a.value}"/></a>
										</div>
									</c:when>
									<c:otherwise>
										<unireg:out id="attributes_${a_rowNum - 1}_${name}" value="${a.value}" clazz="${a.type}"/>
									</c:otherwise>
								</c:choose>
							</c:if>
							</td>
						</c:forEach>
						<c:out value="${name}"/>
						</tr>
					</c:forEach>
				</table>

				<c:if test="${!coll.readonly}">
					<select id="newClass" name="newClass">
						<option>-- Veuillez sélectionner --</option>
						<c:forEach items="${coll.concreteEntityClasses}" var="c">
						<option value="${c.name}">${c.simpleName}</option>
						</c:forEach>
					</select>
					<input type="submit" name="add" value="Ajouter" />
				</c:if>
				<c:if test="${coll.readonly}">
					<div style="padding: 10px"><b>Note :</b> cette collection ne peut pas être modifiée (lecture-seule).</div>
				</c:if>

			</form:form>

		</td>
		<td id="actions_column">
			<jsp:include page="/WEB-INF/jsp/supergra/entityStates.jsp"/>
			<jsp:include page="/WEB-INF/jsp/supergra/actions.jsp"/>
		</td>
		</tr></table>

	</tiles:put>
</tiles:insert>
