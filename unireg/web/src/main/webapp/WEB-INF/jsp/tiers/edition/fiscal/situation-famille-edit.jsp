<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<tiles:insert template="/WEB-INF/jsp/templates/templateIFrame.jsp">
	<tiles:put name="head"></tiles:put>

	<tiles:put name="title"></tiles:put>
	<tiles:put name="body">
		<form:form name="formSituationFamille" id="formSituationFamille">
		<fieldset><legend><span><fmt:message key="label.situation.famille.fiscale" /></span></legend>		
		<table border="0">
			<tr class="<unireg:nextRowClass/>" >
				<td width="25%"><fmt:message key="label.date.debut" />&nbsp;:</td>
				<td width="75%" colspan="3">
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="dateDebut" />
						<jsp:param name="id" value="dateDebut" />
					</jsp:include>
				</td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				<td width="25%"><fmt:message key="label.etat.civil" />&nbsp;:</td>
				<td width="25%">
					<c:if test="${command.natureSituationFamille == 'SituationFamilleMenageCommun'}">
						<form:select path="etatCivil">
							<form:option value="MARIE" ><fmt:message key="option.etat.civil.MARIE" /></form:option>
							<form:option value="LIE_PARTENARIAT_ENREGISTRE"><fmt:message key="option.etat.civil.LIE_PARTENARIAT_ENREGISTRE" /></form:option>
						</form:select>
					</c:if>
					<c:if test="${command.natureSituationFamille != 'SituationFamilleMenageCommun'}">
						<form:select path="etatCivil">
							<form:option value="" ></form:option>
							<form:options items="${etatCivil}" />
						</form:select>
					</c:if>
				</td>
				<td width="25%"><fmt:message key="label.nombre.enfants" />&nbsp;:</td>
				<td width="25%">
					<form:input path="nombreEnfants" size="2" />
					<form:errors path="nombreEnfants" cssClass="error"/>
				</td>
			</tr>
			<c:if test="${command.natureSituationFamille == 'SituationFamilleMenageCommun'}">
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.bareme.is.applicable" />&nbsp;:</td>
					<td width="25%">
						<form:select path="tarifImpotSource" items="${tarifsImpotSource}" />
					</td>
					<td width="25%">&nbsp;</td>
					<td width="25%">&nbsp;</td>
				</tr>
				<unireg:nextRowClass reset="0"/>
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.revenu.plus.eleve" />&nbsp;:</td>
					<td width="25%">
						<form:radiobutton path="numeroTiersRevenuPlusEleve" value="${command.numeroTiers1}"/>${command.nomCourrier1Tiers1}
					</td>
					
					<td width="25%">
						<c:if test="${command.numeroTiers2 != null}">
							<form:radiobutton path="numeroTiersRevenuPlusEleve" value="${command.numeroTiers2}" />${command.nomCourrier1Tiers2}
						</c:if>
					</td>
					
					<td width="25%">&nbsp;</td>
				</tr>
			</c:if>
		</table>
		</fieldset>
		<table border="0">
		<tr>
			<td width="25%">&nbsp;</td>
			<c:choose>
				<c:when test="${command.allowed}">
					<td width="25%"><input type="submit" id="ajouter" value="<fmt:message key="label.bouton.ajouter" />"></td>
				</c:when>
				<c:otherwise>
					<td width="25%"><fmt:message key="error.situationfamille.creation.interdit" /></td>
				</c:otherwise>
			</c:choose>
			<td width="25%"><input type="button" id="annuler" value="<fmt:message key="label.bouton.annuler" />" onclick="self.parent.tb_remove()"></td>
			<td width="25%">&nbsp;</td>
		</tr>
	</table>
	</form:form>
	</tiles:put>
</tiles:insert>