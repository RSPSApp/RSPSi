package com.rspsi;

import com.jagex.map.procedural.Biome;
import com.rspsi.misc.SimplexNoise;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import javax.imageio.ImageIO;

import com.rspsi.util.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TextField;
import javafx.scene.effect.Effect;
import org.apache.commons.compress.utils.Lists;

import com.jagex.util.Constants;
import com.rspsi.controls.ConditionGridNode;
import com.rspsi.misc.TileCondition;
import com.rspsi.resources.ResourceLoader;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TitledPane;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.coobird.thumbnailator.Thumbnails;

import static com.rspsi.misc.SimplexNoise.noise;
import static java.awt.Image.SCALE_FAST;
import static java.awt.Image.SCALE_SMOOTH;

public class GenerateNewMapWindow extends Application {

	private Stage stage;

	boolean okClicked;

	private int[][] heights;

	private int[][] treeMap;
	
	private WritableImage blurredImage;

	private Image finalImage;

	BufferedImage bImage;

	@Override
	public void start(Stage primaryStage) throws Exception {
		this.stage = primaryStage;
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/newregion.fxml"));

		loader.setController(this);
		Parent content = loader.load();
		Scene scene = new Scene(content);



		primaryStage.setTitle("Please select region options");
		primaryStage.initStyle(StageStyle.UTILITY);
		primaryStage.setScene(scene);
		primaryStage.getIcons().add(ResourceLoader.getSingleton().getLogo64());

		FXUtils.centerStage(primaryStage);
		primaryStage.centerOnScreen();
		primaryStage.setAlwaysOnTop(true);


		widthSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));
		lengthSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));


		FXUtils.addSpinnerFocusListeners(widthSpinner, lengthSpinner);

		waterDistanceMax.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 128, 0));
		waterDistanceMin.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 128, 0));

		heightsMin.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 800, 0));
		heightsMax.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10000, 336));

		frequency.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 7.0, 4.57));
		frequency.setEditable(true);

		peaksValleys.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 10.0, 1.87));
		peaksValleys.setEditable(true);

		xCoord.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(-999999.0, 9999999.00, 0.00));
		xCoord.setEditable(true);
		yCoord.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(-999999.0, 9999999.00, 0.00));
		yCoord.setEditable(true);

		seed.setText((Math.random() * 100000) + "");
		seed.setEditable(true);

		treeFreq.setText("3");
		treeFreq.setEditable(true);

		setLinkedMinMax(waterDistanceMin, waterDistanceMax);
		setLinkedMinMax(heightsMin, heightsMax);

		populateBiomeSelection();

		okButton.setOnAction(evt -> {
			primaryStage.hide();
			okClicked = true;
		});
		cancelButton.setOnAction(evt -> {
			reset();
			primaryStage.hide();
		});
		
		browseBtn.setOnAction(evt -> {
			File f = RetentionFileChooser.showOpenDialog(primaryStage, FilterMode.PNG);
			if(f != null) {
				try {
					Image image = new Image(new FileInputStream(f));

					// TODO: Fix browse for heightmap to use Canvas instead of legacy ImageView
					/*
					tileHeightImageView.setFitHeight(lengthSpinner.getValue() * 64);
					tileHeightImageView.setFitWidth(widthSpinner.getValue() * 64);
					tileHeightImageView.setImage(image);
					ColorAdjust desaturate = new ColorAdjust();
			        desaturate.setSaturation(-1);
					tileHeightImageView.setEffect(desaturate);
					primaryStage.sizeToScene();
					Platform.runLater(() -> {
						blurredImage = tileHeightImageView.snapshot(new SnapshotParameters(), null);
						
						setWaterEdges();
					});
					 */
				} catch (Exception e) {
					FXDialogs.showError(primaryStage,"Error while loading image", "There was an error while attempting to load the selected image.");
				}
			}
		});

		generateBtn.setOnAction(evt -> {
			int width = (int)(widthSpinner.getValue() * 64);
			int height = (int)(lengthSpinner.getValue() * 64);
			double frequencyVal = frequency.getValue();
			double peaksValleysVal = peaksValleys.getValue();
			int treeFreqVal = Integer.parseInt(treeFreq.getText());

			double xCoordVal = xCoord.getValue();
			double yCoordVal = yCoord.getValue();

			double seedVal = 68172.68859943567; //Double.parseDouble(seed.getText());
/*
			if (!preserveSeed.isSelected()) {
				double newSeed = (Math.random() * Double.MAX_VALUE);
				seedVal = newSeed;
				seed.setText(newSeed + "");
			}
*/

			WritableImage image = this.generateHeightmap(width, height, frequencyVal, peaksValleysVal, seedVal, 0, 0);
			this.bImage = this.processHeightmap(image, tileHeightCanvas);

			WritableImage image2 = this.generateHeightmap(width, height, frequencyVal, peaksValleysVal, seedVal, xCoordVal, yCoordVal);
			this.processHeightmap(image2, tileHeightCanvas2);

			// Add trees
			this.treeMap = this.generateTreeMap(tileHeightCanvas2, treeFreqVal, seedVal, xCoordVal, yCoordVal);
		});


		primaryStage.sizeToScene();
	}

	public WritableImage generateHeightmap(int width, int height, double frequencyVal, double peaksValleysVal, double seedVal, double xCoordVal, double yCoordVal) {
		WritableImage image = new WritableImage(width, height);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				double nx = ((double) x/width) - 0.5, ny = ((double)y/height) - 0.5;

				double noiseResult = seedVal == 0.0
						? noise((frequencyVal * nx) - xCoordVal, (frequencyVal * ny) - yCoordVal)
						// User has specified a spikes value, include the parameter
						: noise((frequencyVal * nx) - xCoordVal, (frequencyVal * ny) - yCoordVal, seedVal);

				noiseResult = Math.pow(noiseResult, peaksValleysVal);

				// noise() returns values in the range of -1 to 1, but we need between 0.0-1.0
				// Shift all values by 1.0 so they're all between 0.0 and 2.0
				double transformedNoiseResult = noiseResult + 1.0;
				// Calculate the value as a percentage of 2.0 (and transform to decimal)
				double opacity = (transformedNoiseResult/2.0 * 100) / 100;

				Color color = new Color(opacity, opacity, opacity, 1.0);
				image.getPixelWriter().setColor(x, y, color);
			}
		}

		return image;
	}

	public int[][] generateTreeMap(Canvas canvasNode, int R, double seedVal, double xCoordVal, double yCoordVal) {
		int treeFrequency = 50; // Frequency is not density, R number is density
		int width = (int) canvasNode.getWidth() * widthSpinner.getValue(),
				height = (int) canvasNode.getHeight() * lengthSpinner.getValue();
		int[][] result = new int[width][height];

		GraphicsContext gc = canvasNode.getGraphicsContext2D();
		gc.setFill(Color.WHITE);
		double[][] bluenoise = new double[width][height];

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				double nx = ((double) x/width) - 0.5, ny = ((double)y/height) - 0.5;

				double noiseResult = seedVal == 0.0
						? noise((treeFrequency * nx) - xCoordVal, (treeFrequency * ny) - yCoordVal)
						// User has specified a spikes value, include the parameter
						: noise((treeFrequency * nx) - xCoordVal, (treeFrequency * ny) - yCoordVal, seedVal);


				// noise() returns values in the range of -1 to 1, but we need between 0.0-1.0
				// Shift all values by 1.0 so they're all between 0.0 and 2.0
				double transformedNoiseResult = noiseResult + 1.0;
				// Calculate the value as a percentage of 2.0 (and transform to decimal)
				double opacity = (transformedNoiseResult/2.0 * 100) / 100;

				bluenoise[x][y] = opacity;
			}
		}

		for (int yc = 0; yc < height; yc++) {
			for (int xc = 0; xc < width; xc++) {
				double max = 0;
				// there are more efficient algorithms than this
				for (int yn = yc - R; yn <= yc + R; yn++) {
					for (int xn = xc - R; xn <= xc + R; xn++) {
						if (0 <= yn && yn < height && 0 <= xn && xn < width) {
							double e = bluenoise[yn][xn];
							if (e > max) { max = e; }
						}
					}
				}
				if (bluenoise[yc][xc] == max) {
					// Draw dot on map to represent tree
					gc.fillOval(xc, yc, 1, 1);
					// Flag this slot as having a tree
					result[xc][yc] = 1;
				}
			}
		}

		return result;
	}

	public Image scale(Image source, int targetWidth, int targetHeight, boolean preserveRatio) {
		ImageView imageView = new ImageView(source);
		imageView.setPreserveRatio(preserveRatio);
		imageView.setFitWidth(targetWidth);
		imageView.setFitHeight(targetHeight);
		return imageView.snapshot(null, null);
	}

	private BufferedImage processHeightmap(WritableImage image, Canvas canvasNode) {
		//final Bounds bounds = node.getLayoutBounds();

//		final WritableImage image = new WritableImage(
//				(int) Math.round(bounds.getWidth() * scale),
//				(int) Math.round(bounds.getHeight() * scale));

//		final SnapshotParameters spa = new SnapshotParameters();
//		spa.setTransform(javafx.scene.transform.Transform.scale(scale, scale));
//
//		this.finalImage = node.snapshot(spa, image);
//		final ImageView view = new ImageView(this.finalImage);
//		view.setFitWidth(bounds.getWidth());
//		view.setFitHeight(bounds.getHeight());
//
//
//		tileHeightBlurImageView.setImage(this.finalImage);

//		Image originalImage = ((ImageView) node).getImage();


		//tileHeightCanvas.setWidth(widthSpinner.getValue() * 64);
		//tileHeightCanvas.setHeight(lengthSpinner.getValue() * 64);

		GraphicsContext gc = canvasNode.getGraphicsContext2D();
		gc.drawImage(image,0,0, widthSpinner.getValue() * 64, lengthSpinner.getValue() * 64);


		// 1. Convert to BufferedImage so we can read pixels and scale (to blur it)
		BufferedImage buffered = SwingFXUtils.fromFXImage(image, null);

		// Downscale and upscale to blur the image
		java.awt.Image tinyImage = buffered.getScaledInstance(widthSpinner.getValue() * 80,  lengthSpinner.getValue() * 80, SCALE_SMOOTH);

		return toBufferedImage(tinyImage.getScaledInstance(widthSpinner.getValue() * 64, lengthSpinner.getValue() * 64, SCALE_SMOOTH));
		//this.finalImage = canvasNode.snapshot(new SnapshotParameters(), new WritableImage(widthSpinner.getValue() * 64, lengthSpinner.getValue() * 64));
	}

	/**
	 * Converts a given Image into a BufferedImage
	 *
	 * @param img The Image to be converted
	 * @return The converted BufferedImage
	 */
	public static BufferedImage toBufferedImage(java.awt.Image img)
	{
		if (img instanceof BufferedImage)
		{
			return (BufferedImage) img;
		}

		// Create a buffered image with transparency
		BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

		// Draw the image on to the buffered image
		Graphics2D bGr = bimage.createGraphics();
		bGr.drawImage(img, 0, 0, null);
		bGr.dispose();

		// Return the buffered image
		return bimage;
	}

	private void setWaterEdges() {
		//tileHeightImageView.setEffect(null);
		int waterMin = waterDistanceMin.getValue();
		int waterMax = waterDistanceMax.getValue();

		if(waterMin > 0 && waterMax > 0) {
			for(int w = 0;w<blurredImage.getWidth();w++) {
				for(int h = 0;h<blurredImage.getHeight();h++) {
					if(w <= waterMin || h <= waterMin || w >= blurredImage.getWidth() - waterMin || h >= blurredImage.getHeight() - waterMin) {
						blurredImage.getPixelWriter().setColor(w, h, Color.BLACK);
						//heights[w][h] = 0;
					}
				}
			}
			}

		//tileHeightImageView.setImage(blurredImage);
	}


	private void setLinkedMinMax(Spinner<Integer> minSpinner, Spinner<Integer> maxSpinner) {

		minSpinner.setEditable(true);
		maxSpinner.setEditable(true);

		minSpinner.focusedProperty().addListener((observable, oldValue, newValue) -> {
			if (!newValue) {
				minSpinner.increment(0); // won't change value, but will commit editor
			}
		});
		maxSpinner.focusedProperty().addListener((observable, oldValue, newValue) -> {
			if (!newValue) {
				maxSpinner.increment(0); // won't change value, but will commit editor
			}
		});
	/*	minSpinner.getEditor().textProperty().addListener( (observable, oldValue, nv) ->
		{
			// let the user clear the field without complaining
			if(!nv.isEmpty()) {
				Integer newValue = minSpinner.getValue();
				try {
					newValue = minSpinner.getValueFactory().getConverter().fromString(nv);
				} catch (Exception e) {  user typed an illegal character  } 
				minSpinner.getValueFactory().setValue(newValue);
			}
		});
		maxSpinner.getEditor().textProperty().addListener( (observable, oldValue, nv) ->
		{
			// let the user clear the field without complaining
			if(!nv.isEmpty()) {
				Integer newValue = maxSpinner.getValue();
				try {
					newValue = maxSpinner.getValueFactory().getConverter().fromString(nv);
				} catch (Exception e) {  user typed an illegal character  } 
				maxSpinner.getValueFactory().setValue(newValue);
			}
		});*/
		ChangeListenerUtil.addListener(() -> {
			if(minSpinner.getValue() > maxSpinner.getValue()) {
				minSpinner.getValueFactory().setValue(maxSpinner.getValue());
			}
		}, minSpinner.getValueFactory().valueProperty());

		ChangeListenerUtil.addListener(() -> {
			if(maxSpinner.getValue() < minSpinner.getValue()) {
				maxSpinner.getValueFactory().setValue(minSpinner.getValue());
			}
		}, maxSpinner.getValueFactory().valueProperty());
	}

	private void populateBiomeSelection() {
		for (Biome biome : Biome.values()) {
			CheckBox checkBox = new CheckBox(biome.name());
			checkBox.setId("biomeChk_"+biome.name());
			checkBox.setSelected(true);

			this.biomeSelectionBox.getChildren().add(checkBox);
		}
	}

	private Polygon createRegularPolygon() {
		Polygon polygon = new Polygon();  

		//Adding coordinates to the polygon 
		polygon.getPoints().addAll(20.0, 5.0,
				40.0, 5.0,
				45.0, 15.0,
				40.0, 25.0,
				20.0, 25.0,
				15.0, 15.0);
		return polygon;
	}

	private int calculateHeight(int x, int y) {
		int height = interpolatedNoise(x + 45365, y + 0x16713, 4) - 128
				+ (interpolatedNoise(x + 10294, y + 37821, 2) - 128 >> 1) + (interpolatedNoise(x, y, 1) - 128 >> 2);
		height = (int) (height * 0.3D) + 35;
		height *= 8;

		int min = heightsMin.getValue();
		int max = heightsMax.getValue();
		if (height < min) {
			height = min;
		} else if (height > max) {
			height = max;
		}

		return height;
	}

	private int interpolate(int a, int b, int angle, int frequencyReciprocal) {
		int cosine = 0x10000 - Constants.COSINE[angle * 1024 / frequencyReciprocal] >> 1;
		return (a * (0x10000 - cosine) >> 16) + (b * cosine >> 16);
	}

	private int interpolatedNoise(int x, int y, int frequencyReciprocal) {
		int adj_x = x / frequencyReciprocal;
		int i1 = x & frequencyReciprocal - 1;
		int adj_y = y / frequencyReciprocal;
		int k1 = y & frequencyReciprocal - 1;
		int l1 = smoothNoise(adj_x, adj_y);
		int i2 = smoothNoise(adj_x + 1, adj_y);
		int j2 = smoothNoise(adj_x, adj_y + 1);
		int k2 = smoothNoise(adj_x + 1, adj_y + 1);
		int l2 = interpolate(l1, i2, i1, frequencyReciprocal);
		int i3 = interpolate(j2, k2, i1, frequencyReciprocal);
		return interpolate(l2, i3, k1, frequencyReciprocal);
	}

	private int perlinNoise(int x, int y) {
		int n = x + y * 57;
		n = n << 13 ^ n;
		n = n * (n * n * 15731 + 0xc0ae5) + 0x5208dd0d & 0x7fffffff;
		return n >> 19 & 0xff;
	}

	private int smoothNoise(int x, int y) {
		int corners = perlinNoise(x - 1, y - 1) + perlinNoise(x + 1, y - 1) + perlinNoise(x - 1, y + 1)
		+ perlinNoise(x + 1, y + 1);
		int sides = perlinNoise(x - 1, y) + perlinNoise(x + 1, y) + perlinNoise(x, y - 1) + perlinNoise(x, y + 1);
		int center = perlinNoise(x, y);
		return corners / 16 + sides / 8 + center / 4;
	}

	public void show() {
		reset();
		stage.sizeToScene();
		stage.showAndWait();
		if(!okClicked)
			reset();
	}

	public int[][] getHeights() {
		//int[][] heights = new int[(widthSpinner.getValue() * 64) + 1][(lengthSpinner.getValue() * 64) + 1];
		if(heights == null) {
			BufferedImage image = this.bImage;

			if(image != null) {
				// Generating with image
				int max = heightsMax.getValue();
				int min = heightsMin.getValue();

				heights = new int[((widthSpinner.getValue() * 64) + 1)][((lengthSpinner.getValue() * 64) + 1)];
				for(int w = 0;w<image.getWidth();w++) {
					for(int h = 0;h<image.getHeight();h++) {
						int tileColor = this.bImage.getRGB(w, h);
						int generatedHeight = (int) -( min + (((tileColor & 0xff) / 255.0) * (max - min)));

						heights[w][h] = generatedHeight;
					}
				}

			} else {
				// Generating one set height
				heights = new int[(widthSpinner.getValue() * 64) + 1][(lengthSpinner.getValue() * 64) + 1];
				for(int x = 0;x<heights.length;x++)
					Arrays.fill(heights[x], -10);
			}
		}

		return heights;
	}


	public void reset() {
		okClicked = false;
		heights = null;
		//this.tileHeightImageView.setImage(null);
	}



	@FXML
	private Pane overlayPane;

	@FXML
	private Pane underlayPane;

	@FXML
	private TitledPane mapTileHeightBox;

	@FXML
	private Canvas tileHeightCanvas;

	@FXML
	private Canvas tileHeightCanvas2;

	@FXML
	private Button generateBtn;

	@FXML
	private Button browseBtn;

	@FXML
	private Button okButton;

	@FXML
	private Button addConditionBtn;

	@FXML
	private Button cancelButton;

	@FXML
	private Spinner<Integer> widthSpinner;

	@FXML
	private Spinner<Integer> lengthSpinner;

	@FXML
	private Spinner<Integer> waterDistanceMin;

	@FXML
	private Spinner<Integer> waterDistanceMax;

	@FXML
	private Spinner<Integer> heightsMin;

	@FXML
	private Spinner<Integer> heightsMax;

	@FXML
	private Spinner<Double> frequency;

	@FXML
	private Spinner<Double> peaksValleys;

	@FXML
	private TextField seed;

	@FXML
	private TextField treeFreq;

	@FXML
	private Spinner<Double> xCoord;

	@FXML
	private Spinner<Double> yCoord;

	@FXML
	private CheckBox preserveSeed;

	@FXML
	private VBox biomeSelectionBox;

	public int getWidth() {
		// TODO Auto-generated method stub
		return widthSpinner.valueProperty().get();
	}

	public int getLength() {
		// TODO Auto-generated method stub
		return lengthSpinner.valueProperty().get();
	}

	public int getMinHeight() {
		return heightsMin.getValue();
	}

	public int getMaxHeight() {
		return heightsMax.getValue();
	}

	public int[][] getTreeMap() {
		return this.treeMap;
	}
}
