<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="index" value="${param.index}" />

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<%--@elvariable id="rapportEditView" type="ch.vd.unireg.rapport.view.RapportView"--%>
	<tiles:put name="title">
		<fmt:message key="title.edition.rapport">
			<fmt:param>
			<unireg:numCTB numero="${rapportEditView.numeroCourant}"/></fmt:param>
			<fmt:param><unireg:numCTB numero="${rapportEditView.numero}"/></fmt:param>
		</fmt:message>
	</tiles:put>

	<tiles:put name="body">
		<form:form name="formModifRapport" commandName="rapportEditView" id="formModifRapport">
		<fieldset><legend><span><fmt:message key="label.rapport.tiers" /></span></legend>
		<form:hidden path="id"/>
		<form:hidden path="typeRapportEntreTiers"/>
		<form:hidden path="sensRapportEntreTiers"/>
		<form:hidden path="numeroCourant"/>
		<form:hidden path="numero"/>
		<!-- Debut Rapport -->
		<c:if test="${rapportEditView.allowed}">
		<table border="0">
			<unireg:nextRowClass reset="0"/>
			<tr class="<unireg:nextRowClass/>" >
				<td width="25%"><fmt:message key="label.type.rapport"/>&nbsp;:</td>
				<td width="25%">
					<fmt:message key="option.rapport.entre.tiers.${rapportEditView.sensRapportEntreTiers}.${rapportEditView.typeRapportEntreTiers}" />
				</td>
				<td width="25%"><fmt:message key="label.date.debut"/>&nbsp;:</td>
				<td width="25%">
					<unireg:regdate regdate="${rapportEditView.dateDebut}"/>
				</td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				<td width="25%"><fmt:message key="label.numero.tiers"/>&nbsp;:</td>
				<td width="25%">
					<unireg:numCTB numero="${rapportEditView.numero}" />
				</td>
				<td width="25%"><fmt:message key="label.nom.raison"/>&nbsp;:</td>
				<td width="25%">
					<unireg:multiline lines="${rapportEditView.nomCourrier}"/>
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
			<c:if test="${rapportEditView.natureRapportEntreTiers == 'RepresentationConventionnelle'}">
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%" />
					<td width="75%" colspan="3">
						<c:if test="${rapportEditView.extensionExecutionForceeAllowed}">
							<form:checkbox path="extensionExecutionForcee" label="Extension de l'exécution forcée"/>
						</c:if>
						<c:if test="${!rapportEditView.extensionExecutionForceeAllowed}">
							<input id="extensionExecutionForcee1" type="checkbox" <c:if test="${rapportEditView.extensionExecutionForcee}">checked="checked"</c:if> disabled="disabled"/>
							<label for="extensionExecutionForcee1" title="Uniquement autorisée pour les tiers avec un for fiscal principal hors-Suisse" style="color:gray">Extension de l'exécution forcée</label>
							<form:hidden path="extensionExecutionForcee" />
						</c:if>
					</td>
				</tr>
			</c:if>
			<c:if test="${rapportEditView.natureRapportEntreTiers == 'Heritage'}">
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%" ></td>
					<td width="75%" colspan="3">
						<form:checkbox path="principalCommunaute" label="Principal de la communauté d'héritiers" disabled="true"/>
					</td>
				</tr>
			</c:if>
		</table>
		</c:if>
		<c:if test="${!rapportEditView.allowed}">
			<span class="error"><fmt:message key="error.rapport.interdit" /></span>
		</c:if>
	</fieldset>
	<table border="0">
		<tr>
			<td width="25%">&nbsp;</td>
			<c:if test="${rapportEditView.allowed}">
				<td width="25%"><input type="submit" id="maj" value="<fmt:message key="label.bouton.mettre.a.jour" />"></td>
			</c:if>
			<c:if test="${!rapportEditView.allowed}">
				<td width="25%">&nbsp;</td>
			</c:if>
			<td width="25%">
				<input type="button" id="annuler" value="<fmt:message key="label.bouton.annuler"/>" onclick="document.location.href='<c:url value="${rapportEditView.viewRetour}"/>'" />
			</td>
			<td width="25%">&nbsp;</td>
		</tr>
	</table>

	</form:form>
	</tiles:put>
</tiles:insert>

		
