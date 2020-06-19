package net.schwarzbaer.java.tools.bumpmappingtest;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import net.schwarzbaer.image.bumpmapping.BumpMapping.Normal;
import net.schwarzbaer.image.bumpmapping.BumpMapping.NormalXY;
import net.schwarzbaer.image.bumpmapping.ExtraNormalFunction;
import net.schwarzbaer.image.bumpmapping.NormalFunction;
import net.schwarzbaer.image.bumpmapping.NormalFunction.InterpolatingNormalMap;
import net.schwarzbaer.image.bumpmapping.NormalFunction.NormalMap;
import net.schwarzbaer.image.bumpmapping.NormalFunction.NormalMap.NormalMapData;
import net.schwarzbaer.image.bumpmapping.NormalFunction.Polar.RotatedProfile;
import net.schwarzbaer.image.bumpmapping.ProfileXY;

enum NormalFunctions {
		Simple(() ->
			new NormalFunction.Polar.Simple((w, r) ->{
				Normal n;
				if      (30<r && r<40) n = new Normal(-1,0,1).normalize().rotateZ(w);
				else if (60<r && r<70) n = new Normal(1,0,1).normalize().rotateZ(w);
				else                   n = new Normal(0,0,1);
				return n;
			})
		),
		RotaryCtrl(() -> {
			int radius = 100;
			Normal vFace  = new Normal( 0,0,1);
			Normal vInner = new Normal(-1,0,1);
			Normal vOuter = new Normal( 1,0,3);
			return new NormalFunction.Polar.Simple((w, r) -> {
				Normal n;
				int r1 = radius/2;
				int r2 = radius/2+5;
				int r3 = radius-15;
				int r4 = radius;
				if      (r1  <r && r<=r2  ) n = vInner;
				else if (r2  <r && r<=r2+2) n = Normal.blend(r, r2  , r2+2, vInner, vFace);
				//else if (r1-2<r && r<=r1  ) n = Vector3D.blend(r, r1-2, r1  , vFace, vInner);
				else if (r3-2<r && r<=r3  ) n = Normal.blend(r, r3-2, r3  , vFace, vOuter);
				else if (r3  <r && r<=r4  ) n = vOuter;
				//else if (r4  <r && r<=r4+2) n = Vector3D.blend(r, r4  , r4+2, vOuter, vFace);
				else                        n = vFace;
				return n.normalize().rotateZ(w);
			});
		}),
		RotaryCtrl_CNF(() -> {
			int radius = 100;
			int r1 = radius/2;
			int r2 = radius/2+5;
			int r3 = radius-15;
			int r4 = radius;
			NormalXY vFace  = new NormalXY(0,1);
			NormalXY vInner = ProfileXY.Constant.computeNormal(r1, r2, 0, 5);
			NormalXY vOuter = ProfileXY.Constant.computeNormal(r3, r4, 5, 0);
			return new RotatedProfile(
				new ProfileXY.Group(
					new ProfileXY.Constant   (0, r1),
					new ProfileXY.Constant   (r1  , r2  , 0, 5),
					new ProfileXY.LinearBlend(r2  , r2+2, vInner, vFace),
					new ProfileXY.Constant   (r2+2, r3-2),
					new ProfileXY.LinearBlend(r3-2, r3  , vFace, vOuter),
					new ProfileXY.Constant   (r3  , r4  , 5, 0),
					new ProfileXY.Constant   (r4  , Double.POSITIVE_INFINITY)
				)
			);
		}),
		RotaryCtrl_CNF2(() ->
			createRotaryCtrlProfile(100,5,15,2,5)
		),
		RotaryCtrl_CNF2_Extras(() -> {
			double radius = 55;
			double tr = 2;
			double ramp = 1;
			double lineHeight = 2;
			
			NormalXY vFace = new NormalXY(0,1);
			NormalXY vRamp = ProfileXY.Constant.computeNormal(0,ramp, 0,lineHeight);
			
			ProfileXY.Group profileBigLine = new ProfileXY.Group(
				new ProfileXY.Constant   (0.0     , 0.5     ),
				new ProfileXY.RoundBlend (0.5     , 1.5     , vFace, vRamp),
				new ProfileXY.Constant   (1.5     , 1.5+ramp, 0,lineHeight),
				new ProfileXY.RoundBlend (1.5+ramp, 2.5+ramp, vRamp, vFace)
			);
			ProfileXY.Group profileSmallLine = new ProfileXY.Group(
					new ProfileXY.Constant   (0.0       , 0.2       ),
					new ProfileXY.RoundBlend (0.2       , 0.5       , vFace, vRamp),
					new ProfileXY.Constant   (0.5       , 0.5+ramp/2, 0,lineHeight/2),
					new ProfileXY.RoundBlend (0.5+ramp/2, 0.8+ramp/2, vRamp, vFace)
				);
			
			double minRB = radius/2+5+tr*2+profileBigLine  .maxR;
			double maxRB = radius-tr*2+10 -profileBigLine  .maxR; maxRB = radius;
			double minRS = radius/2+5+tr*2+profileSmallLine.maxR; minRS = radius-17;
			double maxRS = radius-tr*2+10 -profileSmallLine.maxR; maxRS = radius+5;
			ExtraNormalFunction.Polar.LineOnX bigLine   = new ExtraNormalFunction.Polar.LineOnX(minRB, maxRB, profileBigLine   );
			ExtraNormalFunction.Polar.LineOnX smallLine = new ExtraNormalFunction.Polar.LineOnX(minRS, maxRS, profileSmallLine );
			double angleIn  = 75.0;
			double angleOut = 0;
			return createRotaryCtrlProfile(radius,5,15,tr,5).setExtras(
				new ExtraNormalFunction.Polar.Group(
					new ExtraNormalFunction.Polar.Stencil(
						(w1,r1)->r1<=radius,
						new ExtraNormalFunction.Polar.Group(
							new ExtraNormalFunction.Polar.Rotated(angleIn    , bigLine)
//									new ExtraNormalFunctionPolar.Rotated(angleIn+=45, smallLine),
//									new ExtraNormalFunctionPolar.Rotated(angleIn+=45, smallLine),
//									new ExtraNormalFunctionPolar.Rotated(angleIn+=45, smallLine),
//									new ExtraNormalFunctionPolar.Rotated(angleIn+=45, smallLine),
//									new ExtraNormalFunctionPolar.Rotated(angleIn+=45, smallLine),
//									new ExtraNormalFunctionPolar.Rotated(angleIn+=45, smallLine),
//									new ExtraNormalFunctionPolar.Rotated(angleIn+=45, smallLine)
						)
					),
					new ExtraNormalFunction.Polar.Stencil(
							(w2,r2)->r2>radius,
							new ExtraNormalFunction.Polar.Group(
								new ExtraNormalFunction.Polar.Rotated(angleOut    , smallLine),
								new ExtraNormalFunction.Polar.Rotated(angleOut+=45, smallLine),
								new ExtraNormalFunction.Polar.Rotated(angleOut+=45, smallLine),
								new ExtraNormalFunction.Polar.Rotated(angleOut+=45, smallLine),
								new ExtraNormalFunction.Polar.Rotated(angleOut+=45, smallLine),
								new ExtraNormalFunction.Polar.Rotated(angleOut+=45, smallLine),
								new ExtraNormalFunction.Polar.Rotated(angleOut+=45, smallLine),
								new ExtraNormalFunction.Polar.Rotated(angleOut+=45, smallLine)
							)
						)
				)
			);
		}),
		Spirale(() ->
			new NormalFunction.Polar.Simple((w, r) -> {
				double pAmpl = 60;
				double rAmpl = r + w*pAmpl/Math.PI;
				double pSpir = 5;
				double rSpir = r + w*pSpir/Math.PI;
				double ampl = Math.sin(rAmpl/pAmpl*Math.PI); ampl *= ampl;
				double spir = Math.sin(rSpir/pSpir*Math.PI) * ampl*ampl;
				double f = 0.9; // 0.7; // 1; // 1/Math.sqrt(2); 
				return new Normal(f*spir,0,Math.sqrt(1-f*f*spir*spir)).normalize().rotateZ(w);
			})
		),
		HemiSphere(() -> {
			Normal vFace  = new Normal( 0,0,1);
			double radius = 100;
			double transition = 3;
			return new NormalFunction.Polar.Simple((w, r) ->{
				Normal n;
				if (r < radius)
					n = new Normal(r,0,Math.sqrt(radius*radius-r*r)).normalize().rotateZ(w);
				else {
					if (r<radius+transition)
						n = Normal.blend(r, radius, radius+transition, new Normal(1,0,0), vFace).normalize().rotateZ(w);
					else
						n = vFace;
				}
				return n;
			});
		}),
		HemiSphere2(() -> {
			Normal vFace  = new Normal( 0,0,1);
			return new NormalFunction.Simple((x,y,width,height)->{
				Normal n = getBubbleNormal(x-width/2.0,y-height/2.0, 100, 3, vFace, false);
				if (n == null) return vFace;
				return n;
			});
		}),
		HemiSphere_CNF_linear(() -> {
			double r1 = 100;
			double r2 = r1+3;
			return new RotatedProfile( new ProfileXY.Group(
				new ProfileXY.LinearBlend( 0, r1, new NormalXY(0,1), new NormalXY(1,0)),
				new ProfileXY.LinearBlend(r1, r2, new NormalXY(1,0), new NormalXY(0,1)),
				new ProfileXY.Constant   (r2, Double.POSITIVE_INFINITY)
			));
		}),
		HemiSphere_CNF_round(() -> {
			double r1 = 100;
			double r2 = r1+3;
			return new RotatedProfile( new ProfileXY.Group(
				new ProfileXY.RoundBlend( 0, r1, new NormalXY(0,1), new NormalXY(1,0)),
				new ProfileXY.RoundBlend(r1, r2, new NormalXY(1,0), new NormalXY(0,1)),
				new ProfileXY.Constant  (r2, Double.POSITIVE_INFINITY)
			));
		}),
		HemiSphere_CNF2_linear(() -> {
			double r1 = 60;
			double r2 = 120;
			NormalXY face = new NormalXY(0,1);
			NormalXY mid  = new NormalXY(1,0);
			return new RotatedProfile( new ProfileXY.Group(
				new ProfileXY.LinearBlend( 0, r1, face, mid),
				new ProfileXY.LinearBlend(r1, r2, mid, face),
				new ProfileXY.Constant   (r2, Double.POSITIVE_INFINITY)
			));
		}),
		HemiSphere_CNF2_round(() -> {
			double r1 = 60;
			double r2 = 120;
			NormalXY face = new NormalXY(0,1);
			NormalXY mid  = new NormalXY(1,0);
			return new RotatedProfile( new ProfileXY.Group(
				new ProfileXY.RoundBlend( 0, r1, face, mid),
				new ProfileXY.RoundBlend(r1, r2, mid, face),
				new ProfileXY.Constant  (r2, Double.POSITIVE_INFINITY)
			));
		}),
		HemiSphere_CNF3_linear(() -> {
			double r1 = 60;
			double r2 = 120;
			NormalXY face = new NormalXY(0,1);
			NormalXY mid  = new NormalXY(1,2).normalize();
			return new RotatedProfile( new ProfileXY.Group(
				new ProfileXY.LinearBlend( 0, r1, face, mid),
				new ProfileXY.LinearBlend(r1, r2, mid, face),
				new ProfileXY.Constant   (r2, Double.POSITIVE_INFINITY)
			));
		}),
		HemiSphere_CNF3_round(() -> {
			double r1 = 60;
			double r2 = 120;
			NormalXY face = new NormalXY(0,1);
			NormalXY mid  = new NormalXY(1,2).normalize();
			return new RotatedProfile( new ProfileXY.Group(
				new ProfileXY.RoundBlend( 0, r1, face, mid),
				new ProfileXY.RoundBlend(r1, r2, mid, face),
				new ProfileXY.Constant  (r2, Double.POSITIVE_INFINITY)
			));
		}),
		HemiSphere_CNF4_round(() -> {
			NormalXY face = new NormalXY( 0  ,1  );
			NormalXY mid1 = new NormalXY(-0.1,1  ).normalize();
			NormalXY mid2 = new NormalXY( 1  ,0.1).normalize();
			return new RotatedProfile( new ProfileXY.Group(
				new ProfileXY.RoundBlend(  0, 40, face, mid1),
				new ProfileXY.RoundBlend( 40, 80, mid1, mid2),
				new ProfileXY.RoundBlend( 80,120, mid2, face),
				new ProfileXY.Constant  (120, Double.POSITIVE_INFINITY)
			));
		}),
		HemiSphereBubblesQ(() ->
			new BubbleRaster(100,3,(raster,p)->{
				p.x = Math.round(p.x/raster)*raster;
				p.y = Math.round(p.y/raster)*raster;
			})
		),
		HemiSphereBubblesT(() ->
			new BubbleRaster(100,3,(raster,p) -> {
				double f3 = p.y/(raster*Math.sin(Math.PI/3));
				double f2 = p.x/raster-f3/2;
				double f1 = 1-f2-f3;
				
				double f3F = f3-Math.floor(f3);
				double f2F = f2-Math.floor(f2);
				double f1F = f1-Math.floor(f1);
				
				if (f1F+f2F+f3F<1.5) {
					if (f1F> f2F && f1F> f3F) { f1 = Math.ceil(f1); f2 = Math.floor(f2); f3 = Math.floor(f3); }
					if (f2F>=f1F && f2F> f3F) { f2 = Math.ceil(f2); f1 = Math.floor(f1); f3 = Math.floor(f3); }
					if (f3F>=f1F && f3F>=f2F) { f3 = Math.ceil(f3); f1 = Math.floor(f1); f2 = Math.floor(f2); }
				} else {
					if (f1F< f2F && f1F< f3F) { f1 = Math.floor(f1); f2 = Math.ceil(f2); f3 = Math.ceil(f3); }
					if (f2F<=f1F && f2F< f3F) { f2 = Math.floor(f2); f1 = Math.ceil(f1); f3 = Math.ceil(f3); }
					if (f3F<=f1F && f3F<=f2F) { f3 = Math.floor(f3); f1 = Math.ceil(f1); f2 = Math.ceil(f2); }
				}
				
				p.y = f3*(raster*Math.sin(Math.PI/3));
				p.x = raster*(f2+f3/2);
			})
		),
		Noise(new Supplier<NormalFunction>() {
			@Override
			public NormalFunction get() {
				int width = 400;
				int height = 300;
				Random rnd = new Random();
				NormalMapData normalMap = new NormalMap.NormalMapData(width,height);
				for (int x1=0; x1<width; ++x1)
					for (int y1=0; y1<height; ++y1)
						normalMap.set(x1,y1,new Normal(rnd.nextDouble(),0,1).normalize().rotateZ(rnd.nextDouble()*Math.PI*2));
				return new NormalMap(normalMap,false);
			}
		}),
		
		NoiseHeight1(() -> NormalMap.createFromHeightMap(new NoiseHeightMap(400, 300, 1594263594,   10).heightMap,0)),
		NoiseHeight2(() -> NormalMap.createFromHeightMap(new NoiseHeightMap(400, 300, 1594263594,    5).heightMap,0)),
		NoiseHeight3(() -> NormalMap.createFromHeightMap(new NoiseHeightMap(400, 300, 1594263594,    2).heightMap,0)),
		NoiseHeight4(() -> NormalMap.createFromHeightMap(new NoiseHeightMap(400, 300, 1594263594,    1).heightMap,0)),
		NoiseHeight5(() -> NormalMap.createFromHeightMap(new NoiseHeightMap(400, 300, 1594263594, 0.5f).heightMap,0)),
		NoiseHeight6(() -> NormalMap.createFromHeightMap(new NoiseHeightMap(400, 300, 1594263594, 0.5f).heightMap,0.25f)),
		NoiseHeight7(() -> NormalMap.createFromHeightMap(new NoiseHeightMap(400, 300, 1594263594, 0.5f).heightMap,0.5f )),
		NoiseHeight8(() -> NormalMap.createFromHeightMap(new NoiseHeightMap(400, 300, 1594263594, 0.5f).heightMap,0.75f)),
		
		RandomHeight1(() -> NormalMap.createFromHeightMap(new RandomHeightMap(400, 300, 1594263594, 0.25f).heightMap,0)),
		RandomHeight2(() -> NormalMap.createFromHeightMap(new RandomHeightMap(400, 300, 1594263594, 0.50f).heightMap,0)),
		RandomHeight3(() -> NormalMap.createFromHeightMap(new RandomHeightMap(400, 300, 1594263594, 0.75f).heightMap,0)),
		RandomHeight4(() -> NormalMap.createFromHeightMap(new RandomHeightMap(400, 300, 1594263594, 1.00f).heightMap,0)),
		RandomHeight5(() -> NormalMap.createFromHeightMap(new RandomHeightMap(400, 300, 1594263594, 2.00f).heightMap,0)),
		
		RandomHeightNColor1(() -> { RandomHeightMap map = new RandomHeightMap(400, 300, 1594263594, 0.25f, Color.BLUE, Color.ORANGE); return NormalMap.createFromHeightMap(map.heightMap, map.colorMap, 0.5, true); }),
		RandomHeightNColor2(() -> { RandomHeightMap map = new RandomHeightMap(400, 300, 1594263594, 0.50f, Color.BLUE, Color.ORANGE); return NormalMap.createFromHeightMap(map.heightMap, map.colorMap, 0.5, true); }),
		RandomHeightNColor3(() -> { RandomHeightMap map = new RandomHeightMap(400, 300, 1594263594, 0.75f, Color.BLUE, Color.ORANGE); return NormalMap.createFromHeightMap(map.heightMap, map.colorMap, 0.5, true); }),
		RandomHeightNColor4(() -> { RandomHeightMap map = new RandomHeightMap(400, 300, 1594263594, 1.00f, Color.BLUE, Color.ORANGE); return NormalMap.createFromHeightMap(map.heightMap, map.colorMap, 0.5, true); }),
		RandomHeightNColor5(() -> { RandomHeightMap map = new RandomHeightMap(400, 300, 1594263594, 2.00f, Color.BLUE, Color.ORANGE); return NormalMap.createFromHeightMap(map.heightMap, map.colorMap, 0.5, true); }),
		
		RandomHeightNColor1Int(() -> { RandomHeightMap map = new RandomHeightMap(400, 300, 1594263594, 0.25f, Color.BLUE, Color.ORANGE); return NormalMap.createFromHeightMap(map.heightMap, map.colorMap, 0.5, InterpolatingNormalMap::new, true); }),
		RandomHeightNColor2Int(() -> { RandomHeightMap map = new RandomHeightMap(400, 300, 1594263594, 0.50f, Color.BLUE, Color.ORANGE); return NormalMap.createFromHeightMap(map.heightMap, map.colorMap, 0.5, InterpolatingNormalMap::new, true); }),
		RandomHeightNColor3Int(() -> { RandomHeightMap map = new RandomHeightMap(400, 300, 1594263594, 0.75f, Color.BLUE, Color.ORANGE); return NormalMap.createFromHeightMap(map.heightMap, map.colorMap, 0.5, InterpolatingNormalMap::new, true); }),
		RandomHeightNColor4Int(() -> { RandomHeightMap map = new RandomHeightMap(400, 300, 1594263594, 1.00f, Color.BLUE, Color.ORANGE); return NormalMap.createFromHeightMap(map.heightMap, map.colorMap, 0.5, InterpolatingNormalMap::new, true); }),
		RandomHeightNColor5Int(() -> { RandomHeightMap map = new RandomHeightMap(400, 300, 1594263594, 2.00f, Color.BLUE, Color.ORANGE); return NormalMap.createFromHeightMap(map.heightMap, map.colorMap, 0.5, InterpolatingNormalMap::new, true); }),
		
		Spikes(() -> {
			int size = 21;
			double maxSpikeHeight = 50;
			int spikeSize = 20;
			int width = size*spikeSize;
			Color[] defaultColors = new Color[] { Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.PINK, Color.MAGENTA, Color.LIGHT_GRAY };
			
			Random rnd = new Random();
			Color[][] colors = new Color[size][size];
			double[][] spikes = new double[size][size];
			boolean[][] isCone = new boolean[size][size];
			int colorRange = size-6;
			for (int x1=0; x1<size; x1++)
				for (int y1=0; y1<size; y1++) {
					isCone[x1][y1] = rnd.nextBoolean();
					spikes[x1][y1] = rnd.nextDouble();
					if (x1>=(size-colorRange)/2 && x1<(size+colorRange)/2 && y1>=(size-colorRange)/2 && y1<(size+colorRange)/2)
						colors[x1][y1] = defaultColors[Math.abs(rnd.nextInt())%defaultColors.length];
					else colors[x1][y1] = null;
				}
			
			return new NormalFunction.Simple((x_, y_, width_, height_) -> {
				int x2 = (int) Math.round(x_-(width_ /2-width/2));
				int y2 = (int) Math.round(y_-(height_/2-width/2));
				if (x2<0 || x2>=width || y2<0 || y2>=width)
					return new Normal(0,0,1);
				
				int xS = x2%spikeSize;
				int yS = y2%spikeSize;
				Color color = colors[x2/spikeSize][y2/spikeSize];
				double spikeHeight = spikes[x2/spikeSize][y2/spikeSize]*maxSpikeHeight;
				
				if (isCone[x2/spikeSize][y2/spikeSize]) {
					
					double m = (spikeSize-1)*0.5;
					double w = Math.atan2(yS-m, xS-m);
					double r = Math.sqrt((yS-m)*(yS-m)+(xS-m)*(xS-m));
					if (r>m)
						return new Normal(0,0,1,color);
					else
						return new Normal( spikeHeight,0,spikeSize, color ).rotateZ(w).normalize();
					
				} else {
					
					if (xS==0 || yS==0)
						return new Normal(0,0,1,color);
					//if (xS==yS || xS+yS==spikeSize)
					//	return new Vector3D( 0,0,1);
					
					boolean leftOrTop = xS+yS<spikeSize;
					boolean leftOrBottom = xS<yS;
					
					if (leftOrTop) {
						if (leftOrBottom) return new Normal( -spikeHeight,0,spikeSize, color ).normalize(); // left
						else              return new Normal( 0,-spikeHeight,spikeSize, color ).normalize(); // top
					} else {
						if (leftOrBottom) return new Normal( 0,spikeHeight,spikeSize, color ).normalize(); // bottom
						else              return new Normal( spikeHeight,0,spikeSize, color ).normalize(); // right
					}
					
				}
			});

		}),
		;
		Supplier<NormalFunction> createNormalFunction;
		NormalFunctions(Supplier<NormalFunction> createNormalFunction) {
			this.createNormalFunction = createNormalFunction;
		}
		
		private static RotatedProfile createRotaryCtrlProfile(double radius, double innerRing, double outerRing, double transition, double height) {
			double r1 = radius/2;
			double r2 = radius/2+innerRing;
			double r3 = radius-outerRing;
			double r4 = radius;
			double tr = transition;
			NormalXY vFace  = new NormalXY(0,1);
			NormalXY vInner = ProfileXY.Constant.computeNormal(r1+tr, r2   , 0, height);
			NormalXY vOuter = ProfileXY.Constant.computeNormal(r3   , r4-tr, height, 0);
			NormalXY vHorizOutside = new NormalXY( 1,0);
			NormalXY vHorizInside  = new NormalXY(-1,0);
			
			return new RotatedProfile(
				new ProfileXY.Group(
					new ProfileXY.Constant  (   0.0, r1-tr ),
					new ProfileXY.RoundBlend(r1-tr , r1    , vFace,vHorizOutside),
					new ProfileXY.RoundBlend(r1    , r1+tr , vHorizInside,vInner),
					new ProfileXY.Constant  (r1+tr , r2    , 0, height),
					new ProfileXY.RoundBlend(r2    , r2+tr , vInner, vFace),
					new ProfileXY.Constant  (r2+tr , r3-tr ),
					new ProfileXY.RoundBlend(r3-tr , r3    , vFace, vOuter),
					new ProfileXY.Constant  (r3    , r4-tr , height, 0),
					new ProfileXY.RoundBlend(r4-tr , r4    , vOuter,vHorizOutside),
					new ProfileXY.RoundBlend(r4    , r4+tr , vHorizInside,vFace),
					new ProfileXY.Constant  (r4+tr , Double.POSITIVE_INFINITY)
				)
			).setColorizer((w,r)->{
				if (r<r1 || r>r4) return Color.GREEN;
				return null;
			});
		}
		
		private static Normal getBubbleNormal(double xCenter, double yCenter, double radius, double transition, Normal face, boolean inverted) {
			double r = Math.sqrt(xCenter*xCenter+yCenter*yCenter);
			
			if (r > radius+transition)
				return null;
			
			double w = Math.atan2(yCenter,xCenter);
			
			if (r < radius)
				return new Normal(inverted?-r:r,0,Math.sqrt(radius*radius-r*r)).normalize().rotateZ(w);
			else
				return Normal.blend(r, radius, radius+transition, new Normal(inverted?-1:1,0,0), face).normalize().rotateZ(w);
		}

		private static class BubbleRaster implements NormalFunction {
			
			private Normal vFace;
			
			private double raster;
			private double radius;
			private double transition;
			private double radiusB;
			private double transitionB;
			
			private BiConsumer<Double,Point2D.Double> getRasterPoint;
			private Point2D.Double rasterPoint;
		
			private BubbleRaster(double radius, double transition, BiConsumer<Double,Point2D.Double> getRasterPoint) {
				this.getRasterPoint = getRasterPoint;
				
				int n = 8;
				this.radius = radius;
				this.transition = transition;
				raster = (radius+transition)/(n-0.5)+0.001;
				transitionB = 2.1;
				radiusB = raster/2-transitionB;
				
				rasterPoint = new Point2D.Double();
				vFace = new Normal( 0,0,1);
			}
			@Override
			public void forceNormalCreation(boolean force) {}
		
			@Override public Normal getNormal(double x, double y, double width, double height) {
				Normal n;
				double xC = x-width/2.0;
				double yC = y-height/2.0;
				
				n = getBubbleNormal(xC,yC, radius, transition, vFace, false);
				if (n!=null) return n;
				
				//double xM = Math.round(xC/raster)*raster;
				//double yM = Math.round(yC/raster)*raster;
				rasterPoint.x = xC;
				rasterPoint.y = yC;
				getRasterPoint.accept(raster,rasterPoint);
				double xM = rasterPoint.x;
				double yM = rasterPoint.y;
				
				if (Math.sqrt(xM*xM+yM*yM) < radius+transition+radiusB+transitionB)
					return vFace;
				
				n = getBubbleNormal(xC-xM,yC-yM, radiusB, transitionB, vFace, false);
				if (n == null)
					return vFace;
				
				return n;
			}
		}

		private static class NoiseHeightMap {
			
			private Random rnd;
			private float[][] heightMap = null;
			@SuppressWarnings("unused")
			private Color[][] colorMap  = null;
			
			NoiseHeightMap(int width, int height, long seed, float maxHeight) {
				rnd = new Random(seed);
				heightMap = new float[width][height];
				for (int x=0; x<width; x++)
					for (int y=0; y<height; y++)
						heightMap[x][y] = rnd.nextFloat()*maxHeight;
			}
			@SuppressWarnings("unused")
			NoiseHeightMap(int width, int height, long seed, float maxHeight, Color min, Color max) {
				this(width, height, seed, maxHeight);
				colorMap = RandomHeightMap.getColorMap(heightMap, min, max);
			}
		}

		private static class RandomHeightMap {
			
			private Random rnd;
			private float[][] heightMap = null;
			private Color[][] colorMap  = null;
			private float variance;
			
			RandomHeightMap(int width, int height, long seed, float variance) {
				this.variance = variance;
				rnd = new Random(seed);
				heightMap = new float[width][height];
				for (float[] col:heightMap) Arrays.fill(col, Float.NaN);
				heightMap[      0][       0] = rnd.nextFloat()*Math.min(width,height);
				heightMap[width-1][       0] = rnd.nextFloat()*Math.min(width,height);
				heightMap[      0][height-1] = rnd.nextFloat()*Math.min(width,height);
				heightMap[width-1][height-1] = rnd.nextFloat()*Math.min(width,height);
				setHeight(0,0,width-1,height-1);
			}
			
			RandomHeightMap(int width, int height, long seed, float variance, Color min, Color max) {
				this(width, height, seed, variance);
				colorMap = getColorMap(heightMap, min, max);
			}
		
			static Color[][] getColorMap(float[][] heightMap, Color min, Color max) {
				int width = heightMap.length;
				int height = heightMap[0].length; 
				float minH = heightMap[0][0];
				float maxH = minH;
				for (float[] col:heightMap)
					for (float h:col) {
						minH = Math.min(h, minH);
						maxH = Math.max(h, maxH);
					}
				Color[][] colorMap = new Color[width][height];
				for (int x=0; x<width; x++) {
					for (int y=0; y<height; y++) {
						float f = (heightMap[x][y]-minH)/(maxH-minH);
						colorMap[x][y] = new Color(
							Math.round(max.getRed  ()*f + min.getRed  ()*(1-f)),
							Math.round(max.getGreen()*f + min.getGreen()*(1-f)),
							Math.round(max.getBlue ()*f + min.getBlue ()*(1-f))
						);
					}
				}
				return colorMap;
			}
		
			private void setHeight(int minX, int minY, int maxX, int maxY) {
				if (minX==maxX || minY==maxY || (minX+1==maxX && minY+1==maxY)) return;
				
				int midX = (maxX+minX)/2;
				int midY = (maxY+minY)/2;
				//midX = Math.round( (maxX+minX)/2f + (maxX-minX)/3f * (rnd.nextFloat()-0.5f) );
				//midY = Math.round( (maxY+minY)/2f + (maxY-minY)/3f * (rnd.nextFloat()-0.5f) );
				//midX = Math.max(minX, Math.min(midX, maxX));
				//midY = Math.max(minY, Math.min(midY, maxY));
				
				Point xy = new Point(minX,minY);
				Point xm = new Point(minX,midY);
				Point xY = new Point(minX,maxY);
				Point Xy = new Point(maxX,minY);
				Point Xm = new Point(maxX,midY);
				Point XY = new Point(maxX,maxY);
				Point my = new Point(midX,minY);
				Point mY = new Point(midX,maxY);
				
				setMid(my, maxX-minX, xy, Xy );
				setMid(mY, maxX-minX, xY, XY );
				setMid(xm, maxY-minY, xy, xY );
				setMid(Xm, maxY-minY, Xy, XY );
				setMid(new Point(midX,midY), Math.min(maxY-minY,maxX-minX), my, mY, xm, Xm );
				
				setHeight(minX, minY, midX, midY);
				setHeight(midX, minY, maxX, midY);
				setHeight(minX, midY, midX, maxY);
				setHeight(midX, midY, maxX, maxY);
			}
		
			private void setMid(Point p, float length, Point... points) {
				if (Float.isNaN(heightMap[p.x][p.y]) && points.length>0) {
					float sum = 0;
					for (Point p1:points) sum += heightMap[p1.x][p1.y];
					heightMap[p.x][p.y] = sum/points.length + (rnd.nextFloat()-0.5f)*length*variance;
				}
			}
		}
	}