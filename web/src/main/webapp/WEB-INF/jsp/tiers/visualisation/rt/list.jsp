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
			<display:table name="command.rapports" id="rapportPrestation" requestURI="/rapports-prestation/list.do" class="display" decorator="ch.vd.unireg.decorator.TableEntityDecorator">

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
						<unireg:raccourciModifier link="../rapport/edit.do?idRapport=${rapportPrestation.id}&sens=SUJET&viewRetour=%2Frapports-prestation%2Flist.do%3FidDpi%3D${command.idDpi}" tooltip="Edition de rapport"/>
						<unireg:raccourciAnnuler onClick="annulerRapportPrestation(${rapportPrestation.id}); return false;" tooltip="Annulation de rapport"/>
					</c:if>
				</display:column>
				<display:setProperty name="paging.banner.all_items_found" value=""/>
				<display:setProperty name="paging.banner.one_item_found" value=""/>
			</display:table>

			<script>
				function annulerRapportPrestation(idRapport) {
					if (confirm('Voulez-vous vraiment annuler ce rapport entre tiers ?')) {
						var form = $('<form method="POST" action="' + App.curl('/rapports-prestation/edit.do') + '">' +
							'<input type="hidden" name="__TARGET__" value="annulerRapport"/>' +
							'<input type="hidden" name="__EVENT_ARGUMENT__" value="' + idRapport + '"/>' +
							'<input type="hidden" name="__URL_RETOUR__" value="' + window.location + '"/></form>');
						form.appendTo('body');
						form.submit();
					}
				}
			</script>
		</c:if>
		<!-- Fin liste de tous les rapports -->

    <div>
		    <unireg:RetourButton link="../tiers/visu.do?id=${command.idDpi}" checkIfModified="true"/>
    </div>

	</tiles:put>
			
</tiles:insert>
