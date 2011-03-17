<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.liste.complete.rapports.prestation.debiteur" /></tiles:put>

	<tiles:put name="body">

		<!-- Debut Caracteristiques generales -->
		<jsp:include page="../../../general/tiers.jsp">
			<jsp:param name="page" value="visu" />
			<jsp:param name="path" value="tiersGeneral" />		
		</jsp:include>
		<!-- Fin Caracteristiques generales -->

		<c:if test="${command.editionAllowed}">
			<unireg:raccourciAjouter link="../rt/list-sourcier.do?numeroDpi=${command.idDpi}&provenance=listeRPI" tooltip="Ajouter rapport" display="label.bouton.ajouter"/>
		</c:if>

		<!-- Debut liste de tous les rapports -->
		<c:if test="${not empty command.rapports}">
			<display:table name="command.rapports" id="rapportPrestation" requestURI="/rapports-prestation/list.do" class="display" >

				<display:column sortable="true" titleKey="label.date.debut">
					<c:if test="${rapportPrestation.annule}"><strike></c:if>
						<unireg:regdate regdate="${rapportPrestation.dateDebut}"/>
					<c:if test="${rapportPrestation.annule}"></strike></c:if>
				</display:column>

				<display:column sortable="true" titleKey="label.date.fin">
					<c:if test="${rapportPrestation.annule}"><strike></c:if>
						<unireg:regdate regdate="${rapportPrestation.dateFin}"/>
					<c:if test="${rapportPrestation.annule}"></strike></c:if>
				</display:column>
				<display:column sortable="true" titleKey="label.type.activite" >
					<c:if test="${rapportPrestation.annule}"><strike></c:if>
						<c:if test="${rapportPrestation.typeActivite != ''}">
							<fmt:message key="option.type.activite.${rapportPrestation.typeActivite}" />
						</c:if>
					<c:if test="${rapportPrestation.annule}"></strike></c:if>
				</display:column>
				<display:column sortable="true" titleKey="label.taux.activite">
					<c:if test="${rapportPrestation.annule}"><strike></c:if>
						<c:if test="${rapportPrestation.tauxActivite  != null}">
							${rapportPrestation.tauxActivite}%
						</c:if>
					<c:if test="${rapportPrestation.annule}"></strike></c:if>
				</display:column>
				<display:column sortable="true" titleKey="label.numero.contribuable">
					<c:if test="${rapportPrestation.annule}"><strike></c:if>
						<a href="../tiers/visu.do?id=${rapportPrestation.noCTB}"><unireg:numCTB numero="${rapportPrestation.noCTB}"></unireg:numCTB></a>
					<c:if test="${rapportPrestation.annule}"></strike></c:if>
				</display:column>
				<display:column sortable="true" titleKey="label.nom.prenom" >
					<c:if test="${rapportPrestation.annule}"><strike></c:if>
						<c:if test="${rapportPrestation.nomCourrier1 != null }">
							${rapportPrestation.nomCourrier1}
						</c:if>
						<c:if test="${rapportPrestation.nomCourrier2 != null }">
							<br />${rapportPrestation.nomCourrier2}
						</c:if>
					<c:if test="${rapportPrestation.annule}"></strike></c:if>
				</display:column>
				<display:column sortable="true" titleKey="label.numero.avs" >
					<c:if test="${rapportPrestation.annule}"><strike></c:if>
						${rapportPrestation.noAVS}
					<c:if test="${rapportPrestation.annule}"></strike></c:if>
				</display:column>
				<display:column style="action">
					<unireg:consulterLog entityNature="RapportEntreTiers" entityId="${rapportPrestation.id}"/>
					<c:if test="${!rapportPrestation.annule && command.editionAllowed}">
						<unireg:raccourciModifier link="../tiers/rapport.do?idRapport=${rapportPrestation.id}&sens=SUJET&viewRetour=%2Frapports-prestation%2Flist.do%3FidDpi%3D${command.idDpi}" tooltip="Edition de rapport"/>
						<unireg:raccourciAnnuler onClick="annulerRapportPrestation(${rapportPrestation.id}); return false;" tooltip="Annulation de rapport"/>
					</c:if>
				</display:column>
				<display:setProperty name="paging.banner.all_items_found" value=""/>
				<display:setProperty name="paging.banner.one_item_found" value=""/>
			</display:table>
		</c:if>
		<!-- Fin liste de tous les rapports -->

	</tiles:put>
			
</tiles:insert>
