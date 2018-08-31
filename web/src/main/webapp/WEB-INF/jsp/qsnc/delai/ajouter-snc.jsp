<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<%--@elvariable id="ajouterDelai" type="ch.vd.unireg.documentfiscal.QuestionnaireSNCAjouterDelaiView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="head">
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/documentfiscal/ajout-delai.js"/>"></script>
	</tiles:put>

	<tiles:put name="title">
		<c:set var="titleKey" value="ajout.delai.qsnc.title.enregistrement.demande.delai"/>
		<fmt:message key="${titleKey}">
			<fmt:param>${ajouterDelai.periode}</fmt:param>
			<fmt:param><unireg:date date="${ajouterDelai.declarationRange.dateDebut}"/></fmt:param>
			<fmt:param><unireg:date date="${ajouterDelai.declarationRange.dateFin}"/></fmt:param>
			<fmt:param><unireg:numCTB numero="${ajouterDelai.tiersId}"/></fmt:param>
		</fmt:message>
	</tiles:put>
	<tiles:put name="body">
		<%--@elvariable id="ajouterDelai" type="ch/vd/unireg/qsnc/QuestionnaireSNCController.java#ajouterDelai"--%>
		<form:form method="post" name="theForm" id="formAddDelai" action="ajouter-snc.do" modelAttribute="ajouterDelai">

			<form:errors cssClass="error"/>
			<form:hidden path="periode"/>
			<form:hidden path="ancienDelaiAccorde"/>
			<form:hidden path="typeImpression" id="typeImpression"/>
			<form:hidden path="idDocumentFiscal"/>
			<form:hidden path="tiersId"/>

			<fieldset>
				<legend><span><fmt:message key="label.etats"/></span></legend>
				<table border="0">
					<unireg:nextRowClass reset="0"/>
					<tr class="<unireg:nextRowClass/>">
						<td style="width: 25%;"><fmt:message key="label.date.demande"/>&nbsp;:</td>
						<td style="width: 25%;">
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateDemande"/>
								<jsp:param name="id" value="dateDemande"/>
								<jsp:param name="mandatory" value="true"/>
							</jsp:include>
						</td>
						<td style="width: 25%;"><fmt:message key="label.date.ancien.delai"/>&nbsp;:</td>
						<td style="width: 25%;"><unireg:date date="${ajouterDelai.ancienDelaiAccorde}"/></td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td><fmt:message key="label.decision"/>&nbsp;:</td>
						<td>
								<%--@elvariable id="decisionsDelai" type="ch/vd/unireg/qsnc/QuestionnaireSNCController.java#ajouterDelai"--%>
							<form:select id="decision" path="decision" onchange="DelaiSNC.toggleDecision();  return true;" items="${decisionsDelai}">
								<%--<form:options items="${decisionsDelai}"/>--%>
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
						<input type="button" id="envoi-auto" value="Envoi courrier automatique" onclick="return DelaiSNC.ajouterDelai(this, 'BATCH');" style="display: none;">
					</td>
					<td width="25%">
						<input type="button" id="envoi-manuel" value="Envoi courrier manuel" onclick="return DelaiSNC.ajouterDelai(this, 'LOCAL');" style="display: none;">
						<input type="button" id="ajouter" value="Ajouter" onclick="return DelaiSNC.ajouterDelai(this, null);">
						<unireg:buttonTo id="retour" name="Retour" visible="false" action="/qsnc/editer.do" method="get" params="{id:${ajouterDelai.idDocumentFiscal}}"/>
					</td>
					<td width="25%">
						<input type="button" value="<fmt:message key="label.bouton.annuler"/>" onclick="Navigation.backTo(['/autresdocs/editer.do', '/qsnc/editer.do'], '/autresdocs/editer.do', 'id=${ajouterDelai.idDocumentFiscal}')"/>
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
