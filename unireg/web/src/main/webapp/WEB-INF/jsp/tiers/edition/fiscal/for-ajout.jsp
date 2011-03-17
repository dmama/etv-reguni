<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="index" value="${param.index}" />

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.creation.fors">
  			<fmt:param><unireg:numCTB numero="${command.numeroCtb}"/></fmt:param>
  		</fmt:message>
  	</tiles:put>
	<tiles:put name="body">
		<form:form name="formFor" id="formFor">
		<fieldset><legend><span><fmt:message key="label.for.fiscal" /></span></legend>		

		<script type="text/javascript">
			function updateMotifsFors(element) {
				// les paramètres ci-dessous correspondent aux ids des selects correspondants
				updateMotifsFor(element, 'motifOuverture', 'motifFermeture', '${command.numeroCtb}', 'genre_impot', 'rattachement');
			}
		</script>

		<!-- Debut For -->
		<table border="0">
			<unireg:nextRowClass reset="0"/>
			<tr class="<unireg:nextRowClass/>" >
				<td><fmt:message key="label.genre.impot"/>&nbsp;:</td>
				<td>
					<form:select path="genreImpot" items="${genresImpot}" id="genre_impot"
						onchange="selectGenreImpot(this.options[this.selectedIndex].value, updateMotifsFors);" />
					<form:errors path="genreImpot" cssClass="error"/>
				</td>
				<td id="div_rattachement_label" ><fmt:message key="label.rattachement"/>&nbsp;:</td>
				<td id="div_rattachement" >
					<form:select path="motifRattachement"
							items="${rattachements}" id="rattachement" 
							onchange="updateMotifsFors(this); selectRattachement(this.options[this.selectedIndex].value);"/>
					<form:errors path="motifRattachement" cssClass="error"/>
				</td>
			</tr>
			<tr id="date_for_periodique"  class="<unireg:nextRowClass/>" >
				<td><fmt:message key="label.date.ouverture" />&nbsp;:</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="dateOuverture" />
						<jsp:param name="id" value="dateOuverture" />
					</jsp:include>
				</td>
				<td><fmt:message key="label.date.fermeture" />&nbsp;:</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="dateFermeture" />
						<jsp:param name="id" value="dateFermeture" />
					</jsp:include>
				</td>
			</tr>
			<tr id="motif_for_periodique"  class="<unireg:nextRowClass/>" >
				<td><fmt:message key="label.motif.ouverture" />&nbsp;:</td>
				<td>
					<form:select path="motifOuverture" cssStyle="width:30ex" />
					<form:errors path="motifOuverture" cssClass="error" />
				</td>
				<td><fmt:message key="label.motif.fermeture" />&nbsp;:</td>
				<td>
					<form:select path="motifFermeture" cssStyle="width:30ex" />
					<form:errors path="motifFermeture" cssClass="error" />
				</td>
			</tr>
			<tr id="for_unique" style="display:none;" class="<unireg:nextRowClass/>" >
				<td><fmt:message key="label.date.evenement" />&nbsp;:</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="dateEvenement" />
						<jsp:param name="id" value="dateEvenement" />
					</jsp:include>
				</td>
				<td>&nbsp;</td>
				<td>&nbsp;</td>
			</tr>
			<jsp:include page="for-lieu.jsp"/>
			
			<tr id="mode_imposition"  class="<unireg:nextRowClass/>" >
				<td><fmt:message key="label.mode.imposition"/>&nbsp;:</td>
				<td>
					<form:select path="modeImposition" items="${modesImposition}" />
					<form:errors path="modeImposition" cssClass="error" />
				</td>
				<td>&nbsp;</td>
				<td>&nbsp;</td>
			</tr>
		</table>
		
		<script type="text/javascript">
			// on initialise les motifs au chargement de la page
			updateMotifsFor(E$('motifFermeture'), 'motifOuverture', 'motifFermeture', '${command.numeroCtb}', 
					'genre_impot', 'rattachement', '${command.motifOuverture}', '${command.motifFermeture}');

			selectGenreImpot('${command.genreImpot}');
		</script>
	</fieldset>
	<table border="0">
		<tr>
			<td width="25%">&nbsp;</td>
			<td width="25%"><input type="submit" id="ajouter" value="<fmt:message key="label.bouton.ajouter" />"></td>
			<td width="25%"><input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="document.location.href='../fiscal/edit.do?id=' + ${command.numeroCtb}" /></td>
			<td width="25%">&nbsp;</td>
		</tr>
	</table>
	</form:form>	

	</tiles:put>
</tiles:insert>
