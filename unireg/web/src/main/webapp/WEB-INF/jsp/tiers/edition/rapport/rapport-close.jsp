<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="index" value="${param.index}" />

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">
		<fmt:message key="title.fermeture.rapport">
			<fmt:param><unireg:numCTB numero="${data.numeroCourant}"/></fmt:param>
			<fmt:param><unireg:numCTB numero="${data.numero}"/></fmt:param>
		</fmt:message>
	</tiles:put>

	<tiles:put name="body">
		<form:form name="formModifRapport" id="formModifRapport" commandName="command">
		<fieldset><legend><span><fmt:message key="label.rapport.tiers" /></span></legend>
		<form:hidden path="idRapportActiviteEconomique"/>
		<!-- Debut Rapport -->
		<table border="0">
			<unireg:nextRowClass reset="0"/>
			<tr class="<unireg:nextRowClass/>" >
				<td width="25%"><fmt:message key="label.type.rapport"/>&nbsp;:</td>
				<td width="25%">
					<fmt:message key="option.rapport.entre.tiers.${data.sensRapportEntreTiers}.${data.typeRapportEntreTiers}" />
				</td>
				<td colspan="2">&nbsp;</td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				<td width="25%"><fmt:message key="label.numero.tiers"/>&nbsp;:</td>
				<td width="25%">
					<unireg:numCTB numero="${data.numero}" />
				</td>
				<td width="25%"><fmt:message key="label.nom.raison"/>&nbsp;:</td>
				<td width="25%">
					<unireg:multiline lines="${data.nomCourrier}"/>
				</td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				<td width="25%"><fmt:message key="label.date.debut"/>&nbsp;:</td>
				<td width="25%">
					<input type="hidden" name="dateDebut" value="<unireg:regdate regdate="${data.regDateDebut}"/>"/>
					<unireg:regdate regdate="${data.regDateDebut}"/>
				</td>
				<td width="25%"><fmt:message key="label.date.fin"/>&nbsp;:</td>
				<td width="25%" colspan="3">
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="dateFin" />
						<jsp:param name="id" value="dateFin" />
					</jsp:include>
					<span style="color: red;">*</span>
				</td>
			</tr>
		</table>
	</fieldset>
	<table border="0">
		<tr>
			<td width="25%">&nbsp;</td>
			<td width="25%"><input type="submit" id="maj" value="<fmt:message key="label.bouton.mettre.a.jour" />"></td>
			<td width="25%">
				<input type="button" id="annuler" value="<fmt:message key="label.bouton.annuler"/>" onclick="document.location.href='<c:url value="${data.viewRetour}"/>'" />
			</td>
			<td width="25%">&nbsp;</td>
		</tr>
	</table>
	</form:form>
	</tiles:put>
</tiles:insert>

		
