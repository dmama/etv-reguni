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
		</tr>
	</table>
	
	<hr />
	
	<div id="envoi">
	<table>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.destination" />&nbsp;:</td>
			<td width="25%">
				<form:radiobutton path="destinationEnvoi" value="UTILISATEUR_ENVOI" onclick="selectEnvoi('UTILISATEUR_ENVOI');" id="radioUtil" /><label for="radioUtil"><fmt:message key="label.utilisateur" /></label>
			</td>
			<td width="50%" colspan="2">
				<div id="utilisateursEnvoi" <c:if test="${param.depuisTache == 'true'}">style="display:none;"</c:if> >
					<form:input path="nomUtilisateurEnvoi" id="nomUtilisateurEnvoi" />
					<span class="mandatory">*</span>
					<form:errors path="visaUtilisateurEnvoi" cssClass="error"/>
					<form:hidden path="visaUtilisateurEnvoi" id="visaUtilisateurEnvoi"  />
					<script>
						$(function() {
							Autocomplete.security('user', '#nomUtilisateurEnvoi', false, function(item) {
								if (item) {
									$('#visaUtilisateurEnvoi').val(item.id1); // le visa de l'opérateur
								}
								else {
									$('#nomUtilisateurEnvoi').val(null);
									$('#visaUtilisateurEnvoi').val(null);
								}
							});
						});
					</script>
				</div>
			</td>
        </tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%">&nbsp;</td>
			<td width="25%">
				<form:radiobutton path="destinationEnvoi" value="COLLECTIVITE" onclick="selectEnvoi('COLLECTIVITE');" id="radioColl"/><label for="radioColl"><fmt:message key="label.collectivite.administrative" /></label>
			</td>
			<td width="50%" colspan="2">
				<div id="collectivites" <c:if test="${param.depuisTache != 'true'}">style="display:none;"</c:if> >
					<form:input path="collAdmDestinataireEnvoi" id="collAdmDestinataireEnvoi" />
					<span class="mandatory">*</span>
					<form:errors path="noCollAdmDestinataireEnvoi" cssClass="error"/>
					<form:hidden path="noCollAdmDestinataireEnvoi" id="noCollAdmDestinataireEnvoi"  />
					<script>
						$(function() {
							Autocomplete.infra('collectiviteAdministrative', '#collAdmDestinataireEnvoi', true, function(item) {
								$('#noCollAdmDestinataireEnvoi').val(item ? item.id1 : null); // le numéro de collectivité
							});
						});
					</script>
				</div>
			</td>
		</tr>
	</table>
	<script>
		$(function() { <%-- [SIFISC-4823]fix l'état du formulaire incoherent en cas d'erreur --%>
			selectEnvoi($('input[@name=destinationEnvoi]:checked').attr('id') === 'radioColl' ? 'COLLECTIVITE': 'UTILISATEUR_ENVOI');
		});
    </script>
	</div>
		
	<div id="reception" style="display:none;">
	<table>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.destination" />&nbsp;:</td>
			<td width="25%">
				<form:radiobutton path="localisation"  value="PERSONNE" onclick="selectReception('PERSONNE');" id="radioPersonne"/><label for="radioPersonne"><fmt:message key="label.utilisateur" /></label>
			</td>
			<td width="50%" colspan="2">
				<div id="utilisateursReception">
					<form:input path="nomUtilisateurReception" id="nomUtilisateurReception" />
					<span class="mandatory">*</span>
					<form:errors path="visaUtilisateurReception" cssClass="error"/>
					<form:hidden path="visaUtilisateurReception" id="visaUtilisateurReception"  />
					<script>
						$(function() {
							Autocomplete.security('user', '#nomUtilisateurReception', false, function(item) {
								if (item) {
									$('#visaUtilisateurReception').val(item.id1); // le visa de l'opérateur
								}
								else {
									$('#nomUtilisateurReception').val(null);
									$('#visaUtilisateurReception').val(null);
								}
							});
						});
					</script>
				</div>
			</td>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%">&nbsp;</td>
			<td width="25%">
				<form:radiobutton path="localisation" value="CLASSEMENT_GENERAL" onclick="selectReception('CLASSEMENT_GENERAL');" id="radioClassementGeneral"/><label for="radioClassementGeneral"><fmt:message key="option.localisation.CLASSEMENT_GENERAL" /></label>
			</td>
			<td width="50%" colspan="2">&nbsp;</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%">&nbsp;</td>
			<td width="25%">
				<form:radiobutton path="localisation" value="CLASSEMENT_INDEPENDANTS" onclick="selectReception('CLASSEMENT_INDEPENDANTS');" id="radioClassementIndependant"/><label for="radioClassementIndependant"><fmt:message key="option.localisation.CLASSEMENT_INDEPENDANTS" /></label>
			</td>
			<td width="50%" colspan="2">&nbsp;</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%">&nbsp;</td>
			<td width="25%">
				<form:radiobutton path="localisation" value="ARCHIVES" onclick="selectReception('ARCHIVES');" id="radioArchives"/><label for="radioArchives"><fmt:message key="option.localisation.ARCHIVES" /></label>
			</td>
			<td width="50%" colspan="2">&nbsp;</td>
		</tr>
	</table>

		<script>
			$(function () {
				//En cas d'erreur on force la selection.
				selectTypeMouvement($('select#type_mouvement option:selected').val());
			});
		</script>
	</div>

	<script type="text/javascript" language="Javascript" src="<c:url value="/js/mouvement.js"/>"></script>

</fieldset>
<!-- Fin  Mouvement de dossier -->