<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Mouvement de dossier -->
<fieldset class="information">
	<legend><span><fmt:message key="caracteristiques.mouvement.dossier" /></span></legend>

	<table>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.type.mouvement" />&nbsp;:</td>
			<td width="25%">
				<form:select path="typeMouvement" items="${typesMouvement}" 
						id="type_mouvement" onchange="selectTypeMouvement(this.options[this.selectedIndex].value);" /> 			
			</td>
			<td width="50%" colspan="2">&nbsp;</td>
			<%-- 
			<td width="25%"><fmt:message key="label.date.mouvement" />&nbsp;:</td>
			<td width="25%">
				<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
					<jsp:param name="path" value="dateMouvement" />
					<jsp:param name="id" value="dateMouvement" />
				</jsp:include>
			</td>
			--%>
		</tr>
	</table>
	
	<hr />
	
	<div id="envoi">
	<table>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.destination" />&nbsp;:</td>
			<td width="25%">
				<form:radiobutton path="destinationEnvoi" value="utilisateurEnvoi" onclick="selectEnvoi('utilisateurEnvoi');" /><label for="utilisateur"><fmt:message key="label.utilisateur" /></label>
			</td>
			<td width="50%" colspan="2">
				<div id="utilisateursEnvoi" <c:if test="${param.depuisTache == 'true'}">style="display:none;"</c:if> >
					<form:input path="utilisateurEnvoi" id="utilisateurEnvoi" />
					<form:errors path="utilisateurEnvoi" cssClass="error"/>
					<form:hidden path="numeroUtilisateurEnvoi" id="numeroUtilisateurEnvoi"  />
					<script type="text/javascript">
							function utilisateurEnvoi_onChange(row) {	
								var form = document.forms["formEditMvt"];
								form.numeroUtilisateurEnvoi.value = ( row ? row.individuNoTechnique : "");
							}
					</script>
					<jsp:include page="/WEB-INF/jsp/include/autocomplete.jsp">
						<jsp:param name="inputId" value="utilisateurEnvoi" />
						<jsp:param name="dataValueField" value="visaOperateur" />
						<jsp:param name="dataTextField" value="{nom} {prenom} ({visaOperateur})" />
						<jsp:param name="dataSource" value="selectionnerUtilisateur" />
						<jsp:param name="onChange" value="utilisateurEnvoi_onChange" />
						<jsp:param name="autoSynchrone" value="false"/>
					</jsp:include>
				</div>
			</td>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%">&nbsp;</td>
			<td width="25%">
				<form:radiobutton path="destinationEnvoi" value="collectivite" onclick="selectEnvoi('collectivite');" /><label for="collectivite"><fmt:message key="label.collectivite.administrative" /></label>
			</td>
			<td width="50%" colspan="2">
				<div id="collectivites" <c:if test="${param.depuisTache != 'true'}">style="display:none;"</c:if> >
					<form:input path="collAdmDestinataireEnvoi" id="collAdmDestinataireEnvoi" />
					<form:hidden path="noCollAdmDestinataireEnvoi" id="noCollAdmDestinataireEnvoi"  />
					<script type="text/javascript">
					function libCollAdmDestinataireEnvoi_onChange(row) {
						document.forms["formEditMvt"].noCollAdmDestinataireEnvoi.value = (row ? row.noColAdm : "");
					}
					</script>
					<jsp:include page="/WEB-INF/jsp/include/autocomplete.jsp">
						<jsp:param name="inputId" value="collAdmDestinataireEnvoi" />
						<jsp:param name="dataValueField" value="nomCourt" />
						<jsp:param name="dataTextField" value="{nomCourt}" />
						<jsp:param name="dataSource" value="selectionnerCollectiviteAdministrative" />
						<jsp:param name="onChange" value="libCollAdmDestinataireEnvoi_onChange" />
						<jsp:param name="autoSynchrone" value="false"/>
					</jsp:include>
				</div>
			</td>
		</tr>
	</table>
	</div>	
		
	<div id="reception" style="display:none;">
	<table>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.destination" />&nbsp;:</td>
			<td width="25%">
				<form:radiobutton path="localisation"  value="PERSONNE" onclick="selectReception('PERSONNE');" /><label for="PERSONNE"><fmt:message key="label.utilisateur" /></label>
			</td>
			<td width="50%" colspan="2">
				<div id="utilisateursReception">
					<form:input path="utilisateurReception" id="utilisateurReception" />
					<form:errors path="utilisateurReception" cssClass="error"/>
					<form:hidden path="numeroUtilisateurReception" id="numeroUtilisateurReception"  />
					<script type="text/javascript">
							function utilisateurReception_onChange(row) {	
								var form = document.forms["formEditMvt"];
								form.numeroUtilisateurReception.value = ( row ? row.individuNoTechnique : "");
							}
					</script>
					<jsp:include page="/WEB-INF/jsp/include/autocomplete.jsp">
						<jsp:param name="inputId" value="utilisateurReception" />
						<jsp:param name="dataValueField" value="visaOperateur" />
						<jsp:param name="dataTextField" value="{nom} {prenom} ({visaOperateur})" />
						<jsp:param name="dataSource" value="selectionnerUtilisateur" />
						<jsp:param name="onChange" value="utilisateurReception_onChange" />
						<jsp:param name="autoSynchrone" value="false"/>
					</jsp:include>
				</div>
			</td>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%">&nbsp;</td>
			<td width="25%">
				<form:radiobutton path="localisation" value="CLASSEMENT_GENERAL" onclick="selectReception('CLASSEMENT_GENERAL');" /><label for="CLASSEMENT_GENERAL"><fmt:message key="option.localisation.CLASSEMENT_GENERAL" /></label>
			</td>
			<td width="50%" colspan="2">&nbsp;</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%">&nbsp;</td>
			<td width="25%">
				<form:radiobutton path="localisation" value="CLASSEMENT_INDEPENDANTS" onclick="selectReception('CLASSEMENT_INDEPENDANTS');" /><label for="CLASSEMENT_INDEPENDANTS"><fmt:message key="option.localisation.CLASSEMENT_INDEPENDANTS" /></label>
			</td>
			<td width="50%" colspan="2">&nbsp;</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%">&nbsp;</td>
			<td width="25%">
				<form:radiobutton path="localisation" value="ARCHIVES" onclick="selectReception('ARCHIVES');" /><label for="ARCHIVES"><fmt:message key="option.localisation.ARCHIVES" /></label>
			</td>
			<td width="50%" colspan="2">&nbsp;</td>
		</tr>
	</table>
	</div>

</fieldset>
<!-- Fin  Mouvement de dossier -->