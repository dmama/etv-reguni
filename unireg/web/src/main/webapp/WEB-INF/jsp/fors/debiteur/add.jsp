<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="index" value="${param.index}"/>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.creation.fors">
			<fmt:param><unireg:numCTB numero="${command.tiersId}"/></fmt:param>
		</fmt:message>
	</tiles:put>
	<tiles:put name="body">
		<form:form id="addForForm" commandName="command" action="add.do">
			<fieldset>
				<legend><span><fmt:message key="label.for.fiscal"/></span></legend>

				<form:hidden path="tiersId"/>

				<script type="text/javascript">
					// [UNIREG-2507] si la date de début du for est avant 2009, on affiche un warning non-bloquant
					<%--@elvariable id="anneeMinimaleForDebiteur" type="java.lang.Integer"--%>
					var anneeMinimaleForDebiteur = <c:out value="${anneeMinimaleForDebiteur}"/>;
					function dateOuverture_OnChange(element) {
						if (element) {
							var m = element.value.match(/\d{1,2}\.\d{1,2}\.(\d{4})/);
							if (m) {
								var annee = parseInt(m[1]);
								if (annee < anneeMinimaleForDebiteur) {
									$('#dateDebut_warning').show();
								}
								else {
									$('#dateDebut_warning').hide();
								}
							}
							else {
								$('#dateDebut_warning').hide();
							}
						}
					}

					function selectAutoriteFiscale(name) {
						if (name == 'COMMUNE_OU_FRACTION_VD') {
							$('#for_commune_vd_label').show();
							$('#for_commune_hc_label').hide();
							$('#autoriteFiscale').val(null);
							$('#noAutoriteFiscale').val(null);
							Fors.autoCompleteCommunesVD('#autoriteFiscale', '#noAutoriteFiscale');
						}
						else if (name == 'COMMUNE_HC') {
							$('#for_commune_vd_label').hide();
							$('#for_commune_hc_label').show();
							$('#autoriteFiscale').val(null);
							$('#noAutoriteFiscale').val(null);
							Fors.autoCompleteCommunesHC('#autoriteFiscale', '#noAutoriteFiscale');
						}
					}

					function updateMotifsFors() {
						Fors.updateMotifsOuverture($('#motifDebut'), '${command.tiersId}', 'DEBITEUR_PRESTATION_IMPOSABLE', null, '${command.motifDebut}');
						Fors.updateMotifsFermeture($('#motifFin'), '${command.tiersId}', 'DEBITEUR_PRESTATION_IMPOSABLE', null, '${command.motifFin}')
					}
				</script>

				<!-- Debut For -->
				<table border="0">
					<tr class="<unireg:nextRowClass/>">
						<td><fmt:message key="label.date.ouverture"/>&nbsp;:</td>
						<td>
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateDebut" />
								<jsp:param name="id" value="dateDebut" />
								<jsp:param name="onChange" value="dateOuverture_OnChange"/>
							</jsp:include>
							<span style="color: red;">*</span>

							<span class="error" style="display:none" id="dateDebut_warning">
								<fmt:message key="warning.for.debiteur.ouvert.avant.date">
									<fmt:param><c:out value="${anneeMinimaleForDebiteur}"/></fmt:param>
								</fmt:message>
							</span>
						</td>
						<td><fmt:message key="label.date.fermeture"/>&nbsp;:</td>
						<td>
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateFin" />
								<jsp:param name="id" value="dateFin" />
							</jsp:include>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.motif.ouverture" />&nbsp;:</td>
						<td>
							<form:select path="motifDebut" cssStyle="width:40ex" />
							<span style="color: red;">*</span>
							<form:errors path="motifDebut" cssClass="error" />
						</td>
						<td><fmt:message key="label.motif.fermeture" />&nbsp;:</td>
						<td>
							<form:select path="motifFin" cssStyle="width:40ex" />
							<form:errors path="motifFin" cssClass="error" />
						</td>
					</tr>

					<tr class="<unireg:nextRowClass/>">

						<td><fmt:message key="label.type.for.fiscal"/>&nbsp;:</td>
						<td>
							<%--@elvariable id="typesForFiscalDPI" type="java.util.Map<TypeAutoriteFiscale, String>"--%>
							<form:select path="typeAutoriteFiscale" items="${typesForFiscal}" id="optionTypeAutoriteFiscale"
							             onchange="selectAutoriteFiscale(this.options[this.selectedIndex].value);"/>
						</td>

						<td>
							<label for="autoriteFiscale">
								<span id="for_commune_vd_label"><fmt:message key="label.commune.fraction"/></span>
								<span id="for_commune_hc_label"><fmt:message key="label.commune"/></span>
								&nbsp;:
							</label>
						</td>
						<td>
							<input id="autoriteFiscale" size="25" />
							<span style="color: red;">*</span>
							<form:errors path="noAutoriteFiscale" cssClass="error" />
							<form:hidden path="noAutoriteFiscale" />
						</td>
					</tr>
				</table>
			</fieldset>

			<table border="0">
				<tr>
					<td width="25%">&nbsp;</td>
					<td width="25%"><input type="submit" value="<fmt:message key="label.bouton.ajouter" />"></td>
					<td width="25%"><unireg:buttonTo name="Retour" action="/fiscal/edit-for-debiteur.do" params="{id:${command.tiersId}}" method="GET"/></td>
					<td width="25%">&nbsp;</td>
				</tr>
			</table>

		</form:form>

		<script type="text/javascript">
			$(function() {
				// pour forcer la validation au chargement
				dateOuverture_OnChange($('#dateDebut').get(0));
				selectAutoriteFiscale('${command.typeAutoriteFiscale}');
				updateMotifsFors();
			});
		</script>

	</tiles:put>
</tiles:insert>
