<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="command" type="ch.vd.uniregctb.fors.AddForSecondaireView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.creation.for.secondaire">
  			<fmt:param><unireg:numCTB numero="${command.tiersId}"/></fmt:param>
  		</fmt:message>
  	</tiles:put>
	<tiles:put name="head">
		<style type="text/css">
			h1 {
				margin-left: 10px;
				padding-left: 40px;
				padding-top: 4px;
				height: 32px;
				background: url(../css/x/fors/secondaire_32.png) no-repeat;
			}
		</style>
	</tiles:put>
	<tiles:put name="body">

		<form:form id="addForForm" commandName="command" action="add.do">
			<fieldset>
				<legend><span><fmt:message key="label.for.fiscal" /></span></legend>

				<form:hidden path="tiersId"/>

				<script type="text/javascript">
					function updateMotifsFors() {
						var rattachement = $('#rattachement').val();
						var genreImpot = $('#genreImpot').val();
						Fors.updateMotifsOuverture($('#motifDebut'), '${command.tiersId}', genreImpot, rattachement, '${command.motifDebut}');
						Fors.updateMotifsFermeture($('#motifFin'), '${command.tiersId}', genreImpot, rattachement, '${command.motifFin}')
					}
				</script>

				<!-- Debut For -->
				<table border="0">
					<unireg:nextRowClass reset="0"/>
					<tr class="<unireg:nextRowClass/>" >
						<td width="20%"><fmt:message key="label.genre.impot"/>&nbsp;:</td>
						<td>
								<%--@elvariable id="genresImpot" type="java.util.Map<GenreImpot, String>"--%>
							<form:select path="genreImpot" items="${genresImpot}" id="genreImpot" onchange="updateMotifsFors();"/>
							<span class="mandatory">*</span>
						</td>
						<td width="20%"><fmt:message key="label.rattachement"/>&nbsp;:</td>
						<td>
							<%--@elvariable id="rattachements" type="java.util.Map<MotifRattachement, String>"--%>
							<form:select path="motifRattachement" items="${rattachements}" id="rattachement" onchange="updateMotifsFors();"/>
							<span class="mandatory">*</span>
							<form:errors path="motifRattachement" cssClass="error"/>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.date.ouverture" />&nbsp;:</td>
						<td>
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateDebut" />
								<jsp:param name="id" value="dateDebut" />
								<jsp:param name="mandatory" value="true" />
							</jsp:include>
						</td>
						<td><fmt:message key="label.date.fermeture" />&nbsp;:</td>
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
							<form:select path="motifDebut" cssStyle="width:30ex" />
							<span class="mandatory">*</span>
							<form:errors path="motifDebut" cssClass="error" />
						</td>
						<td><fmt:message key="label.motif.fermeture" />&nbsp;:</td>
						<td>
							<form:select path="motifFin" cssStyle="width:30ex" />
							<form:errors path="motifFin" cssClass="error" />
						</td>
					</tr>

					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.type.for.fiscal"/>&nbsp;:</td>
						<td><fmt:message key="option.type.for.fiscal.COMMUNE_OU_FRACTION_VD" /></td>
						<td><label for="autoriteFiscale"><fmt:message key="label.commune.fraction"/>&nbsp;:</label></td>
						<td>
							<input id="autoriteFiscale" size="25" />
							<span class="mandatory">*</span>
							<form:errors path="noAutoriteFiscale" cssClass="error" />
							<form:hidden path="noAutoriteFiscale" />
						</td>
					</tr>
				</table>

				<script type="text/javascript">
					// on initialise l'auto-completion de l'autorité fiscale
					Fors.autoCompleteCommunesVD('#autoriteFiscale', '#noAutoriteFiscale');

					// on initialise les motifs au chargement de la page
					updateMotifsFors();
				</script>
			</fieldset>

			<table border="0">
				<tr>
					<td width="25%">&nbsp;</td>
					<td width="25%"><input type="submit" value="<fmt:message key="label.bouton.ajouter" />"></td>
					<td width="25%"><unireg:buttonTo name="Retour" action="/fiscal/edit.do" params="{id:${command.tiersId}}" method="GET"/> </td>
					<td width="25%">&nbsp;</td>
				</tr>
			</table>
		</form:form>

	</tiles:put>
</tiles:insert>
