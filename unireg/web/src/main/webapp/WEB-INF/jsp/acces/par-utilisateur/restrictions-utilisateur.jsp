<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.droits.acces.utilisateur" /></tiles:put>
  	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/acces-par-utilisateur.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
	<tiles:put name="body">
	<form:form  action="restrictions-utilisateur/annuler.do" id="formEditRestriction"  name="theForm" commandName="command">
		<input type="hidden" value="${command.utilisateur.numeroIndividu}" name="noIndividuOperateur"/>
		<input type="hidden" value="false" name="annuleTout" id="annuleTout"/>
		<unireg:nextRowClass reset="1"/>
		<!-- Debut Caracteristiques generales -->
		<jsp:include page="../../general/utilisateur.jsp">
			<jsp:param name="path" value="utilisateur" />
			<jsp:param name="titleKey" value="title.droits.operateur" />
		</jsp:include>
		<!-- Fin Caracteristiques generales -->
		<!-- Debut Liste des restrictions -->
		<fieldset>
		<legend><span><fmt:message key="label.caracteristiques.acces" /></span></legend>
		<authz:authorize ifAnyGranted="ROLE_SEC_DOS_ECR">
			<table border="0">
			<tr>
				<td>
					<a href="list-pp-utilisateur.do?noIndividuOperateur=${command.utilisateur.numeroIndividu}" 
					class="add" title="Ajouter">&nbsp;<fmt:message key="label.bouton.ajouter" /></a>

				<c:if test="${not empty command.restrictions}">
					&nbsp;
					<script>
					function onClickAnnualtion (master) {
						var countDroitAAnnuler = 0;
						$(".slave").each( function(i,elt) { if (elt.checked) ++countDroitAAnnuler; })
						if ( countDroitAAnnuler <= 0 ) {
							alert("Aucun droit n'a été séléctionné pour l'annulation")
						} else {
							var message = "Voulez-vous vraiment annuler le droit d'accès sélectionné ?"
							if (countDroitAAnnuler > 1) {
								message = "Voulez-vous vraiment annuler les " + countDroitAAnnuler + " droits d'accès sélectionnés ?"
							}
							var confirmation = confirm(message)
							if(confirmation) {
								$("#formEditRestriction")[0].submit()
							}
						}
					}
					function onClickToutAnnuler (master) {
						var confirmation = confirm("Voulez-vous vraiment annuler tous les droits d'accès pour l'utilisateur ${command.utilisateur.prenomNom} (${command.utilisateur.visaOperateur}) ?")
						if (confirmation) {
							$("#annuleTout")[0].value ="true";
							$("#formEditRestriction")[0].submit()
						}

					}
					</script>
					<authz:authorize ifAnyGranted="ROLE_SEC_DOS_ECR">
							<a href="javascript:onClickAnnualtion()" class="delete noprint" title="Annule les droits séléctionnés">
								&nbsp;Annuler les droits séléctionnés
							</a>
							<a href="javascript:onClickToutAnnuler()" class="delete noprint" title="Annule tous les droits de l'utilisateur">
								&nbsp;Annuler tous les droits
							</a>
					</authz:authorize>
				</c:if>
				</td>
			</tr>
			</table>
		</authz:authorize>
		<c:if test="${not empty command.restrictions}">
			<script>
				function onClickMaster (master) {
					var masterState = master.checked;
					$(".slave,.master").each( function(i,elt) { elt.checked = masterState })
				}
				function onClickSlave () {
					masterShouldBeChecked = true
					$(".slave").each( function(i,elt) { if (!elt.checked) masterShouldBeChecked = false; })
					$(".master").each( function(i,elt) { elt.checked = masterShouldBeChecked});
				}
			</script>
			<display:table
					name="command.restrictions" id="restriction" pagesize="10" 
					requestURI="${url}"
					class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
				<display:column sortable ="false" title="<input type='checkbox'  class='master' onclick='javascript:onClickMaster(this);' />">
				<authz:authorize ifAnyGranted="ROLE_SEC_DOS_ECR">
					<c:if test="${!restriction.annule}">
						<input type="checkbox" class="slave" name="aAnnuler" value="${restriction.id}" onclick="javascript:onClickSlave();"/>
					</c:if>
				</authz:authorize>
				</display:column>
				<display:column sortable ="true" titleKey="label.type.restriction">
						<fmt:message key="option.type.droit.acces.${restriction.type}"  />
				</display:column>
				<display:column sortable ="true" titleKey="label.numero.contribuable" >
						<unireg:numCTB numero="${restriction.numeroCTB}" />
				</display:column>
				<display:column sortable ="true" titleKey="label.prenom.nom">
						<c:out value="${restriction.prenomNom}" />
				</display:column>
				<display:column sortable ="true" titleKey="label.localite">
						<c:out value="${restriction.localite}" />
				</display:column>
				<display:column sortable ="true" titleKey="label.date.naissance">
						<unireg:date date="${restriction.dateNaissance}"></unireg:date>
				</display:column>
				<display:column sortable ="true" titleKey="label.lecture.seule">
					<input type="checkbox" name="lectureSeule" value="True"   
							<c:if test="${restriction.lectureSeule}">checked </c:if> disabled="disabled" />
				</display:column>
				<display:column style="action">
					<unireg:consulterLog entityNature="DroitAcces"  entityId="${restriction.id}"/>
				</display:column>
				<display:setProperty name="paging.banner.all_items_found" value=""/>
				<display:setProperty name="paging.banner.one_item_found" value=""/>
			</display:table>

			</c:if>
			</fieldset>
		<!-- Fin Liste des restrictions -->
		<!-- Debut Bouton -->
		</form:form>
		<form:form action="restrictions-utilisateur/exporter.do" method="post" id="formExporter"  name="formExporter">
		<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="javascript:document.location.href='select-utilisateur.do';" />
		<c:if test="${not empty command.restrictions}">
			<input type="hidden" value="${command.utilisateur.numeroIndividu}" name="noIndividuOperateur"/>
			<input type="submit" value="<fmt:message key="label.bouton.exporter"/>" name="exporter"/>
		</c:if>
		<!-- Fin Bouton -->
	</form:form>
	</tiles:put>
</tiles:insert>