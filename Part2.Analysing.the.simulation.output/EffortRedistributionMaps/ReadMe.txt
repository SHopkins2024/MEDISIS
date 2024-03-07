In ISIS-Fish, effort is redistributed evenly across all cells within a métier zone.
When a spatial temporal closure occurs, if there are métier zone cells that fall outside of the
spatial closure area, the same amount of effort as would occur across all métier zone cells are 
redistributed of the cells falling outside fo the closure area.

To assess this restribution spatially (i.e., at the métier cell level) in the current MEDISIS model version, 
it is nescessary to force the effort distribution patterns outside of ISIS-Fish. To achieve this, we use the 
cell and zone definition files from ISIS-Fish ('MailleDefinition.csv' and 'ZonesDefinition.csv') as well as the
cleaned up nominal effort data series compiled in the earliear steps 
('EffNomtot_6.4_without-0s'and 'EffNomtot_6.4_true_0s').

Run the codes in the following order:
1) MEDISIS_Fishing_Effort_Distribution_DataPrep_andZoneDefs.Rmd 
- returns zone defintion figures and cell values (Appendix B, Figs. B1 -B23 & Appendix C., Figs. C1 - C.2)
2) MEDISIS_Fishing_Effort_Distribution.Rmd
- returns effort redistribution maps for spatial closure scenarios (Appendix F, Figs. F.2 - F.6) 
