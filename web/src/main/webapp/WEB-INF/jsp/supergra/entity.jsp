<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/templateSuperGra.jsp">
	<tiles:put name="menu" type="String"/>

	<tiles:put name="title" type="String">*** Mode Supergra ***</tiles:put>

	<%--@elvariable id="entity" type="ch.vd.unireg.supergra.view.EntityView"--%>
	<%--@elvariable id="superGraSession" type="ch.vd.unireg.supergra.SuperGraSession"--%>

	<tiles:put name="actions" type="String">
		<c:if test="${!entity.readonly}">
			<ul>
			<c:if test="${!entity.annule}">
				<li><unireg:buttonTo name="Annuler ${entity.key.type.displayArticleName}" action="/supergra/entity/disable.do" params="{id:${entity.key.id},class:'${entity.key.type}'}" method="POST"/></li>
			</c:if>
			<c:if test="${entity.annule}">
				<li><unireg:buttonTo name="Désannuler ${entity.key.type.displayArticleName}" action="/supergra/entity/enable.do" params="{id:${entity.key.id},class:'${entity.key.type}'}" method="POST"/></li>
			</c:if>
			<c:if test="${entity.communauteRF}">
				<li><unireg:buttonTo name="Recalculer les regroupements" action="/supergra/entity/recalcRegroup.do" params="{id:${entity.key.id},class:'${entity.key.type}'}" method="POST"/></li>
			</c:if>
			<c:if test="${entity.personnePhysique}">
				<li><unireg:buttonTo name="Transformer en ménage-commun" action="/supergra/entity/pp2mc.do" params="{id:${entity.key.id},class:'${entity.key.type}'}" method="GET"/></li>
			</c:if>
			<c:if test="${entity.menageCommun}">
				<li><unireg:buttonTo name="Transformer en personne physique" action="/supergra/entity/mc2pp.do" params="{id:${entity.key.id},class:'${entity.key.type}'}" method="GET"/></li>
			</c:if>
			</ul>
		</c:if>
	</tiles:put>

	<tiles:put name="body" type="String">

		<script>
			// ces méthodes sont références dans SuperGraEntityEditor
			function updateEntityLink(a, id) {
				var linkTemplate = a.attr('href-template');
				if (StringUtils.isBlank(id)) {
					a.removeAttr('href');
				}
				else {
					a.attr('href', linkTemplate.replace('ENTITY_ID', id));
				}
			}

			function openTiersPicker(button, input) {
				return Dialog.open_tiers_picker(button, function(id) {
					input.val(id);
					updateEntityLink($(input).siblings('a'), id);
				});
			}
		</script>

		<table border="0"><tr valign="top">
		<td>

			<c:if test="${superGraSession.lastKnownTiersId != null}">
				<c:set var="urlRetourNormal" value="/tiers/visu.do?id=${superGraSession.lastKnownTiersId}" />
			</c:if>
			<c:if test="${superGraSession.lastKnownTiersId == null}">
				<c:set var="urlRetourNormal" value="/tiers/list.do" />
			</c:if>

			<h3>Edition ${entity.key.type.displayPrepositionName} n°${entity.key.id} &nbsp;<a href="<c:url value="${urlRetourNormal}"/>">(retour au mode normal)</a></h3>
			<br/>

			<%-- Affichage des erreurs de validation, si nécessaire --%>
			<c:if test="${entity.validationResults != null && (!empty entity.validationResults.errors || !empty entity.validationResults.warnings)}">
				<table class="validation_error" cellspacing="0" cellpadding="0" border="0">
					<tr><td class="heading iepngfix">Un ou plusieurs problèmes ont été détectés sur cette entité</td></tr>
					<tr><td class="details"><ul>
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
			<form:form commandName="entity" method="post" action="update.do">
				<input type="hidden" name="id" value="${entity.key.id}">
				<input type="hidden" name="class" value="${entity.key.type}">

				<c:if test="${!entity.readonly}">
				<input type="submit" name="save" value="Mémoriser les modifications" style="margin: 1em;"/>
				</c:if>
				<c:if test="${entity.readonly}">
					<div style="padding: 10px"><b>Note :</b> cette entité ne peut pas être modifiée (lecture-seule).</div>
				</c:if>
				<display:table name="${entity.attributes}" id="a" class="display_table">
						<display:column title="Attribute">
							<c:out value="${a.displayName}"/>
						</display:column>
						<display:column title="Type">
							<c:out value="${a.type.simpleName}"/>
						</display:column>
						<display:column title="Valeur">
							<c:if test="${a.collection}">
								<c:if test="${a.readonly}">
									<c:out value="${a.value}"/>
								</c:if>
								<c:if test="${!a.readonly}">
									<a href="<c:url value="/supergra/coll/list.do?id=${entity.key.id}&class=${entity.key.type}&name=${a.name}"/>"><c:out value="${a.value}"/></a>
								</c:if>
							</c:if>
							<c:if test="${!a.collection}">
								<unireg:formInput id="${a.id}" path="attributes[${a_rowNum - 1}].value" type="${a.type}" category="${a.category}" readonly="${a.readonly}"/>
							</c:if>
							<form:errors path="attributes[${a_rowNum - 1}].value" cssClass="error"/>
						</display:column>
				</display:table>
				<c:if test="${!entity.readonly}">
				<input type="submit" name="save" value="Mémoriser les modifications" style="margin: 1em;"/>
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
