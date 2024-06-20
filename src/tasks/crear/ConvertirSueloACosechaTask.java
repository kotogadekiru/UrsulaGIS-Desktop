package tasks.crear;

import java.io.IOException;

import org.geotools.data.FeatureReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;

import dao.config.Cultivo;
import dao.cosecha.CosechaItem;
import dao.cosecha.CosechaLabor;
import dao.suelo.Suelo;
import dao.suelo.SueloItem;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import tasks.ProcessMapTask;
import utils.ProyectionConstants;

public class ConvertirSueloACosechaTask extends ProcessMapTask<CosechaItem,CosechaLabor> {
	Double mmLluviaEstimados=0.0;
	Suelo suelo=null;

	public ConvertirSueloACosechaTask(CosechaLabor cosechaLabor,Suelo _suelo, Double _mm){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		super(cosechaLabor);
		mmLluviaEstimados = _mm;
		suelo=_suelo;

	}

	public void doProcess() throws IOException {		
		//labor.setContorno(suelo.getContorno());
		featureNumber = 0;
		featureCount = this.suelo.outCollection.getCount();
		FeatureReader<SimpleFeatureType, SimpleFeature> reader = this.suelo.outCollection.reader();
		while(reader.hasNext()){
			SimpleFeature f = reader.next();
			SueloItem si = suelo.constructFeatureContainerStandar(f,true);
			CosechaItem ci = estimarCosecha(si);
			ci.setId(si.getId());
			ci.setGeometry(si.getGeometry());
			ci.setElevacion(10.0);

			SimpleFeature nf=ci.getFeature(labor.featureBuilder);

			System.out.println("insertando "+nf);
			boolean ret = labor.outCollection.add(nf);
			featureNumber++;
			updateProgress(featureNumber, featureCount);
			if(!ret){
				System.out.println("no se pudo agregar la feature "+f);
			}
		}

		reader.close();
		labor.constructClasificador();

		runLater(this.getItemsList());
		updateProgress(0, featureCount);
	}
	
	private CosechaItem estimarCosecha(SueloItem si) {
		CosechaItem ci = new CosechaItem();
		Cultivo cultivo = labor.getCultivo();
		Double mmDispo = si.getAguaPerfil()+this.mmLluviaEstimados;
		Double mmTn = cultivo.getAbsAgua();
		Double kgAgua = mmTn!=0?mmDispo/mmTn:0;
		//TODO si hay demaciada agua bajar el rinde; mas de 1800mm/2000mm
		Double encharcamiento = Math.min(2000, mmDispo)/2000;
		
		Double rindeAgua=(encharcamiento>0.5?(1-encharcamiento):1)*kgAgua;
		System.out.println("mmDispo="+mmDispo+" => rindeAgua= "+rindeAgua);
	
		String observaciones ="agua";
		
		Double kgP=Suelo.getKgPHa(si);
		Double rindeP = cultivo.getAbsP()!=0?kgP/cultivo.getAbsP():0;
		System.out.println("kgP="+kgP+" => rindeP= "+rindeP);
		if(rindeAgua>rindeP) {//factor limitante es fosforo
			observaciones="fosforo";
		}
		
		Double kgN= Suelo.getKgNHa(si) 
				+(cultivo.isEstival()?2/3:1/3) * suelo.getKgNOrganicoHa(si);
		Double rindeN = cultivo.getAbsN()!=0?kgN/cultivo.getAbsN():0;
		System.out.println("kgN="+kgN+" => rindeN: "+rindeN);
		
	
		Double rindeLimitante = Math.min(
				Math.min(rindeAgua,rindeP),
				rindeN);
		ci.setRindeTnHa(
				rindeLimitante
				);
		if(rindeLimitante == rindeAgua) {
			if(rindeN<rindeP) {
				ci.setObservaciones("rinde limitado por agua "+mmDispo+"mm despues por kgN y finalmente por kgP");
			}else {
				ci.setObservaciones("rinde limitado por agua "+mmDispo+"mm despues por kgP y finalmente por kgN");
			}
		} else if(rindeLimitante == rindeN) {
			if(rindeAgua<rindeP) {
				Double difRinde = rindeAgua-rindeN;
				Double kgNFaltan = difRinde*cultivo.getAbsN();
				ci.setObservaciones("rinde limitado por kgN faltan "+kgNFaltan+" kgN despues por Agua y finalmente por kgP");
			}else {
				Double difRinde = rindeP-rindeN;
				Double kgNFaltan = difRinde*cultivo.getAbsN();
				ci.setObservaciones("rinde limitado porkgN faltan "+kgNFaltan+" kgN despues por kgP y finalmente por Agua");
			}
		}else if(rindeLimitante == rindeP) {
			if(rindeAgua<rindeN) {
				Double difRinde = rindeAgua-rindeP;
				Double kgPFaltan = difRinde*cultivo.getAbsP();
				ci.setObservaciones("rinde limitado por kgP faltan "+kgPFaltan+" kgP despues por Agua y finalmente por kgN");
			}else {
				Double difRinde = rindeN-rindeP;
				Double kgPFaltan = difRinde*cultivo.getAbsP();
				ci.setObservaciones("rinde limitado por kgP faltan "+kgPFaltan+" kgP despues por kgN y finalmente por Agua");
			}
		}

		return ci;
		
	}
		
	@Override
	protected ExtrudedPolygon getPathTooltip(Geometry poly,	CosechaItem cosechaItem,ExtrudedPolygon  renderablePolygon) {
		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		String tooltipText = CrearCosechaMapTask.buildTooltipText(cosechaItem, area);
		tooltipText+="\n "+cosechaItem.getObservaciones();
		return super.getExtrudedPolygonFromGeom(poly, cosechaItem,tooltipText,renderablePolygon);

	}

	protected int getAmountMin() {
		return 3;
	}

	protected int gerAmountMax() {
		return 15;
	}
}// fin del task