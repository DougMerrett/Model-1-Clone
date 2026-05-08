// Copyright 2021, Douglas Merrett
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
import java.io.FileNotFoundException;
import java.util.Scanner;

//
// Font Test Program to see if the bitmap I have created is right and that
// it will look OK in 800x600.  This is using the "lowercase kit" characters
// from the TRS-80 Model I in a 12x36 font bitmap unmodified and as a
// HiRes version...
//

public class fontTest12x36
{
	// Simple main block calls the two classes below
    public static void main (final String[] args)
    {
        BufferedImage screenImage = fontScreen ("font12x36_HiRes.txt");
        saveBMP (screenImage, "fontScreen800x600_12x36_HiRes.bmp");
        screenImage = fontScreen ("font12x36_Standard.txt");
        saveBMP (screenImage, "fontScreen800x600_12x36_Standard.bmp");
    }

	//
	// Create the font screen
	//
	// The font is 12x36 as per the TRS-80 Model I
	// I am including all 256 characters in the ROM including the "graphics" characters
	// to alleviate the issues with trying to emulate the graphics processing in the
	// original machine since we have a stack more storage in the character generator ROM.
	//
	// Since the screen in 64x16 and we need to fit it into the VGA standard of 800x600,
	// the characters need to be "stretched" to twice as wide to fit neatly onto the screen.
	//    64 chars x 6 pixels per char x 2 = 768
	//
	// Similarly, to get the right vertical size, the 16 lines of 12 pixels need to be
	// stretched three times.
	//    16 lines x 12 pixels per char x 3 = 576
	//

    private static BufferedImage fontScreen (final String fileName)
    {
		BufferedImage fs;
		int xOffset;						// Offset from left edge
		int yOffset;						// Offset from top edge

		fs = new BufferedImage (800, 600, BufferedImage.TYPE_INT_RGB);
		xOffset = 16;
		yOffset = 12;

		final int borderRGB = Color.BLUE.getRGB ();	// Border will be blue to differentiate
		final int pixelRGB = Color.GREEN.getRGB ();	// Green as is my TRS-80 Monitor

		// The character font array [ASCII, X, Y]
		final boolean [][][] fontBitMap = new boolean [256][12][36];

		// counters
		int charNum = 0;	// Character within the 256
		int colNum  = 0;	// Col within Screen (0-63)
		int rowNum  = 0;	// Row within Screen (0-15)
		int charCol = 0;	// Col within Character (0-11)
		int charRow = 0;	// Row within Character (0-35)
		int pixelX = 0;		// Screen pixel location on X axis
		int pixelY = 0;		// Screen pixel location on Y axis
		int lineNum = 0;	// Line number of file for error message

		// Fill the character font bitmap
		try
		{
			// Open the font file

			// The format is 12 characters of 0 or 1 to set the
			// boolean in the appropriate spot within the bitmap
			// with / as the first character meaning a comment,
			// so ignore the line

			File fontFile = new File (fileName);
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
				for (charCol = 0; charCol < 12; charCol ++)
				{
					char bitChar = fontLine.charAt (charCol);
					fontBitMap [charNum] [charCol] [charRow] = (bitChar == '1');
				}

				// We have finished that row, so get the next one
				// and increment the character pointer if we have
				// completed the 36 rows of this character
				charRow++;
				if (charRow >= 36)
				{
					charRow = 0;
					charNum++;
				}
			}
			myReader.close ();
		}
		catch (FileNotFoundException e)
		{
			System.out.println ("An error occurred during font file read at line " + lineNum);
			e.printStackTrace ();
			System.exit (1);
    	}

		//
		// Now we have the font bitmap in the array, time to build the screen
		//

		// Draw left/right and top/bottom borders (if needed)
		for (int x = 0; x < xOffset; x++)
		{
			for (int y = 0; y < 600; y++)
			{
				fs.setRGB (x, y, borderRGB);
				fs.setRGB (799 - x, y, borderRGB);
			}
		}

		for (int y = 0; y < yOffset; y++)
		{
			for (int x = 0; x < 800; x++)
			{
				fs.setRGB (x, y, borderRGB);
				fs.setRGB (x, 599 - y, borderRGB);
			}
		}

		// Fill the middle with text
		// Do the loops in font pixels and then do the 2x and 3x expansion in the loop

		for (pixelY = 0; pixelY < 576; pixelY++)
		{
			for (pixelX = 0; pixelX < 768; pixelX++)
			{
				// Screen coordinates in characters
				rowNum = pixelY / 36;
				colNum = pixelX / 12;

				// Get the font character code for this spot
				//
				// Loop through all the fonts in 4 rows
				// (64 chars x 4 rows = 256 chars in total)
				// Use a modulo calculation to wrap around
				// at 256 so we restart at character 0
				//
				charNum = (rowNum * 64 + colNum) % 256;

				// Calculate the Char col and row
				//
				// Which Character on the screen is this pixel within?
				//
				charCol = pixelX % 12;
				charRow = pixelY % 36;

				// If a dot, then draw it
				// Rememeber, the array holds a boolean to make the 'if' tidier
				//
				if (fontBitMap [charNum] [charCol] [charRow])
				{
					// Draw the Pixel
					fs.setRGB (xOffset + pixelX, yOffset + pixelY, pixelRGB);
			    }
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
			System.out.println ("An error occurred during saveBMP");
			e.printStackTrace ();
			System.exit (1);
        }
    }
}
