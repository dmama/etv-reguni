<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<%@ taglib uri="http://www.unireg.com/uniregTagLib" prefix="unireg" %>

<c:if test="${command.forPrint}">
	<c:set var="templateUrl" value="/WEB-INF/jsp/templates/templateDialog.jsp" />
</c:if>
<c:if test="${!command.forPrint}">
	<c:set var="templateUrl" value="/WEB-INF/jsp/templates/template.jsp" />
</c:if>

<tiles:insert template="${templateUrl}">

	<tiles:put name="head">
		<%-- Code spécifique IE pour faire tourner l'année de 90° à gauche --%>
		<!--[if IE]>
		<style type="text/css">
			.timeline td.annee div {
				position: relative;
				width: 1.5em;
			}

			.timeline td.annee div span {
				position: absolute;
				top: -1.2em;
				left: 2px;
				filter: progid:DXImageTransform.Microsoft.BasicImage(rotation = 3);
			}
		</style>
		<![endif]-->
		<style type="text/css">
			.timeline td {
				border-color: white;
			}
		</style>
	</tiles:put>

	<tiles:put name="title">Vue chronologique des fors fiscaux et des assujettissements</tiles:put>

	<tiles:put name="body">
	
		<c:if test="${command.forPrint}">
				<h1><c:out value="${command.title}"/></h1>
				<h3><c:out value="${command.description}"/></h3>
		</c:if>
		<c:if test="${!command.forPrint}">
			<p style="text-align: center; color:red;">Attention: cette page est une aide pour les développeurs de Unireg. Il ne s'agit en aucune manière d'une page officielle, et aucun support n'est prévu.</p>

			<a href="<c:url value="/tiers/visu.do?id=" /><c:out value="${command.tiersId}" />" >&lt;&lt; Retour à la visualisation</a>

			<unireg:bandeauTiers numero="${command.tiersId}" showValidation="false" showEvenementsCivils="false" showLinks="false"/>

		</c:if>


		<c:if test="${fn:length(command.exceptions) > 0}">
			<table class="validation_error" cellspacing="0" cellpadding="0" border="0">
				<tr><td class="heading iepngfix">Les assujettissements n'ont pas tous pu être calculés pour les raisons suivantes:</td></tr>
				<tr><td class="details"><ul>
				<c:forEach var="err" items="${command.exceptions}">
					<li class="err"><fmt:message key="label.validation.erreur"/>: <c:out value="${err.message}"/></li>
				</c:forEach>
				</ul></td></tr>
			</table>
		</c:if>

		<div id="legend">
			<table>
				<tr><td class="principal_vd">Lausanne</td><td>For fiscal principal dans le canton de Vaud</td></tr>
				<tr><td class="principal_hc">Berne</td><td>For fiscal principal hors canton</td></tr>
				<tr><td class="principal_hs">Bruxelles</td><td>For fiscal principal hors suisse</td></tr>
			</table>
		</div>

		<form action="timeline.do" method="get">
			<span id="timeline_header">Affichage des :
				<input type="hidden" name="id" value="${command.tiersId}">

				<input type="checkbox" id="checkForsGestion" onclick="$('#showForsGestion').val($(this).is(':checked')); $(this).closest('form').submit();" <c:if test="${command.showForsGestion}"> checked</c:if>/>
				<label for="checkForsGestion">Fors de gestion</label>
				<input type="hidden" id="showForsGestion" name="showForsGestion" value="${command.showForsGestion}"/>

				<input type="checkbox" id="checkAssujettissements" onclick="$('#showAssujettissements').val($(this).is(':checked')); $(this).closest('form').submit();" <c:if test="${command.showAssujettissements}"> checked</c:if>/>
				<label for="checkAssujettissements">Assujettissements</label>
				<input type="hidden" id="showAssujettissements" name="showAssujettissements" value="${command.showAssujettissements}"/>

				<input type="checkbox" id="checkPeriodesImposition" onclick="$('#showPeriodesImposition').val($(this).is(':checked')); $(this).closest('form').submit();" <c:if test="${command.showPeriodesImposition}"> checked</c:if>/>
				<label for="checkPeriodesImposition">Périodes d'imposition</label>
				<input type="hidden" id="showPeriodesImposition" name="showPeriodesImposition" value="${command.showPeriodesImposition}"/>
			</span>
		</form>

		<table class="timeline">
			<tr>
				<th colspan="2">Période</th>
				<th colspan="2">Fors Principaux</th>
				<th colspan="<c:out value="${command.table.forsSecondairesSize}"/>">Fors Secondaires</th>
				<c:if test="${command.showForsGestion}"><th>Fors de Gestion</th></c:if>
				<c:if test="${command.showAssujettissements}"><th>Assujettissements</th></c:if>
				<c:if test="${command.showPeriodesImposition}"><th>Périodes d'imposition</th></c:if>
			</tr>

			<tr class="invisibleBorder" />

			<c:forEach var="ligne" varStatus="status" items="${command.table.rows}" >
				<tr>

					<%-- les périodes --%>
					<c:if test="${ligne.periode.multiYears}">
						<td class="periodeFull" colspan="2">
							<unireg:regdate regdate="${ligne.periode.dateDebut}" format="dd.MM.yyyy"/><br/>
							<unireg:regdate regdate="${ligne.periode.dateFin}" format="dd.MM.yyyy" defaultValue="..."/>
						</td>
					</c:if>
					<c:if test="${!ligne.periode.multiYears}">
						<td class="periode">
							<unireg:regdate regdate="${ligne.periode.dateDebut}" format="dd.MM"/><br/>
							<unireg:regdate regdate="${ligne.periode.dateFin}" format="dd.MM"/>
						</td>
						<c:choose>
							<c:when test="${ligne.periode.yearSpan == 1}">
								<td class="annee">
									<div><span><c:out value="${ligne.periode.anneeLabel}"/></span></div>
								</td>
							</c:when>
							<c:when test="${ligne.periode.yearSpan > 0}">
								<td class="annee" rowspan="<c:out value="${ligne.periode.yearSpan}" />">
									<div><span><c:out value="${ligne.periode.anneeLabel}"/></span></div>
								</td>
							</c:when>
							<c:when test="${ligne.periode.yearSpan == 0}">
								<%-- rien à mettre, le rowspan est automatiquement rempli --%>
							</c:when>
						</c:choose>
					</c:if>

					<td class="invisibleBorder" />

					<%-- fors principaux --%>
					<c:choose>
						<c:when test="${ligne.forPrincipal.filler}">			
							<td class="filler" />
						</c:when>
						<c:when test="${!ligne.forPrincipal.span && !ligne.forPrincipal.filler}">
							<c:set var="fp" value="${ligne.forPrincipal.range}" />
							<c:choose>
								<c:when test="${fp.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD'}">			
									<td class="principal_vd tooltip_cell" id="ffp-${fp.id}" rowspan="<c:out value="${ligne.forPrincipal.longueurAffichage}" />">
									    <unireg:commune ofs="${fp.numeroOfsAutoriteFiscale}" displayProperty="nomMinuscule" date="${fp.dateDebut}"/>
										<div id="ffp-${fp.id}-tooltip" style="display:none;">
										    For fiscal principal <b>#${fp.id}</b><br/>
										    Ouverture : <b><unireg:date date="${fp.dateDebut}"/></b> - <b><fmt:message key="option.motif.ouverture.${fp.motifOuverture}"/></b><br/>
										    Fermeture : <b><unireg:date date="${fp.dateFin}"/></b><c:if test="${fp.motifFermeture != null}"> - <b><fmt:message key="option.motif.fermeture.${fp.motifFermeture}"/></b></c:if><br/>
										    Motif de rattachement : <b><fmt:message key="option.rattachement.${fp.motifRattachement}"/></b><br/>
										    Mode d'imposition : <b><fmt:message key="option.mode.imposition.${fp.modeImposition}"/></b>
										</div>
									</td>
								</c:when>
								<c:when test="${fp.typeAutoriteFiscale == 'COMMUNE_HC'}">
									<td class="principal_hc tooltip_cell" id="ffp-${fp.id}" rowspan="<c:out value="${ligne.forPrincipal.longueurAffichage}" />">
                                        <unireg:commune ofs="${fp.numeroOfsAutoriteFiscale}" displayProperty="nomMinuscule" date="${fp.dateDebut}"/>
                                        (<unireg:commune ofs="${fp.numeroOfsAutoriteFiscale}" displayProperty="sigleCanton" date="${fp.dateDebut}"/>)
										<div id="ffp-${fp.id}-tooltip" style="display:none;">
										    For fiscal principal <b>#${fp.id}</b><br/>
										    Ouverture : <b><unireg:date date="${fp.dateDebut}"/></b> - <b><fmt:message key="option.motif.ouverture.${fp.motifOuverture}"/></b><br/>
										    Fermeture : <b><unireg:date date="${fp.dateFin}"/></b><c:if test="${fp.motifFermeture != null}"> - <b><fmt:message key="option.motif.fermeture.${fp.motifFermeture}"/></b></c:if><br/>
										    Motif de rattachement : <b><fmt:message key="option.rattachement.${fp.motifRattachement}"/></b><br/>
										    Mode d'imposition : <b><fmt:message key="option.mode.imposition.${fp.modeImposition}"/></b>
										</div>
									</td>
								</c:when>
								<c:when test="${fp.typeAutoriteFiscale == 'PAYS_HS'}">
									<td class="principal_hs tooltip_cell" id="ffp-${fp.id}" rowspan="<c:out value="${ligne.forPrincipal.longueurAffichage}" />">
                                        <unireg:infra entityId="${fp.numeroOfsAutoriteFiscale}" entityType="pays" entityPropertyName="nomMinuscule"></unireg:infra>
										<div id="ffp-${fp.id}-tooltip" style="display:none;">
										    For fiscal principal <b>#${fp.id}</b><br/>
										    Ouverture : <b><unireg:date date="${fp.dateDebut}"/></b> - <b><fmt:message key="option.motif.ouverture.${fp.motifOuverture}"/></b><br/>
										    Fermeture : <b><unireg:date date="${fp.dateFin}"/></b><c:if test="${fp.motifFermeture != null}"> - <b><fmt:message key="option.motif.fermeture.${fp.motifFermeture}"/></b></c:if><br/>
										    Motif de rattachement : <b><fmt:message key="option.rattachement.${fp.motifRattachement}"/></b><br/>
										    Mode d'imposition : <b><fmt:message key="option.mode.imposition.${fp.modeImposition}"/></b>
										</div>
									</td>
								</c:when>
							</c:choose>
						</c:when>
						<c:when test="${ligne.forPrincipal.span}">
							<%-- rien à mettre, le rowspan est automatiquement rempli --%> 
						</c:when>
					</c:choose>
					
					<%-- fors secondaires --%>
					<c:forEach var="fs" items="${ligne.forsSecondaires}" >
						<c:choose>
							<c:when test="${fs.filler}">			
								<td class="filler" />
							</c:when>
							<c:when test="${!fs.span && !fs.filler}">
								<td class="secondaire tooltip_cell" id="ffs-${fs.range.id}" rowspan="<c:out value="${fs.longueurAffichage}" />">
                                    <unireg:commune ofs="${fs.range.numeroOfsAutoriteFiscale}" displayProperty="nomMinuscule" date="${fs.range.dateDebut}"/>
                                    <div id="ffs-${fs.range.id}-tooltip" style="display:none;">
                                        For fiscal secondaire <b>#${fs.range.id}</b><br/>
                                        Ouverture : <b><unireg:date date="${fs.range.dateDebut}"/></b><c:if test="${fs.range.motifOuverture != null}"> - <b><fmt:message key="option.motif.ouverture.${fs.range.motifOuverture}"/></b></c:if><br/>
                                        Fermeture : <b><unireg:date date="${fs.range.dateFin}"/></b><c:if test="${fs.range.motifFermeture != null}"> - <b><fmt:message key="option.motif.fermeture.${fs.range.motifFermeture}"/></b></c:if><br/>
                                        Motif de rattachement : <b><fmt:message key="option.rattachement.${fs.range.motifRattachement}"/></b><br/>
                                    </div>
								</td>
							</c:when>
							<c:when test="${fs.span}">
								<%-- rien à mettre, le rowspan est automatiquement rempli --%> 
							</c:when>
						</c:choose>
					</c:forEach>

					<%-- fors de gestion --%>
					<c:if test="${command.showForsGestion}">
						<c:choose>
							<c:when test="${ligne.forGestion.filler}">
								<td class="filler" />
							</c:when>
							<c:when test="${!ligne.forGestion.span && !ligne.forGestion.filler}">
								<c:set var="fg" value="${ligne.forGestion.range}" />
								<td class="gestion tooltip_cell" id="fg-<unireg:regdate regdate="${fg.dateDebut}" format="yyyyMMdd"/>" rowspan="<c:out value="${ligne.forGestion.longueurAffichage}" />">
                                    <unireg:commune ofs="${fg.noOfsCommune}" displayProperty="nomMinuscule" date="${fg.dateDebut}"/>
	                                <div id="fg-<unireg:regdate regdate="${fg.dateDebut}" format="yyyyMMdd"/>-tooltip" style="display:none;">
	                                    Ouverture : <b><unireg:date date="${fg.dateDebut}"/></b><br/>
	                                    Fermeture : <b><unireg:date date="${fg.dateFin}"/></b><br/>
	                                    Sous-jacent : <b>for fiscal #${fg.sousjacent.id}</b><br/>
	                                </div>
								</td>
							</c:when>
							<c:when test="${ligne.forGestion.span}">
								<%-- rien à mettre, le rowspan est automatiquement rempli --%>
							</c:when>
						</c:choose>
					</c:if>

					<%-- assujettissements --%>
					<c:if test="${command.showAssujettissements}">
						<c:choose>
							<c:when test="${ligne.assujettissement.filler}">
								<td class="filler" />
							</c:when>
							<c:when test="${!ligne.assujettissement.span && !ligne.assujettissement.filler}">
								<c:set var="a" value="${ligne.assujettissement.range}" />
								<td class="assujettissement tooltip_cell" id="a-<unireg:regdate regdate="${a.dateDebut}" format="yyyyMMdd"/>" rowspan="<c:out value="${ligne.assujettissement.longueurAffichage}" />">
                                    <c:out value="${a.description}" />
	                                <div id="a-<unireg:regdate regdate="${a.dateDebut}" format="yyyyMMdd"/>-tooltip" style="display:none;">
	                                    Début : <b><unireg:date date="${a.dateDebut}"/></b>
	                                    <c:if test="${a.motifFractDebut != null}">
	                                        - <b><fmt:message key="option.motif.ouverture.${a.motifFractDebut}"/></b>
	                                    </c:if>
	                                    <br/>
	                                    Fin : <b><unireg:date date="${a.dateFin}"/></b>
	                                    <c:if test="${a.motifFractFin != null}">
	                                         - <b><fmt:message key="option.motif.fermeture.${a.motifFractFin}"/></b>
	                                    </c:if>
	                                    <br/>
	                                    <c:if test="${a.class.name == 'ch.vd.uniregctb.metier.assujettissement.SourcierPur' || a.class.name == 'ch.vd.uniregctb.metier.assujettissement.SourcierMixte'}">
	                                        Type autorité : <b><fmt:message key="option.type.autorite.fiscale.${a.typeAutoriteFiscale}"/></b>
	                                    </c:if>
	                                </div>
								</td>
							</c:when>
							<c:when test="${ligne.assujettissement.span}">
								<%-- rien à mettre, le rowspan est automatiquement rempli --%>
							</c:when>
						</c:choose>
					</c:if>

					<%-- périodes d'imposition --%>
					<c:if test="${command.showPeriodesImposition}">
						<c:choose>
							<c:when test="${ligne.periodeImposition.filler}">
								<td class="filler" />
							</c:when>
							<c:when test="${!ligne.periodeImposition.span && !ligne.periodeImposition.filler}">
								<c:set var="pi" value="${ligne.periodeImposition.range}" />
								<td class="periodeImposition tooltip_cell" id="pi-<unireg:regdate regdate="${pi.dateDebut}" format="yyyyMMdd"/>" rowspan="<c:out value="${ligne.periodeImposition.longueurAffichage}" />">
	                                <fmt:message key="option.type.contribuable.${pi.typeContribuable}"/> /
	                                <fmt:message key="option.type.document.${pi.typeDocument}"/>
                                    <c:if test="${pi.optionnelle}">
                                        (optionnelle)
                                    </c:if>
                                    <c:if test="${pi.remplaceeParNote}">
                                        (remplacée par note)
                                    </c:if>
                                    <c:if test="${pi.diplomateSuisseSansImmeuble}">
                                        (diplomate suisse sans immeuble)
                                    </c:if>
	                                <div id="pi-<unireg:regdate regdate="${pi.dateDebut}" format="yyyyMMdd"/>-tooltip" style="display:none;">
	                                    Début : <b><unireg:date date="${pi.dateDebut}"/></b><br/>
	                                    Fin : <b><unireg:date date="${pi.dateFin}"/></b><br/>
	                                    Type de contribuable : <b><fmt:message key="option.type.contribuable.${pi.typeContribuable}"/></b><br/>
	                                    Type de document : <b><fmt:message key="option.type.document.${pi.typeDocument}"/></b><br/>
	                                    Qualification : <c:if test="${pi.qualification} != null"><b><fmt:message key="option.qualification.${pi.qualification}"/></b></c:if><br/>
	                                    Adresse de retour : <b><fmt:message key="option.type.adresse.retour.${pi.adresseRetour}"/></b><br/>
	                                    Optionnelle : <b><fmt:message key="option.ouinon.${pi.optionnelle}"/></b><br/>
	                                    Remplacée par note : <b><fmt:message key="option.ouinon.${pi.remplaceeParNote}"/></b>
	                                </div>
								</td>
							</c:when>
							<c:when test="${ligne.periodeImposition.span}">
								<%-- rien à mettre, le rowspan est automatiquement rempli --%>
							</c:when>
						</c:choose>
					</c:if>
					
				</tr>
			</c:forEach>
		</table>

		<c:if test="${!command.forPrint}">
			<a href="<c:url value="/tiers/visu.do?id=" /><c:out value="${command.tiersId}" />" >&lt;&lt; Retour à la visualisation</a>
		</c:if>

		<script type="text/javascript">
			$(function() {
				$('.tooltip_cell').tooltip({
					items: "[id]",
					content: function(response) {
						var id = $(this).attr("id");
						return $('#' + id + '-tooltip').html();
					}
				});
				$("#legend").dialog({title: 'Légende', position: ['right','bottom']});
			});
		</script>

		<![if !IE 6]>
		<script type="text/javascript">
			$(function () {
				$("table.timeline").show('slide', { direction:"left" });
			});
		</script>
		<![endif]-->

	</tiles:put>
	
</tiles:insert>
