package net.schwarzbaer.java.tools.bumpmappingtest;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.schwarzbaer.gui.BumpmappingSunControl;
import net.schwarzbaer.gui.Canvas;
import net.schwarzbaer.gui.HSColorChooser;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.image.BumpMapping;
import net.schwarzbaer.image.BumpMapping.ExtraNormalFunctionPolar;
import net.schwarzbaer.image.BumpMapping.Indexer;
import net.schwarzbaer.image.BumpMapping.Normal;
import net.schwarzbaer.image.BumpMapping.NormalFunction;
import net.schwarzbaer.image.BumpMapping.NormalXY;
import net.schwarzbaer.image.BumpMapping.OverSampling;
import net.schwarzbaer.image.BumpMapping.ProfileXY;
import net.schwarzbaer.image.BumpMapping.RotatedProfile;
import net.schwarzbaer.image.BumpMapping.Shading;
import net.schwarzbaer.image.BumpMapping.Shading.GUISurfaceShading;
import net.schwarzbaer.image.BumpMapping.Shading.MaterialShading;
import net.schwarzbaer.image.BumpMapping.Shading.MixedShading;
import net.schwarzbaer.image.BumpMapping.Shading.NormalImage;

public class BumpMappingTest {

	public static void main(String[] args) {
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		new BumpMappingTest().createGUI();
		
		//testRotZ(2,1,0,90);
		//testRotZ(2,0,1,90);
		
	}

	@SuppressWarnings("unused")
	private static void testRotZ(double x, double y, double z, double w_degree) {
		Normal n = new Normal(x,y,z);
		System.out.printf(Locale.ENGLISH, "%s.rotateZ(%1.1f°) -> %s%n", n, w_degree, n.rotateZ(w_degree/180*Math.PI));
		System.out.printf(Locale.ENGLISH, "%s.rotateY(%1.1f°) -> %s%n", n, w_degree, n.rotateY(w_degree/180*Math.PI));
	}

	private Normal sun;
	private BumpMapping bumpMapping;
	private ResultView resultView;
	private JPanel optionsPanel;
	private JPanel currentValuePanel=null;
	private StandardMainWindow mainwindow;

	private void createGUI() {
		GridBagConstraints c = new GridBagConstraints();
		mainwindow = new StandardMainWindow("BumpMappingTest");
		
		sun = new Normal(1,-1,2).normalize();
		NormalFunctions initialNormalFunction = NormalFunctions.HemiSphereBubblesT;
		Shadings initialShading = Shadings.Material;
		
		bumpMapping = new BumpMapping(true);
		
		resultView = new ResultView(bumpMapping);
		resultView.setBorder(BorderFactory.createTitledBorder("Result"));
		resultView.setPreferredSize(new Dimension(300,300));
		
		JPanel buttonPanel = new JPanel(new GridLayout(1,0,3,3));
		buttonPanel.add(createButton("Copy"   ,(JButton b)->{copyImageToClipboard(b,resultView.renderScaledImage(1));}));
		buttonPanel.add(createButton("Copy 2x",(JButton b)->{copyImageToClipboard(b,resultView.renderScaledImage(2));}));
		buttonPanel.add(createButton("Copy 4x",(JButton b)->{copyImageToClipboard(b,resultView.renderScaledImage(4));}));
		buttonPanel.add(createButton("Copy 8x",(JButton b)->{copyImageToClipboard(b,resultView.renderScaledImage(8));}));
		
		JPanel resultViewPanel = new JPanel(new BorderLayout(3,3));
		resultViewPanel.add(resultView,BorderLayout.CENTER);
		resultViewPanel.add(buttonPanel,BorderLayout.SOUTH);
		
		JTextField sunOutput = new JTextField(String.format(Locale.ENGLISH, "new Normal( %1.3f, %1.3f, %1.3f )", sun.x,sun.y,sun.z));
		sunOutput.setEditable(false);
		
		BumpmappingSunControl directionControl = new BumpmappingSunControl(sun.x,sun.y,sun.z);
		directionControl.setPreferredSize(new Dimension(300,300));
		directionControl.addValueChangeListener((x,y,z)->{
			sun = new Normal(x,y,z);
			bumpMapping.setSun(x,y,z);
			resultView.repaint();
			sunOutput.setText(String.format(Locale.ENGLISH, "new Normal( %1.3f, %1.3f, %1.3f )", x,y,z));
		});
		
		JPanel rightPanel = new JPanel(new BorderLayout(3,3));
		rightPanel.setBorder(BorderFactory.createTitledBorder("Light"));
		rightPanel.add(directionControl,BorderLayout.CENTER);
		rightPanel.add(sunOutput,BorderLayout.NORTH);
		
		GBC.reset(c);
		GBC.setFill(c, GBC.GridFill.BOTH);
		JPanel selectionPanel = new JPanel(new GridBagLayout());
		GBC.setWeights(c,0,1);
		
		selectionPanel.add(new JLabel("OverSampling: "  ),GBC.setGridPos(c,0,0));
		selectionPanel.add(new JLabel("NormalFunction: "),GBC.setGridPos(c,0,1));
		selectionPanel.add(new JLabel("Shading: "       ),GBC.setGridPos(c,0,2));
		GBC.setWeights(c,1,1);
		selectionPanel.add(createComboBox(OverSampling   .values(), bumpMapping.getOverSampling(), this::setOverSampling  ),GBC.setGridPos(c,1,0));
		selectionPanel.add(createComboBox(NormalFunctions.values(), initialNormalFunction,         this::setNormalFunction),GBC.setGridPos(c,1,1));
		selectionPanel.add(createComboBox(Shadings       .values(), initialShading,                this::setShading       ),GBC.setGridPos(c,1,2));
		
		optionsPanel = new JPanel(new BorderLayout(3,3));
		optionsPanel.setBorder(BorderFactory.createTitledBorder("Options"));
		optionsPanel.add(selectionPanel,BorderLayout.NORTH);
		
		for (Shadings sh:Shadings.values()) {
			sh.valuePanel = new JPanel(new GridBagLayout());
			sh.valuePanel.setBorder(BorderFactory.createTitledBorder("Shading Values"));
			GBC.reset(c);
			switch(sh) {
			case GUISurface: {
				GUISurfaceShading shading = (GUISurfaceShading)sh.shading;
				GBC.setFill(c, GBC.GridFill.HORIZONTAL);
				GBC.setWeights(c,0,0);
				sh.valuePanel.add(new JLabel("Highlight: "),GBC.setGridPos(c,0,0));
				sh.valuePanel.add(new JLabel("Face: "     ),GBC.setGridPos(c,0,1));
				sh.valuePanel.add(new JLabel("Shadow: "   ),GBC.setGridPos(c,0,2));
				GBC.setWeights(c,1,0);
				sh.valuePanel.add(createColorbutton(shading.getHighlightColor(), "Select Highlight Color", shading::setHighlightColor),GBC.setGridPos(c,1,0));
				sh.valuePanel.add(createColorbutton(shading.getFaceColor()     , "Select Face Color"     , shading::setFaceColor     ),GBC.setGridPos(c,1,1));
				sh.valuePanel.add(createColorbutton(shading.getShadowColor()   , "Select Shadow Color"   , shading::setShadowColor   ),GBC.setGridPos(c,1,2));
				GBC.setGridPos(c,0,3);
				GBC.setLineEnd(c);
				GBC.setWeights(c,1,1);
				sh.valuePanel.add(new JLabel(),c);
			} break;
			case Material:
//				private Color diffuseColor;
//				private double minIntensity;
//				private double phongExp;
				MaterialShading shading = (MaterialShading)sh.shading;
				GBC.setFill(c, GBC.GridFill.HORIZONTAL);
				GBC.setWeights(c,0,0);
				sh.valuePanel.add(new JLabel("Material: "            ),GBC.setGridPos(c,0,0));
				sh.valuePanel.add(new JLabel("Ambient Intensity: "   ),GBC.setGridPos(c,0,1));
				sh.valuePanel.add(new JLabel("Phong Exponent: "      ),GBC.setGridPos(c,0,2));
				sh.valuePanel.add(new JLabel("With Reflection: "     ),GBC.setGridPos(c,0,3));
				sh.valuePanel.add(new JLabel("Reflection Intensity: "),GBC.setGridPos(c,0,4));
				GBC.setWeights(c,1,0);
				sh.valuePanel.add(createColorbutton    (shading.getMaterialColor()   , "Select Material Color", shading::setMaterialColor),GBC.setGridPos(c,1,0));
				sh.valuePanel.add(createDoubleTextField(shading.getAmbientIntensity(), shading::setAmbientIntensity),GBC.setGridPos(c,1,1));
				sh.valuePanel.add(createDoubleTextField(shading.getPhongExp()        , shading::setPhongExp        ),GBC.setGridPos(c,1,2));
				sh.valuePanel.add(createCheckBox       (shading.getReflection()      , shading::setReflection      ),GBC.setGridPos(c,1,3));
				sh.valuePanel.add(createDoubleTextField(shading.getReflIntensity()   , shading::setReflIntensity   ),GBC.setGridPos(c,1,4));
				GBC.setGridPos(c,0,5);
				GBC.setLineEnd(c);
				GBC.setWeights(c,1,1);
				sh.valuePanel.add(new JLabel(),c);
				break;
			case NormalImage:
				break;
			case MixedShading:
				break;
			}
		}
		
		setNormalFunction(initialNormalFunction);
		setShading(initialShading);
		
		JPanel contentPane = new JPanel(new BorderLayout(3,3));
		contentPane.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
		contentPane.add(optionsPanel,BorderLayout.WEST);
		contentPane.add(resultViewPanel,BorderLayout.CENTER);
		contentPane.add(rightPanel,BorderLayout.EAST);
		
		mainwindow.startGUI(contentPane);
	}

	private void copyImageToClipboard(JComponent comp, BufferedImage image) {
		new Thread(()->{
			try { SwingUtilities.invokeAndWait(()->comp.setEnabled(false)); }
			catch (InvocationTargetException | InterruptedException e) {}
			
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			Clipboard clipboard = toolkit.getSystemClipboard();
			TransferableImage content = new TransferableImage(image);
			//DataHandler content = new DataHandler(image,"image/x-java-image");
			try { clipboard.setContents(content,null); }
			catch (IllegalStateException e1) { e1.printStackTrace(); }
			
			SwingUtilities.invokeLater(()->comp.setEnabled(true));
		}).start();
	}
	
	private static class TransferableImage implements Transferable {

		private BufferedImage image;
		public TransferableImage(BufferedImage image) { this.image = image; }

		@Override public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if (isDataFlavorSupported(flavor)) return image;
			throw new UnsupportedFlavorException( flavor );
		}

		@Override public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[] { DataFlavor.imageFlavor }; }
		@Override public boolean isDataFlavorSupported(DataFlavor flavor) { return DataFlavor.imageFlavor.equals(flavor); }
		
	}
	
	private JCheckBox createCheckBox(boolean isSelected, Consumer<Boolean> setValue) {
		JCheckBox comp = new JCheckBox();
		comp.setSelected(isSelected);
		if (setValue!=null) comp.addActionListener(e->{
			setValue.accept(comp.isSelected());
			bumpMapping.resetImage();
			resultView.repaint();
		});
		return comp;
	}
	
	@SuppressWarnings("unused")
	private JButton createButton(String title, ActionListener al) {
		JButton comp = new JButton(title);
		if (al!=null) comp.addActionListener(al);
		return comp;
	}
	
	private JButton createButton(String title, Consumer<JButton> action) {
		JButton comp = new JButton(title);
		if (action!=null) comp.addActionListener(e->action.accept(comp));
		return comp;
	}
	
	private JTextField createDoubleTextField(double value, Consumer<Double> setValue) {
		JTextField comp = new JTextField(Double.toString(value));
		Consumer<Double> modifiedSetValue = d->{
			setValue.accept(d);
			bumpMapping.resetImage();
			resultView.repaint();
		};
		Color defaultBG = comp.getBackground();
		comp.addActionListener(e->{ readTextField(comp,modifiedSetValue,defaultBG); });
		comp.addFocusListener(new FocusListener() {
			@Override public void focusLost(FocusEvent e) { readTextField(comp,modifiedSetValue,defaultBG); }
			@Override public void focusGained(FocusEvent e) {}
		});
		return comp;
	}

	private void readTextField(JTextField comp, Consumer<Double> setValue, Color defaultBG) {
		double d = parseDouble(comp.getText());
		if (Double.isNaN(d)) {
			comp.setBackground(Color.RED);
		} else {
			comp.setBackground(defaultBG);
			setValue.accept(d);
		}
	}

	private double parseDouble(String str) {
		try {
			return Double.parseDouble(str);
		} catch (NumberFormatException e) {
			return Double.NaN;
		}
	}

	private JButton createColorbutton(Color initColor, String dialogTitle, Consumer<Color> setcolor) {
		JButton colorbutton = HSColorChooser.createColorbutton(
				initColor, mainwindow, dialogTitle, HSColorChooser.PARENT_CENTER,
				color->{
					setcolor.accept(color);
					bumpMapping.resetImage();
					resultView.repaint();
				}
		);
		colorbutton.setPreferredSize(new Dimension(30,30));
		return colorbutton;
	}
	
	private static class GBC {
		enum GridFill {
			BOTH(GridBagConstraints.BOTH),
			HORIZONTAL(GridBagConstraints.HORIZONTAL),
			H(GridBagConstraints.HORIZONTAL),
			VERTICAL(GridBagConstraints.VERTICAL),
			V(GridBagConstraints.VERTICAL),
			NONE(GridBagConstraints.NONE),
			;
			private int value;
			GridFill(int value) {
				this.value = value;
				
			}
		}

		static void reset(GridBagConstraints c) {
			c.gridx = GridBagConstraints.RELATIVE;
			c.gridy = GridBagConstraints.RELATIVE;
			c.weightx = 0;
			c.weighty = 0;
			c.fill = GridBagConstraints.NONE;
			c.gridwidth = 1;
			c.insets = new Insets(0,0,0,0);
		}

		static GridBagConstraints setWeights(GridBagConstraints c, double weightx, double weighty) {
			c.weightx = weightx;
			c.weighty = weighty;
			return c;
		}

		static GridBagConstraints setGridPos(GridBagConstraints c, int gridx, int gridy) {
			c.gridx = gridx;
			c.gridy = gridy;
			return c;
		}

		static GridBagConstraints setFill(GridBagConstraints c, GridFill fill) {
			c.fill = fill==null?GridBagConstraints.NONE:fill.value;
			return c;
		}

		@SuppressWarnings("unused")
		static GridBagConstraints setInsets(GridBagConstraints c, int top, int left, int bottom, int right) {
			c.insets = new Insets(top, left, bottom, right);
			return c;
		}

		static GridBagConstraints setLineEnd(GridBagConstraints c) {
			c.gridwidth = GridBagConstraints.REMAINDER;
			return c;
		}

		@SuppressWarnings("unused")
		static GridBagConstraints setLineMid(GridBagConstraints c) {
			c.gridwidth = 1;
			return c;
		}

		@SuppressWarnings("unused")
		static GridBagConstraints setGridWidth(GridBagConstraints c, int gridwidth) {
			c.gridwidth = gridwidth;
			return c;
		}
	}
	
	private void setShading(Shadings sh) {
		if (currentValuePanel!=null) optionsPanel.remove(currentValuePanel);
		currentValuePanel = sh.valuePanel;
		optionsPanel.add(sh.valuePanel, BorderLayout.CENTER);
		optionsPanel.revalidate();
		optionsPanel.repaint();
		bumpMapping.setShading(sh.shading);
		bumpMapping.setSun(sun.x,sun.y,sun.z);
		resultView.repaint();
	}

	private void setNormalFunction(NormalFunctions nf) {
		nf.setNormalFunction.accept(bumpMapping);
		resultView.repaint();
	}

	private void setOverSampling(OverSampling os) {
		bumpMapping.setOverSampling(os);
		resultView.repaint();
	}

	private static <T> JComboBox<T> createComboBox(T[] values, T selectedValue, Consumer<T> valueChanged) {
		JComboBox<T> comp = new JComboBox<T>(values);
		comp.setSelectedItem(selectedValue);
		if (valueChanged!=null) comp.addActionListener(e->{
			int i = comp.getSelectedIndex();
			valueChanged.accept(i<0?null:comp.getItemAt(i));
		});
		return comp;
	}
	
	private enum Shadings {
		NormalImage(new NormalImage()),
		GUISurface  (new GUISurfaceShading(new Normal(1,-1,2).normalize(), Color.WHITE,new Color(0xf0f0f0),new Color(0x707070))),
		Material    (new MaterialShading  (new Normal(1,-1,2).normalize(), Color.RED, 0, 40, false, 0)),
		MixedShading(new MixedShading     ((Indexer.Polar)(w,r)->27.5<=r && r<55 ? 0 : 1,Shadings.Material.shading,Shadings.GUISurface.shading)),
		;
		public JPanel valuePanel;
		private final Shading shading;
		Shadings(Shading shading) {
			this.shading = shading;
			valuePanel = null;
		}
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
	
	private enum NormalFunctions {
		Simple(bm->{
			bm.setNormalFunction((NormalFunction.Polar) (w, r) ->{
					Normal n;
					if      (30<r && r<40) n = new Normal(-1,0,1).normalize().rotateZ(w);
					else if (60<r && r<70) n = new Normal(1,0,1).normalize().rotateZ(w);
					else                   n = new Normal(0,0,1);
					return n;
			});
		}),
		RotaryCtrl(bm->{
			int radius = 100;
			Normal vFace  = new Normal( 0,0,1);
			Normal vInner = new Normal(-1,0,1);
			Normal vOuter = new Normal( 1,0,3);
			bm.setNormalFunction((NormalFunction.Polar) (w, r) -> {
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
				}
			);
		}),
		RotaryCtrl_CNF(bm -> {
			int radius = 100;
			int r1 = radius/2;
			int r2 = radius/2+5;
			int r3 = radius-15;
			int r4 = radius;
			NormalXY vFace  = new NormalXY(0,1);
			NormalXY vInner = ProfileXY.Constant.computeNormal(r1, r2, 0, 5);
			NormalXY vOuter = ProfileXY.Constant.computeNormal(r3, r4, 5, 0);
			bm.setNormalFunction(
				new RotatedProfile(
					new ProfileXY.Group(
						new ProfileXY.Constant   (0, r1),
						new ProfileXY.Constant   (r1  , r2  , 0, 5),
						new ProfileXY.LinearBlend(r2  , r2+2, vInner, vFace),
						new ProfileXY.Constant   (r2+2, r3-2),
						new ProfileXY.LinearBlend(r3-2, r3  , vFace, vOuter),
						new ProfileXY.Constant   (r3  , r4  , 5, 0),
						new ProfileXY.Constant   (r4  , Double.POSITIVE_INFINITY)
					)
				)
			);
		}),
		RotaryCtrl_CNF2(bm -> {
			bm.setNormalFunction(
				createRotaryCtrlProfile(100,5,15,2,5)
			);
		}),
		RotaryCtrl_CNF2_Extras(new Consumer<BumpMapping>() {
			@Override
			public void accept(BumpMapping bm) {
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
				ExtraNormalFunctionPolar.LineOnX bigLine   = new ExtraNormalFunctionPolar.LineOnX(minRB, maxRB, profileBigLine   );
				ExtraNormalFunctionPolar.LineOnX smallLine = new ExtraNormalFunctionPolar.LineOnX(minRS, maxRS, profileSmallLine );
				double angleIn  = 75.0;
				double angleOut = 0;
				bm.setNormalFunction(
					createRotaryCtrlProfile(radius,5,15,tr,5).setExtras(
						new ExtraNormalFunctionPolar.Group(
							new ExtraNormalFunctionPolar.Stencil(
								(w,r)->r<=radius,
								new ExtraNormalFunctionPolar.Group(
									new ExtraNormalFunctionPolar.Rotated(angleIn    , bigLine)
//									new ExtraNormalFunctionPolar.Rotated(angleIn+=45, smallLine),
//									new ExtraNormalFunctionPolar.Rotated(angleIn+=45, smallLine),
//									new ExtraNormalFunctionPolar.Rotated(angleIn+=45, smallLine),
//									new ExtraNormalFunctionPolar.Rotated(angleIn+=45, smallLine),
//									new ExtraNormalFunctionPolar.Rotated(angleIn+=45, smallLine),
//									new ExtraNormalFunctionPolar.Rotated(angleIn+=45, smallLine),
//									new ExtraNormalFunctionPolar.Rotated(angleIn+=45, smallLine)
								)
							),
							new ExtraNormalFunctionPolar.Stencil(
									(w,r)->r>radius,
									new ExtraNormalFunctionPolar.Group(
										new ExtraNormalFunctionPolar.Rotated(angleOut    , smallLine),
										new ExtraNormalFunctionPolar.Rotated(angleOut+=45, smallLine),
										new ExtraNormalFunctionPolar.Rotated(angleOut+=45, smallLine),
										new ExtraNormalFunctionPolar.Rotated(angleOut+=45, smallLine),
										new ExtraNormalFunctionPolar.Rotated(angleOut+=45, smallLine),
										new ExtraNormalFunctionPolar.Rotated(angleOut+=45, smallLine),
										new ExtraNormalFunctionPolar.Rotated(angleOut+=45, smallLine),
										new ExtraNormalFunctionPolar.Rotated(angleOut+=45, smallLine)
									)
								)
						)
					)
				);
			}
		}),
		Spirale(bm -> bm.setNormalFunction((NormalFunction.Polar) (w, r) -> {
			double pAmpl = 60;
			double rAmpl = r + w*pAmpl/Math.PI;
			double pSpir = 5;
			double rSpir = r + w*pSpir/Math.PI;
			double ampl = Math.sin(rAmpl/pAmpl*Math.PI); ampl *= ampl;
			double spir = Math.sin(rSpir/pSpir*Math.PI) * ampl*ampl;
			double f = 0.9; // 0.7; // 1; // 1/Math.sqrt(2); 
			return new Normal(f*spir,0,Math.sqrt(1-f*f*spir*spir)).normalize().rotateZ(w);
		})),
		HemiSphere(bm->{
			Normal vFace  = new Normal( 0,0,1);
			double radius = 100;
			double transition = 3;
			bm.setNormalFunction((NormalFunction.Polar) (w, r) ->{
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
		HemiSphere2(bm->{
			Normal vFace  = new Normal( 0,0,1);
			bm.setNormalFunction((x,y,width,height)->{
				Normal n = NormalFunctions.getBubbleNormal(x-width/2.0,y-height/2.0, 100, 3, vFace, false);
				if (n!=null) return n;
				return vFace;
			});
		}),
		HemiSphere_CNF_linear(bm -> {
			double r1 = 100;
			double r2 = r1+3;
			bm.setNormalFunction( new RotatedProfile( new ProfileXY.Group(
				new ProfileXY.LinearBlend( 0, r1, new NormalXY(0,1), new NormalXY(1,0)),
				new ProfileXY.LinearBlend(r1, r2, new NormalXY(1,0), new NormalXY(0,1)),
				new ProfileXY.Constant   (r2, Double.POSITIVE_INFINITY)
			)));
		}),
		HemiSphere_CNF_round(bm -> {
			double r1 = 100;
			double r2 = r1+3;
			bm.setNormalFunction( new RotatedProfile( new ProfileXY.Group(
				new ProfileXY.RoundBlend( 0, r1, new NormalXY(0,1), new NormalXY(1,0)),
				new ProfileXY.RoundBlend(r1, r2, new NormalXY(1,0), new NormalXY(0,1)),
				new ProfileXY.Constant  (r2, Double.POSITIVE_INFINITY)
			)));
		}),
		HemiSphere_CNF2_linear(bm -> {
			double r1 = 60;
			double r2 = 120;
			NormalXY face = new NormalXY(0,1);
			NormalXY mid  = new NormalXY(1,0);
			bm.setNormalFunction( new RotatedProfile( new ProfileXY.Group(
				new ProfileXY.LinearBlend( 0, r1, face, mid),
				new ProfileXY.LinearBlend(r1, r2, mid, face),
				new ProfileXY.Constant   (r2, Double.POSITIVE_INFINITY)
			)));
		}),
		HemiSphere_CNF2_round(bm -> {
			double r1 = 60;
			double r2 = 120;
			NormalXY face = new NormalXY(0,1);
			NormalXY mid  = new NormalXY(1,0);
			bm.setNormalFunction( new RotatedProfile( new ProfileXY.Group(
				new ProfileXY.RoundBlend( 0, r1, face, mid),
				new ProfileXY.RoundBlend(r1, r2, mid, face),
				new ProfileXY.Constant  (r2, Double.POSITIVE_INFINITY)
			)));
		}),
		HemiSphere_CNF3_linear(bm -> {
			double r1 = 60;
			double r2 = 120;
			NormalXY face = new NormalXY(0,1);
			NormalXY mid  = new NormalXY(1,2).normalize();
			bm.setNormalFunction( new RotatedProfile( new ProfileXY.Group(
				new ProfileXY.LinearBlend( 0, r1, face, mid),
				new ProfileXY.LinearBlend(r1, r2, mid, face),
				new ProfileXY.Constant   (r2, Double.POSITIVE_INFINITY)
			)));
		}),
		HemiSphere_CNF3_round(bm -> {
			double r1 = 60;
			double r2 = 120;
			NormalXY face = new NormalXY(0,1);
			NormalXY mid  = new NormalXY(1,2).normalize();
			bm.setNormalFunction( new RotatedProfile( new ProfileXY.Group(
				new ProfileXY.RoundBlend( 0, r1, face, mid),
				new ProfileXY.RoundBlend(r1, r2, mid, face),
				new ProfileXY.Constant  (r2, Double.POSITIVE_INFINITY)
			)));
		}),
		HemiSphere_CNF4_round(bm -> {
			NormalXY face = new NormalXY( 0  ,1  );
			NormalXY mid1 = new NormalXY(-0.1,1  ).normalize();
			NormalXY mid2 = new NormalXY( 1  ,0.1).normalize();
			bm.setNormalFunction( new RotatedProfile( new ProfileXY.Group(
				new ProfileXY.RoundBlend(  0, 40, face, mid1),
				new ProfileXY.RoundBlend( 40, 80, mid1, mid2),
				new ProfileXY.RoundBlend( 80,120, mid2, face),
				new ProfileXY.Constant  (120, Double.POSITIVE_INFINITY)
			)));
		}),
		HemiSphereBubblesQ(bm -> {
			bm.setNormalFunction(new BubbleRaster(100,3,(raster,p)->{
				p.x = Math.round(p.x/raster)*raster;
				p.y = Math.round(p.y/raster)*raster;
			}));
		}),
		HemiSphereBubblesT(bm -> {
			bm.setNormalFunction(new BubbleRaster(100,3,(raster,p) -> {
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
			}));
		}),
		Noise(bm -> {
			int width = 400;
			int height = 300;
			Random rnd = new Random();
			Normal[][] normalMap = new Normal[width][height];
			for (int x1=0; x1<width; ++x1)
				for (int y1=0; y1<height; ++y1)
					normalMap[x1][y1] = new Normal(rnd.nextDouble(),0,1).normalize().rotateZ(rnd.nextDouble()*Math.PI*2);
			bm.setNormalMap(normalMap);
		}),
		
		NoiseHeight1(bm -> bm.setHeightMap(new NoiseHeightMap(400, 300, 1594263594,   10).heightMap,0)),
		NoiseHeight2(bm -> bm.setHeightMap(new NoiseHeightMap(400, 300, 1594263594,    5).heightMap,0)),
		NoiseHeight3(bm -> bm.setHeightMap(new NoiseHeightMap(400, 300, 1594263594,    2).heightMap,0)),
		NoiseHeight4(bm -> bm.setHeightMap(new NoiseHeightMap(400, 300, 1594263594,    1).heightMap,0)),
		NoiseHeight5(bm -> bm.setHeightMap(new NoiseHeightMap(400, 300, 1594263594, 0.5f).heightMap,0)),
		NoiseHeight6(bm -> bm.setHeightMap(new NoiseHeightMap(400, 300, 1594263594, 0.5f).heightMap,0.25f)),
		NoiseHeight7(bm -> bm.setHeightMap(new NoiseHeightMap(400, 300, 1594263594, 0.5f).heightMap,0.5f)),
		NoiseHeight8(bm -> bm.setHeightMap(new NoiseHeightMap(400, 300, 1594263594, 0.5f).heightMap,0.75f)),
		
		RandomHeight1(bm -> bm.setHeightMap(new RandomHeightMap(400, 300, 1594263594, 0.25f).heightMap,0)),
		RandomHeight2(bm -> bm.setHeightMap(new RandomHeightMap(400, 300, 1594263594, 0.50f).heightMap,0)),
		RandomHeight3(bm -> bm.setHeightMap(new RandomHeightMap(400, 300, 1594263594, 0.75f).heightMap,0)),
		RandomHeight4(bm -> bm.setHeightMap(new RandomHeightMap(400, 300, 1594263594, 1.00f).heightMap,0)),
		RandomHeight5(bm -> bm.setHeightMap(new RandomHeightMap(400, 300, 1594263594, 2.00f).heightMap,0)),
		
		RandomHeightNColor1(bm -> { RandomHeightMap map = new RandomHeightMap(400, 300, 1594263594, 0.25f, Color.BLUE, Color.ORANGE); bm.setHeightMap(map.heightMap, map.colorMap, 0.5); }),
		RandomHeightNColor2(bm -> { RandomHeightMap map = new RandomHeightMap(400, 300, 1594263594, 0.50f, Color.BLUE, Color.ORANGE); bm.setHeightMap(map.heightMap, map.colorMap, 0.5); }),
		RandomHeightNColor3(bm -> { RandomHeightMap map = new RandomHeightMap(400, 300, 1594263594, 0.75f, Color.BLUE, Color.ORANGE); bm.setHeightMap(map.heightMap, map.colorMap, 0.5); }),
		RandomHeightNColor4(bm -> { RandomHeightMap map = new RandomHeightMap(400, 300, 1594263594, 1.00f, Color.BLUE, Color.ORANGE); bm.setHeightMap(map.heightMap, map.colorMap, 0.5); }),
		RandomHeightNColor5(bm -> { RandomHeightMap map = new RandomHeightMap(400, 300, 1594263594, 2.00f, Color.BLUE, Color.ORANGE); bm.setHeightMap(map.heightMap, map.colorMap, 0.5); }),
		
		Spikes(bm -> {
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
			
			bm.setNormalFunction((x_, y_, width_, height_) -> {
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
		Consumer<BumpMapping> setNormalFunction;
		NormalFunctions(Consumer<BumpMapping> setNormalFunction) {
			this.setNormalFunction = setNormalFunction;
		}
		
		static Normal getBubbleNormal(double xCenter, double yCenter, double radius, double transition, Normal face, boolean inverted) {
			double r = Math.sqrt(xCenter*xCenter+yCenter*yCenter);
			
			if (r > radius+transition)
				return null;
			
			double w = Math.atan2(yCenter,xCenter);
			
			if (r < radius)
				return new Normal(inverted?-r:r,0,Math.sqrt(radius*radius-r*r)).normalize().rotateZ(w);
			else
				return Normal.blend(r, radius, radius+transition, new Normal(inverted?-1:1,0,0), face).normalize().rotateZ(w);
		}
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
		
		@Override public Normal getNormal(double x, double y, double width, double height) {
			Normal n;
			double xC = x-width/2.0;
			double yC = y-height/2.0;
			
			n = NormalFunctions.getBubbleNormal(xC,yC, radius, transition, vFace, false);
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
			
			n = NormalFunctions.getBubbleNormal(xC-xM,yC-yM, radiusB, transitionB, vFace, false);
			if (n!=null) return n;
			
			return vFace;
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
	
	private static class ResultView extends Canvas {
		private static final long serialVersionUID = 6410507776663767205L;
		private BumpMapping bumpMapping;

		public ResultView(BumpMapping bumpMapping) {
			this.bumpMapping = bumpMapping;
		}

		public BufferedImage renderScaledImage(float scale) {
			return bumpMapping.renderScaledImage_uncached(width,height,scale);
		}

		@Override
		protected void paintCanvas(Graphics g, int x, int y, int width, int height) {
			g.drawImage(bumpMapping.renderImage(width, height), x, y, null);
		}
		
	}
}
