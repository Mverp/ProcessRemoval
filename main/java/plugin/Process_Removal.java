package plugin;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

/**
 *
 * @author Merijn van Erp
 *
 */
public class Process_Removal implements PlugIn
{
	// Process settings
	private static final boolean COMPLETE_THICKNESS = false; // Use the 'complete' option for the local thickness calculation (true) or the 'masked, calibrated, silent' version (false)
	private static final boolean DEBUG = false;

	// Voxel settings. Set these to the original imaging settings.
	private static final int X_SIZE = 20;
	private static final int Y_SIZE = 20;
	private static final int Z_SIZE = 40;
	private static final String UNIT = "um";


	private ImagePlus getLocalThickness(ImagePlus aImage)
	{
		ImagePlus thicknessStack = IJ.createImage("ThicknessStack", "32-bit black", aImage.getWidth(), aImage.getHeight(), aImage.getNSlices());

		for (int slice = 1; slice <= aImage.getNSlices(); slice++)
		{
			aImage.setSlice(slice);
			IJ.run("Duplicate...", "use");
			ImagePlus curSlice = IJ.getImage();

			// Measure the local thickness
			if (COMPLETE_THICKNESS)
			{
				IJ.run("Local Thickness (complete process)", "threshold=1");
			}
			else
			{
				IJ.run("Local Thickness (masked, calibrated, silent)");
			}

			ImagePlus thickSlice = IJ.getImage();
			thicknessStack.setSlice(slice);
			thicknessStack.setProcessor(thickSlice.getProcessor());

			curSlice.changes = false;
			curSlice.close();
			thickSlice.changes = false;
			thickSlice.close();
		}

		return thicknessStack;
	}


	@Override
	public void run(String arg)
	{
		ImagePlus currentImage = IJ.getImage();
		int width = currentImage.getWidth();
		int height = currentImage.getHeight();
		int depth = currentImage.getNSlices();
		String title = currentImage.getTitle();
		int pointIndex = title.lastIndexOf(".");
		if (pointIndex > 0)
		{
			title = title.substring(0, pointIndex);
		}

		// Gather image information
//		getDimensions(xWidth, yHeight, nrChannels, nrSlices, nrFrames)
//		getVoxelSize(xSize, ySize, zSize, vUnit)
//		title = getTitle();
//		pointIndex = lastIndexOf(title, ".");
//		if(pointIndex > 0)
//		{
//			title = substring(title, 0, pointIndex);
//		}

		// Set the correct voxel sizes (as actually imaged)
//		Calibration calib = currentImage.getCalibration();
//		calib.pixelWidth = X_SIZE;
//		calib.pixelHeight = Y_SIZE;
//		calib.pixelDepth = Z_SIZE;
//		calib.setUnit(UNIT);
//		currentImage.setCalibration(calib);
//
//		// Resize so x and y to get the voxel to cube-size
//		IJ.log("Start of rescaling.");
//		double xScaleFactor = (double) X_SIZE / Z_SIZE;
//		double yScaleFactor = (double) Y_SIZE / Z_SIZE;
//		width *= xScaleFactor;
//		height *= yScaleFactor;
//		IJ.run("Scale...", "x=" + xScaleFactor + " y=" + yScaleFactor + " z=1.0 width=" + width + " height=" + height + " depth=" + depth + " interpolation=None average process create");
//		ImagePlus scaledImage = IJ.getImage();
//		scaledImage.setTitle(title + "_Rescaled");

		// Fill any fully enclosed holes in the segments
		IJ.run("Multiply...", "value=255.000 stack");
		IJ.run("Fill Holes", "stack");

		width = currentImage.getWidth();
		height = currentImage.getHeight();

		double[][][] xView = new double[width][height][depth];
		double[][][] yView = new double[width][height][depth];
		double[][][] zView = new double[width][height][depth];

		IJ.log("Start of local thickness + prepro plugin.");

		// Make sure the black background option is set for binary images
		IJ.run("Options...", "black");

		IJ.log("Start of local thickness measurements.");
		ImagePlus thicknessImage = getLocalThickness(currentImage);
		thicknessImage.show();

		for (int z = 0; z < depth; z++)
		{
			thicknessImage.setSlice(z + 1);
			ImageProcessor processor = thicknessImage.getProcessor();
			for (int x = 0; x < width; x++)
			{
				for (int y = 0; y < height; y++)
				{
					zView[x][y][z] = processor.getPixelValue(x, y);
				}
			}
		}

		if (!DEBUG)
		{
			thicknessImage.changes = false;
			thicknessImage.close();
		}

		IJ.selectWindow(currentImage.getID());
		IJ.run("TransformJ Turn", "z-angle=0 y-angle=90 x-angle=0"); // X and Z switch place, x = 0 is last slice, lowest z-slice becomes highest width value
		ImagePlus xRotation = IJ.getImage();
		ImagePlus xThickness = getLocalThickness(xRotation);
		xThickness.show();
		IJ.run("TransformJ Turn", "z-angle=0 y-angle=270 x-angle=0"); // Switch back
		ImagePlus xResult = IJ.getImage();
		xResult.setTitle("X thickness");
		xRotation.changes = false;
		xRotation.close();
		xThickness.changes = false;
		xThickness.close();

		for (int z = 0; z < depth; z++)
		{
			xResult.setSlice(z + 1);
			ImageProcessor processor = xResult.getProcessor();
			for (int x = 0; x < width; x++)
			{
				for (int y = 0; y < height; y++)
				{
					xView[x][y][z] = processor.getPixelValue(x, y);
				}
			}
		}

		if (!DEBUG)
		{
			xResult.changes = false;
			xResult.close();
		}

		IJ.selectWindow(currentImage.getID());
		IJ.run("TransformJ Turn", "z-angle=0 y-angle=0 x-angle=90"); // X and Z switch place, x = 0 is last slice, lowest z-slice becomes highest width value
		ImagePlus yRotation = IJ.getImage();
		ImagePlus yThickness = getLocalThickness(yRotation);
		yThickness.show();
		IJ.run("TransformJ Turn", "z-angle=0 y-angle=0 x-angle=270"); // Switch back
		ImagePlus yResult = IJ.getImage();
		yResult.setTitle("Y thickness");
		yRotation.changes = false;
		yRotation.close();
		yThickness.changes = false;
		yThickness.close();

		for (int z = 0; z < depth; z++)
		{
			yResult.setSlice(z + 1);
			ImageProcessor processor = yResult.getProcessor();
			for (int x = 0; x < width; x++)
			{
				for (int y = 0; y < height; y++)
				{
					yView[x][y][z] = processor.getPixelValue(x, y);
				}
			}
		}

		if (!DEBUG)
		{
			yResult.changes = false;
			yResult.close();
		}

		ImagePlus thicknessStack = IJ.createImage("Thickness Total", "32-bit black", width, height, depth);
		for (int z = 0; z < depth; z++)
		{
			thicknessStack.setSlice(z);
			ImageProcessor proc = thicknessStack.getProcessor();
			for (int x = 0; x < width; x++)
			{
				for (int y = 0; y < height; y++)
				{
					double xValue = xView[x][y][z];
					double yValue = yView[x][y][z];
					double zValue = zView[x][y][z];
					proc.putPixelValue(x, y, Math.max(Math.max(yValue, zValue), xValue));
//					if (xValue >= yValue && xValue >= zValue)
//					{
//						proc.putPixelValue(x, y, Math.max(yValue, zValue));
//					}
//					else if (yValue >= xValue && yValue >= zValue)
//					{
//						proc.putPixelValue(x, y, Math.max(xValue, zValue));
//					}
//					else
//					{
//						proc.putPixelValue(x, y, Math.max(yValue, xValue));
//					}
				}
			}
		}
		thicknessStack.show();
		thicknessStack.updateAndDraw();

		// Clean up
//		scaledImage.changes = false;
//		scaledImage.close();

		IJ.log("End of local thickness + prepro plugin.");
	}

}