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

		<unireg:bandeauTiers numero="${command.tiersId}" showLinks="false" showComplements="false" showEvenementsCivils="false" showValidation="false" showAvatar="false"/>

		<form:form id="AddEtatEntrepriseForm" commandName="command" action="add.do">

			<c:if test="${! empty command.previousType}">
				<fieldset>
					<legend><span><fmt:message key="label.etat.actuel" /></span></legend>

					<!-- Debut Etat PM -->
					<table border="0">
						<unireg:nextRowClass reset="0"/>
						<tr class="<unireg:nextRowClass/>" >
							<td width="20%"><fmt:message key="label.type"/>&nbsp;:</td>
							<td>
								<fmt:message key="option.etat.entreprise.${command.previousType}"/>
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
			</c:if>

			<fieldset>
				<legend><span><fmt:message key="label.etat.nouveau" /></span></legend>

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
										<c:choose>
											<c:when test="${previousDateAfterDateObtention}">
												<fmt:message key="label.etats.aucun.dispo.date">
													<fmt:param><unireg:date date="${command.previousDate}"/></fmt:param>
												</fmt:message>
											</c:when>
											<c:otherwise>
												<fmt:message key="label.etats.aucun.dispo.courant"/>
											</c:otherwise>
										</c:choose>
									</span>
								</c:when>
								<c:otherwise>
									<form:select path="type" name="type">
										<form:option value="" />
										<form:options items="${transitionDisponibles}"/>
									</form:select>
									<span class="mandatory">*</span>
									<form:errors path="type" cssClass="error" />
								</c:otherwise>
							</c:choose>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.date.obtention" />&nbsp;:</td>
						<td>
							<c:if test="${! (!previousDateAfterDateObtention && empty transitionDisponibles)}">
								<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
									<jsp:param name="path" value="dateObtention" />
									<jsp:param name="id" value="dateObtention" />
									<jsp:param name="onChange" value="reSubmit()"/>
								</jsp:include>
							</c:if>
						</td>
					</tr>
				</table>
			</fieldset>

			<table border="0">
				<tr>
					<td width="25%">&nbsp;</td>

					<c:if test="${empty transitionDisponibles || previousDateAfterDateObtention}">
						<c:set var="disabled" value="disabled='true'"/>
					</c:if>
					<td width="25%"><input type="submit" value="<fmt:message key="label.bouton.ajouter" />" ${disabled}></td>
					<td width="25%"><unireg:buttonTo name="Retour" action="/entreprise/etats/edit.do" params="{id:${command.tiersId}}" method="GET"/> </td>
					<td width="25%">&nbsp;</td>
				</tr>
			</table>
		</form:form>
		<script>
			function reSubmit() {
				var dateObtention = $('#dateObtention').get(0).value;
				if (!DateUtils.validate(dateObtention)) {
					$('#dateObtention').addClass('error');
					return false;
				}
				var type = "";
				if ($('#type').get(0)) {
					type = $('#type').get(0).value;
				}
				window.location='<c:url value="/entreprise/etats/add.do"/>?tiersId=' + ${command.tiersId} + '&date=' + dateObtention + '&type=' + type;
			}
		</script>

	</tiles:put>
</tiles:insert>
