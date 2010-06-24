<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.edition.fors">
  			<fmt:param><unireg:numCTB numero="${command.numeroCtb}"/></fmt:param>
  		</fmt:message>
  	</tiles:put>

	<tiles:put name="body">

		<table border="0"><tr valign="top">
		<td>
			<%-- Formulaire principal --%>
			<form:form name="formFor" id="formFor">
			<fieldset><legend><span><fmt:message key="label.for.fiscal" /></span></legend>

			<table border="0">

				<tr class="<unireg:nextRowClass/>" >
					<%-- Genre d'impôt --%>
					<td><fmt:message key="label.genre.impot"/>&nbsp;:</td>
					<td><fmt:message key="option.genre.impot.${command.genreImpot}" /></td>

					<%-- Motif de rattachement --%>
					<c:if test="${command.natureForFiscal != 'ForFiscalAutreImpot'}">
						<td id="div_rattachement_label"><fmt:message key="label.rattachement"/>&nbsp;:</td>
						<td id="div_rattachement"><fmt:message key="option.rattachement.${command.motifRattachement}" /></td>
					</c:if>
					<c:if test="${command.natureForFiscal == 'ForFiscalAutreImpot'}">
						<td>&nbsp;</td>
						<td>&nbsp;</td>
					</c:if>
				</tr>

				<c:if test="${command.natureForFiscal != 'ForFiscalAutreImpot'}">

					<tr id="date_for_periodique" class="<unireg:nextRowClass/>" >

						<c:if test="${command.natureForFiscal == 'ForFiscalSecondaire'}">
							<%-- Date d'ouverture (éditable) --%>
							<td><fmt:message key="label.date.ouverture" />&nbsp;:</td>
							<td>
								<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
									<jsp:param name="path" value="dateOuverture" />
									<jsp:param name="id" value="dateOuverture" />
									<jsp:param name="onChange" value="dateOuverture_onChange" />
								</jsp:include>
							</td>

							<%-- Motif d'ouverture (éditable) --%>
							<td><fmt:message key="label.motif.ouverture" />&nbsp;:</td>
							<td>
								<form:select path="motifOuverture" onchange="updateSyncActions();" />
								<form:errors path="motifOuverture" cssClass="error" />
							</td>
						</c:if>

						<c:if test="${command.natureForFiscal != 'ForFiscalSecondaire'}">
							<%-- Date d'ouverture (non éditable) --%>
							<td><fmt:message key="label.date.ouverture" />&nbsp;:</td>
							<td><fmt:formatDate value="${command.dateOuverture}" pattern="dd.MM.yyyy"/></td>

							<%-- Motif d'ouverture (non éditable) --%>
							<td><fmt:message key="label.motif.ouverture" />&nbsp;:</td>
							<td><fmt:message key="option.motif.ouverture.${command.motifOuverture}" /></td>
						</c:if>
					</tr>

					<jsp:include page="for-lieu.jsp">
						<jsp:param name="limited" value="true" />
						<jsp:param name="onChange" value="updateSyncActions" />
					</jsp:include>

					<tr id="motif_for_periodique" class="<unireg:nextRowClass/>" >

						<%-- [UNIREG-2322] la date de fin est éditable sur les fors principaux ouverts et sur tous les fors secondaires --%>
						<c:if test="${command.dateFermeture == null || command.natureForFiscal == 'ForFiscalSecondaire'}">
							<%-- Date de fermeture (éditable) --%>
							<td><fmt:message key="label.date.fermeture" />&nbsp;:</td>
							<td>
								<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
									<jsp:param name="path" value="dateFermeture" />
									<jsp:param name="id" value="dateFermeture" />
									<jsp:param name="onChange" value="dateFermeture_onChange" />
								</jsp:include>
							</td>

							<%-- Motif de fermeture --%>
							<td><fmt:message key="label.motif.fermeture" />&nbsp;:</td>
							<td>
								<form:select path="motifFermeture" onchange="updateSyncActions();" />
								<form:errors path="motifFermeture" cssClass="error" />
							</td>
						</c:if>
						<c:if test="${command.dateFermeture != null && command.natureForFiscal != 'ForFiscalSecondaire'}">
							<%-- Date de fermeture (non éditable) --%>
							<td><fmt:message key="label.date.fermeture" />&nbsp;:</td>
							<td><fmt:formatDate value="${command.dateFermeture}" pattern="dd.MM.yyyy"/></td>

							<%-- Motif de fermeture (non éditable) --%>
							<td><fmt:message key="label.motif.fermeture" />&nbsp;:</td>
							<td><fmt:message key="option.motif.fermeture.${command.motifFermeture}" /></td>
						</c:if>
					</tr>
				</c:if>

			</table>

			<script type="text/javascript">
				// on met-à-jour les motifs d'ouverture et de fermeture au chargement de la page (genre impôt et rattachement sont fixés)
				<c:if test="${command.natureForFiscal == 'ForFiscalSecondaire'}">
				updateMotifsOuverture(E$('motifOuverture'), 'motifOuverture', '${command.numeroCtb}', '${command.genreImpot}', '${command.motifRattachement}', '${command.motifOuverture}');
				</c:if>
				<c:if test="${command.dateFermeture == null || command.natureForFiscal == 'ForFiscalSecondaire'}">
				updateMotifsFermeture(E$('motifFermeture'), 'motifFermeture', '${command.numeroCtb}', '${command.genreImpot}', '${command.motifRattachement}', '${command.motifFermeture}');
				</c:if>
			</script>

			</fieldset>

			<form:errors cssClass="error" />
			<table border="0">
				<tr>
					<td width="25%">&nbsp;</td>
					<td width="25%"><input type="submit" id="maj" value="<fmt:message key="label.bouton.mettre.a.jour" />"></td>
					<td width="25%"><input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="document.location.href='../fiscal/edit.do?id=' + ${command.numeroCtb}" /></td>
					<td width="25%">&nbsp;</td>
				</tr>
			</table>

			</form:form>

		</td>
		<td id="actions_column" style="display:none">
			<div id="actions_list"/>
		</td>
		</tr></table>

		<script type="text/javascript">
			function updateSyncActions() {

				var motifsOuvertureSelect = E$('motifOuverture');
				var motifsOuverture;
				if (motifsOuvertureSelect) {
					motifsOuverture = (motifsOuvertureSelect.selectedIndex < 0 ? null : motifsOuvertureSelect.options[motifsOuvertureSelect.selectedIndex].value);
				}
				else {
					motifsOuverture = '${command.motifOuverture}';
				}

				var dateOuvertureInput = E$('dateOuverture');
				var dateOuverture;
				if (dateOuvertureInput) {
					dateOuverture = dateOuvertureInput.value;
				}
				else {
					dateOuverture = '<unireg:regdate regdate="${command.regDateOuverture}" />'
				}

				var motifsFermetureSelect = E$('motifFermeture');
				var motifsFermeture;
				if (motifsFermetureSelect) {
					motifsFermeture = (motifsFermetureSelect.selectedIndex < 0 ? null : motifsFermetureSelect.options[motifsFermetureSelect.selectedIndex].value);
				}
				else {
					motifsFermeture = '${command.motifFermeture}';
				}
				
				var dateFermetureInput = E$('dateFermeture');
				var dateFermeture;
				if (dateFermetureInput) {
					dateFermeture = dateFermetureInput.value;
				}
				else {
					dateFermeture = '<unireg:regdate regdate="${command.regDateFermeture}" />'
				}
				

				var f = F$('formFor');
				var noOfsAut = f.numeroForFiscalCommune.value + f.numeroForFiscalCommuneHorsCanton.value + f.numeroForFiscalPays.value;

				XT.doAjaxAction('buildSynchronizeActionsTableSurModificationDeFor', E$('actions_list'), {
					'forId' : ${command.id},
					'dateOuverture' : dateOuverture,
					'motifOuverture' : motifsOuverture,
					'dateFermeture' : dateFermeture,
					'motifFermeture' : motifsFermeture,
					'nOfsAutoriteFiscale' : noOfsAut
				});
			}

			function dateOuverture_onChange() {
				updateSyncActions();
			}

			function dateFermeture_onChange() {
				updateSyncActions();
			}
		</script>

	</tiles:put>
</tiles:insert>

		
