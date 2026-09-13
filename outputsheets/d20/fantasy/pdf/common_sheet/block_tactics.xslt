<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
	version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format"
	>
	<xsl:import href="../inc_pagedimensions.xslt"/>
	<!--
====================================
====================================
	TEMPLATE - TACTICAL SHEET
====================================
====================================-->
	<xsl:template match="tactics">
		<!-- BEGIN Tactical Sheet Page -->
		<xsl:if test="count(.//tactical_entry) &gt; 0">
			<fo:page-sequence master-reference="Portrait">
				<xsl:attribute name="font-family"><xsl:value-of select="$PCGenFont"/></xsl:attribute>
				<xsl:call-template name="page.footer"/>
				<fo:flow flow-name="body" font-size="8pt">
					<fo:block font-size="14pt" font-weight="bold" space-after.optimum="2mm" break-before="page">
						Tactical Sheet
					</fo:block>
					<xsl:for-each select="tactical_section">
						<fo:block font-size="11pt" font-weight="bold" space-after.optimum="1mm"
							space-before.optimum="4mm" keep-with-next.within-page="always">
							<xsl:value-of select="title"/>
						</fo:block>
						<fo:table table-layout="fixed" width="100%">
							<fo:table-column column-width="25%"/>
							<fo:table-column column-width="50%"/>
							<fo:table-column column-width="25%"/>
							<fo:table-header>
								<fo:table-row>
									<fo:table-cell padding="1pt" border-bottom="0.5pt solid black">
										<fo:block font-size="7pt" font-weight="bold">When</fo:block>
									</fo:table-cell>
									<fo:table-cell padding="1pt" border-bottom="0.5pt solid black">
										<fo:block font-size="7pt" font-weight="bold">Do</fo:block>
									</fo:table-cell>
									<fo:table-cell padding="1pt" border-bottom="0.5pt solid black">
										<fo:block font-size="7pt" font-weight="bold">Note</fo:block>
									</fo:table-cell>
								</fo:table-row>
							</fo:table-header>
							<fo:table-body>
								<xsl:for-each select="tactical_entry">
									<fo:table-row>
										<fo:table-cell padding="1pt">
											<fo:block font-size="8pt"><xsl:value-of select="trigger"/></fo:block>
										</fo:table-cell>
										<fo:table-cell padding="1pt">
											<fo:block font-size="8pt"><xsl:value-of select="actions"/></fo:block>
										</fo:table-cell>
										<fo:table-cell padding="1pt">
											<fo:block font-size="7pt"><xsl:value-of select="note"/></fo:block>
										</fo:table-cell>
									</fo:table-row>
								</xsl:for-each>
							</fo:table-body>
						</fo:table>
					</xsl:for-each>
				</fo:flow>
			</fo:page-sequence>
		</xsl:if>
		<!-- END Tactical Sheet Page -->
	</xsl:template>
</xsl:stylesheet>
