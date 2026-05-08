// Copyright 2021, Doug Merrett
//
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// - Redistributions of source code must retain the above copyright notice,
//   this list of conditions and the following disclaimer.
// - Redistributions in binary form must reproduce the above copyright notice,
//   this list of conditions and the following disclaimer in the documentation
//   and/or other materials provided with the distribution.
// - My name may not be used to endorse or promote products derived from this
//   software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;
//import java.util.HashMap;
//import java.util.Map;
import java.util.Properties;


//
// Font Diagram Program to see if the bitmap I have created is right
//

public class fontDiagram
{
	// Font size in file
	static int fontWidth = 0;
	static int fontHeight = 0;
	static String fontFilename = "";
	static int xOffset = 40;	// Offset from left edge
	static int yOffset = 40;	// Offset from top edge
	static int borderWidth = 2;	// Number of dots per border
	static int pixelSize = 4;	// Pixel size in dots
	static int spacerWidth = 8;	// Gap between characters
	static int numCols = 32;
	static int numRows = 8;

	static int pixelRGB = Color.BLACK.getRGB ();		// Pixel colour
	static int borderRGB = Color.BLACK.getRGB ();		// Border colour
	static int backgroundRGB = Color.WHITE.getRGB ();	// Background for the image

	// Simple main block calls the two classes below
    public static void main (final String[] args)
    {
		Properties appSettings = new Properties ();
        try
        {
			appSettings.load (new FileInputStream ("fontDiagram.properties"));
		}
		catch (Exception e)
		{
			System.err.println ("An error occurred loading properties");
			e.printStackTrace ();
			System.exit (1);
		}

        fontFilename = appSettings.getProperty ("fontWidth") + "x" + appSettings.getProperty ("fontHeight") + "_";
        fontWidth = Integer.parseInt (appSettings.getProperty ("fontWidth"));
        fontHeight = Integer.parseInt (appSettings.getProperty ("fontHeight"));
        pixelRGB = Integer.parseInt (appSettings.getProperty ("pixelRGB"));
		borderRGB = Integer.parseInt (appSettings.getProperty ("borderRGB"));
		backgroundRGB = Integer.parseInt (appSettings.getProperty ("backgroundRGB"));

		xOffset = Integer.parseInt (appSettings.getProperty ("xOffset"));			// Offset from left edge
		yOffset = Integer.parseInt (appSettings.getProperty ("yOffset"));			// Offset from top edge
		borderWidth = Integer.parseInt (appSettings.getProperty ("borderWidth"));	// Number of dots per border
		pixelSize = Integer.parseInt (appSettings.getProperty ("pixelSize"));		// Pixel size in dots
		spacerWidth = Integer.parseInt (appSettings.getProperty ("spacerWidth"));	// Gap between characters
		numCols = Integer.parseInt (appSettings.getProperty ("numCols"));			// Characters across
		numRows = Integer.parseInt (appSettings.getProperty ("numRows"));			// Lines down

        BufferedImage screenImage = fontScreen ("font" + fontFilename + "HiRes.txt");
        saveBMP (screenImage, "fontDiagram" + fontFilename + "HiRes.bmp");
        screenImage = fontScreen ("font" + fontFilename + "Standard.txt");
        saveBMP (screenImage, "fontDiagram" + fontFilename + "Standard.bmp");
    }

	//
	// Create the font screen
	//
	// I am including all 256 characters in the ROM including the "graphics" characters
	// to alleviate the issues with trying to emulate the graphics processing in the
	// original machine since we have a stack more storage in the character generator ROM.
	//

    private static BufferedImage fontScreen (String filename)
    {
		// Calculate the Image size
		final int charWidth = (borderWidth + pixelSize) * fontWidth + borderWidth;		// Left border of each pixel + pixel then add the right border
		final int charHeight = (borderWidth + pixelSize) * fontHeight + borderWidth;	// Same calc for height
		final int imageWidth = xOffset + numCols * (charWidth + spacerWidth);  			// Leave a gap on the right and bottom
		final int imageHeight = yOffset + numRows * (charHeight + spacerWidth);
		final BufferedImage fs = new BufferedImage (imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);

		// Fill the background
		for (int x = 0; x < imageWidth; x++)
		{
			for (int y = 0; y < imageHeight; y++)
			{
				fs.setRGB (x, y, backgroundRGB);
			}
		}

		// The character font array [ASCII, X, Y]
		final boolean [][][] xFontBitMap;

		// allocates memory for the full character set bitmap
        xFontBitMap = new boolean [256][fontWidth][fontHeight];

		// counters
		int charNum = 0;	// Character within the 256
		int screenX  = 0;	// Col within Screen (0-31)
		int screenY  = 0;	// Row within Screen (0-7)
		int charX = 0;		// Col within Character (0-fontWidth)
		int charY = 0;		// Row within Character (0-11)
		int pixelX = 0;		// Screen pixel location on X axis
		int pixelY = 0;		// Screen pixel location on Y axis
		int topLeftX;		// Top Left Coordinate of the character
		int topLeftY;		// Top Left Coordinate of the character
		int lineNum = 0;	// Line number of file for error message

		// Fill the character font bitmap
		try
		{
			// Open the font file

			// The format is fontWidth characters of 0 or 1 to set the
			// boolean in the appropriate spot within the bitmap
			// with / as the first character meaning a comment,
			// so ignore the line

			File fontFile = new File (filename);
			Scanner myReader = new Scanner (fontFile);

			while (myReader.hasNextLine ())
			{
				// Fetch the line of "bits"
				String fontLine = myReader.nextLine ();
				lineNum++;

				// Ignore comments
				if (fontLine.charAt (0) == '/')
				{
					continue;
				}

				// Convert from characters into the binary array
				for (charX = 0; charX < fontWidth; charX ++)
				{
					char xBit = fontLine.charAt (charX);
					xFontBitMap [charNum] [charX] [charY] = (xBit == '1');
				}

				// We have finished that row, so get the next one
				// and increment the character pointer if we have
				// completed the fontHeight rows of this character
				charY++;
				if (charY >= fontHeight)
				{
					charY = 0;
					charNum++;
				}
			}
			myReader.close ();
		}
		catch (FileNotFoundException e)
		{
			System.err.println ("An error occurred during font file read at line " + lineNum);
			e.printStackTrace ();
			System.exit (1);
    	}

		//
		// Now we have the font bitmap in the array, time to build the screen
		//
		// Do the loops in characters

		charNum = 0;  // Start from the beginning

		for (screenY = 0; screenY < numRows; screenY++)
		{
			// Calculate the top left coordinate of the character box
			topLeftY = yOffset + screenY * (charHeight + spacerWidth);

			for (screenX = 0; screenX < numCols; screenX++)
			{
				// Calculate the top left coordinate of the character box
				topLeftX = xOffset + screenX * (charWidth + spacerWidth);

				// Draw the horizontal lines for the borders surrounding the pixels
				for (int x = 0; x < charWidth; x++)
				{
					// Do 13 because the bottom of the fontHeightth real pixel is the top of the non-existant 13th pixel
					for (int numPixels = 0; numPixels <= fontHeight; numPixels++)
					{
						for (int bw = 0; bw < borderWidth; bw++)
						{
							fs.setRGB (topLeftX + x, topLeftY + numPixels * (pixelSize + borderWidth) + bw, borderRGB);
						}
					}
				}

				// Draw the vertical lines for the borders surrounding the pixels
				for (int y = 0; y < charHeight; y++)
				{
					// Do 7 because the right of the fontWidthth real pixel is the left of the non-existant 7th pixel
					for (int numPixels = 0; numPixels <= fontWidth; numPixels++)
					{
						for (int bw = 0; bw < borderWidth; bw++)
						{
							fs.setRGB (topLeftX + numPixels * (pixelSize + borderWidth) + bw, topLeftY + y, borderRGB);
						}
					}
				}

				// Draw the pixels for this character
				for (int y = 0; y < fontHeight; y++)
				{
					for (int x = 0; x < fontWidth; x++)
					{
						// If a dot, then draw it
						// Rememeber, the array holds a boolean to make the 'if' tidier
						//
						if (xFontBitMap [charNum] [x] [y])
						{
							for (int px = 0; px < pixelSize; px++)
							{
								for (int py = 0; py < pixelSize; py++)
								{
									fs.setRGB (topLeftX + borderWidth + ((pixelSize + borderWidth) * x) + px, topLeftY + borderWidth + ((pixelSize + borderWidth) * y) + py, pixelRGB);
								}
							}
						}
					}
				}

				// The next character
				charNum++;
			}
		}
        return fs;
    }

    private static void saveBMP (final BufferedImage bi, final String path)
    {
        try
        {
            RenderedImage rendImage = bi;
            ImageIO.write (rendImage, "bmp", new File (path));
        }
        catch (IOException e)
        {
			System.err.println ("An error occurred during saveBMP");
			e.printStackTrace ();
			System.exit (1);
        }
    }
}