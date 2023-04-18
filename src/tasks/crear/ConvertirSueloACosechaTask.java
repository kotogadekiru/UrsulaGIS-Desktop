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
		labor.setContorno(suelo.getContorno());
		featureNumber = 0;
		featureCount = this.suelo.outCollection.getCount();
		FeatureReader<SimpleFeatureType, SimpleFeature> reader = this.suelo.outCollection.reader();
		while(reader.hasNext()){
			SimpleFeature f = reader.next();
			SueloItem si = suelo.constructFeatureContainerStandar(f,true);
			CosechaItem ci = estimarCosecha(si);
			ci.setId(si.getId());
			ci.setGeometry(si.getGeometry());


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
		Double kgP=this.suelo.getKgPHa(si);
		Double rindeP = cultivo.getAbsP()!=0?kgP/cultivo.getAbsP():0;
		System.out.println("kgP="+kgP+" => rindeP= "+rindeP);
		Double kgN= this.suelo.getKgNHa(si) 
				+(cultivo.isEstival()?2/3:1/3) * suelo.getKgNOrganicoHa(si);
		Double rindeN = cultivo.getAbsN()!=0?kgN/cultivo.getAbsN():0;
		System.out.println("kgN="+kgN+" => rindeN: "+rindeN);
		ci.setRindeTnHa(
				Math.min(
						Math.min(rindeAgua,rindeP),
						rindeN)
				);
		return ci;
	}
		
	@Override
	protected ExtrudedPolygon getPathTooltip(Geometry poly,	CosechaItem cosechaItem,ExtrudedPolygon  renderablePolygon) {
		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		String tooltipText = CrearCosechaMapTask.buildTooltipText(cosechaItem, area);
		return super.getExtrudedPolygonFromGeom(poly, cosechaItem,tooltipText,renderablePolygon);

	}

	protected int getAmountMin() {
		return 3;
	}

	protected int gerAmountMax() {
		return 15;
	}
}// fin del task