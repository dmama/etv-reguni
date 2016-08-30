<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.edition.qsnc.periode.selection" /></tiles:put>

	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>

		<form:form name="theForm">
			<fieldset class="select-di">
				<legend><span><fmt:message key="label.qsnc.periode.selection" /></span></legend>
				<c:forEach var="periode" items="${periodes}">
					<input type="radio" id="pf-${periode}" name="selection" value="<c:out value="${periode}"/>">
					<label for="pf-${periode}">
						<fmt:message key="label.periode.annee">
							<fmt:param>${periode}</fmt:param>
						</fmt:message>
					</label>
					<br/>
				</c:forEach>
			</fieldset>

			<!-- Debut boutons -->
			<input type="button" onclick="return creerQuestionnaire();" value="Créer"/>
			<unireg:buttonTo name="Annuler" action="/qsnc/list.do" method="get" params="{tiersId:${tiersId}}"/>
			<!-- Fin boutons -->
		</form:form>

		<script type="text/javascript">
			function creerQuestionnaire() {
				var selection = $('input[name=selection]:checked').val();
				if (!selection) {
					alert("Veuillez sélectionner une période");
					return false;
				}
				window.location.href='<c:url value="/qsnc/add.do?tiersId=${tiersId}"/>&pf=' + selection;
				return false;
			}
		</script>

	</tiles:put>
</tiles:insert>