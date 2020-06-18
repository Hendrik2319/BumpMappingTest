package net.schwarzbaer.java.tools.bumpmappingtest;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
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
import net.schwarzbaer.gui.ProgressDialog;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.image.bumpmapping.BumpMapping;
import net.schwarzbaer.image.bumpmapping.BumpMapping.Indexer;
import net.schwarzbaer.image.bumpmapping.BumpMapping.Normal;
import net.schwarzbaer.image.bumpmapping.BumpMapping.OverSampling;
import net.schwarzbaer.image.bumpmapping.Shading;
import net.schwarzbaer.image.bumpmapping.Shading.GUISurfaceShading;
import net.schwarzbaer.image.bumpmapping.Shading.MaterialShading;
import net.schwarzbaer.image.bumpmapping.Shading.MixedShading;
import net.schwarzbaer.image.bumpmapping.Shading.NormalImage;

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
		System.out.printf(Locale.ENGLISH, "%s.rotateZ(%1.1f�) -> %s%n", n, w_degree, n.rotateZ(w_degree/180*Math.PI));
		System.out.printf(Locale.ENGLISH, "%s.rotateY(%1.1f�) -> %s%n", n, w_degree, n.rotateY(w_degree/180*Math.PI));
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
		
		bumpMapping = new BumpMapping(true,true);
		
		resultView = new ResultView(bumpMapping);
		resultView.setBorder(BorderFactory.createTitledBorder("Result"));
		resultView.setPreferredSize(new Dimension(300,300));
		
		JPanel buttonPanel = new JPanel(new GridLayout(1,0,3,3));
		buttonPanel.add(createButton("Copy"   ,(JButton b)->copyImageToClipboard(b,1)));
		buttonPanel.add(createButton("Copy 2x",(JButton b)->copyImageToClipboard(b,2)));
		buttonPanel.add(createButton("Copy 4x",(JButton b)->copyImageToClipboard(b,4)));
		buttonPanel.add(createButton("Copy 8x",(JButton b)->copyImageToClipboard(b,8)));
		
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
			
			runWithProgressDialog(mainwindow, "Copy Scaled Image to Clipboard", pd->{
				BufferedImage image = resultView.renderScaledImage(scale,pd);
				
				processInEventThreadAndWait(()->{
					pd.setTaskTitle("Copy to Clipboard");
					pd.setIndeterminate(true);
				});
				
				Toolkit toolkit = Toolkit.getDefaultToolkit();
				Clipboard clipboard = toolkit.getSystemClipboard();
				TransferableImage content = new TransferableImage(image);
				//DataHandler content = new DataHandler(image,"image/x-java-image");
				
				try { clipboard.setContents(content,null); }
				catch (IllegalStateException e1) { e1.printStackTrace(); }
				
				processInEventThread(()->comp.setEnabled(true));
			});
			
			
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
			bumpMapping.reset();
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
			bumpMapping.reset();
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
				bumpMapping.reset();
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
		bumpMapping.setNormalFunction(nf.createNormalFunction.get());
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
