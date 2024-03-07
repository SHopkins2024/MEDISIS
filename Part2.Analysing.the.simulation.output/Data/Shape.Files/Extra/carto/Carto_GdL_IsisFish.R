# packeges requis
if(require("maps")==F){install.packages('maps')}
if(require("maptools")==F){install.packages('maptools')}
if(require("rgdal")==F){install.packages('rgdal')}
if(require("sp")==F){install.packages('sp')}
if(require("raster")==F){install.packages('raster')}
if(require("rgeos")==F){install.packages('rgeos')}
library(maps)
library(maptools)
library(rgdal)
library(sp)
library(raster)
library(rgeos)

# chemin des shapefiles
path_shape <- 'C:/Users/shopkins/Documents/RevisedPECHALO_directory/Zone_closure_spatial_analyses/Shapefiles/Extra/carto/shapefile/'
par(mar=c(7,8,7,7))
# Fond de carte
plot(0,xlim=c(2.5,6.5),ylim=c(41.5,44),xlab='',ylab='',axes=F) 
#europe <- readOGR(paste(path_shape,'/Europe_coastline_shapefile/Europe_coastline_poly.shp',sep=''))
#europeWGS84 <- spTransform(europe, CRS("+init=epsg:4326")) #passage en WGS84
#matbbox <- matrix(c(2,7,41,44.5),byrow=T,ncol=2,
#               dimnames = list(c('x','y'),c('min','max')))
#bboxProj <- Spatial(matbbox,proj4string = CRS("+init=epsg:4326"))

library(raster)
library(rgeos)
# Fonction permettant de découper la zone d'étude à l'intérieur d'un shape file, nécessite au préalable
# une bbox que l'on créer avec une matrice transformée en objet spatial 'Spatial(matrix, proj4string)'
# cf : étape du dessus
#gClip <- function(shp, bb){
#  if(class(bb) == "matrix") b_poly <- as(extent(as.vector(t(bb))), "SpatialPolygons")
#  else b_poly <- as(extent(bb), "SpatialPolygons")
#  gIntersection(shp, b_poly, byid = T)
#}
#GdL <- gClip(europeWGS84, bboxProj)
load(paste(path_shape,'/Europe_coastline_shapefile/GdL.Rdata',sep=''))
plot(GdL,add=T,col='grey40')


# grid creation
xmin <- 3.0
xmax <- 6.0
ymin <- 42
ymax <- 43.5

mat_grid <- matrix(c(xmin,xmax,ymin,ymax),byrow=T,ncol=2,
                   dimnames = list(c('x','y'),c('min','max')))
mat_grid_proj <- Spatial(mat_grid,proj4string = CRS("+init=epsg:4326"))

grille <- gridlines(mat_grid_proj, easts = seq(3.0,6.0,0.05), norths = seq(42,43.5,0.05))


plot(grille, add=T, col='red')

# ajout des shapefiles intéressants
  # FRA
FRA <- readOGR(paste(path_shape,'FRA/FRAs_WGS84.shp',sep=''))
plot(FRA[FRA$COUNTRY_EN=='France',],add=T,col=adjustcolor('orange',0.5), border=adjustcolor('orange',0.5))
  # AMP
AMP <- readOGR(paste(path_shape,'AMP/National_MPAs_WGS84.shp',sep=''))
plot(AMP[AMP$COUNTRY_EN=='France' & AMP$NAME_EN != "Port-Cros",],add=T,
     col=adjustcolor('green',0.5) , border=adjustcolor('green',0.5))
  # Fermes
eol_leucate <- readOGR(paste(path_shape,'éoliennes Leucate/Leucate_éoliennes_ferme pilote.shp',sep=''))
plot(eol_leucate,add=T,col=adjustcolor('blue',0.5),border=adjustcolor('blue',0.5))
  # cable + éolienne
files_zip <- list.files(paste(path_shape,'ZIP Eoliennes et Raccordement maritime',sep=''),pattern='.shp')
zip1 <- spTransform(readOGR(paste(path_shape,'ZIP Eoliennes et Raccordement maritime/',files_zip[1],sep='')),CRS("+init=epsg:4326"))
zip2 <- spTransform(readOGR(paste(path_shape,'ZIP Eoliennes et Raccordement maritime/',files_zip[2],sep='')),CRS("+init=epsg:4326"))
zip3 <- spTransform(readOGR(paste(path_shape,'ZIP Eoliennes et Raccordement maritime/',files_zip[3],sep='')),CRS("+init=epsg:4326"))
plot(zip1,add=T,col=adjustcolor('red4',0.5),border=adjustcolor('red4',0.5))
plot(zip2,add=T,col=adjustcolor('red4',0.5),border=adjustcolor('red4',0.5))
plot(zip3,add=T,col=adjustcolor('red4',0.5),border=adjustcolor('red4',0.5))


# mise en forme
map.scale(x=5.4, y=41.65,cex=0.8)
axis(1,at = seq(3.0,6.0,0.5), parse(text=degreeLabelsEW(seq(3.0,6.0,0.5))))
axis(2,las=1,seq(41.5,44.0,0.5), parse(text=degreeLabelsNS(seq(41.5,44.0,0.5))))
axis(3,at = seq(3.0,6.0,0.5), parse(text=degreeLabelsEW(seq(3.0,6.0,0.5))))
axis(4,las=1,seq(41.5,44.0,0.5), parse(text=degreeLabelsNS(seq(41.5,44.0,0.5))))
box()
