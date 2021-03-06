<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<%--@elvariable id="command" type="ch.vd.unireg.tiers.view.RapportsPrestationView"--%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.liste.complete.rapports.prestation.debiteur" /></tiles:put>

	<tiles:put name="body">

		<!-- Debut Caracteristiques generales -->
		<unireg:bandeauTiers numero="${command.tiersGeneral.numero}" showValidation="true" showEvenementsCivils="true" showLinks="true" urlRetour="${urlRetour}"/>
		<!-- Fin Caracteristiques generales -->

		<c:if test="${command.editionAllowed}">
			<unireg:linkTo name="Ajouter" action="/rapports-prestation/search-sourcier.do" params="{numeroDebiteur:${command.idDpi}}" link_class="add" title="Ajouter rapport de travail"/>
		</c:if>

		<!-- liste de tous les rapports de prestation (non-paginé) d'un débiteur -->
		<c:if test="${not empty command.rapports}">
			<display:table name="command.rapports" id="rapportPrestation" requestURI="/rapports-prestation/full-list.do" class="display" decorator="ch.vd.unireg.decorator.TableEntityDecorator">

				<display:column sortable="true" titleKey="label.date.debut" sortProperty="dateDebut">
					<unireg:regdate regdate="${rapportPrestation.dateDebut}"/>
				</display:column>

				<display:column sortable="true" titleKey="label.date.fin" sortProperty="dateFin">
					<unireg:regdate regdate="${rapportPrestation.dateFin}"/>
				</display:column>
				<display:column sortable="true" titleKey="label.numero.contribuable">
					<a href="../tiers/visu.do?id=${rapportPrestation.noCTB}"><unireg:numCTB numero="${rapportPrestation.noCTB}"/></a>
				</display:column>
				<display:column sortable="true" titleKey="label.nom.prenom" >
					<unireg:multiline lines="${rapportPrestation.nomCourrier}"/>
				</display:column>
				<display:column sortable="true" titleKey="label.numero.avs" >
					${rapportPrestation.noAVS}
				</display:column>
				<display:column style="action">
					<unireg:consulterLog entityNature="RapportEntreTiers" entityId="${rapportPrestation.id}"/>
					<c:if test="${!rapportPrestation.annule && command.editionAllowed}">
						<unireg:raccourciModifier link="../rapport/edit.do?idRapport=${rapportPrestation.id}&sens=SUJET" tooltip="Edition de rapport"/>
						<unireg:linkTo name="" title="Annulation de rapport" confirm="Voulez-vous vraiment annuler ce rapport de prestation ?"
						               action="/rapports-prestation/cancel.do" method="post" params="{rapportId:${rapportPrestation.id}}" link_class="delete"/>
					</c:if>
				</display:column>
				<display:setProperty name="paging.banner.all_items_found" value=""/>
				<display:setProperty name="paging.banner.one_item_found" value=""/>
			</display:table>
		</c:if>

    <div>
		    <unireg:RetourButton link="../tiers/visu.do?id=${command.idDpi}" checkIfModified="true"/>
    </div>

	</tiles:put>
			
</tiles:insert>
