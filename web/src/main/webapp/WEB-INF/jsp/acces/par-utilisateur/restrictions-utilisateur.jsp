<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.droits.acces.utilisateur" /></tiles:put>
  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onClick="ouvrirAide('<c:url value='/docs/acces-par-utilisateur.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
	<tiles:put name="body">

	<form:form  action="annuler-restriction.do" id="formEditRestriction"  name="theForm" commandName="command">
		<input type="hidden" value="${command.utilisateur.numeroIndividu}" name="noIndividuOperateur"/>
		<input type="hidden" value="false" name="annuleTout" id="annuleTout"/>
		<unireg:nextRowClass reset="1"/>
		<!-- Debut Caracteristiques generales -->
		<jsp:include page="../../general/utilisateur.jsp">
			<jsp:param name="path" value="utilisateur" />
			<jsp:param name="titleKey" value="title.droits.operateur" />
		</jsp:include>
		<!-- Fin Caracteristiques generales -->

		<!-- Début de la liste des conflits rencontrés -->
		<c:if test="${not empty conflicts}">
			<fieldset>
				<legend><span><fmt:message key="label.conflits"/></span></legend>
				<table border="0">
					<tr>
						<td>
							<unireg:linkTo name="Exporter" action="/acces/par-utilisateur/exporter-conflits.do" method="get" link_class="download noprint"/>
						</td>
					</tr>
				</table>

				<table class="display conflicts">
					<tr>
						<th rowspan="2"><fmt:message key="label.numero.contribuable"/></th>
						<th rowspan="2"><fmt:message key="label.nom.raison"/></th>
						<th rowspan="2"><fmt:message key="label.localite"/></th>
						<th rowspan="2"><fmt:message key="label.date.naissance.ou.rc"/></th>
						<th colspan="2" style="text-align: center;"><fmt:message key="label.droit.acces.preexistant"/></th>
						<th colspan="2" style="text-align: center;"><fmt:message key="label.droit.acces.souhaite"/></th>
					</tr>
					<tr>
						<th><fmt:message key="label.type.restriction"/></th>
						<th><fmt:message key="label.lecture.seule"/></th>
						<th><fmt:message key="label.type.restriction"/></th>
						<th><fmt:message key="label.lecture.seule"/></th>
					</tr>
					<unireg:nextRowClass reset="1"/>
					<c:forEach items="${conflicts}" var="conflict">
						<tr class="<unireg:nextRowClass/>">
							<td><unireg:numCTB numero="${conflict.noContribuable}"/></td>
							<td><c:out value="${conflict.prenomNom}"/></td>
							<td><c:out value="${conflict.npaLocalite}"/></td>
							<td><unireg:regdate regdate="${conflict.dateNaissance}" format="dd.MM.yyyy"/></td>
							<td><fmt:message key="option.type.droit.acces.${conflict.accesPreexistant.type}"/></td>
							<td>
								<input type="checkbox" name="lectureSeule" value="True"
							                               <c:if test="${conflict.accesPreexistant.niveau == 'LECTURE'}">checked </c:if> disabled="disabled" />
							</td>
							<td <c:if test="${conflict.accesPreexistant.type != conflict.accesCopie.type}">class="conflict"</c:if>>
								<fmt:message key="option.type.droit.acces.${conflict.accesCopie.type}"/>
							</td>
							<td <c:if test="${conflict.accesPreexistant.niveau != conflict.accesCopie.niveau}">class="conflict"</c:if>>
								<input type="checkbox" name="lectureSeule" value="True"
								       <c:if test="${conflict.accesCopie.niveau == 'LECTURE'}">checked </c:if> disabled="disabled" />
							</td>
						</tr>
					</c:forEach>
				</table>
			</fieldset>
		</c:if>
		<!-- Fin de la liste des conflits rencontrés -->

		<!-- Debut Liste des restrictions -->
		<unireg:nextRowClass reset="1"/>
		<fieldset>
		<legend><span><fmt:message key="label.caracteristiques.acces" /></span></legend>
		<authz:authorize access="hasAnyRole('ROLE_SEC_DOS_ECR')">
			<table border="0">
			<tr>
				<td>
					<a href="ajouter-restriction.do?noIndividuOperateur=${command.utilisateur.numeroIndividu}"
					class="add" title="Ajouter"><fmt:message key="label.bouton.ajouter" /></a>

				<c:if test="${not empty command.restrictions}">
					&nbsp;
					<script>
					function onClickAnnualtion (master) {
						var countDroitAAnnuler = 0;
						$(".slave").each( function(i,elt) { if (elt.checked) ++countDroitAAnnuler; });
						if ( countDroitAAnnuler <= 0 ) {
							alert("Aucun droit n'a été séléctionné pour l'annulation")
						} else {
							var message = "Voulez-vous vraiment annuler le droit d'accès sélectionné ?";
							if (countDroitAAnnuler > 1) {
								message = "Voulez-vous vraiment annuler les " + countDroitAAnnuler + " droits d'accès sélectionnés ?"
							}
							var confirmation = confirm(message);
							if(confirmation) {
								$("#formEditRestriction")[0].submit();
							}
						}
					}
					function onClickToutAnnuler (master) {
						var confirmation = confirm("Voulez-vous vraiment annuler tous les droits d'accès pour l'utilisateur ${command.utilisateur.prenomNom} (${command.utilisateur.visaOperateur}) ?");
						if (confirmation) {
							$("#annuleTout")[0].value ="true";
							$("#formEditRestriction")[0].submit()
						}

					}
					</script>
					<authz:authorize access="hasAnyRole('ROLE_SEC_DOS_ECR')">
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
					masterShouldBeChecked = true;
					$(".slave").each( function(i,elt) { if (!elt.checked) masterShouldBeChecked = false; });
					$(".master").each( function(i,elt) { elt.checked = masterShouldBeChecked});
				}
			</script>
			<display:table
					name="command.restrictions" id="restriction" pagesize="25"
					requestURI="${url}" defaultsort="1" defaultorder="ascending" partialList="true" sort="external" size="command.size"
					class="display" decorator="ch.vd.unireg.decorator.TableEntityDecorator">
				<display:column sortable ="false" title="<input type='checkbox'  class='master' onclick='javascript:onClickMaster(this);' />">
				<authz:authorize access="hasAnyRole('ROLE_SEC_DOS_ECR')">
					<c:if test="${!restriction.annule}">
						<input type="checkbox" class="slave" name="aAnnuler" value="${restriction.id}" onclick="onClickSlave();"/>
					</c:if>
				</authz:authorize>
				</display:column>
				<display:column sortable ="false" titleKey="label.type.restriction">
					<fmt:message key="option.type.droit.acces.${restriction.type}"  />
				</display:column>
				<display:column sortable ="false" titleKey="label.numero.contribuable">
					<unireg:numCTB numero="${restriction.numeroCTB}" />
				</display:column>
				<display:column sortable ="false" titleKey="label.date.debut">
					<unireg:regdate regdate="${restriction.dateDebut}" format="dd.MM.yyyy"/>
				</display:column>
				<display:column sortable ="false" titleKey="label.date.fin">
					<unireg:regdate regdate="${restriction.dateFin}" format="dd.MM.yyyy"/>
				</display:column>
				<display:column sortable ="false" titleKey="label.nom.raison">
					<c:choose>
						<c:when test="${restriction.erreur != null}">
							<span class="erreur"><c:out value="${restriction.erreur}"/></span>
						</c:when>
						<c:otherwise>
							<c:out value="${restriction.prenomNom}" />
						</c:otherwise>
					</c:choose>
				</display:column>
				<display:column sortable ="false" titleKey="label.localite">
					<c:out value="${restriction.localite}" />
				</display:column>
				<display:column sortable ="false" titleKey="label.date.naissance.ou.rc">
					<unireg:date date="${restriction.dateNaissance}"/>
				</display:column>
				<display:column sortable ="false" titleKey="label.lecture.seule">
					<input type="checkbox" name="lectureSeule" value="True"   
							<c:if test="${restriction.lectureSeule}">checked </c:if> disabled="disabled" />
				</display:column>
				<display:column style="action" sortable ="false">
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
		<form:form action="exporter-restrictions.do" method="post" id="formExporter"  name="formExporter">
		<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="document.location.href='../par-utilisateur.do';" />
		<c:if test="${not empty command.restrictions && command.nbDroitsNonAnnules > 0}">
			<input type="hidden" value="${command.utilisateur.numeroIndividu}" name="noIndividuOperateur"/>
			<input type="submit" value="<fmt:message key="label.bouton.exporter"/>" name="exporter"/>
		</c:if>
		<!-- Fin Bouton -->
	</form:form>
	</tiles:put>
</tiles:insert>