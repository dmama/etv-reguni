<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="docsAvecSuivi" type="ch.vd.unireg.documentfiscal.AutreDocumentFiscalListView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.autre.document.fiscal.edition">
			<fmt:param>
				<unireg:numCTB numero="${pmId}"/>
			</fmt:param>
		</fmt:message>
	</tiles:put>
	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
		<unireg:bandeauTiers numero="${pmId}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" titre="Caractéristiques du contribuable"/>
		<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
		<fieldset>
			<legend><span><fmt:message key="label.autre.document.fiscal.nouvel.envoi"/></span></legend>

			<%--@elvariable id="isRadieeRCOuDissoute" type="java.lang.Boolean"--%>
			<%--@elvariable id="typesLettreBienvenue" type="java.util.List"--%>
			<%--@elvariable id="print" type="ch.vd.unireg.documentfiscal.ImprimerAutreDocumentFiscalView"--%>
			<form:form modelAttribute="print" action="print.do" id="newDocForm" onsubmit="return NewAutreDoc.print();">

				<form:hidden path="noEntreprise"/>

				<unireg:nextRowClass reset="1"/>
				<table id="newDocParams">
					<tr class="<unireg:nextRowClass/>">
						<td style="width: 20%;"><label for="newTypeDoc"><fmt:message key="label.autre.document.fiscal.type.nouvel.envoi"/>&nbsp;:</label></td>
						<td>
							<form:select path="typeDocument" id="newTypeDoc" onchange="NewAutreDoc.onChangeSelectedType(this.options[this.selectedIndex].value);">
								<form:option value=""/>
								<form:options items="${typesDocument}"/>
							</form:select>
						</td>
					</tr>

					<!-- les spécificités du document d'autorisation de radiation -->
					<tr class="doc-autrad" style="display:none;">
						<td colspan="2">
							<div>
								<table style="border: 0; margin-left: 15%; width: 85%;">
									<tr>
										<td width="20%;"><fmt:message key="label.autre.document.fiscal.date.requisition.radiation.rc"/>&nbsp;:</td>
										<td width="30%;">
											<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
												<jsp:param name="path" value="dateReference" />
												<jsp:param name="id" value="dateReferenceAutorisationRadiation" />
												<jsp:param name="mandatory" value="true" />
											</jsp:include>
										</td>
										<td>
											<c:choose>
												<c:when test="${isRadieeRCOuDissoute}">
													<button type="button" name="print" disabled="disabled" class="dead"><fmt:message key="label.bouton.imprimer"/></button>
													<span class="error error_icon" style="padding-left: 2em; margin-left: 2em;">
														<fmt:message key="label.entreprise.deja.radiee.ou.dissoute"/>
													</span>
												</c:when>
												<c:otherwise>
													<button type="submit" name="print"><fmt:message key="label.bouton.imprimer"/></button>
												</c:otherwise>
											</c:choose>
										</td>
									</tr>
								</table>
							</div>
						</td>
					</tr>

					<!-- les spécificités du document de demande de bilan final -->
					<tr class="doc-bilfin" style="display: none;">
						<td colspan="2">
							<div>
								<table style="border: 0; margin-left: 15%; width: 85%;">
									<tr>
										<td width="20%;"><fmt:message key="label.autre.document.fiscal.date.requisition.radiation.rc"/>&nbsp;:</td>
										<td width="30%;">
											<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
												<jsp:param name="path" value="dateReference" />
												<jsp:param name="id" value="dateReferenceBilanFinal" />
												<jsp:param name="mandatory" value="true" />
											</jsp:include>
										</td>
										<td rowspan="2">
											<c:choose>
												<c:when test="${isRadieeRCOuDissoute}">
													<button type="button" name="print" disabled="disabled" class="dead"><fmt:message key="label.bouton.imprimer"/></button>
													<span class="error error_icon" style="padding-left: 2em; margin-left: 2em;">
														<fmt:message key="label.entreprise.deja.radiee.ou.dissoute"/>
													</span>
												</c:when>
												<c:otherwise>
													<button type="submit" name="print"><fmt:message key="label.bouton.imprimer"/></button>
												</c:otherwise>
											</c:choose>
										</td>
									</tr>
									<tr>
										<td><fmt:message key="label.periode.fiscale"/>&nbsp;:</td>
										<td>
											<form:input path="periodeFiscale"/>
											<span class="mandatory">*</span>
											<form:errors cssClass="error" path="periodeFiscale"/>
										</td>
									</tr>
								</table>
							</div>
						</td>
					</tr>

					<!-- les spécificités du document d'information de liquidation -->
					<tr class="doc-letliq" style="display: none;">
						<td colspan="2">
							<div style="margin-left: 58%; width: 42%; height: 2em;">
								<button type="submit" name="print"><fmt:message key="label.bouton.imprimer"/></button>
							</div>
						</td>
					</tr>

					<!-- les spécificités du document de lettre de bienvenue -->
					<tr class="doc-bienvenue" style="display: none;">
						<td colspan="2">
							<div>
								<table style="border: 0; margin-left: 15%; width: 85%;">
									<tr>
										<td width="20%;"><fmt:message key="label.autre.document.fiscal.delai.retour"/>&nbsp;:</td>
										<td width="30%;">
											<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
												<jsp:param name="path" value="delaiRetour" />
												<jsp:param name="id" value="dateDelaiRetour" />
												<jsp:param name="mandatory" value="true" />
											</jsp:include>
										</td>
										<td rowspan="2">
											<c:choose>
												<c:when test="${isRadieeRCOuDissoute}">
													<button type="button" name="print" disabled="disabled" class="dead"><fmt:message key="label.bouton.imprimer"/></button>
													<span class="error error_icon" style="padding-left: 2em; margin-left: 2em;">
														<fmt:message key="label.entreprise.deja.radiee.ou.dissoute"/>
													</span>
												</c:when>
												<c:otherwise>
													<button type="submit" name="print"><fmt:message key="label.bouton.imprimer"/></button>
												</c:otherwise>
											</c:choose>
										</td>
									</tr>
									<tr>
										<td><fmt:message key="label.types.lettre.bienvenue"/>&nbsp;:</td>
										<td>
											<form:select path="typeLettreBienvenue">
												<form:option value=""/>
												<form:options items="${typesLettreBienvenue}"/>
											</form:select>
											<span class="mandatory">*</span>
											<form:errors cssClass="error" path="typeLettreBienvenue"/>
										</td>
									</tr>
								</table>
							</div>
						</td>
					</tr>

				</table>

			</form:form>

			<script type="application/javascript">
				const NewAutreDoc = {
					onChangeSelectedType: function(type) {
						const baseTable = $('#newDocParams');
						baseTable.find('tr.doc-autrad, tr.doc-bilfin, tr.doc-letliq, tr.doc-bienvenue').hide();
						baseTable.find(':input').not('#newTypeDoc, :button').prop("disabled", true);
						switch (type) {
						case 'AUTORISATION_RADIATION':
							baseTable.find('tr.doc-autrad :input:not(.dead)').prop("disabled", false);
							baseTable.find('tr.doc-autrad').show();
							break;
						case 'DEMANDE_BILAN_FINAL':
							baseTable.find('tr.doc-bilfin :input:not(.dead)').prop("disabled", false);
							baseTable.find('tr.doc-bilfin').show();
							break;
						case 'LETTRE_TYPE_INFORMATION_LIQUIDATION':
							baseTable.find('tr.doc-letliq :input').prop("disabled", false);
							baseTable.find('tr.doc-letliq').show();
							break;
						case 'LETTRE_BIENVENUE':
							baseTable.find('tr.doc-bienvenue :input').prop("disabled", false);
							baseTable.find('tr.doc-bienvenue').show();
							break;
						}
						baseTable.find('tr:visible').removeClass("odd even");
						baseTable.find('tr:visible:even').addClass("odd");
						baseTable.find('tr:visible:odd').addClass("even");
					},

					print: function() {
						const form = $('#newDocForm');
						form.find(":button").prop("disabled", true);                        // le bouton "imprimer"
						form.find(':input').prop("readOnly", true);                         // les textes en read-only
						form.find('select option:not(:selected)').prop("disabled", true);   // la combo box (en éliminant les options non-choisies)
						return true;
					}
				};

				$(function() {
					const select = $('#newTypeDoc')[0];
					NewAutreDoc.onChangeSelectedType(select.options[select.selectedIndex].value);
				});
			</script>

		</fieldset>


		<!-- Debut Caracteristiques autres documents avec suivi -->
		<fieldset>
			<legend><span><fmt:message key="label.autres.documents.fiscaux.suivis" /></span></legend>

			<c:if test="${not empty docsAvecSuivi.docs}">
				<%--@elvariable id="doc" type="ch.vd.unireg.documentfiscal.AutreDocumentFiscalView"--%>
				<display:table name="docsAvecSuivi.docs" id="doc" class="display" decorator="ch.vd.unireg.decorator.TableEntityDecorator" requestURI="/autresdocs/edit-list.do" sort="list">
					<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.aucun.doc.trouve" /></span></display:setProperty>
					<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.doc.trouve" /></span></display:setProperty>
					<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.docs.trouves" /></span></display:setProperty>
					<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.docs.trouves" /></span></display:setProperty>

					<display:column sortable="true" titleKey="label.autre.document.fiscal.type.document">
						${doc.libelleTypeDocument}
					</display:column>
					<display:column sortable="true" titleKey="label.autre.document.fiscal.soustype.document">
						${doc.libelleSousType}
					</display:column>
					<display:column sortable ="true" titleKey="label.date.emission" sortProperty="dateEnvoi">
						<unireg:regdate regdate="${doc.dateEnvoi}"/>
						<c:if test="${doc.urlVisualisationExterneDocument != null}">
							&nbsp;<a href="#" class="pdf" title="Visualisation du courrier" onclick="VisuExterneDoc.openWindow('${doc.urlVisualisationExterneDocument}');">&nbsp;</a>
						</c:if>
					</display:column>
					<display:column sortable ="true" titleKey="label.date.delai.accorde" sortProperty="delaiAccorde">
						<unireg:regdate regdate="${doc.delaiAccorde}"/>
					</display:column>
					<display:column sortable ="true" titleKey="label.date.rappel" sortProperty="dateRappel">
						<unireg:regdate regdate="${doc.dateRappel}"/>
						<c:if test="${doc.urlVisualisationExterneRappel != null}">
							&nbsp;<a href="#" class="pdf" title="Visualisation du courrier de rappel" onclick="VisuExterneDoc.openWindow('${doc.urlVisualisationExterneRappel}');">&nbsp;</a>
						</c:if>
					</display:column>
					<display:column sortable ="true" titleKey="label.date.retour" sortProperty="dateRetour">
						<unireg:regdate regdate="${doc.dateRetour}"/>
					</display:column>
					<display:column sortable ="true" titleKey="label.etat.avancement" >
						<fmt:message key="option.etat.avancement.f.${doc.etat}" />
						<c:if test="${doc.dateRetour != null}">
							<c:if test="${doc.sourceRetour == null}">
								(<fmt:message key="option.source.quittancement.UNKNOWN" />)
							</c:if>
							<c:if test="${doc.sourceRetour != null}">
								(<fmt:message key="option.source.quittancement.${doc.sourceRetour}" />)
							</c:if>
						</c:if>
					</display:column>
					<display:column style="action">
						<authz:authorize access="hasAnyRole('GEST_QUIT_LETTRE_BIENVENUE')">
							<c:if test="${!doc.annule}">
								<unireg:linkTo name="" action="/autresdocs/editer.do" method="get" params="{id:${doc.id}}" title="Editer le document fiscal" link_class="edit"/>
							</c:if>
						</authz:authorize>
						<authz:authorize access="hasAnyRole('GEST_QUIT_LETTRE_BIENVENUE')">
							<c:if test="${doc.annule}">
								<unireg:linkTo name="" title="Désannuler le document fiscal" action="/autresdocs/desannuler.do" method="post" params="{id:${doc.id}}"
								               confirm="Voulez-vous vraiment désannuler ce document fiscal ?" link_class="undelete" />
							</c:if>
						</authz:authorize>
					</display:column>
					<display:setProperty name="paging.banner.all_items_found" value=""/>
					<display:setProperty name="paging.banner.one_item_found" value=""/>
				</display:table>
			</c:if>
		</fieldset>
		<!-- Fin Caracteristiques autres documents avec suivi -->

		<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
		<fieldset>
			<legend><span><fmt:message key="label.autres.documents.fiscaux.non.suivis"/></span></legend>

			<c:choose>
				<c:when test="${not empty documents}">
					<display:table name="${documents}" id="docFiscal" htmlId="docFiscalSansSuivi" requestURI="edit-list.do" class="display" decorator="ch.vd.unireg.decorator.TableEntityDecorator" sort="list">
						<display:column sortable="true" titleKey="label.autre.document.fiscal.type.document">
							${docFiscal.libelleTypeDocument}
						</display:column>
						<display:column sortable="true" titleKey="label.autre.document.fiscal.soustype.document">
							${docFiscal.libelleSousType}
						</display:column>
						<display:column sortable ="true" titleKey="label.date.envoi" sortProperty="dateEnvoi">
							<unireg:regdate regdate="${docFiscal.dateEnvoi}"/>
							<c:choose>
								<c:when test="${docFiscal.urlVisualisationExterneDocument != null}">
									&nbsp;<a href="#" class="pdf" title="Visualisation du courrier" onclick="VisuExterneDoc.openWindow('${docFiscal.urlVisualisationExterneDocument}');">&nbsp;</a>
								</c:when>
								<c:when test="${docFiscal.avecCopieConformeEnvoi}">
									&nbsp;<a href="../autresdocs/copie-conforme-envoi.do?idDoc=${docFiscal.id}&url_memorize=false" class="pdf" id="print-envoi-${docFiscal.id}" title="Courrier envoyé" onclick="Link.tempSwap(this, '#disabled-print-envoi-${docFiscal.id}');">&nbsp;</a>
									<span class="pdf-grayed" id="disabled-print-envoi-${docFiscal.id}" style="display: none;">&nbsp;</span>
								</c:when>
							</c:choose>
						</display:column>
					</display:table>
				</c:when>
				<c:otherwise>
					<div style="padding-top: 1em; padding-bottom: 1em; padding-left: 1ex;">
						<span style="font-style: italic;"><fmt:message key="label.autre.document.fiscal.liste.vide"/></span>
					</div>
				</c:otherwise>
			</c:choose>

		</fieldset>

		<!-- Debut Bouton -->
		<unireg:buttonTo name="Retour" action="/tiers/visu.do" params="{id:${pmId}}" method="GET"/>
		<!-- Fin Bouton -->

	</tiles:put>

</tiles:insert>
