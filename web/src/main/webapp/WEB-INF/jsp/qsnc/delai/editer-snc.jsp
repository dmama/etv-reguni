<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>


<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="head">
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/documentfiscal/ajout-delai.js"/>"></script>
	</tiles:put>
	<tiles:put name="title">
		<fmt:message key="ajout.delai.qsnc.title.modification.demande.delai">
			<fmt:param>${modifDelai.declarationPeriode}</fmt:param>
			<fmt:param><unireg:date date="${modifDelai.declarationRange.dateDebut}"/></fmt:param>
			<fmt:param><unireg:date date="${modifDelai.declarationRange.dateFin}"/></fmt:param>
			<fmt:param><unireg:numCTB numero="${modifDelai.tiersId}"/></fmt:param>
		</fmt:message>
	</tiles:put>
	<tiles:put name="body">
		<%--@elvariable id="modifDelai" type="ch/vd/unireg/qsnc/QuestionnaireSNCController.java#editerDemandeDelaiPM"--%>
		<form:form method="post" name="theForm" id="modifDelai" action="editer-snc.do" modelAttribute="modifDelai">

			<form:errors cssClass="error"/>

			<form:hidden path="idDeclaration"/>
			<form:hidden path="idDelai"/>
			<form:hidden path="ancienDelaiAccorde"/>
			<form:hidden path="typeImpression" id="typeImpression"/>
			<form:hidden path="dateDemande"/>

			<fieldset>
				<legend><span><fmt:message key="label.etats"/></span></legend>
				<table border="0">
					<unireg:nextRowClass reset="0"/>
					<tr class="<unireg:nextRowClass/>">
						<td style="width: 25%;"><fmt:message key="label.date.demande"/>&nbsp;:</td>
						<td style="width: 25%;"><unireg:date date="${modifDelai.dateDemande}"/></td>
						<td style="width: 25%;"><fmt:message key="label.date.ancien.delai"/>&nbsp;:</td>
						<td style="width: 25%;"><unireg:date date="${modifDelai.ancienDelaiAccorde}"/></td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td><fmt:message key="label.decision"/>&nbsp;:</td>
						<td>
							<form:select id="decision" path="decision" onchange="DelaiSNC.toggleDecision();">
								<form:options items="${decisionsDelai}"/>
							</form:select>
						</td>
						<td>
							<div class="siDelaiAccorde">
								<fmt:message key="label.date.delai.accorde"/>&nbsp;:
							</div>
						</td>
						<td>
							<div class="siDelaiAccorde">
								<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
									<jsp:param name="path" value="delaiAccordeAu"/>
									<jsp:param name="id" value="delaiAccordeAu"/>
									<jsp:param name="mandatory" value="true" />
								</jsp:include>
							</div>
						</td>
					</tr>
				</table>

			</fieldset>

			<table border="0">
				<tr>
					<td width="25%" align="right">
						<input type="button" id="envoi-auto" value="Envoi courrier automatique" onclick="return DelaiSNC.modifierDelai(this, 'BATCH');" style="display: none;">
					</td>
					<td width="25%">
						<input type="button" id="envoi-manuel" value="Envoi courrier manuel" onclick="return DelaiSNC.modifierDelai(this, 'LOCAL');" style="display: none;">
						<unireg:buttonTo id="retour" name="Retour" visible="false" action="/qsnc/editer.do" method="get" params="{id:${modifDelai.idDeclaration}}"/>
					</td>
					<td width="25%">
						<unireg:buttonTo id="annuler" name="Annuler" action="/qsnc/editer.do" method="get" params="{id:${modifDelai.idDeclaration}}"/>
					<td width="25%">&nbsp;</td>
				</tr>
			</table>
		</form:form>

		<script type="text/javascript">
			// première exécution au chargement de la page...
			DelaiSNC.toggleDecision();

		</script>
	</tiles:put>
</tiles:insert>
