<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="command" type="ch.vd.uniregctb.entreprise.AddEtatEntrepriseView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.creation.etat.entreprise">
  			<fmt:param><unireg:numCTB numero="${command.tiersId}"/></fmt:param>
  		</fmt:message>
  	</tiles:put>
	<tiles:put name="body">

		<form:form id="AddEtatEntrepriseForm" commandName="command" action="add.do">
			<fieldset>
				<legend><span><fmt:message key="label.etat" /></span></legend>

				<form:hidden path="tiersId"/>

				<!-- Debut Etat PM -->
				<table border="0">
					<unireg:nextRowClass reset="0"/>
					<tr class="<unireg:nextRowClass/>" >
						<td width="20%"><fmt:message key="label.type"/>&nbsp;:</td>
						<td>
							<c:choose>
								<c:when test="${empty transitionDisponibles}">
									<span style="padding: 2px; display: block">
										Aucun changement d'état n'est possible
										<c:choose>
											<c:when test="${command.previousDate.isAfter(command.dateObtention)}">
												à la date sélectionnée. Veuillez Selectionner une date à partir du <unireg:date date="${command.previousDate}"/>.
											</c:when>
											<c:when test="${command.previousDate.isBefore(command.dateObtention)}">
												à la suite de l'état courant. Ce dernier doit d'abord être annulé.
											</c:when>
										</c:choose>
									</span>
								</c:when>
								<c:when test="${! empty transitionDisponibles}">
									<form:select path="type" name="type">
										<form:option value="" />
										<form:options items="${transitionDisponibles}"/>
									</form:select>
								</c:when>
							</c:choose>
							<form:errors path="type" cssClass="error" />
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.date.obtention" />&nbsp;:</td>
						<td>
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateObtention" />
								<jsp:param name="id" value="dateObtention" />
								<jsp:param name="onChange" value="reSubmit()"/>
							</jsp:include>
						</td>
					</tr>
				</table>
			</fieldset>
			<fieldset>
				<legend><span><fmt:message key="label.etat.precedant" /></span></legend>

				<!-- Debut Etat PM -->
				<table border="0">
					<unireg:nextRowClass reset="0"/>
					<tr class="<unireg:nextRowClass/>" >
						<td width="20%"><fmt:message key="label.type"/>&nbsp;:</td>
						<td>
							<c:if test="${! empty command.previousType}">
								<fmt:message key="option.etat.entreprise.${command.previousType}"/>
							</c:if>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.date.obtention" />&nbsp;:</td>
						<td>
							<unireg:date date="${command.previousDate}"/>
						</td>
					</tr>
				</table>
			</fieldset>

			<table border="0">
				<tr>
					<td width="25%">&nbsp;</td>
					<td width="25%"><input type="submit" value="<fmt:message key="label.bouton.ajouter" />"></td>
					<td width="25%"><unireg:buttonTo name="Retour" action="/entreprise/etats/edit.do" params="{id:${command.tiersId}}" method="GET"/> </td>
					<td width="25%">&nbsp;</td>
				</tr>
			</table>
		</form:form>
		<script>
			function reSubmit() {
				var dateObtention = $('#dateObtention').get(0).value;
				var type = "";
				if ($('#type').get(0)) {
					type = $('#type').get(0).value;
				}
				window.location='<c:url value="/entreprise/etats/add.do"/>?tiersId=' + ${command.tiersId} + '&date=' + dateObtention + '&type=' + type;
			}
		</script>

	</tiles:put>
</tiles:insert>
