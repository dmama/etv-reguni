<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="index" value="${param.index}" />

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<%--@elvariable id="command" type="ch.vd.uniregctb.rapport.view.RapportView"--%>
	<tiles:put name="title">
		<fmt:message key="title.edition.rapport">
			<fmt:param>
			<unireg:numCTB numero="${command.numeroCourant}"/></fmt:param>
			<fmt:param><unireg:numCTB numero="${command.numero}"/></fmt:param>
		</fmt:message>
	</tiles:put>

	<tiles:put name="body">
		<form:form name="formModifRapport" id="formModifRapport">
		<fieldset><legend><span><fmt:message key="label.rapport.tiers" /></span></legend>
		<!-- Debut Rapport -->
		<c:if test="${command.allowed}">
		<table border="0">
			<unireg:nextRowClass reset="0"/>
			<tr class="<unireg:nextRowClass/>" >
				<td width="25%"><fmt:message key="label.type.rapport"/>&nbsp;:</td>
				<td width="25%">
					<fmt:message key="option.rapport.entre.tiers.${command.sensRapportEntreTiers}.${command.typeRapportEntreTiers}" />
				</td>
				<td width="25%"><fmt:message key="label.date.debut"/>&nbsp;:</td>
				<td width="25%">
					<unireg:regdate regdate="${command.dateDebut}"/>
				</td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				<td width="25%"><fmt:message key="label.numero.tiers"/>&nbsp;:</td>
				<td width="25%">
					<unireg:numCTB numero="${command.numero}" />
				</td>
				<td width="25%"><fmt:message key="label.nom.raison"/>&nbsp;:</td>
				<td width="25%">
					<unireg:multiline lines="${command.nomCourrier}"/>
				</td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				<td width="25%"><fmt:message key="label.date.fin"/>&nbsp;:</td>
				<td width="75%" colspan="3">
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="dateFin" />
						<jsp:param name="id" value="dateFin" />
					</jsp:include>
				</td>
			</tr>
			<c:if test="${command.natureRapportEntreTiers == 'RepresentationConventionnelle'}">
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%" />
					<td width="75%" colspan="3">
						<c:if test="${command.extensionExecutionForceeAllowed}">
							<form:checkbox path="extensionExecutionForcee" label="Extension de l'exécution forcée"/>
						</c:if>
						<c:if test="${!command.extensionExecutionForceeAllowed}">
							<input id="extensionExecutionForcee1" type="checkbox" <c:if test="${command.extensionExecutionForcee}">checked="checked"</c:if> disabled="disabled"/>
							<label for="extensionExecutionForcee1" title="Uniquement autorisée pour les tiers avec un for fiscal principal hors-Suisse" style="color:gray">Extension de l'exécution forcée</label>
						</c:if>
					</td>
				</tr>
			</c:if>
		</table>
		</c:if>
		<c:if test="${!command.allowed}">
			<span class="error"><fmt:message key="error.rapport.interdit" /></span>
		</c:if>
	</fieldset>
	<table border="0">
		<tr>
			<td width="25%">&nbsp;</td>
			<c:if test="${command.allowed}">
				<td width="25%"><input type="submit" id="maj" value="<fmt:message key="label.bouton.mettre.a.jour" />"></td>
			</c:if>
			<c:if test="${!command.allowed}">
				<td width="25%">&nbsp;</td>
			</c:if>
			<td width="25%">
				<input type="button" id="annuler" value="<fmt:message key="label.bouton.annuler"/>" onclick="document.location.href='<c:url value="${command.viewRetour}"/>'" />
			</td>
			<td width="25%">&nbsp;</td>
		</tr>
	</table>
	<%--
	<script type="text/javascript">
			Rapport.selectTypeActivite('${command.typeActivite}');
	</script>
	--%>
	</form:form>
	</tiles:put>
</tiles:insert>

		
