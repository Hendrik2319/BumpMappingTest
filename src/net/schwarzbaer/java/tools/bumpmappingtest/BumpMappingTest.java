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
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.schwarzbaer.gui.BumpmappingSunControl;
import net.schwarzbaer.gui.Canvas;
import net.schwarzbaer.gui.HSColorChooser;
import net.schwarzbaer.gui.ProgressDialog;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.image.bumpmapping.BumpMapping;
import net.schwarzbaer.image.bumpmapping.BumpMapping.Indexer;
import net.schwarzbaer.image.bumpmapping.BumpMapping.Normal;
import net.schwarzbaer.image.bumpmapping.BumpMapping.NormalXY;
import net.schwarzbaer.image.bumpmapping.BumpMapping.OverSampling;
import net.schwarzbaer.image.bumpmapping.ExtraNormalFunction;
import net.schwarzbaer.image.bumpmapping.ExtraNormalFunction.Cart.AlphaCharSquence;
import net.schwarzbaer.image.bumpmapping.ExtraNormalFunction.Centerer;
import net.schwarzbaer.image.bumpmapping.ExtraNormalFunction.Polar.BentCartExtra;
import net.schwarzbaer.image.bumpmapping.NormalFunction;
import net.schwarzbaer.image.bumpmapping.ProfileXY;
import net.schwarzbaer.image.bumpmapping.Shading;
import net.schwarzbaer.image.bumpmapping.Shading.GUISurfaceShading;
import net.schwarzbaer.image.bumpmapping.Shading.MaterialShading;
import net.schwarzbaer.image.bumpmapping.Shading.MixedShading;
import net.schwarzbaer.image.bumpmapping.Shading.NormalImage;
import net.schwarzbaer.system.ClipboardTools;
import net.schwarzbaer.system.Settings;

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
	
	private static class MainWindowSettings extends Settings<MainWindowSettings.ValueGroup,MainWindowSettings.ValueKey> {
		
		enum ValueGroup implements Settings.GroupKeys<ValueKey> {
			WindowPos (ValueKey.WindowX, ValueKey.WindowY),
			WindowSize(ValueKey.WindowWidth, ValueKey.WindowHeight);
			ValueKey[] keys;
			ValueGroup(ValueKey...keys) { this.keys = keys;}
			@Override public ValueKey[] getKeys() { return keys; }
		}
		
		enum ValueKey {
			WindowX, WindowY, WindowWidth, WindowHeight,
			Polar_Text,Polar_Radius,Polar_RadiusOffset,Polar_Angle,Polar_FontSize,Polar_LineWidth,Polar_LineHeight,
			Cart_Text,Cart_TextPosX,Cart_TextPosY,Cart_FontSize,Cart_LineWidth,Cart_LineHeight
		}

		public MainWindowSettings() { super(BumpMappingTest.class); }

		public Point getWindowPos() {
			int x = getInt(ValueKey.WindowX);
			int y = getInt(ValueKey.WindowY);
			return new Point(x,y);
		}
		public void setWindowPos(Point location) {
			putInt(ValueKey.WindowX, location.x);
			putInt(ValueKey.WindowY, location.y);
		}

		public Dimension getWindowSize() {
			int w = getInt(ValueKey.WindowWidth );
			int h = getInt(ValueKey.WindowHeight);
			return new Dimension(w,h);
		}
		public void setWindowSize(Dimension size) {
			putInt(ValueKey.WindowWidth , size.width );
			putInt(ValueKey.WindowHeight, size.height);
		}
	}

	private Normal sun;
	private BumpMapping bumpMapping;
	private CartTextOverlay  cartTextOverlay = null;
	private PolarTextOverlay polarTextOverlay = null;
	private MainWindowSettings settings;
	private GUI gui = null;
	
	private BumpMappingTest() {
		sun = new Normal(1,-1,2).normalize();
		bumpMapping = new BumpMapping(true,true);
		settings = new MainWindowSettings();
	}
	
	private void createGUI() {
		gui = new GUI();
		gui.create();
	}
	
	private class GUI {
		private StandardMainWindow mainwindow = null;
		private ResultView resultView = null;
		private JPanel optionsPanel = null;
		private JPanel currentShadingOptionsPanel=null;
		private JPanel currentTextOptionPanel = null;
		private JPanel cartTextOptionPanel = null;
		private JPanel polarTextOptionPanel = null;
		private JPanel dummyTextOptionPanel = null;
	
		private void create() {
			GridBagConstraints c = new GridBagConstraints();
			mainwindow = new StandardMainWindow("BumpMappingTest");
			settings = new MainWindowSettings();
			
			NormalFunctions initialNormalFunction = NormalFunctions.HemiSphereBubblesT;
			Shadings initialShading = Shadings.Material;
			
			resultView = new ResultView(bumpMapping);
			resultView.setPreferredSize(new Dimension(450,350));
			
			JPanel buttonPanel = new JPanel(new GridLayout(1,0,3,3));
			buttonPanel.add(createButton("Copy"   ,(JButton b)->copyImageToClipboard(b,1)));
			buttonPanel.add(createButton("Copy 2x",(JButton b)->copyImageToClipboard(b,2)));
			buttonPanel.add(createButton("Copy 4x",(JButton b)->copyImageToClipboard(b,4)));
			buttonPanel.add(createButton("Copy 8x",(JButton b)->copyImageToClipboard(b,8)));
			
			JPanel resultViewPanel = new JPanel(new BorderLayout(3,3));
			resultViewPanel.setBorder(BorderFactory.createTitledBorder("Result"));
			resultViewPanel.add(resultView,BorderLayout.CENTER);
			
			JPanel centerPanel = new JPanel(new BorderLayout(3,3));
			centerPanel.add(resultViewPanel,BorderLayout.CENTER);
			centerPanel.add(buttonPanel,BorderLayout.SOUTH);
			
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
			selectionPanel.add(createComboBox(OverSampling   .values(), bumpMapping.getOverSampling(), BumpMappingTest.this::setOverSampling  ),GBC.setGridPos(c,1,0));
			selectionPanel.add(createComboBox(NormalFunctions.values(), initialNormalFunction,         BumpMappingTest.this::setNormalFunction),GBC.setGridPos(c,1,1));
			selectionPanel.add(createComboBox(Shadings       .values(), initialShading,                BumpMappingTest.this::setShading       ),GBC.setGridPos(c,1,2));
			
			
			cartTextOverlay  = new CartTextOverlay (this,settings,"MxXBabcd", -100, -50, 30, 5, 1);
			polarTextOverlay = new PolarTextOverlay(this,settings,"MxXBabcd", 100, 15, -90, 30, 5, 1);
			cartTextOptionPanel  = cartTextOverlay.createOptionsPanel();
			polarTextOptionPanel = polarTextOverlay.createOptionsPanel();
			dummyTextOptionPanel = new JPanel(new GridBagLayout());
			dummyTextOptionPanel.setBorder(BorderFactory.createTitledBorder(""));
			
			optionsPanel = new JPanel(new BorderLayout(3,3));
			optionsPanel.setBorder(BorderFactory.createTitledBorder("Options"));
			optionsPanel.add(selectionPanel,BorderLayout.NORTH);
			//optionsPanel.add(textPanel,BorderLayout.SOUTH);
			
			for (Shadings sh:Shadings.values()) {
				sh.valuePanel = new JPanel(new GridBagLayout());
				sh.valuePanel.setBorder(BorderFactory.createTitledBorder("Shading Options"));
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
					sh.valuePanel.add(createColorbutton(shading.getMaterialColor()   , "Select Material Color", shading::setMaterialColor),GBC.setGridPos(c,1,0));
					sh.valuePanel.add(createDoubleInput(shading.getAmbientIntensity(), shading::setAmbientIntensity),GBC.setGridPos(c,1,1));
					sh.valuePanel.add(createDoubleInput(shading.getPhongExp()        , shading::setPhongExp        ),GBC.setGridPos(c,1,2));
					sh.valuePanel.add(createCheckBox   (shading.getReflection()      , shading::setReflection      ),GBC.setGridPos(c,1,3));
					sh.valuePanel.add(createDoubleInput(shading.getReflIntensity()   , shading::setReflIntensity   ),GBC.setGridPos(c,1,4));
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
			contentPane.add(centerPanel,BorderLayout.CENTER);
			contentPane.add(rightPanel,BorderLayout.EAST);
			
			mainwindow.startGUI(contentPane);
			
			if (settings.isSet(MainWindowSettings.ValueGroup.WindowPos )) mainwindow.setLocation(settings.getWindowPos ());
			if (settings.isSet(MainWindowSettings.ValueGroup.WindowSize)) mainwindow.setSize    (settings.getWindowSize());
			
			mainwindow.addComponentListener(new ComponentListener() {
				@Override public void componentShown  (ComponentEvent e) {}
				@Override public void componentHidden (ComponentEvent e) {}
				@Override public void componentResized(ComponentEvent e) { settings.setWindowSize( mainwindow.getSize() ); }
				@Override public void componentMoved  (ComponentEvent e) { settings.setWindowPos ( mainwindow.getLocation() ); }
			});
		}

		void setShadingOptionsPanel(JPanel valuePanel) {
			if (currentShadingOptionsPanel!=null) optionsPanel.remove(currentShadingOptionsPanel);
			optionsPanel.add(currentShadingOptionsPanel = valuePanel, BorderLayout.CENTER);
			optionsPanel.revalidate();
			optionsPanel.repaint();
		}

		void setTextOptionsPanel(JPanel textOptionPanel) {
			if (currentTextOptionPanel!=null) optionsPanel.remove(currentTextOptionPanel);
			optionsPanel.add(currentTextOptionPanel = textOptionPanel, BorderLayout.SOUTH);
			optionsPanel.revalidate();
			optionsPanel.repaint();
		}
		void resetBumpMappingAndView() {
			BumpMappingTest.this.resetBumpMappingAndView();
		}

		private JCheckBox createCheckBox(boolean isSelected, Consumer<Boolean> setValue) {
			JCheckBox comp = new JCheckBox();
			comp.setSelected(isSelected);
			if (setValue!=null) comp.addActionListener(e->{
				setValue.accept(comp.isSelected());
				resetBumpMappingAndView();
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

		private JButton createColorbutton(Color initColor, String dialogTitle, Consumer<Color> setcolor) {
			JButton colorbutton = HSColorChooser.createColorbutton(
				initColor, mainwindow, dialogTitle, HSColorChooser.PARENT_CENTER,
				color->{
					setcolor.accept(color);
					resetBumpMappingAndView();
				}
			);
			colorbutton.setPreferredSize(new Dimension(30,30));
			return colorbutton;
		}

		private JTextField createDoubleInput(double value, Consumer<Double> setValue) {
			return createDoubleInput(value, setValue, v->true);
		}

		private JTextField createDoubleInput(double value, Consumer<Double> setValue, Predicate<Double> isOK) {
			Function<String,Double> parse = str->{ try { return Double.parseDouble(str); } catch (NumberFormatException e) { return Double.NaN; } };
			Predicate<Double> isOK2 = v->v!=null && !Double.isNaN(v) && isOK.test(v);
			Function<Double, String> toString = v->v==null ? "" : v.toString();
			return createGenericTextField(value, toString, parse, isOK2, setValue);
		}

		private JTextField createTextInput(String value, Consumer<String> setValue, Predicate<String> isOK) {
			return createGenericTextField(value, v->v, v->v, isOK, setValue, setValue);
		}

		private <V> JTextField createGenericTextField(V value, Function<V,String> toString, Function<String,V> parse, Predicate<V> isOK, Consumer<V> setValue) {
			return createGenericTextField(value, toString, parse, isOK, setValue, null);
		}

		private <V> JTextField createGenericTextField(V value, Function<V,String> toString, Function<String,V> parse, Predicate<V> isOK, Consumer<V> setValue, Consumer<V> setValueWhileAdjusting) {
			JTextField comp = new JTextField(toString.apply(value));
			Color defaultBG = comp.getBackground();
			if (setValueWhileAdjusting!=null) {
				comp.addCaretListener (e -> {
					readTextField(comp,defaultBG,parse,isOK,v -> {
						setValueWhileAdjusting.accept(v);
						resetBumpMappingAndView();
					});
				});
			}
			Consumer<V> modifiedSetValue = d -> {
				setValue.accept(d);
				resetBumpMappingAndView();
			};
			comp.addActionListener(e->{ readTextField(comp,defaultBG,parse,isOK,modifiedSetValue); });
			comp.addFocusListener(new FocusListener() {
				@Override public void focusLost  (FocusEvent e) { readTextField(comp,defaultBG,parse,isOK,modifiedSetValue); }
				@Override public void focusGained(FocusEvent e) {}
			});
			return comp;
		}

		private <V> void readTextField(JTextField comp, Color defaultBG, Function<String,V> parse, Predicate<V> isOK, Consumer<V> setValue) {
			V d = parse.apply(comp.getText());
			if (isOK.test(d)) {
				comp.setBackground(defaultBG);
				setValue.accept(d);
			} else {
				comp.setBackground(Color.RED);
			}
		}

		private <T> JComboBox<T> createComboBox(T[] values, T selectedValue, Consumer<T> valueChanged) {
			JComboBox<T> comp = new JComboBox<T>(values);
			comp.setSelectedItem(selectedValue);
			if (valueChanged!=null) comp.addActionListener(e->{
				int i = comp.getSelectedIndex();
				valueChanged.accept(i<0?null:comp.getItemAt(i));
			});
			return comp;
		}
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

	private static void processInEventThread(Runnable task) {
		SwingUtilities.invokeLater(task);
	}

	private static void processInEventThreadAndWait(Runnable task) {
		try { SwingUtilities.invokeAndWait(task); }
		catch (InvocationTargetException | InterruptedException e) {}
	}

	private static void runWithProgressDialog(Window parent, String title, Consumer<ProgressDialog> useProgressDialog) {
		ProgressDialog.runWithProgressDialog(parent, title, 400, useProgressDialog);
	}

	private void copyImageToClipboard(JComponent comp, float scale) {
		new Thread(()->{
			processInEventThreadAndWait(()->comp.setEnabled(false));
			
			runWithProgressDialog(gui.mainwindow, "Copy Scaled Image to Clipboard", pd->{
				BufferedImage image = gui.resultView.renderScaledImage(scale,pd);
				
				processInEventThreadAndWait(()->{
					pd.setTaskTitle("Copy to Clipboard");
					pd.setIndeterminate(true);
				});
				
				ClipboardTools.copyToClipBoard(image);
				
				processInEventThread(()->comp.setEnabled(true));
			});
			
			
		}).start();
	}
	
	private void setShading(Shadings sh) {
		gui.setShadingOptionsPanel(sh.valuePanel);
		bumpMapping.setShading(sh.shading);
		bumpMapping.setSun(sun.x,sun.y,sun.z);
		gui.resultView.repaint();
	}

	private void setNormalFunction(NormalFunctions nf) {
		NormalFunction normalFunction = nf.createNormalFunction.get();
		
		JPanel textOptionPanel = gui.dummyTextOptionPanel;
		if (normalFunction instanceof ExtraNormalFunction.Host) {
			ExtraNormalFunction.Host host = (ExtraNormalFunction.Host) normalFunction;
			host.setExtras(cartTextOverlay.getExtraObj());
			textOptionPanel = gui.cartTextOptionPanel;
			
		} else if (normalFunction instanceof ExtraNormalFunction.PolarHost) {
			ExtraNormalFunction.PolarHost host = (ExtraNormalFunction.PolarHost) normalFunction;
			host.setExtras(polarTextOverlay.getExtraObj());
			textOptionPanel = gui.polarTextOptionPanel;
		}
		
		gui.setTextOptionsPanel(textOptionPanel);
		bumpMapping.setNormalFunction(normalFunction);
		gui.resultView.repaint();
	}

	private void setOverSampling(OverSampling os) {
		bumpMapping.setOverSampling(os);
		gui.resultView.repaint();
	}
	private void resetBumpMappingAndView() {
		bumpMapping.reset();
		gui.resultView.repaint();
	}

	private static ProfileXY createProfile(double lineWidth, double lineHeight) {
		lineWidth  = Math.max(lineWidth , 0.01);
		lineHeight = Math.max(lineHeight, 0.01);
		
		double x1 =  0 + lineWidth/6;
		double x2 = x1 + lineWidth/6;
		double x3 = x2 + lineWidth/6;
		double xO = x3 + lineWidth/6;
		
		NormalXY vFace  = new NormalXY(0,1);
		NormalXY vRamp = ProfileXY.Constant.computeNormal(x2,x3, 0,lineHeight);
		
		return new ProfileXY.Group(
			new ProfileXY.Constant  ( 0, x1 ),
			new ProfileXY.RoundBlend(x1, x2, vFace,vRamp),
			new ProfileXY.Constant  (x2, x3, vRamp),
			new ProfileXY.RoundBlend(x3, xO, vRamp,vFace)
		);
	}

	private static abstract class AbstractTextOverlay<ExtraObjType extends ExtraNormalFunction> {
		/*
		enum ValueGroup implements Settings.GroupKeys<ValueKey> {
			;
			ValueKey[] keys;
			ValueGroup(ValueKey...keys) { this.keys = keys;}
			@Override public ValueKey[] getKeys() { return keys; }
		}
		*/
		
		protected GUI gui;
		private MainWindowSettings settings;
		
		protected AbstractTextOverlay(GUI gui, MainWindowSettings settings) {
			this.gui = gui;
			this.settings = settings;
		}
		
		public abstract ExtraObjType getExtraObj();
		public abstract JPanel createOptionsPanel();
		
		protected double setDoubleValue(double oldValue, double newValue, MainWindowSettings.ValueKey valueKey, Runnable setValue) {
			return setValue(oldValue, newValue, ()->{ settings.putDouble(valueKey, newValue); setValue.run(); });
		}
		protected String setStringValue(String oldValue, String newValue, MainWindowSettings.ValueKey valueKey, Runnable setValue) {
			return setValue(oldValue, newValue, ()->{ settings.putString(valueKey, newValue); setValue.run(); });
		}
		private <V> V setValue(V oldValue, V newValue, Runnable setValue) {
			if (oldValue.equals(newValue)) return oldValue;
			setValue.run();
			gui.resetBumpMappingAndView();
			return newValue;
		}
	}

	private static class PolarTextOverlay extends AbstractTextOverlay<ExtraNormalFunction.Polar> {
		
		private static final double DEG2RAD = 1/180.0*Math.PI;
		private String text;
		private double radius_px;
		private double radiusOffset_px;
		private double angle_deg;
		private double fontSize_px;
		private double lineWidth_px;
		private double lineHeight_px;
		private final AlphaCharSquence alphaCharSquence;
		private final BentCartExtra bender;

		public PolarTextOverlay(GUI gui, MainWindowSettings settings, String text, double radius_px, double radiusOffset_px, double angle_deg, double fontSize_px, double lineWidth_px, double lineHeight_px) {
			super(gui,settings);
			this.text            = settings.getString(MainWindowSettings.ValueKey.Polar_Text        , text           );
			this.radius_px       = settings.getDouble(MainWindowSettings.ValueKey.Polar_Radius      , radius_px      );
			this.radiusOffset_px = settings.getDouble(MainWindowSettings.ValueKey.Polar_RadiusOffset, radiusOffset_px);
			this.angle_deg       = settings.getDouble(MainWindowSettings.ValueKey.Polar_Angle       , angle_deg      );
			this.fontSize_px     = settings.getDouble(MainWindowSettings.ValueKey.Polar_FontSize    , fontSize_px    );
			this.lineWidth_px    = settings.getDouble(MainWindowSettings.ValueKey.Polar_LineWidth   , lineWidth_px   );
			this.lineHeight_px   = settings.getDouble(MainWindowSettings.ValueKey.Polar_LineHeight  , lineHeight_px  );
			ProfileXY profile = createProfile(this.lineWidth_px,this.lineHeight_px);
			alphaCharSquence = new AlphaCharSquence(0,-this.radiusOffset_px, this.fontSize_px/100, profile, this.text);
			bender = new BentCartExtra(this.radius_px, this.angle_deg*DEG2RAD, alphaCharSquence);
		}

		@Override
		public ExtraNormalFunction.Polar getExtraObj() {
			return bender;
		}

		@Override
		public JPanel createOptionsPanel() {
			GridBagConstraints c = new GridBagConstraints();
			JPanel textPanel = new JPanel(new GridBagLayout());
			textPanel.setBorder(BorderFactory.createTitledBorder("Text Options"));
			GBC.reset(c);
			GBC.setFill(c, GBC.GridFill.HORIZONTAL);
			GBC.setWeights(c,0,0);
			textPanel.add(new JLabel("Text: "              , SwingConstants.RIGHT),GBC.setGridPos(c,0,0));
			textPanel.add(new JLabel("Font Size (px): "    , SwingConstants.RIGHT),GBC.setGridPos(c,0,1));
			textPanel.add(new JLabel("Radius (px): "       , SwingConstants.RIGHT),GBC.setGridPos(c,0,2));
			textPanel.add(new JLabel("Radius Offset (px): ", SwingConstants.RIGHT),GBC.setGridPos(c,0,3));
			textPanel.add(new JLabel("Start Angle (deg): " , SwingConstants.RIGHT),GBC.setGridPos(c,0,4));
			textPanel.add(new JLabel("Line Width (px): "   , SwingConstants.RIGHT),GBC.setGridPos(c,0,5));
			textPanel.add(new JLabel("Line Depth (px): "   , SwingConstants.RIGHT),GBC.setGridPos(c,0,6));
			GBC.setWeights(c,1,0);
			textPanel.add(gui.createTextInput  (text           , this::setText        , v->!v.isEmpty()),GBC.setGridPos(c,1,0));
			textPanel.add(gui.createDoubleInput(fontSize_px    , this::setFontSize    , v->v>0         ),GBC.setGridPos(c,1,1));
			textPanel.add(gui.createDoubleInput(radius_px      , this::setRadius      , v->v>0         ),GBC.setGridPos(c,1,2));
			textPanel.add(gui.createDoubleInput(radiusOffset_px, this::setRadiusOffset, v->v>-radius_px),GBC.setGridPos(c,1,3));
			textPanel.add(gui.createDoubleInput(angle_deg      , this::setAngle                        ),GBC.setGridPos(c,1,4));
			textPanel.add(gui.createDoubleInput(lineWidth_px   , this::setLineWidth   , v->v>0.01      ),GBC.setGridPos(c,1,5));
			textPanel.add(gui.createDoubleInput(lineHeight_px  , this::setLineHeight  , v->v>0.01      ),GBC.setGridPos(c,1,6));
			return textPanel;
		}
		
		private void setText        (String text           ) { this.text            = setStringValue( this.text           , text           , MainWindowSettings.ValueKey.Polar_Text        , ()->alphaCharSquence.setText   ( text                                    ) ); }
		private void setRadius      (double radius_px      ) { this.radius_px       = setDoubleValue( this.radius_px      , radius_px      , MainWindowSettings.ValueKey.Polar_Radius      , ()->bender      .setZeroYRadius( radius_px                               ) ); }
		private void setRadiusOffset(double radiusOffset_px) { this.radiusOffset_px = setDoubleValue( this.radiusOffset_px, radiusOffset_px, MainWindowSettings.ValueKey.Polar_RadiusOffset, ()->alphaCharSquence.setY      (-radiusOffset_px                         ) ); }
		private void setAngle       (double angle_deg      ) { this.angle_deg       = setDoubleValue( this.angle_deg      , angle_deg      , MainWindowSettings.ValueKey.Polar_Angle       , ()->bender      .setZeroXAngle ( angle_deg*DEG2RAD                       ) ); }
		private void setFontSize    (double fontSize_px    ) { this.fontSize_px     = setDoubleValue( this.fontSize_px    , fontSize_px    , MainWindowSettings.ValueKey.Polar_FontSize    , ()->alphaCharSquence.setScale  ( fontSize_px/100                         ) ); }
		private void setLineWidth   (double lineWidth_px   ) { this.lineWidth_px    = setDoubleValue( this.lineWidth_px   , lineWidth_px   , MainWindowSettings.ValueKey.Polar_LineWidth   , ()->alphaCharSquence.setProfile(createProfile(lineWidth_px,lineHeight_px)) ); }
		private void setLineHeight  (double lineHeight_px  ) { this.lineHeight_px   = setDoubleValue( this.lineHeight_px  , lineHeight_px  , MainWindowSettings.ValueKey.Polar_LineHeight  , ()->alphaCharSquence.setProfile(createProfile(lineWidth_px,lineHeight_px)) ); }
	}

	private static class CartTextOverlay extends AbstractTextOverlay<ExtraNormalFunction> {
		
		private String text;
		private double textPosX_px;
		private double textPosY_px;
		private double fontSize_px;
		private double lineWidth_px;
		private double lineHeight_px;
		private final AlphaCharSquence alphaCharSquence;
		private final Centerer centerer;
		
		CartTextOverlay(GUI gui, MainWindowSettings settings, String text, double textPosX_px, double textPosY_px, double fontSize_px, double lineWidth_px, double lineHeight_px) {
			super(gui,settings);
			this.text           = settings.getString(MainWindowSettings.ValueKey.Cart_Text      , text         );
			this.textPosX_px    = settings.getDouble(MainWindowSettings.ValueKey.Cart_TextPosX  , textPosX_px  );
			this.textPosY_px    = settings.getDouble(MainWindowSettings.ValueKey.Cart_TextPosY  , textPosY_px  );
			this.fontSize_px    = settings.getDouble(MainWindowSettings.ValueKey.Cart_FontSize  , fontSize_px  );
			this.lineWidth_px   = settings.getDouble(MainWindowSettings.ValueKey.Cart_LineWidth , lineWidth_px );
			this.lineHeight_px  = settings.getDouble(MainWindowSettings.ValueKey.Cart_LineHeight, lineHeight_px);
			ProfileXY profile = createProfile(this.lineWidth_px, this.lineHeight_px);
			alphaCharSquence = new AlphaCharSquence(this.textPosX_px, this.textPosY_px, this.fontSize_px/100, profile, this.text);
			centerer = new Centerer(alphaCharSquence);
		}

		@Override
		public ExtraNormalFunction getExtraObj() {
			return centerer;
		}

		@Override
		public JPanel createOptionsPanel() {
			GridBagConstraints c = new GridBagConstraints();
			JPanel textPanel = new JPanel(new GridBagLayout());
			textPanel.setBorder(BorderFactory.createTitledBorder("Text Options"));
			GBC.reset(c);
			GBC.setFill(c, GBC.GridFill.HORIZONTAL);
			GBC.setWeights(c,0,0);
			textPanel.add(new JLabel("Text: "           , SwingConstants.RIGHT),GBC.setGridPos(c,0,0));
			textPanel.add(new JLabel("Font Size (px): " , SwingConstants.RIGHT),GBC.setGridPos(c,0,1));
			textPanel.add(new JLabel("X Offset: "       , SwingConstants.RIGHT),GBC.setGridPos(c,0,2));
			textPanel.add(new JLabel("Y Offset: "       , SwingConstants.RIGHT),GBC.setGridPos(c,0,3));
			textPanel.add(new JLabel("Line Width (px): ", SwingConstants.RIGHT),GBC.setGridPos(c,0,4));
			textPanel.add(new JLabel("Line Depth (px): ", SwingConstants.RIGHT),GBC.setGridPos(c,0,5));
			GBC.setWeights(c,1,0);
			textPanel.add(gui.createTextInput  (text         , this::setText      , v->!v.isEmpty()),GBC.setGridPos(c,1,0));
			textPanel.add(gui.createDoubleInput(fontSize_px  , this::setFontSize  , v->v>0         ),GBC.setGridPos(c,1,1));
			textPanel.add(gui.createDoubleInput(textPosX_px  , this::setTextPosX                   ),GBC.setGridPos(c,1,2));
			textPanel.add(gui.createDoubleInput(textPosY_px  , this::setTextPosY                   ),GBC.setGridPos(c,1,3));
			textPanel.add(gui.createDoubleInput(lineWidth_px , this::setLineWidth , v->v>0.01      ),GBC.setGridPos(c,1,4));
			textPanel.add(gui.createDoubleInput(lineHeight_px, this::setLineHeight, v->v>0.01      ),GBC.setGridPos(c,1,5));
			return textPanel;
		}

		private void setText      (String text         ) { this.text          = setStringValue( this.text         , text         , MainWindowSettings.ValueKey.Cart_Text      , ()->alphaCharSquence.setText   (text                                      ) ); }
		private void setTextPosX  (double textPosX_px  ) { this.textPosX_px   = setDoubleValue( this.textPosX_px  , textPosX_px  , MainWindowSettings.ValueKey.Cart_TextPosX  , ()->alphaCharSquence.setX      (textPosX_px                               ) ); }
		private void setTextPosY  (double textPosY_px  ) { this.textPosY_px   = setDoubleValue( this.textPosY_px  , textPosY_px  , MainWindowSettings.ValueKey.Cart_TextPosY  , ()->alphaCharSquence.setY      (textPosY_px                               ) ); }
		private void setFontSize  (double fontSize_px  ) { this.fontSize_px   = setDoubleValue( this.fontSize_px  , fontSize_px  , MainWindowSettings.ValueKey.Cart_FontSize  , ()->alphaCharSquence.setScale  (fontSize_px/100                           ) ); }
		private void setLineWidth (double lineWidth_px ) { this.lineWidth_px  = setDoubleValue( this.lineWidth_px , lineWidth_px , MainWindowSettings.ValueKey.Cart_LineWidth , ()->alphaCharSquence.setProfile(createProfile(lineWidth_px, lineHeight_px)) ); }
		private void setLineHeight(double lineHeight_px) { this.lineHeight_px = setDoubleValue( this.lineHeight_px, lineHeight_px, MainWindowSettings.ValueKey.Cart_LineHeight, ()->alphaCharSquence.setProfile(createProfile(lineWidth_px, lineHeight_px)) ); }
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
	
	private static class ResultView extends Canvas {
		private static final long serialVersionUID = 6410507776663767205L;
		private BumpMapping bumpMapping;

		public ResultView(BumpMapping bumpMapping) {
			this.bumpMapping = bumpMapping;
		}

		public BufferedImage renderScaledImage(float scale, ProgressDialog pd) {
			return bumpMapping.renderImage_uncached(width,height,scale,new BumpMapping.RenderProgressListener() {
				private int lastX = 0;
				@Override public void setSize(int width, int height) {
					this.lastX = 0;
					processInEventThreadAndWait(()->{
						pd.setTaskTitle("Render Image ("+width+"x"+height+" Pixels)");
						pd.setValue(0, width);
					});
				}
				@Override public void wasRendered(int x, int y) {
					if (lastX==x) return;
					processInEventThread(()->pd.setValue(x+1));
					lastX = x+1;
				}
			});
		}

		@Override
		protected void paintCanvas(Graphics g, int x, int y, int width, int height) {
			g.drawImage(bumpMapping.renderImage(width, height), x, y, null);
		}
		
	}
}
