<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="lr" type="ch.vd.unireg.lr.view.ListeRecapitulativeDetailView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.edition.lr" /></tiles:put>

	<tiles:put name="body">

		<unireg:nextRowClass reset="1"/>

		<!-- Debut Caracteristiques generales -->
		<unireg:bandeauTiers numero="${lr.idDebiteur}" titre="caracteristiques.debiteur.is" showValidation="false" showEvenementsCivils="false" showLinks="false" showAvatar="false" showComplements="true"/>
		<!-- Fin Caracteristiques generales -->

		<!-- Debut LR -->
		<fieldset class="information">
			<legend><span><fmt:message key="caracteristiques.lr" /></span></legend>

			<table>
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.date.debut.periode" />&nbsp;:</td>
					<td width="25%"><unireg:regdate regdate="${lr.dateDebut}"/></td>
					<td width="25%"><fmt:message key="label.date.fin.periode" />&nbsp;:</td>
					<td width="25%"><unireg:regdate regdate="${lr.dateFin}"/></td>
				</tr>
				<c:choose>
					<c:when test="${lr.dateRetour != null}">
						<tr class="<unireg:nextRowClass/>" >
							<td width="25%"><fmt:message key="label.date.retour" />&nbsp;:</td>
							<td width="75%" colspan="3"><unireg:regdate regdate="${lr.dateRetour}"/></td>
						</tr>
					</c:when>
					<c:when test="${lr.etat != null}">
						<tr class="<unireg:nextRowClass/>">
							<td width="25%"><fmt:message key="label.etat.actuel"/>&nbsp;:</td>
							<td width="25%"><fmt:message key="option.etat.avancement.f.${lr.etat}"/></td>
							<td width="25%"><fmt:message key="label.date.obtention.etat.actuel"/>&nbsp;:</td>
							<td width="25%"><unireg:regdate regdate="${lr.dateObtentionEtat}"/></td>
						</tr>
					</c:when>
				</c:choose>
			</table>
		</fieldset>
		<!-- Fin  Listes recapitulatives -->

		<!-- Debut Delais -->
		<fieldset>
			<legend><span><fmt:message key="label.delais" /></span></legend>

			<c:if test="${lr.allowedDelai}">
				<table border="0">
					<tr>
						<td>
							<a href="add-delai.do?idListe=${lr.idListe}" class="add" title="Ajouter"><fmt:message key="label.bouton.ajouter"/></a>
						</td>
					</tr>
				</table>
			</c:if>

			<display:table 	name="lr.delais" id="delai" pagesize="10" class="display" decorator="ch.vd.unireg.decorator.TableEntityDecorator">
				<display:column titleKey="label.date.demande">
					<unireg:regdate regdate="${delai.dateDemande}" />
				</display:column>
				<display:column titleKey="label.date.delai.accorde">
					<unireg:regdate regdate="${delai.delaiAccordeAu}" />
				</display:column>
				<display:column titleKey="label.confirmation.ecrite">
					<input type="checkbox" name="decede" value="True" <c:if test="${delai.confirmationEcrite}">checked </c:if> disabled="disabled" />
				</display:column>
				<display:column titleKey="label.date.traitement">
					<unireg:regdate regdate="${delai.dateTraitement}" />
				</display:column>
				<display:column style="action">
					<c:if test="${(!delai.annule) && (!delai.first)}">
						<unireg:linkTo name="" title="Annuler le délai"  confirm="Voulez-vous vraiment annuler ce delai ?"
						               action="/declaration/delai/annuler.do" method="post" params="{id:${delai.id}}" link_class="delete"/>
					</c:if>
				</display:column>

				<display:setProperty name="paging.banner.all_items_found" value=""/>
				<display:setProperty name="paging.banner.one_item_found" value=""/>
			</display:table>

		</fieldset>
		<!-- Fin Delais -->
		<!-- Fin LR -->

		<!-- Debut Boutons -->
		<unireg:buttonTo name="label.bouton.retour" action="/lr/edit-debiteur.do" params="{numero:${lr.idDebiteur}}" method="get"/>
		<unireg:buttonTo name="label.bouton.imprimer.duplicata" action="/lr/duplicata.do" params="{idListe:${lr.idListe}}" method="post" confirm="Voulez-vous vraiment imprimer un duplicata de cette liste recapitulative ?"/>
		<c:if test="${lr.annulable}">
			<unireg:buttonTo name="label.bouton.annuler.liste" action="/lr/annuler.do" params="{idListe:${lr.idListe}}" method="post" confirm="Voulez-vous vraiment annuler cette liste recapitulative ?"/>
		</c:if>
		<!-- Fin Boutons -->

	</tiles:put>
</tiles:insert>
